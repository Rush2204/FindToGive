package sv.edu.catolica.findtogive.ClasesDiseño;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.ApiService;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.HistorialAdapter;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.SharedPreferencesManager;
import sv.edu.catolica.findtogive.Modelado.SolicitudDonacion;
import sv.edu.catolica.findtogive.Modelado.Usuario;
import sv.edu.catolica.findtogive.R;

public class HistorialDonaciones extends AppCompatActivity implements HistorialAdapter.OnItemDeleteListener {

    private static final String TAG = "HistorialDonaciones";
    private static final String PREF_ELIMINADOS = "historial_eliminados";

    private RecyclerView recyclerViewHistorial;
    private LinearLayout layoutEmptyStateHistory;
    private BottomNavigationView bottomNavigation;

    private List<SolicitudDonacion> solicitudList;
    private HistorialAdapter historialAdapter;
    private Usuario usuarioActual;
    private Set<Integer> solicitudesEliminadas; // Para guardar IDs eliminados localmente

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.desing_historial_donaciones);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Verificar y obtener usuario logueado
        usuarioActual = SharedPreferencesManager.getCurrentUser(this);
        if (usuarioActual == null || !SharedPreferencesManager.isLoggedIn(this)) {
            Toast.makeText(this, "Debe iniciar sesión.", Toast.LENGTH_LONG).show();
            navigateToLogin();
            return;
        }



        initializeViews();
        setupRecyclerView();
        setupBottomNavigation();
        loadUserSolicitudes(usuarioActual.getUsuarioid());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar el historial al reanudar la actividad
        if (usuarioActual != null) {
            loadUserSolicitudes(usuarioActual.getUsuarioid());
        }

        // Asegurar que el ítem de historial esté seleccionado
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_historial);
        }
    }

    private void initializeViews() {
        recyclerViewHistorial = findViewById(R.id.recycler_view_historial);
        layoutEmptyStateHistory = findViewById(R.id.layout_empty_state_history);
        bottomNavigation = findViewById(R.id.bottom_navigation_bar);

        solicitudList = new ArrayList<>();
        solicitudesEliminadas = new HashSet<>();
    }

    private void setupRecyclerView() {
        historialAdapter = new HistorialAdapter(solicitudList, this);
        recyclerViewHistorial.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewHistorial.setAdapter(historialAdapter);
    }

    private void setupBottomNavigation() {
        if (bottomNavigation == null) return;

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_inicio) {
                startActivity(new Intent(this, FeedDonacion.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_crear) {
                if (usuarioActual != null && (usuarioActual.getRolid() == 2 || usuarioActual.getRolid() == 3)) {
                    startActivity(new Intent(this, SolicitudDonacionC.class));
                } else {
                    Toast.makeText(this, "Solo receptores pueden crear solicitudes", Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (itemId == R.id.nav_notificaciones) {
                startActivity(new Intent(this, Notificaciones.class));
                finish();
                Toast.makeText(this, "Avisos y notificaciones", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_historial) {
                // Ya estamos aquí
                return true;
            } else if (itemId == R.id.nav_perfil) {
                startActivity(new Intent(this, PerfilUsuario.class));
                return true;
            } else if (itemId == R.id.nav_mensajeria) {
                startActivity(new Intent(this, Mensajeria.class));
                return true;
            }
            return false;
        });

        // Ocultar ítem "Crear" del menú para donantes
        if (usuarioActual != null && usuarioActual.getRolid() == 1) {
            bottomNavigation.getMenu().findItem(R.id.nav_crear).setVisible(false);
        }

        // Marcar el ítem correcto
        bottomNavigation.setSelectedItemId(R.id.nav_historial);
    }

    /**
     * Carga las SOLICITUDES del usuario logueado, filtrando las eliminadas
     */
    /**
     * Carga las SOLICITUDES del usuario logueado, mostrando solo las activas
     */
    /**
     * Carga las SOLICITUDES del usuario logueado, mostrando solo las activas
     */
    private void loadUserSolicitudes(int usuarioId) {
        showLoadingState();

        ApiService.getSolicitudesByUsuarioId(usuarioId, new ApiService.ListCallback<SolicitudDonacion>() {
            @Override
            public void onSuccess(List<SolicitudDonacion> result) {
                runOnUiThread(() -> {
                    solicitudList.clear();
                    if (result != null) {
                        // FILTRAR: Solo mostrar solicitudes con estado "activa"
                        for (SolicitudDonacion solicitud : result) {
                            if ("activa".equalsIgnoreCase(solicitud.getEstado())) {
                                solicitudList.add(solicitud);
                            }
                        }
                        Log.d(TAG, "✅ " + solicitudList.size() + " solicitudes activas (de " + result.size() + " totales)");

                        // Debug: mostrar estados de todas las solicitudes
                        for (SolicitudDonacion s : result) {
                            Log.d(TAG, "   Solicitud ID " + s.getSolicitudid() + " - Estado: " + s.getEstado());
                        }
                    } else {
                        Log.d(TAG, "⚠️ Resultado nulo para solicitudes del usuario " + usuarioId);
                    }
                    historialAdapter.notifyDataSetChanged();
                    updateUIState();
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error al cargar solicitudes: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(HistorialDonaciones.this, "Error al cargar solicitudes: " + error, Toast.LENGTH_LONG).show();
                    updateUIState();
                });
            }
        });
    }

    /**
     * Implementación para "eliminar" solicitudes del historial (cambiando estado a "eliminada")
     */
    @Override
    public void onDeleteClick(SolicitudDonacion solicitud, int position) {
        Snackbar.make(recyclerViewHistorial, "¿Eliminar esta solicitud de tu historial?", Snackbar.LENGTH_LONG)
                .setAction("ELIMINAR", view -> eliminarDelHistorial(solicitud, position))
                .show();
    }

    private void eliminarDelHistorial(SolicitudDonacion solicitud, int position) {
        showLoadingState();

        Log.d(TAG, "🔄 Cambiando estado de solicitud " + solicitud.getSolicitudid() + " a: cancelada");


        ApiService.updateSolicitudEstado(solicitud.getSolicitudid(), "cancelada", new ApiService.ApiCallback<SolicitudDonacion>() {
            @Override
            public void onSuccess(SolicitudDonacion result) {
                runOnUiThread(() -> {
                    Log.d(TAG, "✅ Estado cambiado a 'inactiva' para solicitud ID: " + solicitud.getSolicitudid());

                    // Remover de la lista local
                    if (position >= 0 && position < solicitudList.size()) {
                        solicitudList.remove(position);
                        historialAdapter.notifyItemRemoved(position);
                    }

                    Snackbar.make(recyclerViewHistorial, "Solicitud eliminada", Snackbar.LENGTH_SHORT).show();
                    updateUIState();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "❌ Error al cambiar estado: " + error);
                    Toast.makeText(HistorialDonaciones.this, "Error al eliminar solicitud: " + error, Toast.LENGTH_LONG).show();
                    updateUIState();
                });
            }
        });
    }



    // --- Métodos de Estado de UI ---

    private void showLoadingState() {
        recyclerViewHistorial.setVisibility(View.GONE);
        layoutEmptyStateHistory.setVisibility(View.GONE);
    }

    private void showDonationList() {
        recyclerViewHistorial.setVisibility(View.VISIBLE);
        layoutEmptyStateHistory.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        recyclerViewHistorial.setVisibility(View.GONE);
        layoutEmptyStateHistory.setVisibility(View.VISIBLE);
    }

    private void updateUIState() {
        if (solicitudList.isEmpty()) {
            showEmptyState();
        } else {
            showDonationList();
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}