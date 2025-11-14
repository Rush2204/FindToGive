package sv.edu.catolica.findtogive.ClasesDiseño;

import android.content.Intent;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;

import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.NotificacionAdapter;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.ApiService;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.SharedPreferencesManager;
import sv.edu.catolica.findtogive.Modelado.Notificacion;
import sv.edu.catolica.findtogive.Modelado.Usuario;
import sv.edu.catolica.findtogive.R;

public class Notificaciones extends AppCompatActivity {

    private RecyclerView recyclerViewNotifications;
    private LinearLayout layoutEmptyState;
    private NotificacionAdapter notificacionAdapter;
    private List<Notificacion> notificacionesList;
    private Usuario usuarioActual;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.desing_notificaciones);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        usuarioActual = SharedPreferencesManager.getCurrentUser(this);
        initializeViews();
        setupRecyclerView();
        setupBottomNavigation();
        loadNotificaciones();

        debugNotificaciones();
    }

    private void initializeViews() {
        recyclerViewNotifications = findViewById(R.id.recycler_view_notifications);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        bottomNavigation = findViewById(R.id.bottom_navigation_bar);
    }

    private void setupRecyclerView() {
        notificacionesList = new ArrayList<>();
        notificacionAdapter = new NotificacionAdapter(notificacionesList, this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewNotifications.setLayoutManager(layoutManager);
        recyclerViewNotifications.setAdapter(notificacionAdapter);
    }
    // ⭐⭐ NUEVO MÉTODO: Actualizar navegación según rol ⭐⭐
    private void actualizarNavegacionSegunRol() {
        if (usuarioActual != null) {
            boolean esDonante = usuarioActual.getRolid() == 1;
            bottomNavigation.getMenu().findItem(R.id.nav_crear).setVisible(!esDonante);
        }
    }

    private void setupBottomNavigation() {
        actualizarNavegacionSegunRol();
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_inicio) {
                Intent intent = new Intent(this, FeedDonacion.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
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
                // Ya estamos en notificaciones
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

        bottomNavigation.setSelectedItemId(R.id.nav_notificaciones);
    }


    private void loadNotificaciones() {
        if (usuarioActual == null) {
            System.out.println("❌ Usuario actual es null");
            return;
        }

        System.out.println("🔄 Cargando notificaciones para usuario: " + usuarioActual.getUsuarioid());

        ApiService.getNotificacionesByUsuario(usuarioActual.getUsuarioid(), new ApiService.ListCallback<Notificacion>() {
            @Override
            public void onSuccess(List<Notificacion> notificaciones) {
                runOnUiThread(() -> {
                    if (notificaciones != null && !notificaciones.isEmpty()) {
                        System.out.println("✅ " + notificaciones.size() + " notificaciones cargadas");
                        notificacionAdapter.setNotificaciones(notificaciones);
                        showNotificationsList();
                    } else {
                        System.out.println("ℹ️ No hay notificaciones");
                        showEmptyState();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    System.out.println("❌ Error cargando notificaciones: " + error);
                    showEmptyState();
                });
            }
        });
    }

    private void debugNotificaciones() {
        System.out.println("🐛 DEBUG: Verificando notificaciones en BD");

        ApiService.getNotificacionesByUsuario(usuarioActual.getUsuarioid(), new ApiService.ListCallback<Notificacion>() {
            @Override
            public void onSuccess(List<Notificacion> notificaciones) {
                System.out.println("📋 NOTIFICACIONES EN BD: " + (notificaciones != null ? notificaciones.size() : 0));
                if (notificaciones != null) {
                    for (Notificacion notif : notificaciones) {
                        System.out.println("🔔 ID: " + notif.getNotificacionid() +
                                " - Título: " + notif.getTitulo() +
                                " - Mensaje: " + notif.getMensaje() +
                                " - Leída: " + notif.isLeida());
                    }
                }
            }

            @Override
            public void onError(String error) {
                System.out.println("❌ ERROR obteniendo notificaciones: " + error);
            }
        });
    }

    // Llama a este método en onCreate después de loadNotificaciones()


    private void showNotificationsList() {
        recyclerViewNotifications.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        recyclerViewNotifications.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar notificaciones al volver a la actividad
        actualizarNavegacionSegunRol();
        loadNotificaciones();
        bottomNavigation.setSelectedItemId(R.id.nav_notificaciones);
    }
}