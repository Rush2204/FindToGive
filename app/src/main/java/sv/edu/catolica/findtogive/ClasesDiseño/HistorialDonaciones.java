package sv.edu.catolica.findtogive.ClasesDiseño;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
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
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.ApiService;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.HistorialAdapter;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.HistorialFiltroDialog;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.SharedPreferencesManager;
import sv.edu.catolica.findtogive.Modelado.Chat;
import sv.edu.catolica.findtogive.Modelado.Mensaje;
import sv.edu.catolica.findtogive.Modelado.SolicitudDonacion;
import sv.edu.catolica.findtogive.Modelado.Usuario;
import sv.edu.catolica.findtogive.R;

public class HistorialDonaciones extends AppCompatActivity implements
        HistorialAdapter.OnItemDeleteListener,
        HistorialAdapter.OnItemCompleteListener,
        HistorialFiltroDialog.HistorialFiltroListener {

    private RecyclerView recyclerViewHistorial;
    private LinearLayout layoutEmptyStateHistory;
    private BottomNavigationView bottomNavigation;
    private ImageButton btnFilterHistorial;
    private TextView textTitleHistorial;
    private TextView textFilterIndicator;

    private List<SolicitudDonacion> solicitudList;
    private List<SolicitudDonacion> todasLasSolicitudes;
    private List<Chat> chatsDelUsuario;
    private HistorialAdapter historialAdapter;
    private Usuario usuarioActual;
    private Set<Integer> solicitudesEliminadas, solicitudesCompletadas;

    // Variables para filtros
    private String currentEstado = "activa";
    private String currentRol = "todas";
    private Map<Integer, Boolean> mensajesNoLeidosPorSolicitud;

    private Handler autoRefreshHandler;
    private Runnable autoRefreshRunnable;
    private static final long AUTO_REFRESH_INTERVAL = 500;

    private Handler mensajesNoLeidosHandler;
    private Runnable mensajesNoLeidosRunnable;
    private static final long MENSAJES_CHECK_INTERVAL = 2000;

    /**
     * Método principal que inicializa la actividad del historial de donaciones
     * Configura la vista, verifica autenticación y carga los datos iniciales
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.desing_historial_donaciones);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        usuarioActual = SharedPreferencesManager.getCurrentUser(this);
        if (usuarioActual == null || !SharedPreferencesManager.isLoggedIn(this)) {
            Toast.makeText(this, R.string.debe_iniciar_sesion, Toast.LENGTH_LONG).show();
            navigateToLogin();
            return;
        }

        mensajesNoLeidosPorSolicitud = new HashMap<>();

        initializeViews();
        setupRecyclerView();
        setupBottomNavigation();
        setupClickListeners();

        actualizarTituloPorDefecto();
        loadChatsDelUsuario();

        startAggressiveAutoRefresh();
        startMensajesNoLeidosChecker();
    }

    /**
     * Método del ciclo de vida que se ejecuta al reanudar la actividad
     * Reactiva las actualizaciones automáticas y verifica mensajes no leídos
     */
    @Override
    protected void onResume() {
        super.onResume();

        verificarTodosLosMensajesNoLeidos();

        if (usuarioActual != null) {
            loadChatsDelUsuario();
        }

        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_historial);
        }

        startAggressiveAutoRefresh();
        startMensajesNoLeidosChecker();
    }

    /**
     * Método del ciclo de vida que se ejecuta al pausar la actividad
     * Detiene las actualizaciones automáticas para ahorrar recursos
     */
    @Override
    protected void onPause() {
        super.onPause();
        stopAutoRefresh();
        stopMensajesNoLeidosChecker();
    }

    /**
     * Método del ciclo de vida que se ejecuta al destruir la actividad
     * Limpia recursos y detiene todos los handlers
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAutoRefresh();
        stopMensajesNoLeidosChecker();
    }

    /**
     * Inicializa todos los componentes visuales de la interfaz
     * Obtiene referencias a los views del layout
     */
    private void initializeViews() {
        recyclerViewHistorial = findViewById(R.id.recycler_view_historial);
        layoutEmptyStateHistory = findViewById(R.id.layout_empty_state_history);
        bottomNavigation = findViewById(R.id.bottom_navigation_bar);
        btnFilterHistorial = findViewById(R.id.btn_filter_historial);
        textTitleHistorial = findViewById(R.id.text_header);
        textFilterIndicator = findViewById(R.id.text_filter_indicator);

        solicitudList = new ArrayList<>();
        todasLasSolicitudes = new ArrayList<>();
        chatsDelUsuario = new ArrayList<>();
        solicitudesEliminadas = new HashSet<>();
        solicitudesCompletadas = new HashSet<>();
    }

    /**
     * Actualiza el título por defecto de la actividad
     * Muestra "Solicitudes Activas" cuando no hay filtros aplicados
     */
    private void actualizarTituloPorDefecto() {
        if ("activa".equals(currentEstado) && "todas".equals(currentRol)) {
            textTitleHistorial.setText(R.string.solicitudes_activas);
            textFilterIndicator.setVisibility(View.GONE);
        }
    }

    /**
     * Configura el RecyclerView para mostrar la lista del historial
     * Inicializa el adapter y establece el layout manager
     */
    private void setupRecyclerView() {
        historialAdapter = new HistorialAdapter(solicitudList, this, this, usuarioActual);
        historialAdapter.setOnItemClickListener(new HistorialAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(SolicitudDonacion solicitud) {
                navigateToMensajeriaWithFilter(solicitud.getSolicitudid(), solicitud.getEstado());
            }
        });
        recyclerViewHistorial.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewHistorial.setAdapter(historialAdapter);

        recyclerViewHistorial.post(new Runnable() {
            @Override
            public void run() {
                if (historialAdapter != null) {
                    historialAdapter.notifyDataSetChanged();
                    forceImmediateRedraw();
                }
            }
        });
    }

    /**
     * Configura los listeners de clic para los botones
     * Asigna las acciones a realizar cuando se presionan los botones
     */
    private void setupClickListeners() {
        btnFilterHistorial.setOnClickListener(v -> {
            mostrarDialogoFiltroHistorial();
        });
    }

    /**
     * Muestra el diálogo de filtro del historial
     * Permite al usuario aplicar filtros por estado y rol
     */
    private void mostrarDialogoFiltroHistorial() {
        HistorialFiltroDialog dialog = new HistorialFiltroDialog(this, this);
        dialog.setFiltrosActuales(currentEstado, currentRol);
        dialog.show();
    }

    /**
     * Configura la navegación inferior de la aplicación
     * Define las acciones para cada ítem del menú de navegación
     */
    private void setupBottomNavigation() {
        if (bottomNavigation == null) return;

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
                Intent intent = new Intent(this, Notificaciones.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_historial) {
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

        bottomNavigation.setSelectedItemId(R.id.nav_historial);
    }

    /**
     * Inicia la actualización automática agresiva del historial
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
        if (historialAdapter != null) {
            historialAdapter.notifyDataSetChanged();
            recyclerViewHistorial.invalidate();
            recyclerViewHistorial.post(new Runnable() {
                @Override
                public void run() {
                    recyclerViewHistorial.requestLayout();
                }
            });
        }
    }

    /**
     * Detiene la actualización automática del historial
     * Elimina los callbacks pendientes para evitar fugas de memoria
     */
    private void stopAutoRefresh() {
        if (autoRefreshHandler != null && autoRefreshRunnable != null) {
            autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        }
    }

    /**
     * Inicia la verificación periódica de mensajes no leídos
     * Verifica cada 2 segundos si hay mensajes nuevos en los chats
     */
    private void startMensajesNoLeidosChecker() {
        if (mensajesNoLeidosHandler != null) {
            mensajesNoLeidosHandler.removeCallbacks(mensajesNoLeidosRunnable);
        }

        mensajesNoLeidosHandler = new Handler();
        mensajesNoLeidosRunnable = new Runnable() {
            @Override
            public void run() {
                verificarTodosLosMensajesNoLeidos();
                mensajesNoLeidosHandler.postDelayed(this, MENSAJES_CHECK_INTERVAL);
            }
        };
        mensajesNoLeidosHandler.postDelayed(mensajesNoLeidosRunnable, MENSAJES_CHECK_INTERVAL);
    }

    /**
     * Detiene la verificación de mensajes no leídos
     * Elimina los callbacks pendientes del handler
     */
    private void stopMensajesNoLeidosChecker() {
        if (mensajesNoLeidosHandler != null && mensajesNoLeidosRunnable != null) {
            mensajesNoLeidosHandler.removeCallbacks(mensajesNoLeidosRunnable);
        }
    }

    /**
     * Verifica TODOS los mensajes no leídos de forma eficiente
     * Recorre todos los chats del usuario y verifica mensajes no leídos
     */
    private void verificarTodosLosMensajesNoLeidos() {
        if (usuarioActual == null || chatsDelUsuario.isEmpty()) {
            return;
        }

        for (Chat chat : chatsDelUsuario) {
            verificarMensajesNoLeidosEnChat(chat, false);
        }

        actualizarUIconMensajesNoLeidos();
    }

    /**
     * Fuerza la verificación manual de mensajes no leídos
     * Útil cuando se necesita una actualización inmediata
     */
    public void forzarVerificacionMensajesNoLeidos() {
        if (usuarioActual != null && !chatsDelUsuario.isEmpty()) {
            cargarMensajesNoLeidos();
        }
    }

    /**
     * Carga los chats del usuario para el filtro de donante
     * Obtiene todos los chats en los que participa el usuario actual
     */
    private void loadChatsDelUsuario() {
        ApiService.getChatsByUsuario(usuarioActual.getUsuarioid(), new ApiService.ListCallback<Chat>() {
            @Override
            public void onSuccess(List<Chat> chats) {
                runOnUiThread(() -> {
                    if (chats != null) {
                        chatsDelUsuario.clear();
                        chatsDelUsuario.addAll(chats);
                        cargarMensajesNoLeidos();
                    }
                    loadTodasLasSolicitudesRelevantes();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    loadTodasLasSolicitudesRelevantes();
                });
            }
        });
    }

    /**
     * Carga TODAS las solicitudes relevantes: las del usuario + las de otros donde tiene chats
     * Combina solicitudes propias con aquellas donde el usuario participa como donante
     */
    private void loadTodasLasSolicitudesRelevantes() {
        showLoadingState();

        ApiService.getSolicitudesByUsuarioId(usuarioActual.getUsuarioid(), new ApiService.ListCallback<SolicitudDonacion>() {
            @Override
            public void onSuccess(List<SolicitudDonacion> misSolicitudes) {
                runOnUiThread(() -> {
                    todasLasSolicitudes.clear();

                    if (misSolicitudes != null) {
                        todasLasSolicitudes.addAll(misSolicitudes);
                    }

                    cargarSolicitudesDeOtrosUsuarios();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    cargarSolicitudesDeOtrosUsuarios();
                });
            }
        });
    }

    /**
     * Carga las solicitudes de otros usuarios donde el usuario actual tiene chats
     * Obtiene las solicitudes en las que el usuario participa como donante
     */
    private void cargarSolicitudesDeOtrosUsuarios() {
        Set<Integer> solicitudIdsDeOtros = new HashSet<>();

        for (Chat chat : chatsDelUsuario) {
            if (chat.getUsuario1id() == usuarioActual.getUsuarioid()) {
                solicitudIdsDeOtros.add(chat.getSolicitudid());
            }
        }

        if (solicitudIdsDeOtros.isEmpty()) {
            aplicarFiltros();
            return;
        }

        Set<Integer> idsSolicitudesPropias = new HashSet<>();
        for (SolicitudDonacion solicitud : todasLasSolicitudes) {
            idsSolicitudesPropias.add(solicitud.getSolicitudid());
        }

        Set<Integer> solicitudIdsUnicos = new HashSet<>();
        for (int solicitudId : solicitudIdsDeOtros) {
            if (!idsSolicitudesPropias.contains(solicitudId)) {
                solicitudIdsUnicos.add(solicitudId);
            }
        }

        if (solicitudIdsUnicos.isEmpty()) {
            aplicarFiltros();
            return;
        }

        final int[] solicitudesCargadas = {0};
        final int totalSolicitudes = solicitudIdsUnicos.size();

        for (int solicitudId : solicitudIdsUnicos) {
            cargarSolicitudPorId(solicitudId, new ApiService.ApiCallback<SolicitudDonacion>() {
                @Override
                public void onSuccess(SolicitudDonacion solicitud) {
                    runOnUiThread(() -> {
                        if (solicitud != null) {
                            boolean esDuplicada = false;
                            for (SolicitudDonacion existente : todasLasSolicitudes) {
                                if (existente.getSolicitudid() == solicitud.getSolicitudid()) {
                                    esDuplicada = true;
                                    break;
                                }
                            }

                            if (!esDuplicada) {
                                todasLasSolicitudes.add(solicitud);
                            }
                        }

                        solicitudesCargadas[0]++;
                        if (solicitudesCargadas[0] == totalSolicitudes) {
                            aplicarFiltros();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        solicitudesCargadas[0]++;
                        if (solicitudesCargadas[0] == totalSolicitudes) {
                            aplicarFiltros();
                        }
                    });
                }
            });
        }
    }

    /**
     * Método auxiliar para cargar una solicitud por ID
     * @param solicitudId ID de la solicitud a cargar
     * @param callback Callback para manejar el resultado
     */
    private void cargarSolicitudPorId(int solicitudId, ApiService.ApiCallback<SolicitudDonacion> callback) {
        ApiService.getSolicitudById(solicitudId, new ApiService.ApiCallback<SolicitudDonacion>() {
            @Override
            public void onSuccess(SolicitudDonacion solicitud) {
                callback.onSuccess(solicitud);
            }

            @Override
            public void onError(String error) {
                buscarSolicitudEnActivas(solicitudId, callback);
            }
        });
    }

    /**
     * Busca una solicitud en la lista de solicitudes activas como fallback
     * @param solicitudId ID de la solicitud a buscar
     * @param callback Callback para manejar el resultado
     */
    private void buscarSolicitudEnActivas(int solicitudId, ApiService.ApiCallback<SolicitudDonacion> callback) {
        ApiService.getSolicitudesActivas(new ApiService.ListCallback<SolicitudDonacion>() {
            @Override
            public void onSuccess(List<SolicitudDonacion> todasLasSolicitudesActivas) {
                if (todasLasSolicitudesActivas != null) {
                    for (SolicitudDonacion solicitud : todasLasSolicitudesActivas) {
                        if (solicitud.getSolicitudid() == solicitudId) {
                            callback.onSuccess(solicitud);
                            return;
                        }
                    }
                }
                callback.onError("Solicitud no encontrada");
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Aplica los filtros actuales a la lista de solicitudes
     * Filtra por estado y rol según las selecciones del usuario
     */
    private void aplicarFiltros() {
        List<SolicitudDonacion> solicitudesFiltradas = new ArrayList<>();

        for (SolicitudDonacion solicitud : todasLasSolicitudes) {
            boolean cumpleEstado = true;
            boolean cumpleRol = true;

            if (!"todas".equals(currentEstado)) {
                cumpleEstado = currentEstado.equalsIgnoreCase(solicitud.getEstado());
            }

            if (!"todas".equals(currentRol)) {
                if ("receptor".equals(currentRol)) {
                    cumpleRol = solicitud.getUsuarioid() == usuarioActual.getUsuarioid();
                } else if ("donante".equals(currentRol)) {
                    boolean esCreador = solicitud.getUsuarioid() == usuarioActual.getUsuarioid();
                    boolean esDonanteEnChat = esDonanteEnSolicitud(solicitud.getSolicitudid());
                    cumpleRol = !esCreador && esDonanteEnChat;
                }
            }

            if (cumpleEstado && cumpleRol) {
                solicitudesFiltradas.add(solicitud);
            }
        }

        if (historialAdapter != null) {
            historialAdapter.actualizarListaSolicitudes(solicitudesFiltradas);
            historialAdapter.actualizarInfoChats(chatsDelUsuario, mensajesNoLeidosPorSolicitud);
            forceImmediateRedraw();
        }

        updateUIState();
        mostrarIndicadorFiltros();
    }

    /**
     * Verifica si el usuario es DONANTE en la solicitud específica
     * @param solicitudId ID de la solicitud a verificar
     * @return true si el usuario es donante en la solicitud, false en caso contrario
     */
    private boolean esDonanteEnSolicitud(int solicitudId) {
        for (Chat chat : chatsDelUsuario) {
            if (chat.getSolicitudid() == solicitudId &&
                    chat.getUsuario1id() == usuarioActual.getUsuarioid()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Muestra indicadores visuales de que hay filtros activos
     * Actualiza el texto del indicador y el título según los filtros aplicados
     */
    private void mostrarIndicadorFiltros() {
        StringBuilder filtros = new StringBuilder();
        boolean tieneFiltros = false;

        if (!"activa".equals(currentEstado)) {
            filtros.append(getString(R.string.estado)).append(convertirEstadoANombre(currentEstado));
            tieneFiltros = true;
        }

        if (!"todas".equals(currentRol)) {
            if (tieneFiltros) filtros.append(" • ");
            filtros.append(getString(R.string.rol)).append(convertirRolANombre(currentRol));
            tieneFiltros = true;
        }

        if (tieneFiltros) {
            textFilterIndicator.setText(filtros.toString());
            textFilterIndicator.setVisibility(View.VISIBLE);
            textTitleHistorial.setText(getString(R.string.mi_historial_filtrado));
        } else {
            textFilterIndicator.setVisibility(View.GONE);
            textTitleHistorial.setText(getString(R.string.solicitudes_activas));
        }
    }

    /**
     * Convierte un estado de solicitud a su nombre legible
     * @param estado Estado de la solicitud
     * @return Nombre legible del estado
     */
    private String convertirEstadoANombre(String estado) {
        switch (estado) {
            case "activa":
                return getString(R.string.estado_activas);
            case "completada":
                return getString(R.string.estado_completadas);
            case "cancelada":
                return getString(R.string.estado_canceladas);
            case "todas":
                return getString(R.string.estado_todas);
            default:
                return estado;
        }
    }

    /**
     * Convierte un rol a su nombre legible
     * @param rol Rol del usuario
     * @return Nombre legible del rol
     */
    private String convertirRolANombre(String rol) {
        switch (rol) {
            case "receptor":
                return getString(R.string.rol_receptor);
            case "donante":
                return getString(R.string.rol_donante);
            case "todas":
                return getString(R.string.rol_todos);
            default:
                return rol;
        }
    }

    /**
     * Se ejecuta cuando se aplican filtros desde el diálogo
     * @param estado Estado seleccionado para filtrar
     * @param rol Rol seleccionado para filtrar
     */
    @Override
    public void onAplicarFiltros(String estado, String rol) {
        currentEstado = estado;
        currentRol = rol;
        aplicarFiltros();
        Toast.makeText(this, R.string.filtros_aplicados, Toast.LENGTH_SHORT).show();
    }

    /**
     * Se ejecuta cuando se limpian los filtros desde el diálogo
     * Restablece los filtros a sus valores por defecto
     */
    @Override
    public void onLimpiarFiltros() {
        currentEstado = "activa";
        currentRol = "todas";
        aplicarFiltros();
        Toast.makeText(this, R.string.filtros_limpiados, Toast.LENGTH_SHORT).show();
    }

    /**
     * Navega a la mensajería filtrando por una solicitud específica
     * @param solicitudId ID de la solicitud seleccionada
     * @param estadoSolicitud Estado actual de la solicitud
     */
    private void navigateToMensajeriaWithFilter(int solicitudId, String estadoSolicitud) {
        Intent intent = new Intent(this, Mensajeria.class);
        intent.putExtra("filter_by_solicitud", true);
        intent.putExtra("solicitud_id", solicitudId);
        intent.putExtra("solicitud_estado", estadoSolicitud);
        startActivity(intent);
    }

    /**
     * Se ejecuta cuando se hace clic en eliminar una solicitud
     * @param solicitud Solicitud a eliminar
     * @param position Posición en la lista
     */
    @Override
    public void onDeleteClick(SolicitudDonacion solicitud, int position) {
        if (solicitud.getUsuarioid() != usuarioActual.getUsuarioid()) {
            Toast.makeText(this, R.string.solo_el_creador_de_la_solicitud_puede_cancelarla, Toast.LENGTH_SHORT).show();
            return;
        }

        Snackbar.make(recyclerViewHistorial, R.string.desea_cancelar_esta_solicitud, Snackbar.LENGTH_LONG)
                .setAction(R.string.cancelar, view -> eliminarDelHistorial(solicitud, position))
                .show();
    }

    /**
     * Se ejecuta cuando se hace clic en completar una solicitud
     * @param solicitud Solicitud a completar
     * @param position Posición en la lista
     */
    @Override
    public void onCompleteClick(SolicitudDonacion solicitud, int position) {
        if (solicitud.getUsuarioid() != usuarioActual.getUsuarioid()) {
            Toast.makeText(this, R.string.solo_el_creador_de_la_solicitud_puede_completar, Toast.LENGTH_SHORT).show();
            return;
        }

        Snackbar.make(recyclerViewHistorial, R.string.marcar_esta_solicitud_como_completada, Snackbar.LENGTH_LONG)
                .setAction(R.string.completar, view -> completarDelHistorial(solicitud, position))
                .show();
    }

    /**
     * Elimina una solicitud del historial cambiando su estado a "cancelada"
     * @param solicitud Solicitud a cancelar
     * @param position Posición en la lista
     */
    private void eliminarDelHistorial(SolicitudDonacion solicitud, int position) {
        showLoadingState();

        ApiService.updateSolicitudEstado(solicitud.getSolicitudid(), "cancelada", new ApiService.ApiCallback<SolicitudDonacion>() {
            @Override
            public void onSuccess(SolicitudDonacion result) {
                runOnUiThread(() -> {
                    loadChatsDelUsuario();
                    Snackbar.make(recyclerViewHistorial, R.string.solicitud_cancelada, Snackbar.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(HistorialDonaciones.this, getString(R.string.error_al_cancelar_solicitud) + error, Toast.LENGTH_LONG).show();
                    updateUIState();
                });
            }
        });
    }

    /**
     * Carga información de mensajes no leídos para cada solicitud
     * Verifica en todos los chats si hay mensajes pendientes de leer
     */
    private void cargarMensajesNoLeidos() {
        if (usuarioActual == null) return;

        mensajesNoLeidosPorSolicitud.clear();

        for (Chat chat : chatsDelUsuario) {
            verificarMensajesNoLeidosEnChat(chat, false);
        }

        new Handler().postDelayed(() -> {
            actualizarUIconMensajesNoLeidos();
        }, 1000);
    }

    /**
     * Verifica mensajes no leídos en un chat específico
     * @param chat Chat a verificar
     * @param updateUIInmediato Si debe actualizar la UI inmediatamente
     */
    private void verificarMensajesNoLeidosEnChat(Chat chat, boolean updateUIInmediato) {
        if (chat == null) return;

        ApiService.getMensajesByChat(chat.getChatid(), new ApiService.ListCallback<Mensaje>() {
            @Override
            public void onSuccess(List<Mensaje> mensajes) {
                runOnUiThread(() -> {
                    if (mensajes != null) {
                        boolean tieneMensajesNoLeidos = false;
                        int contadorNoLeidos = 0;

                        for (Mensaje mensaje : mensajes) {
                            if (!mensaje.isLeido() && mensaje.getEmisorioid() != usuarioActual.getUsuarioid()) {
                                tieneMensajesNoLeidos = true;
                                contadorNoLeidos++;
                            }
                        }

                        mensajesNoLeidosPorSolicitud.put(chat.getSolicitudid(), tieneMensajesNoLeidos);

                        if (updateUIInmediato) {
                            actualizarUIconMensajesNoLeidos();
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    mensajesNoLeidosPorSolicitud.put(chat.getSolicitudid(), false);
                    if (updateUIInmediato) {
                        actualizarUIconMensajesNoLeidos();
                    }
                });
            }
        });
    }

    /**
     * Actualiza la UI con la información de mensajes no leídos
     * Notifica al adapter sobre los cambios en los mensajes no leídos
     */
    private void actualizarUIconMensajesNoLeidos() {
        if (historialAdapter != null) {
            historialAdapter.actualizarInfoChats(chatsDelUsuario, mensajesNoLeidosPorSolicitud);
            forceImmediateRedraw();
        }
    }

    /**
     * Completa una solicitud del historial cambiando su estado a "completada"
     * @param solicitud Solicitud a completar
     * @param position Posición en la lista
     */
    private void completarDelHistorial(SolicitudDonacion solicitud, int position) {
        showLoadingState();

        ApiService.updateSolicitudEstado(solicitud.getSolicitudid(), "completada", new ApiService.ApiCallback<SolicitudDonacion>() {
            @Override
            public void onSuccess(SolicitudDonacion result) {
                runOnUiThread(() -> {
                    loadChatsDelUsuario();
                    Snackbar.make(recyclerViewHistorial, R.string.solicitud_completada, Snackbar.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(HistorialDonaciones.this, getString(R.string.error_al_completar_solicitud) + error, Toast.LENGTH_LONG).show();
                    updateUIState();
                });
            }
        });
    }

    /**
     * Muestra el estado de carga (oculta lista y estado vacío)
     */
    private void showLoadingState() {
        recyclerViewHistorial.setVisibility(View.GONE);
        layoutEmptyStateHistory.setVisibility(View.GONE);
    }

    /**
     * Muestra la lista de donaciones (oculta estado vacío)
     */
    private void showDonationList() {
        recyclerViewHistorial.setVisibility(View.VISIBLE);
        layoutEmptyStateHistory.setVisibility(View.GONE);
    }

    /**
     * Muestra el estado vacío (oculta lista de donaciones)
     */
    private void showEmptyState() {
        recyclerViewHistorial.setVisibility(View.GONE);
        layoutEmptyStateHistory.setVisibility(View.VISIBLE);
    }

    /**
     * Actualiza el estado de la UI según si hay datos o no
     * Muestra lista o estado vacío según la cantidad de solicitudes
     */
    private void updateUIState() {
        if (solicitudList.isEmpty()) {
            showEmptyState();
        } else {
            showDonationList();
        }
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
}