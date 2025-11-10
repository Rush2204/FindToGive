package sv.edu.catolica.findtogive.ClasesDiseño;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
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

    private static final String TAG = "HistorialDonaciones";

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
    //Mapa para trackear mensajes no leídos por solicitud
    private Map<Integer, Boolean> mensajesNoLeidosPorSolicitud;

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
            Toast.makeText(this, "Debe iniciar sesión.", Toast.LENGTH_LONG).show();
            navigateToLogin();
            return;
        }

        // INICIALIZAR EL MAPA QUE FALTABA
        mensajesNoLeidosPorSolicitud = new HashMap<>(); // ESTA LÍNEA FALTABA

        initializeViews();
        setupRecyclerView();
        setupBottomNavigation();
        setupClickListeners();

        // NUEVO: Actualizar el título para reflejar el filtro por defecto
        actualizarTituloPorDefecto();

        loadChatsDelUsuario();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (usuarioActual != null) {
            loadChatsDelUsuario();
        }

        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_historial);
        }
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
            textTitleHistorial.setText("Solicitudes Activas");
            textFilterIndicator.setVisibility(View.GONE);
        }
    }

    private void setupRecyclerView() {
        // MODIFICADO: Pasar usuarioActual al adapter
        historialAdapter = new HistorialAdapter(solicitudList, this, this, usuarioActual);
        historialAdapter.setOnItemClickListener(new HistorialAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(SolicitudDonacion solicitud) {
                navigateToMensajeriaWithFilter(solicitud.getSolicitudid(), solicitud.getEstado());
            }
        });
        recyclerViewHistorial.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewHistorial.setAdapter(historialAdapter);
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
                return true;
            } else if (itemId == R.id.nav_historial) {
                return true;
            } else if (itemId == R.id.nav_perfil) {
                startActivity(new Intent(this, PerfilUsuario.class));
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
     * Cargar los chats del usuario para el filtro de donante
     */
    private void loadChatsDelUsuario() {
        ApiService.getChatsByUsuario(usuarioActual.getUsuarioid(), new ApiService.ListCallback<Chat>() {
            @Override
            public void onSuccess(List<Chat> chats) {
                runOnUiThread(() -> {
                    if (chats != null) {
                        chatsDelUsuario.clear();
                        chatsDelUsuario.addAll(chats);
                        Log.d(TAG, "✅ " + chatsDelUsuario.size() + " chats cargados para filtro de donante");

                        // NUEVO: Cargar información de mensajes no leídos
                        cargarMensajesNoLeidos();

                        // Log detallado de los chats
                        for (Chat chat : chatsDelUsuario) {
                            Log.d(TAG, "💬 Chat ID: " + chat.getChatid() +
                                    ", Solicitud: " + chat.getSolicitudid() +
                                    ", Usuario1: " + chat.getUsuario1id() +
                                    ", Usuario2: " + chat.getUsuario2id() +
                                    ", Yo soy: " + (chat.getUsuario1id() == usuarioActual.getUsuarioid() ? "Usuario1 (Donante)" : "Usuario2 (Receptor)"));
                        }
                    } else {
                        Log.d(TAG, "⚠️ No se encontraron chats para el usuario");
                    }
                    // Cargar TODAS las solicitudes necesarias (propias y de otros)
                    loadTodasLasSolicitudesRelevantes();
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error cargando chats: " + error);
                runOnUiThread(() -> {
                    loadTodasLasSolicitudesRelevantes();
                });
            }
        });
    }

    /**
     * Carga TODAS las solicitudes relevantes: las del usuario + las de otros donde tiene chats
     */
    private void loadTodasLasSolicitudesRelevantes() {
        showLoadingState();

        // Primero cargar las solicitudes del usuario actual
        ApiService.getSolicitudesByUsuarioId(usuarioActual.getUsuarioid(), new ApiService.ListCallback<SolicitudDonacion>() {
            @Override
            public void onSuccess(List<SolicitudDonacion> misSolicitudes) {
                runOnUiThread(() -> {
                    todasLasSolicitudes.clear();

                    if (misSolicitudes != null) {
                        todasLasSolicitudes.addAll(misSolicitudes);
                        Log.d(TAG, "✅ " + misSolicitudes.size() + " solicitudes propias cargadas");
                    }

                    // Ahora cargar las solicitudes de otros usuarios donde el usuario actual tiene chats
                    cargarSolicitudesDeOtrosUsuarios();
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error cargando solicitudes propias: " + error);
                runOnUiThread(() -> {
                    cargarSolicitudesDeOtrosUsuarios();
                });
            }
        });
    }

    /**
     * Carga las solicitudes de otros usuarios donde el usuario actual tiene chats
     */
    private void cargarSolicitudesDeOtrosUsuarios() {
        // Obtener IDs únicos de solicitudes de otros usuarios donde tenemos chats
        Set<Integer> solicitudIdsDeOtros = new HashSet<>();

        for (Chat chat : chatsDelUsuario) {
            // Solo considerar chats donde el usuario actual es usuario1id (inició el chat)
            if (chat.getUsuario1id() == usuarioActual.getUsuarioid()) {
                solicitudIdsDeOtros.add(chat.getSolicitudid());
            }
        }

        Log.d(TAG, "🔍 Buscando " + solicitudIdsDeOtros.size() + " solicitudes de otros usuarios con chats");

        if (solicitudIdsDeOtros.isEmpty()) {
            aplicarFiltros();
            return;
        }

        // VERIFICAR DUPLICADOS: Eliminar IDs que ya están en las solicitudes propias
        Set<Integer> idsSolicitudesPropias = new HashSet<>();
        for (SolicitudDonacion solicitud : todasLasSolicitudes) {
            idsSolicitudesPropias.add(solicitud.getSolicitudid());
        }

        // Filtrar solo las solicitudes que NO están ya en la lista
        Set<Integer> solicitudIdsUnicos = new HashSet<>();
        for (int solicitudId : solicitudIdsDeOtros) {
            if (!idsSolicitudesPropias.contains(solicitudId)) {
                solicitudIdsUnicos.add(solicitudId);
            } else {
                Log.d(TAG, "⚠️ Omitiendo solicitud duplicada: " + solicitudId);
            }
        }

        Log.d(TAG, "📋 Solicitudes únicas a cargar: " + solicitudIdsUnicos.size() + " de " + solicitudIdsDeOtros.size());

        if (solicitudIdsUnicos.isEmpty()) {
            aplicarFiltros();
            return;
        }

        // Cargar cada solicitud individualmente
        final int[] solicitudesCargadas = {0};
        final int totalSolicitudes = solicitudIdsUnicos.size();

        for (int solicitudId : solicitudIdsUnicos) {
            cargarSolicitudPorId(solicitudId, new ApiService.ApiCallback<SolicitudDonacion>() {
                @Override
                public void onSuccess(SolicitudDonacion solicitud) {
                    runOnUiThread(() -> {
                        if (solicitud != null) {
                            // VERIFICACIÓN FINAL: Asegurar que no sea duplicada
                            boolean esDuplicada = false;
                            for (SolicitudDonacion existente : todasLasSolicitudes) {
                                if (existente.getSolicitudid() == solicitud.getSolicitudid()) {
                                    esDuplicada = true;
                                    Log.d(TAG, "🚫 Solicitud duplicada detectada y omitida: " + solicitud.getSolicitudid());
                                    break;
                                }
                            }

                            if (!esDuplicada) {
                                todasLasSolicitudes.add(solicitud);
                                Log.d(TAG, "✅ Solicitud de otro usuario cargada: ID " + solicitud.getSolicitudid() +
                                        " - Creada por usuario: " + solicitud.getUsuarioid() +
                                        " - Estado: " + solicitud.getEstado());
                            }
                        }

                        solicitudesCargadas[0]++;
                        if (solicitudesCargadas[0] == totalSolicitudes) {
                            Log.d(TAG, "📊 Total de solicitudes cargadas: " + todasLasSolicitudes.size() +
                                    " (propias: " + (todasLasSolicitudes.size() - solicitudIdsUnicos.size()) +
                                    ", de otros: " + solicitudIdsUnicos.size() + ")");
                            aplicarFiltros();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "❌ Error cargando solicitud: " + error);
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
     */
    private void cargarSolicitudPorId(int solicitudId, ApiService.ApiCallback<SolicitudDonacion> callback) {
        // Usar directamente el método que ya existe en ApiService
        ApiService.getSolicitudById(solicitudId, new ApiService.ApiCallback<SolicitudDonacion>() {
            @Override
            public void onSuccess(SolicitudDonacion solicitud) {
                callback.onSuccess(solicitud);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error cargando solicitud " + solicitudId + ": " + error);
                // Si falla, intentar buscar en todas las solicitudes activas como fallback
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

    /**
     * Aplica los filtros actuales a la lista de solicitudes
     */
    /**
     * Aplica los filtros actuales a la lista de solicitudes
     */
    private void aplicarFiltros() {
        solicitudList.clear();

        for (SolicitudDonacion solicitud : todasLasSolicitudes) {
            boolean cumpleEstado = true;
            boolean cumpleRol = true;

            // Filtrar por estado
            if (!"todas".equals(currentEstado)) {
                cumpleEstado = currentEstado.equalsIgnoreCase(solicitud.getEstado());
            }

            // Filtrar por rol (receptor vs donante)
            if (!"todas".equals(currentRol)) {
                if ("receptor".equals(currentRol)) {
                    // El usuario actual es el creador de la solicitud
                    cumpleRol = solicitud.getUsuarioid() == usuarioActual.getUsuarioid();
                } else if ("donante".equals(currentRol)) {
                    // CORRECCIÓN: El usuario actual es DONANTE en esta solicitud:
                    // - NO es el creador de la solicitud
                    // - Y tiene un chat donde usuario1id = usuarioActual.getUsuarioid() (él inició el chat como donante)
                    boolean esCreador = solicitud.getUsuarioid() == usuarioActual.getUsuarioid();
                    boolean esDonanteEnChat = esDonanteEnSolicitud(solicitud.getSolicitudid());
                    cumpleRol = !esCreador && esDonanteEnChat;

                    if (cumpleRol) {
                        Log.d(TAG, "🎯 Solicitud de donante encontrada: ID " + solicitud.getSolicitudid() +
                                " - Creada por: " + solicitud.getUsuarioid() + ", Yo: " + usuarioActual.getUsuarioid());
                    }
                }
            }

            if (cumpleEstado && cumpleRol) {
                solicitudList.add(solicitud);
            }
        }

        Log.d(TAG, "🔍 Filtros aplicados - Estado: " + currentEstado + ", Rol: " + currentRol);
        Log.d(TAG, "📊 Resultado: " + solicitudList.size() + " de " + todasLasSolicitudes.size() + " solicitudes");

        // NUEVO: Actualizar el adapter con la información de chats y mensajes no leídos
        if (historialAdapter != null) {
            historialAdapter.actualizarInfoChats(chatsDelUsuario, mensajesNoLeidosPorSolicitud);
        }

        historialAdapter.notifyDataSetChanged();
        updateUIState();
        mostrarIndicadorFiltros();
    }

    /**
     * Verifica si el usuario es DONANTE en la solicitud específica
     * (usuario1id = usuarioActual.getUsuarioid() en la tabla chat)
     */
    private boolean esDonanteEnSolicitud(int solicitudId) {
        for (Chat chat : chatsDelUsuario) {
            if (chat.getSolicitudid() == solicitudId &&
                    chat.getUsuario1id() == usuarioActual.getUsuarioid()) {
                Log.d(TAG, "✅ Es donante en solicitud " + solicitudId + " - Chat ID: " + chat.getChatid());
                return true;
            }
        }
        Log.d(TAG, "❌ NO es donante en solicitud " + solicitudId);
        return false;
    }

    private void mostrarIndicadorFiltros() {
        StringBuilder filtros = new StringBuilder();
        boolean tieneFiltros = false;

        // MODIFICADO: No considerar "activa" como filtro especial
        if (!"activa".equals(currentEstado)) {
            filtros.append("Estado: ").append(convertirEstadoANombre(currentEstado));
            tieneFiltros = true;
        }

        if (!"todas".equals(currentRol)) {
            if (tieneFiltros) filtros.append(" • ");
            filtros.append("Rol: ").append(convertirRolANombre(currentRol));
            tieneFiltros = true;
        }

        if (tieneFiltros) {
            textFilterIndicator.setText(filtros.toString());
            textFilterIndicator.setVisibility(View.VISIBLE);
            textTitleHistorial.setText("Mi Historial Filtrado");
        } else {
            textFilterIndicator.setVisibility(View.GONE);
            // MODIFICADO: Título diferente para el estado por defecto
            textTitleHistorial.setText("Solicitudes Activas");
        }
    }

    private String convertirEstadoANombre(String estado) {
        switch (estado) {
            case "activa": return "Activas";
            case "completada": return "Completadas";
            case "cancelada": return "Canceladas";
            case "todas": return "Todas";
            default: return estado;
        }
    }

    private String convertirRolANombre(String rol) {
        switch (rol) {
            case "receptor": return "Como Receptor";
            case "donante": return "Como Donante";
            case "todas": return "Todos";
            default: return rol;
        }
    }

    @Override
    public void onAplicarFiltros(String estado, String rol) {
        currentEstado = estado;
        currentRol = rol;
        aplicarFiltros();
        Toast.makeText(this, "Filtros aplicados", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLimpiarFiltros() {
        currentEstado = "activa";
        currentRol = "todas";
        aplicarFiltros();
        Toast.makeText(this, "Filtros limpiados", Toast.LENGTH_SHORT).show();
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
        // Validación adicional: asegurar que el usuario es el creador
        if (solicitud.getUsuarioid() != usuarioActual.getUsuarioid()) {
            Toast.makeText(this, "Solo el creador de la solicitud puede cancelarla", Toast.LENGTH_SHORT).show();
            return;
        }

        Snackbar.make(recyclerViewHistorial, "¿Desea cancelar esta solicitud?", Snackbar.LENGTH_LONG)
                .setAction("CANCELAR", view -> eliminarDelHistorial(solicitud, position))
                .show();
    }

    @Override
    public void onCompleteClick(SolicitudDonacion solicitud, int position) {
        // Validación adicional: asegurar que el usuario es el creador
        if (solicitud.getUsuarioid() != usuarioActual.getUsuarioid()) {
            Toast.makeText(this, "Solo el creador de la solicitud puede completarla", Toast.LENGTH_SHORT).show();
            return;
        }

        Snackbar.make(recyclerViewHistorial, "¿Marcar esta solicitud como completada?", Snackbar.LENGTH_LONG)
                .setAction("COMPLETAR", view -> completarDelHistorial(solicitud, position))
                .show();
    }

    private void eliminarDelHistorial(SolicitudDonacion solicitud, int position) {
        showLoadingState();
        Log.d(TAG, "🔄 Cambiando estado de solicitud " + solicitud.getSolicitudid() + " a: cancelada");

        ApiService.updateSolicitudEstado(solicitud.getSolicitudid(), "cancelada", new ApiService.ApiCallback<SolicitudDonacion>() {
            @Override
            public void onSuccess(SolicitudDonacion result) {
                runOnUiThread(() -> {
                    Log.d(TAG, "✅ Estado cambiado a 'cancelada' para solicitud ID: " + solicitud.getSolicitudid());
                    loadChatsDelUsuario(); // Recargar todo
                    Snackbar.make(recyclerViewHistorial, "Solicitud cancelada", Snackbar.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "❌ Error al cambiar estado: " + error);
                    Toast.makeText(HistorialDonaciones.this, "Error al cancelar solicitud: " + error, Toast.LENGTH_LONG).show();
                    updateUIState();
                });
            }
        });
    }

    /**
     * Cargar información de mensajes no leídos para cada solicitud
     */
    private void cargarMensajesNoLeidos() {
        if (usuarioActual == null) return;

        // Limpiar mapa anterior
        mensajesNoLeidosPorSolicitud.clear();

        // Para cada chat del usuario, verificar si hay mensajes no leídos
        for (Chat chat : chatsDelUsuario) {
            verificarMensajesNoLeidosEnChat(chat);
        }
    }

    private void verificarMensajesNoLeidosEnChat(Chat chat) {
        ApiService.getMensajesByChat(chat.getChatid(), new ApiService.ListCallback<Mensaje>() {
            @Override
            public void onSuccess(List<Mensaje> mensajes) {
                runOnUiThread(() -> {
                    if (mensajes != null) {
                        boolean tieneMensajesNoLeidos = false;

                        for (Mensaje mensaje : mensajes) {
                            // Un mensaje no leído es aquel que:
                            // 1. No está marcado como leído (leido = false)
                            // 2. No fue enviado por el usuario actual
                            if (!mensaje.isLeido() && mensaje.getEmisorioid() != usuarioActual.getUsuarioid()) {
                                tieneMensajesNoLeidos = true;
                                break;
                            }
                        }

                        // Actualizar el mapa
                        mensajesNoLeidosPorSolicitud.put(chat.getSolicitudid(), tieneMensajesNoLeidos);

                        // Notificar al adapter
                        if (historialAdapter != null) {
                            historialAdapter.actualizarInfoChats(chatsDelUsuario, mensajesNoLeidosPorSolicitud);
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error cargando mensajes para chat " + chat.getChatid() + ": " + error);
            }
        });
    }

    private void completarDelHistorial(SolicitudDonacion solicitud, int position) {
        showLoadingState();
        Log.d(TAG, "🔄 Cambiando estado de solicitud " + solicitud.getSolicitudid() + " a: completada");

        ApiService.updateSolicitudEstado(solicitud.getSolicitudid(), "completada", new ApiService.ApiCallback<SolicitudDonacion>() {
            @Override
            public void onSuccess(SolicitudDonacion result) {
                runOnUiThread(() -> {
                    Log.d(TAG, "✅ Estado cambiado a 'completada' para solicitud ID: " + solicitud.getSolicitudid());
                    loadChatsDelUsuario(); // Recargar todo
                    Snackbar.make(recyclerViewHistorial, "Solicitud completada", Snackbar.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(HistorialDonaciones.this, "Error al completar solicitud: " + error, Toast.LENGTH_LONG).show();
                    updateUIState();
                });
            }
        });
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