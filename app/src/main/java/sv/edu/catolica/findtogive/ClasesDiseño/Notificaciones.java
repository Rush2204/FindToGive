package sv.edu.catolica.findtogive.ClasesDiseño;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
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

    /**
     * Método principal que inicializa la actividad de notificaciones
     * Configura la vista, navegación y carga las notificaciones del usuario
     */
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
    }

    /**
     * Inicializa todos los componentes visuales de la interfaz
     * Obtiene referencias a los views del layout
     */
    private void initializeViews() {
        recyclerViewNotifications = findViewById(R.id.recycler_view_notifications);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        bottomNavigation = findViewById(R.id.bottom_navigation_bar);
    }

    /**
     * Configura el RecyclerView para mostrar la lista de notificaciones
     * Inicializa el adapter y establece el layout manager
     */
    private void setupRecyclerView() {
        notificacionesList = new ArrayList<>();
        notificacionAdapter = new NotificacionAdapter(notificacionesList, this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewNotifications.setLayoutManager(layoutManager);
        recyclerViewNotifications.setAdapter(notificacionAdapter);
    }

    /**
     * Actualiza la navegación inferior según el rol del usuario
     * Oculta el ítem de creación para usuarios donantes
     */
    private void actualizarNavegacionSegunRol() {
        if (usuarioActual != null) {
            boolean esDonante = usuarioActual.getRolid() == 1;
            bottomNavigation.getMenu().findItem(R.id.nav_crear).setVisible(!esDonante);
        }
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
                    Toast.makeText(this, R.string.solo_receptores_pueden_crear_solicitudes, Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (itemId == R.id.nav_notificaciones) {
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

    /**
     * Carga las notificaciones del usuario desde la API
     * Actualiza la lista de notificaciones en el adapter
     */
    private void loadNotificaciones() {
        if (usuarioActual == null) {
            return;
        }

        ApiService.getNotificacionesByUsuario(usuarioActual.getUsuarioid(), new ApiService.ListCallback<Notificacion>() {
            @Override
            public void onSuccess(List<Notificacion> notificaciones) {
                runOnUiThread(() -> {
                    if (notificaciones != null && !notificaciones.isEmpty()) {
                        notificacionAdapter.setNotificaciones(notificaciones);
                        showNotificationsList();
                    } else {
                        showEmptyState();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showEmptyState();
                });
            }
        });
    }

    /**
     * Muestra la lista de notificaciones (oculta estado vacío)
     */
    private void showNotificationsList() {
        recyclerViewNotifications.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
    }

    /**
     * Muestra el estado vacío (oculta lista de notificaciones)
     */
    private void showEmptyState() {
        recyclerViewNotifications.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);
    }

    /**
     * Método del ciclo de vida que se ejecuta al reanudar la actividad
     * Recarga las notificaciones y actualiza la navegación
     */
    @Override
    protected void onResume() {
        super.onResume();
        actualizarNavegacionSegunRol();
        loadNotificaciones();
        bottomNavigation.setSelectedItemId(R.id.nav_notificaciones);
    }
}