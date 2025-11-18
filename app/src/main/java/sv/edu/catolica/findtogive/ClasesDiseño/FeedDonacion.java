package sv.edu.catolica.findtogive.ClasesDiseño;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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

import android.content.Context;

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
    private static final long AUTO_REFRESH_INTERVAL = 500;

    // Variables para filtros
    private String currentQuery = "";
    private int currentTipoSangreId = -1;

    private ImageButton btnFilterLocation;
    private boolean filtroUbicacionActivo = false;
    private static final int PERMISSION_REQUEST_LOCATION = 1001;

    /**
     * Método principal que inicializa la actividad del feed de donaciones
     * Configura la vista, permisos, navegación y carga las solicitudes
     */
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

        if (!NotificationPermissionManager.areNotificationsEnabled(this)) {
            NotificationPermissionManager.requestNotificationPermission(this);
        } else {
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

        startAggressiveAutoRefresh();
    }

    /**
     * Inicializa todos los componentes visuales de la interfaz
     * Obtiene referencias a los views del layout
     */
    private void initializeViews() {
        rvSolicitudes = findViewById(R.id.rv_solicitudes);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        btnCreateFirstRequest = findViewById(R.id.btn_create_first_request);
        btnFilterSearch = findViewById(R.id.btn_filter_search);
        btnFilterLocation = findViewById(R.id.btn_filter_location);
        textTitle = findViewById(R.id.text_title);
        bottomNavigation = findViewById(R.id.bottom_navigation_bar);

        solicitudesList = new ArrayList<>();
    }

    /**
     * Configura la vista según el rol del usuario (donante o receptor)
     * Modifica mensajes y visibilidad de elementos según el tipo de usuario
     */
    @SuppressLint("SetTextI18n")
    private void configurarVistaSegunRol() {
        if (usuarioActual == null) return;

        actualizarNavegacionSegunRol();

        TextView emptyStateTitle = null;
        TextView emptyStateMessage = null;

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

        if (usuarioActual.getRolid() == 1) {
            btnCreateFirstRequest.setVisibility(View.GONE);

            if (emptyStateTitle != null) {
                emptyStateTitle.setText(R.string.no_hay_solicitudes_activas);
            }
            if (emptyStateMessage != null) {
                emptyStateMessage.setText(R.string.sin_solicitudes_en_el_area);
            }
        } else {
            btnCreateFirstRequest.setVisibility(View.VISIBLE);

            if (emptyStateTitle != null) {
                emptyStateTitle.setText(R.string.sin_solicitudes);
            }
            if (emptyStateMessage != null) {
                emptyStateMessage.setText(R.string.vuelve_pronto);
            }
        }
    }

    /**
     * Actualiza la navegación inferior según el rol del usuario
     * Oculta el ítem de creación para donantes
     */
    private void actualizarNavegacionSegunRol() {
        if (usuarioActual != null) {
            boolean esDonante = usuarioActual.getRolid() == 1;
            bottomNavigation.getMenu().findItem(R.id.nav_crear).setVisible(!esDonante);
        }
    }

    /**
     * Configura el RecyclerView para mostrar la lista de solicitudes
     * Inicializa el adapter y establece el layout manager
     */
    private void setupRecyclerView() {
        adapter = new SolicitudesAdapter(solicitudesList, this);
        rvSolicitudes.setLayoutManager(new LinearLayoutManager(this));
        rvSolicitudes.setAdapter(adapter);

        rvSolicitudes.post(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
                forceImmediateRedraw();
            }
        });
    }

    /**
     * Configura los listeners de clic para los botones
     * Asigna las acciones a realizar cuando se presionan los botones
     */
    private void setupClickListeners() {
        btnCreateFirstRequest.setOnClickListener(v -> {
            if (usuarioActual != null && (usuarioActual.getRolid() == 2 || usuarioActual.getRolid() == 3)) {
                Intent intent = new Intent(this, SolicitudDonacionC.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, R.string.solo_receptores_pueden_crear_solicitudes, Toast.LENGTH_SHORT).show();
            }
        });

        btnFilterSearch.setOnClickListener(v -> {
            mostrarDialogoFiltroBusqueda();
        });

        btnFilterLocation.setOnClickListener(v -> {
            if (filtroUbicacionActivo) {
                desactivarFiltroUbicacion();
            } else {
                activarFiltroUbicacion();
            }
        });
    }

    /**
     * Muestra el diálogo de filtro de búsqueda
     * Permite al usuario aplicar filtros por texto y tipo de sangre
     */
    private void mostrarDialogoFiltroBusqueda() {
        FiltroBusquedaDialog dialog = new FiltroBusquedaDialog(this, this);
        dialog.setFiltrosActuales(currentQuery, currentTipoSangreId);
        dialog.show();
    }

    /**
     * Configura la navegación inferior de la aplicación
     * Define las acciones para cada ítem del menú de navegación
     */
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
                    Toast.makeText(this, R.string.solo_receptores_pueden_crear_solicitudes, Toast.LENGTH_SHORT).show();
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

    /**
     * Maneja los resultados de las solicitudes de permisos
     * @param requestCode Código de la solicitud de permisos
     * @param permissions Permisos solicitados
     * @param grantResults Resultados de la concesión de permisos
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if (grantResults.length > 0 &&
                    grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                obtenerUbicacionYFiltrar();
            } else {
                Toast.makeText(this,
                        R.string.permisos_de_ubicacion,
                        Toast.LENGTH_LONG).show();
            }
        } else {
            if (NotificationPermissionManager.handlePermissionResult(requestCode, grantResults)) {
                if (AppNotificationManager.areNotificationsEnabled(this)) {
                    AppNotificationManager.startNotificationService(this);
                }
                Toast.makeText(this, R.string.notificaciones_activadas, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.las_notificaciones_estaran_desactivadas, Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Carga las solicitudes de donación desde la API
     * Aplica filtros si están activos, de lo contrario carga todas las solicitudes activas
     */
    private void loadSolicitudes() {
        if (!currentQuery.isEmpty() || currentTipoSangreId != -1) {
            cargarSolicitudesConFiltros();
        } else {
            ApiService.getSolicitudesActivas(new ApiService.ListCallback<SolicitudDonacion>() {
                @Override
                public void onSuccess(List<SolicitudDonacion> solicitudes) {
                    runOnUiThread(() -> {
                        if (solicitudes != null && !solicitudes.isEmpty()) {
                            adapter.updateData(solicitudes);
                            showSolicitudesList();
                            forceImmediateRedraw();

                            new Handler().postDelayed(() -> {
                                forceImmediateRedraw();
                            }, 1000);

                        } else {
                            showEmptyState();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> showEmptyState());
                }
            });
        }
    }

    /**
     * Carga las solicitudes aplicando los filtros activos
     * Realiza búsqueda por texto y/o tipo de sangre
     */
    private void cargarSolicitudesConFiltros() {
        showLoadingState();

        ApiService.buscarSolicitudes(currentQuery,
                currentTipoSangreId != -1 ? currentTipoSangreId : null,
                new ApiService.ListCallback<SolicitudDonacion>() {
                    @Override
                    public void onSuccess(List<SolicitudDonacion> solicitudes) {
                        runOnUiThread(() -> {
                            if (solicitudes != null && !solicitudes.isEmpty()) {
                                adapter.updateData(solicitudes);
                                showSolicitudesList();
                                forceImmediateRedraw();
                                mostrarMensajeResultados(solicitudes.size());
                            } else {
                                showEmptyState();
                                personalizarMensajeEmptyStateConFiltros();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            showEmptyState();
                            Toast.makeText(FeedDonacion.this, R.string.error_al_buscar_solicitudes, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    /**
     * Muestra un mensaje con la cantidad de resultados encontrados
     * @param cantidad Número de solicitudes encontradas
     */
    private void mostrarMensajeResultados(int cantidad) {
        String mensaje;

        if (!currentQuery.isEmpty() && currentTipoSangreId != -1) {
            // Caso: ambos filtros activos (query + tipo sangre)
            String tipoSangre = convertirTipoSangreIdANombre(currentTipoSangreId);
            mensaje = getString(R.string.resultados_ambos_filtros, cantidad, currentQuery, tipoSangre);
        } else if (!currentQuery.isEmpty()) {
            // Caso: solo query de búsqueda
            mensaje = getString(R.string.resultados_solo_query, cantidad, currentQuery);
        } else if (currentTipoSangreId != -1) {
            // Caso: solo tipo sangre
            String tipoSangre = convertirTipoSangreIdANombre(currentTipoSangreId);
            mensaje = getString(R.string.resultados_solo_sangre, cantidad, tipoSangre);
        } else {
            // Caso: sin filtros - solo cantidad
            mensaje = getString(R.string.resultados_sin_filtros, cantidad);
        }

        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
    }

    /**
     * Personaliza el mensaje del estado vacío cuando hay filtros activos
     * Muestra información específica sobre los filtros aplicados
     */
    private void personalizarMensajeEmptyStateConFiltros() {
        TextView emptyStateTitle = findViewById(R.id.text_empty_title);
        TextView emptyStateMessage = findViewById(R.id.text_empty_message);

        if (emptyStateTitle != null && emptyStateMessage != null) {
            emptyStateTitle.setText(R.string.no_se_encontraron_resultados);

            String mensaje;

            if (!currentQuery.isEmpty() && currentTipoSangreId != -1) {
                // Caso: ambos filtros activos
                String tipoSangre = convertirTipoSangreIdANombre(currentTipoSangreId);
                mensaje = getString(R.string.empty_state_ambos_filtros, currentQuery, tipoSangre);
            } else if (!currentQuery.isEmpty()) {
                // Caso: solo query
                mensaje = getString(R.string.empty_state_solo_query, currentQuery);
            } else if (currentTipoSangreId != -1) {
                // Caso: solo tipo sangre
                String tipoSangre = convertirTipoSangreIdANombre(currentTipoSangreId);
                mensaje = getString(R.string.empty_state_solo_sangre, tipoSangre);
            } else {
                // Caso: sin filtros
                mensaje = getString(R.string.empty_state_sin_filtros);
            }

            emptyStateMessage.setText(mensaje);
        }
    }

    /**
     * Convierte un ID de tipo de sangre a su nombre correspondiente
     * @param tipoSangreId ID del tipo de sangre
     * @return Nombre del tipo de sangre (A+, A-, B+, etc.)
     */
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

    /**
     * Se ejecuta cuando se aplican filtros desde el diálogo de búsqueda
     * @param query Texto de búsqueda
     * @param tipoSangreId ID del tipo de sangre seleccionado
     */
    @Override
    public void onAplicarFiltros(String query, int tipoSangreId) {
        currentQuery = query;
        currentTipoSangreId = tipoSangreId;
        mostrarIndicadorFiltros();
        cargarSolicitudesConFiltros();
    }

    /**
     * Se ejecuta cuando se limpian los filtros desde el diálogo de búsqueda
     * Restablece los filtros y carga todas las solicitudes
     */
    @Override
    public void onLimpiarFiltros() {
        currentQuery = "";
        currentTipoSangreId = -1;
        ocultarIndicadorFiltros();
        loadSolicitudes();
        Toast.makeText(this, R.string.filtros_limpiados, Toast.LENGTH_SHORT).show();
    }

    /**
     * Muestra indicadores visuales de que hay filtros activos
     * Cambia el color y texto del título
     */
    private void mostrarIndicadorFiltros() {
        boolean tieneFiltros = false;
        String mensajeFiltros = "";

        if (!currentQuery.isEmpty() && currentTipoSangreId != -1) {
            // Ambos filtros
            String tipoSangre = convertirTipoSangreIdANombre(currentTipoSangreId);
            mensajeFiltros = getString(R.string.ambos_filtros_activos, currentQuery, tipoSangre);
            tieneFiltros = true;
        } else if (!currentQuery.isEmpty()) {
            // Solo query
            mensajeFiltros = getString(R.string.solo_query_activo, currentQuery);
            tieneFiltros = true;
        } else if (currentTipoSangreId != -1) {
            // Solo tipo sangre
            String tipoSangre = convertirTipoSangreIdANombre(currentTipoSangreId);
            mensajeFiltros = getString(R.string.solo_sangre_activo, tipoSangre);
            tieneFiltros = true;
        }

        if (tieneFiltros) {
            textTitle.setText(mensajeFiltros);
            textTitle.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    /**
     * Oculta los indicadores de filtros activos
     * Restablece el título a su estado normal
     */
    private void ocultarIndicadorFiltros() {
        textTitle.setText(R.string.solicitudes_de_donaci_n);
        textTitle.setTextColor(getResources().getColor(android.R.color.black));
    }

    /**
     * Inicia la actualización automática agresiva del feed
     * Actualiza la vista cada 0.5 segundos para mantener los datos frescos
     */
    private void startAggressiveAutoRefresh() {
        autoRefreshHandler = new Handler();
        autoRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                forceImmediateRedraw();
                autoRefreshHandler.postDelayed(this, AUTO_REFRESH_INTERVAL);
            }
        };
        autoRefreshHandler.post(autoRefreshRunnable);
    }

    /**
     * Fuerza el redibujado inmediato de la interfaz
     * Actualiza el adapter y solicita nuevo layout del RecyclerView
     */
    private void forceImmediateRedraw() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            rvSolicitudes.invalidate();
            rvSolicitudes.post(new Runnable() {
                @Override
                public void run() {
                    rvSolicitudes.requestLayout();
                }
            });
        }
    }

    /**
     * Detiene la actualización automática del feed
     * Elimina los callbacks pendientes para evitar fugas de memoria
     */
    private void stopAutoRefresh() {
        if (autoRefreshHandler != null && autoRefreshRunnable != null) {
            autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        }
    }

    /**
     * Muestra el estado de carga (oculta lista y estado vacío)
     */
    private void showLoadingState() {
        rvSolicitudes.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.GONE);
    }

    /**
     * Muestra la lista de solicitudes (oculta estado vacío)
     */
    private void showSolicitudesList() {
        rvSolicitudes.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
    }

    /**
     * Muestra el estado vacío (oculta lista de solicitudes)
     */
    private void showEmptyState() {
        rvSolicitudes.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);
    }

    /**
     * Navega a la actividad de login
     * Limpia el stack de actividades
     */
    private void navigateToLogin() {
        Intent intent = new Intent(this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Método del ciclo de vida que se ejecuta al reanudar la actividad
     * Recarga datos y reactiva la actualización automática
     */
    @Override
    protected void onResume() {
        super.onResume();
        usuarioActual = SharedPreferencesManager.getCurrentUser(this);
        actualizarNavegacionSegunRol();
        loadSolicitudes();
        startAggressiveAutoRefresh();
        bottomNavigation.setSelectedItemId(R.id.nav_inicio);

        new Handler().postDelayed(() -> {
            forceImmediateRedraw();
        }, 2000);
    }

    /**
     * Método del ciclo de vida que se ejecuta al pausar la actividad
     * Detiene la actualización automática para ahorrar recursos
     */
    @Override
    protected void onPause() {
        super.onPause();
        stopAutoRefresh();
    }

    /**
     * Método del ciclo de vida que se ejecuta al destruir la actividad
     * Limpia recursos y detiene la actualización automática
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAutoRefresh();
    }

    /**
     * Método del ciclo de vida que se ejecuta al detener la actividad
     * Detiene la actualización automática
     */
    @Override
    protected void onStop() {
        super.onStop();
        stopAutoRefresh();
    }

    // ========== MÉTODOS PARA FILTRO POR UBICACIÓN ==========

    /**
     * Activa el filtro por ubicación
     * Verifica permisos y obtiene la ubicación del usuario
     */
    private void activarFiltroUbicacion() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                        },
                        PERMISSION_REQUEST_LOCATION
                );
                return;
            }
        }

        obtenerUbicacionYFiltrar();
    }

    /**
     * Obtiene la ubicación del usuario y aplica el filtro
     * Utiliza el GPS o red para obtener la ubicación actual
     */
    private void obtenerUbicacionYFiltrar() {
        showLoadingState();
        Toast.makeText(this, R.string.obteniendo_ubicacion, Toast.LENGTH_SHORT).show();

        try {
            android.location.LocationManager locationManager =
                    (android.location.LocationManager) getSystemService(Context.LOCATION_SERVICE);

            if (locationManager == null) {
                Toast.makeText(this, R.string.error_al_acceder_al_servicio_de_ubicacion, Toast.LENGTH_SHORT).show();
                loadSolicitudes();
                return;
            }

            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                loadSolicitudes();
                return;
            }

            android.location.Location lastLocation =
                    locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);

            if (lastLocation == null) {
                lastLocation = locationManager.getLastKnownLocation(
                        android.location.LocationManager.NETWORK_PROVIDER);
            }

            if (lastLocation != null) {
                procesarUbicacion(lastLocation.getLatitude(), lastLocation.getLongitude());
            } else {
                locationManager.requestSingleUpdate(
                        android.location.LocationManager.GPS_PROVIDER,
                        new android.location.LocationListener() {
                            @Override
                            public void onLocationChanged(android.location.Location location) {
                                procesarUbicacion(location.getLatitude(), location.getLongitude());
                            }

                            @Override
                            public void onStatusChanged(String provider, int status, android.os.Bundle extras) {}

                            @Override
                            public void onProviderEnabled(String provider) {}

                            @Override
                            public void onProviderDisabled(String provider) {
                                Toast.makeText(FeedDonacion.this,
                                        R.string.por_favor_activa_el_gps, Toast.LENGTH_LONG).show();
                                loadSolicitudes();
                            }
                        },
                        null
                );
            }

        } catch (Exception e) {
            Toast.makeText(this, R.string.error_al_obtener_ubicacion, Toast.LENGTH_SHORT).show();
            loadSolicitudes();
        }
    }

    /**
     * Procesa la ubicación obtenida y la guarda en la base de datos
     * @param latitud Latitud obtenida
     * @param longitud Longitud obtenida
     */
    private void procesarUbicacion(double latitud, double longitud) {
        if (usuarioActual != null) {
            ApiService.updateUserLocation(
                    usuarioActual.getUsuarioid(),
                    latitud,
                    longitud,
                    new ApiService.ApiCallback<Usuario>() {
                        @Override
                        public void onSuccess(Usuario usuarioActualizado) {
                            usuarioActual.setLatitud(latitud);
                            usuarioActual.setLongitud(longitud);
                            SharedPreferencesManager.saveCurrentUser(FeedDonacion.this, usuarioActual);
                            filtrarSolicitudesCercanas(latitud, longitud);
                        }

                        @Override
                        public void onError(String error) {
                            filtrarSolicitudesCercanas(latitud, longitud);
                        }
                    }
            );
        } else {
            filtrarSolicitudesCercanas(latitud, longitud);
        }
    }

    /**
     * Filtra las solicitudes cercanas a la ubicación del usuario
     * @param latitud Latitud del usuario
     * @param longitud Longitud del usuario
     */
    private void filtrarSolicitudesCercanas(double latitud, double longitud) {
        final double RADIO_KM = 3.5;

        ApiService.getSolicitudesCercanas(latitud, longitud, RADIO_KM,
                new ApiService.ListCallback<SolicitudDonacion>() {
                    @Override
                    public void onSuccess(List<SolicitudDonacion> solicitudes) {
                        runOnUiThread(() -> {
                            if (solicitudes != null && !solicitudes.isEmpty()) {
                                adapter.updateData(solicitudes);
                                showSolicitudesList();

                                filtroUbicacionActivo = true;
                                actualizarBotonUbicacion();

                                Toast.makeText(FeedDonacion.this,
                                        getString(R.string.se_encontraron) + solicitudes.size() +
                                                getString(R.string.solicitudes),
                                        Toast.LENGTH_LONG).show();

                                forceImmediateRedraw();
                            } else {
                                showEmptyState();
                                personalizarMensajeEmptyStateUbicacion();

                                filtroUbicacionActivo = true;
                                actualizarBotonUbicacion();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(FeedDonacion.this,
                                    R.string.error_filtrar_ubicacion, Toast.LENGTH_SHORT).show();
                            loadSolicitudes();
                        });
                    }
                });
    }

    /**
     * Desactiva el filtro por ubicación
     * Recarga todas las solicitudes sin filtros de ubicación
     */
    private void desactivarFiltroUbicacion() {
        filtroUbicacionActivo = false;
        actualizarBotonUbicacion();
        loadSolicitudes();
        Toast.makeText(this, R.string.filtro_ubicacion_desactivado, Toast.LENGTH_SHORT).show();
    }

    /**
     * Actualiza la apariencia del botón de ubicación según su estado
     * Cambia color e indica visualmente si el filtro está activo
     */
    private void actualizarBotonUbicacion() {
        if (filtroUbicacionActivo) {
            btnFilterLocation.setColorFilter(getResources().getColor(android.R.color.holo_red_dark));
            textTitle.setText(R.string.solicitudes_cercanas);
            textTitle.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            btnFilterLocation.setColorFilter(getResources().getColor(android.R.color.black));
            if (currentQuery.isEmpty() && currentTipoSangreId == -1) {
                textTitle.setText(R.string.solicitudes_donacion);
                textTitle.setTextColor(getResources().getColor(android.R.color.black));
            }
        }
    }

    /**
     * Personaliza el mensaje del estado vacío cuando el filtro de ubicación está activo
     * Informa al usuario que no hay solicitudes en su área cercana
     */
    private void personalizarMensajeEmptyStateUbicacion() {
        TextView emptyStateTitle = findViewById(R.id.text_empty_title);
        TextView emptyStateMessage = findViewById(R.id.text_empty_message);

        if (emptyStateTitle != null && emptyStateMessage != null) {
            emptyStateTitle.setText(R.string.no_hay_solicitudes_cercanas);
            emptyStateMessage.setText(R.string.no_solicitudes_en_km);
        }
    }
}