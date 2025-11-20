package sv.edu.catolica.findtogive.ClasesDiseño;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.ApiService;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.HistorialAdapter;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.HistorialFiltroDialog;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.HistorialRealtimeService;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.SharedPreferencesManager;
import sv.edu.catolica.findtogive.Modelado.Chat;
import sv.edu.catolica.findtogive.Modelado.Mensaje;
import sv.edu.catolica.findtogive.Modelado.SolicitudDonacion;
import sv.edu.catolica.findtogive.Modelado.Usuario;
import sv.edu.catolica.findtogive.R;

public class HistorialDonaciones extends AppCompatActivity implements
        HistorialAdapter.OnItemDeleteListener,
        HistorialAdapter.OnItemCompleteListener,
        HistorialFiltroDialog.HistorialFiltroListener,
        HistorialRealtimeService.HistorialListener {

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
    private static final long MENSAJES_CHECK_INTERVAL = 3000; // 3 segundos

    // Servicio de tiempo real para historial
    private HistorialRealtimeService historialRealtimeService;

    // Control para evitar recargas múltiples
    private boolean isLoadingData = false;
    private long lastDataLoadTime = 0;
    private static final long MIN_LOAD_INTERVAL = 10000; // 10 segundos mínimo entre recargas

    /**
     * Método principal que inicializa la actividad del historial de donaciones
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

        // Inicializar servicio de tiempo real
        historialRealtimeService = HistorialRealtimeService.getInstance();

        initializeViews();
        setupRecyclerView();
        setupBottomNavigation();
        setupClickListeners();

        actualizarTituloPorDefecto();

        // Cargar datos iniciales
        loadInitialData();

        startAggressiveAutoRefresh();
        startMensajesNoLeidosChecker();
    }

    /**
     * Carga los datos iniciales una sola vez
     */
    private void loadInitialData() {
        if (isLoadingData) return;

        isLoadingData = true;
        showLoadingState();

        loadChatsDelUsuario();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Registrar listener del servicio de tiempo real
        historialRealtimeService.agregarListener(this);

        // Verificar mensajes no leídos sin recargar todo
        verificarTodosLosMensajesNoLeidos();

        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_historial);
        }

        startAggressiveAutoRefresh();
        startMensajesNoLeidosChecker();

        // Forzar carga de hospitales si es necesario
        if (historialAdapter != null) {
            new Handler().postDelayed(() -> {
                historialAdapter.cargarTodosLosHospitales();
            }, 1000);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        historialRealtimeService.removerListener(this);
        stopAutoRefresh();
        stopMensajesNoLeidosChecker();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        historialRealtimeService.removerListener(this);
        stopAutoRefresh();
        stopMensajesNoLeidosChecker();
    }

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

    private void actualizarTituloPorDefecto() {
        if ("activa".equals(currentEstado) && "todas".equals(currentRol)) {
            textTitleHistorial.setText(R.string.solicitudes_activas);
            textFilterIndicator.setVisibility(View.GONE);
        }
    }

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

        // Forzar carga de hospitales después de un delay
        recyclerViewHistorial.postDelayed(() -> {
            if (historialAdapter != null) {
                historialAdapter.cargarTodosLosHospitales();
            }
        }, 500);
    }

    private void setupClickListeners() {
        btnFilterHistorial.setOnClickListener(v -> {
            mostrarDialogoFiltroHistorial();
        });
    }

    private void mostrarDialogoFiltroHistorial() {
        HistorialFiltroDialog dialog = new HistorialFiltroDialog(this, this);
        dialog.setFiltrosActuales(currentEstado, currentRol);
        dialog.show();
    }

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
     * Actualización agresiva solo de UI, no de datos
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

    private void forceImmediateRedraw() {
        if (historialAdapter != null) {
            // Solo actualizar la vista, no los datos
            recyclerViewHistorial.invalidate();
            recyclerViewHistorial.post(() -> {
                recyclerViewHistorial.requestLayout();
            });
        }
    }

    private void stopAutoRefresh() {
        if (autoRefreshHandler != null && autoRefreshRunnable != null) {
            autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        }
    }

    private void startMensajesNoLeidosChecker() {
        if (mensajesNoLeidosHandler != null) {
            mensajesNoLeidosHandler.removeCallbacks(mensajesNoLeidosRunnable);
        }

        mensajesNoLeidosHandler = new Handler();
        mensajesNoLeidosRunnable = new Runnable() {
            @Override
            public void run() {
                // Solo verificar mensajes, no recargar datos completos
                verificarTodosLosMensajesNoLeidos();
                mensajesNoLeidosHandler.postDelayed(this, MENSAJES_CHECK_INTERVAL);
            }
        };
        mensajesNoLeidosHandler.postDelayed(mensajesNoLeidosRunnable, MENSAJES_CHECK_INTERVAL);
    }

    private void stopMensajesNoLeidosChecker() {
        if (mensajesNoLeidosHandler != null && mensajesNoLeidosRunnable != null) {
            mensajesNoLeidosHandler.removeCallbacks(mensajesNoLeidosRunnable);
        }
    }

    private void verificarTodosLosMensajesNoLeidos() {
        if (usuarioActual == null || chatsDelUsuario.isEmpty()) {
            return;
        }

        final int[] chatsVerificados = {0};
        final int totalChats = chatsDelUsuario.size();
        final boolean[] huboCambios = {false};

        for (Chat chat : chatsDelUsuario) {
            ApiService.getMensajesByChat(chat.getChatid(), new ApiService.ListCallback<Mensaje>() {
                @Override
                public void onSuccess(List<Mensaje> mensajes) {
                    runOnUiThread(() -> {
                        if (mensajes != null) {
                            boolean tieneMensajesNoLeidos = false;

                            for (Mensaje mensaje : mensajes) {
                                if (!mensaje.isLeido() && mensaje.getEmisorioid() != usuarioActual.getUsuarioid()) {
                                    tieneMensajesNoLeidos = true;
                                    break;
                                }
                            }

                            // Verificar si cambió el estado
                            boolean estadoAnterior = mensajesNoLeidosPorSolicitud.getOrDefault(chat.getSolicitudid(), false);
                            if (estadoAnterior != tieneMensajesNoLeidos) {
                                huboCambios[0] = true;
                            }

                            mensajesNoLeidosPorSolicitud.put(chat.getSolicitudid(), tieneMensajesNoLeidos);
                        }

                        chatsVerificados[0]++;
                        if (chatsVerificados[0] == totalChats && huboCambios[0]) {
                            aplicarFiltros(); // Reordenar solo si hubo cambios
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        boolean estadoAnterior = mensajesNoLeidosPorSolicitud.getOrDefault(chat.getSolicitudid(), false);
                        if (estadoAnterior != false) {
                            huboCambios[0] = true;
                        }
                        mensajesNoLeidosPorSolicitud.put(chat.getSolicitudid(), false);

                        chatsVerificados[0]++;
                        if (chatsVerificados[0] == totalChats && huboCambios[0]) {
                            aplicarFiltros(); // Reordenar solo si hubo cambios
                        }
                    });
                }
            });
        }
    }

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

    private void loadTodasLasSolicitudesRelevantes() {
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

    private void cargarSolicitudesDeOtrosUsuarios() {
        Set<Integer> solicitudIdsDeOtros = new HashSet<>();

        for (Chat chat : chatsDelUsuario) {
            if (chat.getUsuario1id() == usuarioActual.getUsuarioid()) {
                solicitudIdsDeOtros.add(chat.getSolicitudid());
            }
        }

        if (solicitudIdsDeOtros.isEmpty()) {
            finalizarCargaDatos();
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
            finalizarCargaDatos();
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
                            finalizarCargaDatos();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        solicitudesCargadas[0]++;
                        if (solicitudesCargadas[0] == totalSolicitudes) {
                            finalizarCargaDatos();
                        }
                    });
                }
            });
        }
    }

    /**
     * Finaliza la carga de datos y aplica filtros
     */
    private void finalizarCargaDatos() {
        isLoadingData = false;
        lastDataLoadTime = System.currentTimeMillis();
        aplicarFiltros();

        // Cargar hospitales inmediatamente después de aplicar filtros
        new Handler().postDelayed(() -> {
            if (historialAdapter != null) {
                historialAdapter.cargarTodosLosHospitales();
            }
        }, 100);
    }

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

        // ORDENAR: Primero las que tienen mensajes no leídos, luego las demás
        solicitudesFiltradas.sort((s1, s2) -> {
            boolean s1TieneMensajesNoLeidos = mensajesNoLeidosPorSolicitud.getOrDefault(s1.getSolicitudid(), false);
            boolean s2TieneMensajesNoLeidos = mensajesNoLeidosPorSolicitud.getOrDefault(s2.getSolicitudid(), false);

            if (s1TieneMensajesNoLeidos && !s2TieneMensajesNoLeidos) {
                return -1; // s1 va primero
            } else if (!s1TieneMensajesNoLeidos && s2TieneMensajesNoLeidos) {
                return 1; // s2 va primero
            } else {
                // Si ambas tienen o no tienen mensajes no leídos, mantener orden original
                return 0;
            }
        });
        //Collections.reverse(solicitudesFiltradas);

        if (historialAdapter != null) {
            historialAdapter.actualizarListaSolicitudes(solicitudesFiltradas);
            historialAdapter.actualizarInfoChats(chatsDelUsuario, mensajesNoLeidosPorSolicitud);
        }

        updateUIState();
        mostrarIndicadorFiltros();
    }

    private boolean esDonanteEnSolicitud(int solicitudId) {
        for (Chat chat : chatsDelUsuario) {
            if (chat.getSolicitudid() == solicitudId &&
                    chat.getUsuario1id() == usuarioActual.getUsuarioid()) {
                return true;
            }
        }
        return false;
    }

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

    private String convertirEstadoANombre(String estado) {
        switch (estado) {
            case "activa": return getString(R.string.estado_activas);
            case "completada": return getString(R.string.estado_completadas);
            case "cancelada": return getString(R.string.estado_canceladas);
            case "todas": return getString(R.string.estado_todas);
            default: return estado;
        }
    }

    private String convertirRolANombre(String rol) {
        switch (rol) {
            case "receptor": return getString(R.string.rol_receptor);
            case "donante": return getString(R.string.rol_donante);
            case "todas": return getString(R.string.rol_todos);
            default: return rol;
        }
    }

    @Override
    public void onAplicarFiltros(String estado, String rol) {
        currentEstado = estado;
        currentRol = rol;
        aplicarFiltros();
        Toast.makeText(this, R.string.filtros_aplicados, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLimpiarFiltros() {
        currentEstado = "activa";
        currentRol = "todas";
        aplicarFiltros();
        Toast.makeText(this, R.string.filtros_limpiados, Toast.LENGTH_SHORT).show();
    }

    private void navigateToMensajeriaWithFilter(int solicitudId, String estadoSolicitud) {
        Intent intent = new Intent(this, Mensajeria.class);
        intent.putExtra("filter_by_solicitud", true);
        intent.putExtra("solicitud_id", solicitudId);
        intent.putExtra("solicitud_estado", estadoSolicitud);
        startActivity(intent);
    }

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

    private void eliminarDelHistorial(SolicitudDonacion solicitud, int position) {
        showLoadingState();

        ApiService.updateSolicitudEstado(solicitud.getSolicitudid(), "cancelada", new ApiService.ApiCallback<SolicitudDonacion>() {
            @Override
            public void onSuccess(SolicitudDonacion result) {
                runOnUiThread(() -> {
                    historialRealtimeService.notificarCambioEstado(solicitud.getSolicitudid(), "cancelada");

                    // Actualizar localmente sin recargar todo
                    for (SolicitudDonacion s : todasLasSolicitudes) {
                        if (s.getSolicitudid() == solicitud.getSolicitudid()) {
                            s.setEstado("cancelada");
                            break;
                        }
                    }
                    aplicarFiltros();

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

    private void completarDelHistorial(SolicitudDonacion solicitud, int position) {
        showLoadingState();

        ApiService.updateSolicitudEstado(solicitud.getSolicitudid(), "completada", new ApiService.ApiCallback<SolicitudDonacion>() {
            @Override
            public void onSuccess(SolicitudDonacion result) {
                runOnUiThread(() -> {
                    historialRealtimeService.notificarCambioEstado(solicitud.getSolicitudid(), "completada");

                    // Actualizar localmente sin recargar todo
                    for (SolicitudDonacion s : todasLasSolicitudes) {
                        if (s.getSolicitudid() == solicitud.getSolicitudid()) {
                            s.setEstado("completada");
                            break;
                        }
                    }
                    aplicarFiltros();

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

    private void verificarMensajesNoLeidosEnChat(Chat chat, boolean updateUIInmediato) {
        if (chat == null) return;

        ApiService.getMensajesByChat(chat.getChatid(), new ApiService.ListCallback<Mensaje>() {
            @Override
            public void onSuccess(List<Mensaje> mensajes) {
                runOnUiThread(() -> {
                    if (mensajes != null) {
                        boolean tieneMensajesNoLeidos = false;

                        for (Mensaje mensaje : mensajes) {
                            if (!mensaje.isLeido() && mensaje.getEmisorioid() != usuarioActual.getUsuarioid()) {
                                tieneMensajesNoLeidos = true;
                                break;
                            }
                        }

                        mensajesNoLeidosPorSolicitud.put(chat.getSolicitudid(), tieneMensajesNoLeidos);

                        historialRealtimeService.notificarNuevosMensajes(chat.getSolicitudid(), tieneMensajesNoLeidos);

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

    private void actualizarUIconMensajesNoLeidos() {
        if (historialAdapter != null) {
            historialAdapter.actualizarInfoChats(chatsDelUsuario, mensajesNoLeidosPorSolicitud);
        }
    }

    // ========== IMPLEMENTACIÓN DE HISTORIALREALTIMESERVICE ==========

    @Override
    public void onSolicitudActualizada(SolicitudDonacion solicitud) {
        runOnUiThread(() -> {
            // Actualizar silenciosamente sin recargar todo
            for (int i = 0; i < todasLasSolicitudes.size(); i++) {
                if (todasLasSolicitudes.get(i).getSolicitudid() == solicitud.getSolicitudid()) {
                    todasLasSolicitudes.set(i, solicitud);
                    aplicarFiltros();
                    break;
                }
            }
        });
    }

    @Override
    public void onSolicitudEstadoCambiado(int solicitudId, String nuevoEstado) {
        runOnUiThread(() -> {
            // Actualizar solo el estado
            for (SolicitudDonacion solicitud : todasLasSolicitudes) {
                if (solicitud.getSolicitudid() == solicitudId) {
                    solicitud.setEstado(nuevoEstado);
                    aplicarFiltros();
                    break;
                }
            }
        });
    }

    @Override
    public void onNuevosMensajes(int solicitudId, boolean tieneMensajesNoLeidos) {
        runOnUiThread(() -> {
            mensajesNoLeidosPorSolicitud.put(solicitudId, tieneMensajesNoLeidos);
            actualizarUIconMensajesNoLeidos();
        });
    }

    @Override
    public void onError(String error) {
        Log.e("HistorialRealtime", error);
    }

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