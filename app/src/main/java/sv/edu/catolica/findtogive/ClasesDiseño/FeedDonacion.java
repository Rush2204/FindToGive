package sv.edu.catolica.findtogive.ClasesDiseño;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.ApiService;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.FiltroBusquedaDialog;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.AppNotificationManager;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.NotificationPermissionManager;
import sv.edu.catolica.findtogive.Modelado.SolicitudDonacion;
import sv.edu.catolica.findtogive.Modelado.Usuario;
import sv.edu.catolica.findtogive.R;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.SharedPreferencesManager;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.SolicitudesAdapter;

public class FeedDonacion extends AppCompatActivity implements FiltroBusquedaDialog.FiltroBusquedaListener {

    private RecyclerView rvSolicitudes;
    private LinearLayout layoutEmptyState;
    private Button btnCreateFirstRequest;
    private ImageButton btnFilterSearch;
    private TextView textTitle;
    private BottomNavigationView bottomNavigation;

    private List<SolicitudDonacion> solicitudesList;
    private SolicitudesAdapter adapter;
    private Usuario usuarioActual;

    private Handler autoRefreshHandler;
    private Runnable autoRefreshRunnable;
    private static final long AUTO_REFRESH_INTERVAL = 500; // 0.5 segundos

    // Variables para filtros
    private String currentQuery = "";
    private int currentTipoSangreId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.desing_feed_donacion);

        if (AppNotificationManager.areNotificationsEnabled(this)) {
            AppNotificationManager.startNotificationService(this);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Verificar permisos y solicitar si es necesario
        if (!NotificationPermissionManager.areNotificationsEnabled(this)) {
            NotificationPermissionManager.requestNotificationPermission(this);
        } else {
            // Solo iniciar servicio si tiene permisos
            if (AppNotificationManager.areNotificationsEnabled(this)) {
                AppNotificationManager.startNotificationService(this);
            }
        }

        usuarioActual = SharedPreferencesManager.getCurrentUser(this);
        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        setupBottomNavigation();
        configurarVistaSegunRol();
        loadSolicitudes();

        // Iniciar actualización automática AGRESIVA
        startAggressiveAutoRefresh();
    }

    private void initializeViews() {
        rvSolicitudes = findViewById(R.id.rv_solicitudes);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        btnCreateFirstRequest = findViewById(R.id.btn_create_first_request);
        btnFilterSearch = findViewById(R.id.btn_filter_search);
        textTitle = findViewById(R.id.text_title);
        bottomNavigation = findViewById(R.id.bottom_navigation_bar);

        solicitudesList = new ArrayList<>();
    }

    private void configurarVistaSegunRol() {
        if (usuarioActual == null) return;

        actualizarNavegacionSegunRol();

        // Obtener referencias a los textos del empty state
        TextView emptyStateTitle = null;
        TextView emptyStateMessage = null;

        // Buscar los TextViews en el layout empty state
        for (int i = 0; i < layoutEmptyState.getChildCount(); i++) {
            View child = layoutEmptyState.getChildAt(i);
            if (child instanceof TextView) {
                TextView textView = (TextView) child;
                if (emptyStateTitle == null) {
                    emptyStateTitle = textView;
                } else if (emptyStateMessage == null) {
                    emptyStateMessage = textView;
                }
            }
        }

        // Configurar según el rol
        if (usuarioActual.getRolid() == 1) { // DONANTE
            // Ocultar botón de crear solicitud
            btnCreateFirstRequest.setVisibility(View.GONE);

            // Modificar mensajes del empty state
            if (emptyStateTitle != null) {
                emptyStateTitle.setText("No hay solicitudes activas");
            }
            if (emptyStateMessage != null) {
                emptyStateMessage.setText("Actualmente no hay solicitudes de donación en tu área. Los receptores crearán nuevas solicitudes pronto.");
            }
        } else { // RECEPTOR (2) o AMBOS (3)
            // Mostrar botón de crear solicitud
            btnCreateFirstRequest.setVisibility(View.VISIBLE);

            // Mensajes normales para receptores
            if (emptyStateTitle != null) {
                emptyStateTitle.setText("¡No hay solicitudes activas por ahora!");
            }
            if (emptyStateMessage != null) {
                emptyStateMessage.setText("Parece que todos están cubiertos. ¡Vuelve pronto o crea una si lo necesitas!");
            }
        }
    }

    // ⭐⭐ NUEVO MÉTODO: Actualizar navegación según rol ⭐⭐
    private void actualizarNavegacionSegunRol() {
        if (usuarioActual != null) {
            boolean esDonante = usuarioActual.getRolid() == 1;
            bottomNavigation.getMenu().findItem(R.id.nav_crear).setVisible(!esDonante);
        }
    }

    private void setupRecyclerView() {
        adapter = new SolicitudesAdapter(solicitudesList, this);
        rvSolicitudes.setLayoutManager(new LinearLayoutManager(this));
        rvSolicitudes.setAdapter(adapter);

        // FORZAR la medición y layout inmediatamente
        rvSolicitudes.post(new Runnable() {
            @Override
            public void run() {
                System.out.println("🎯 FORZANDO PRIMERA ACTUALIZACIÓN DEL RECYCLERVIEW EN FEED");
                adapter.notifyDataSetChanged();
                forceImmediateRedraw();
            }
        });
    }

    private void setupClickListeners() {
        btnCreateFirstRequest.setOnClickListener(v -> {
            // Verificar rol antes de navegar
            if (usuarioActual != null && (usuarioActual.getRolid() == 2 || usuarioActual.getRolid() == 3)) {
                Intent intent = new Intent(this, SolicitudDonacionC.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Solo receptores pueden crear solicitudes", Toast.LENGTH_SHORT).show();
            }
        });

        btnFilterSearch.setOnClickListener(v -> {
            mostrarDialogoFiltroBusqueda();
        });
    }

    private void mostrarDialogoFiltroBusqueda() {
        FiltroBusquedaDialog dialog = new FiltroBusquedaDialog(this, this);
        dialog.setFiltrosActuales(currentQuery, currentTipoSangreId);
        dialog.show();
    }

    private void setupBottomNavigation() {
        actualizarNavegacionSegunRol();
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_inicio) {
                return true;
            } else if (itemId == R.id.nav_crear) {
                if (usuarioActual != null && (usuarioActual.getRolid() == 2 || usuarioActual.getRolid() == 3)) {
                    Intent intent = new Intent(this, SolicitudDonacionC.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Solo receptores pueden crear solicitudes", Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (itemId == R.id.nav_notificaciones) {
                Intent intent = new Intent(this, Notificaciones.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_historial) {
                Intent intent = new Intent(this, HistorialDonaciones.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_perfil) {
                Intent intent = new Intent(this, PerfilUsuario.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            }
            return false;
        });

        if (usuarioActual != null && usuarioActual.getRolid() == 1) {
            bottomNavigation.getMenu().findItem(R.id.nav_crear).setVisible(false);
        }

        bottomNavigation.setSelectedItemId(R.id.nav_inicio);
    }

    // Agregar manejo de resultado de permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (NotificationPermissionManager.handlePermissionResult(requestCode, grantResults)) {
            // Permiso concedido, iniciar servicio
            if (AppNotificationManager.areNotificationsEnabled(this)) {
                AppNotificationManager.startNotificationService(this);
            }
            Toast.makeText(this, "Notificaciones activadas", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Las notificaciones estarán desactivadas", Toast.LENGTH_LONG).show();
        }
    }

    private void loadSolicitudes() {
        // Si hay filtros activos, usar búsqueda filtrada
        if (!currentQuery.isEmpty() || currentTipoSangreId != -1) {
            cargarSolicitudesConFiltros();
        } else {
            Log.d("FeedDonacion", "🔄 Cargando todas las solicitudes...");

            ApiService.getSolicitudesActivas(new ApiService.ListCallback<SolicitudDonacion>() {
                @Override
                public void onSuccess(List<SolicitudDonacion> solicitudes) {
                    Log.d("FeedDonacion", "✅ Solicitudes cargadas: " + (solicitudes != null ? solicitudes.size() : 0));

                    runOnUiThread(() -> {
                        if (solicitudes != null && !solicitudes.isEmpty()) {
                            // Log detallado
                            for (SolicitudDonacion solicitud : solicitudes) {
                                Log.d("FeedDonacion",
                                        "📋 ID: " + solicitud.getSolicitudid() +
                                                " | UsuarioID: " + solicitud.getUsuarioid() +
                                                " | Título: " + solicitud.getTitulo() +
                                                " | Imagen: " + (solicitud.getImagenUrl() != null ? "SI" : "NO"));
                            }

                            adapter.updateData(solicitudes);
                            showSolicitudesList();

                            // FORZAR ACTUALIZACIÓN INMEDIATA después de cargar datos
                            forceImmediateRedraw();

                            // Forzar otra actualización después de 1 segundo para asegurar
                            new Handler().postDelayed(() -> {
                                forceImmediateRedraw();
                            }, 1000);

                        } else {
                            Log.d("FeedDonacion", "📭 No hay solicitudes activas");
                            showEmptyState();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    Log.e("FeedDonacion", "❌ Error cargando solicitudes: " + error);
                    runOnUiThread(() -> showEmptyState());
                }
            });
        }
    }

    private void cargarSolicitudesConFiltros() {
        Log.d("FeedDonacion", "🔍 Cargando solicitudes con filtros - Query: '" + currentQuery + "', TipoSangre: " + currentTipoSangreId);

        showLoadingState();

        ApiService.buscarSolicitudes(currentQuery,
                currentTipoSangreId != -1 ? currentTipoSangreId : null,
                new ApiService.ListCallback<SolicitudDonacion>() {
                    @Override
                    public void onSuccess(List<SolicitudDonacion> solicitudes) {
                        Log.d("FeedDonacion", "✅ Solicitudes filtradas cargadas: " + (solicitudes != null ? solicitudes.size() : 0));

                        runOnUiThread(() -> {
                            if (solicitudes != null && !solicitudes.isEmpty()) {
                                adapter.updateData(solicitudes);
                                showSolicitudesList();
                                forceImmediateRedraw();

                                // Mostrar mensaje de resultados
                                mostrarMensajeResultados(solicitudes.size());
                            } else {
                                Log.d("FeedDonacion", "📭 No hay solicitudes que coincidan con los filtros");
                                showEmptyState();
                                personalizarMensajeEmptyStateConFiltros();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("FeedDonacion", "❌ Error cargando solicitudes filtradas: " + error);
                        runOnUiThread(() -> {
                            showEmptyState();
                            Toast.makeText(FeedDonacion.this, "Error al buscar solicitudes", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void mostrarMensajeResultados(int cantidad) {
        StringBuilder mensaje = new StringBuilder("Se encontraron " + cantidad + " solicitudes");

        if (!currentQuery.isEmpty()) {
            mensaje.append(" con '").append(currentQuery).append("'");
        }

        if (currentTipoSangreId != -1) {
            if (!currentQuery.isEmpty()) {
                mensaje.append(" y");
            }
            mensaje.append(" de tipo ").append(convertirTipoSangreIdANombre(currentTipoSangreId));
        }

        Toast.makeText(this, mensaje.toString(), Toast.LENGTH_SHORT).show();
    }

    private void personalizarMensajeEmptyStateConFiltros() {
        TextView emptyStateTitle = findViewById(R.id.text_empty_title);
        TextView emptyStateMessage = findViewById(R.id.text_empty_message);

        if (emptyStateTitle != null && emptyStateMessage != null) {
            emptyStateTitle.setText("No se encontraron resultados");

            StringBuilder mensaje = new StringBuilder("No hay solicitudes que coincidan con ");

            if (!currentQuery.isEmpty() && currentTipoSangreId != -1) {
                mensaje.append("'").append(currentQuery).append("' y tipo de sangre ").append(convertirTipoSangreIdANombre(currentTipoSangreId));
            } else if (!currentQuery.isEmpty()) {
                mensaje.append("'").append(currentQuery).append("'");
            } else if (currentTipoSangreId != -1) {
                mensaje.append("el tipo de sangre ").append(convertirTipoSangreIdANombre(currentTipoSangreId));
            }

            emptyStateMessage.setText(mensaje.toString());
        }
    }

    private String convertirTipoSangreIdANombre(int tipoSangreId) {
        switch (tipoSangreId) {
            case 1: return "A+";
            case 2: return "A-";
            case 3: return "B+";
            case 4: return "B-";
            case 5: return "AB+";
            case 6: return "AB-";
            case 7: return "O+";
            case 8: return "O-";
            default: return "Desconocido";
        }
    }

    // ========== IMPLEMENTACIÓN DE FILTROBUSQUEDALISTENER ==========

    @Override
    public void onAplicarFiltros(String query, int tipoSangreId) {
        currentQuery = query;
        currentTipoSangreId = tipoSangreId;

        // Mostrar indicador de filtros activos
        mostrarIndicadorFiltros();

        // Cargar solicitudes con filtros
        cargarSolicitudesConFiltros();
    }

    @Override
    public void onLimpiarFiltros() {
        currentQuery = "";
        currentTipoSangreId = -1;

        // Ocultar indicador de filtros
        ocultarIndicadorFiltros();

        // Cargar todas las solicitudes
        loadSolicitudes();

        Toast.makeText(this, "Filtros limpiados", Toast.LENGTH_SHORT).show();
    }

    private void mostrarIndicadorFiltros() {
        StringBuilder filtros = new StringBuilder("Filtros activos: ");
        boolean tieneFiltros = false;

        if (!currentQuery.isEmpty()) {
            filtros.append("'").append(currentQuery).append("'");
            tieneFiltros = true;
        }

        if (currentTipoSangreId != -1) {
            if (tieneFiltros) filtros.append(", ");
            filtros.append(convertirTipoSangreIdANombre(currentTipoSangreId));
            tieneFiltros = true;
        }

        if (tieneFiltros) {
            // Cambiar el título para mostrar filtros activos
            textTitle.setText("Solicitudes Filtradas");
            textTitle.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    private void ocultarIndicadorFiltros() {
        textTitle.setText("Solicitudes de Donación");
        textTitle.setTextColor(getResources().getColor(android.R.color.black));
    }

    private void startAggressiveAutoRefresh() {
        autoRefreshHandler = new Handler();
        autoRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                System.out.println("💥 FEED: ACTUALIZACIÓN AUTOMÁTICA FORZADA");
                forceImmediateRedraw();
                autoRefreshHandler.postDelayed(this, AUTO_REFRESH_INTERVAL);
            }
        };
        // Iniciar inmediatamente y repetir cada 0.5 segundos
        autoRefreshHandler.post(autoRefreshRunnable);
    }

    private void forceImmediateRedraw() {
        if (adapter != null) {
            // Método 1: Notificar cambio completo
            adapter.notifyDataSetChanged();

            // Método 2: Invalidar el RecyclerView
            rvSolicitudes.invalidate();

            // Método 3: Forzar re-draw
            rvSolicitudes.post(new Runnable() {
                @Override
                public void run() {
                    rvSolicitudes.requestLayout();
                }
            });

            System.out.println("🎯 FEED: Vistas forzadas a redibujarse");
        }
    }

    private void stopAutoRefresh() {
        if (autoRefreshHandler != null && autoRefreshRunnable != null) {
            autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
            System.out.println("⏹️ FEED: Auto-refresh detenido");
        }
    }

    private void showLoadingState() {
        rvSolicitudes.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.GONE);
    }

    private void showSolicitudesList() {
        rvSolicitudes.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        rvSolicitudes.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        System.out.println("🔄 FeedDonacion onResume - Activando modo agresivo");

        usuarioActual = SharedPreferencesManager.getCurrentUser(this);
        actualizarNavegacionSegunRol();

        // Recargar solicitudes cuando la actividad se reanude
        loadSolicitudes();

        // Reactivar actualización automática
        startAggressiveAutoRefresh();

        // Asegurar que la navegación muestre el ítem correcto
        bottomNavigation.setSelectedItemId(R.id.nav_inicio);

        // Forzar una actualización extra después de 2 segundos
        new Handler().postDelayed(() -> {
            forceImmediateRedraw();
        }, 2000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        System.out.println("⏸️ FeedDonacion onPause");
        stopAutoRefresh();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.out.println("🗑️ FeedDonacion onDestroy");
        stopAutoRefresh();
    }

    @Override
    protected void onStop() {
        super.onStop();
        System.out.println("🛑 FeedDonacion onStop");
        stopAutoRefresh();
    }
}