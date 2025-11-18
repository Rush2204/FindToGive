package sv.edu.catolica.findtogive.ClasesDiseño;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.ApiService;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.ChatsAdapter;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.SharedPreferencesManager;
import sv.edu.catolica.findtogive.Modelado.Chat;
import sv.edu.catolica.findtogive.Modelado.Mensaje;
import sv.edu.catolica.findtogive.Modelado.Usuario;
import sv.edu.catolica.findtogive.R;

public class Mensajeria extends AppCompatActivity {

    private RecyclerView recyclerViewChats;
    private LinearLayout layoutEmptyState;
    private ChatsAdapter chatsAdapter;
    private List<Chat> chatsList;
    private Usuario usuarioActual;

    private Handler autoRefreshHandler;
    private Handler messagesPollingHandler;
    private Runnable autoRefreshRunnable;
    private Runnable messagesPollingRunnable;
    private static final long AUTO_REFRESH_INTERVAL = 100;
    private static final long MESSAGES_POLLING_INTERVAL = 5000;

    // Variables para filtro
    private boolean filterBySolicitud = false;
    private int filteredSolicitudId = -1;

    /**
     * Método principal que inicializa la actividad de mensajería
     * Configura la vista, filtros, navegación y carga los chats
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.desing_mensajeria);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent intent = getIntent();
        if (intent != null) {
            filterBySolicitud = intent.getBooleanExtra("filter_by_solicitud", false);
            filteredSolicitudId = intent.getIntExtra("solicitud_id", -1);
        }

        usuarioActual = SharedPreferencesManager.getCurrentUser(this);
        initializeViews();
        setupRecyclerView();
        setupBottomNavigation();

        updateTitle();
        loadChats();

        startTimeRefresh();
        startMessagesPolling();
    }

    /**
     * Inicializa todos los componentes visuales de la interfaz
     * Obtiene referencias a los views del layout
     */
    private void initializeViews() {
        recyclerViewChats = findViewById(R.id.recycler_view_chats_list);
        layoutEmptyState = findViewById(R.id.layout_empty_state_chats);
    }

    /**
     * Configura el RecyclerView para mostrar la lista de chats
     * Inicializa el adapter y establece el layout manager
     */
    private void setupRecyclerView() {
        chatsList = new ArrayList<>();
        chatsAdapter = new ChatsAdapter(chatsList, this);

        chatsAdapter.setOnFotoPerfilClickListener(this::mostrarInfoUsuario);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewChats.setLayoutManager(layoutManager);
        recyclerViewChats.setAdapter(chatsAdapter);
        recyclerViewChats.setHasFixedSize(false);
    }

    /**
     * Muestra la información del usuario en un diálogo modal
     * @param usuario Usuario cuya información se mostrará
     */
    private void mostrarInfoUsuario(Usuario usuario) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_info_usuario);
        dialog.setCancelable(true);

        ImageView imageFotoPerfil = dialog.findViewById(R.id.image_foto_perfil);
        TextView textNombreCompleto = dialog.findViewById(R.id.text_nombre_completo);
        TextView textEdad = dialog.findViewById(R.id.text_edad);
        TextView textTipoSangre = dialog.findViewById(R.id.text_tipo_sangre);
        TextView textRol = dialog.findViewById(R.id.text_rol);
        Button buttonCerrar = dialog.findViewById(R.id.button_cerrar);

        textNombreCompleto.setText(usuario.getNombreCompleto());
        textEdad.setText(getString(R.string.edad_formato, usuario.getEdad()));
        textTipoSangre.setText(getString(R.string.tipo_sangre_formato, obtenerTipoSangre(usuario.getTiposangreid())));
        textRol.setText(getString(R.string.rol_formato, obtenerRol(usuario.getRolid())));

        if (usuario.getFotoUrl() != null && !usuario.getFotoUrl().isEmpty()) {
            Glide.with(this)
                    .load(usuario.getFotoUrl())
                    .placeholder(R.drawable.ico_logo_findtogive)
                    .error(R.drawable.ico_logo_findtogive)
                    .apply(RequestOptions.circleCropTransform())
                    .into(imageFotoPerfil);
        } else {
            imageFotoPerfil.setImageResource(R.drawable.ico_logo_findtogive);
        }

        buttonCerrar.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * Convierte un ID de tipo de sangre a su nombre correspondiente
     * @param tipoSangreId ID del tipo de sangre
     * @return Nombre del tipo de sangre (A+, A-, B+, etc.)
     */
    private String obtenerTipoSangre(int tipoSangreId) {
        switch (tipoSangreId) {
            case 1: return "A+";
            case 2: return "A-";
            case 3: return "B+";
            case 4: return "B-";
            case 5: return "AB+";
            case 6: return "AB-";
            case 7: return "O+";
            case 8: return "O-";
            default: return getString(R.string.desconocido);
        }
    }

    /**
     * Maneja nuevos intents cuando la actividad ya está creada
     * @param intent Nuevo intent recibido
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);

        filterBySolicitud = intent.getBooleanExtra("filter_by_solicitud", false);
        filteredSolicitudId = intent.getIntExtra("solicitud_id", -1);

        updateTitle();
        loadChats();
    }

    /**
     * Convierte un ID de rol a su nombre correspondiente
     * @param rolId ID del rol
     * @return Nombre del rol (Donante, Receptor, Ambos)
     */
    private String obtenerRol(int rolId) {
        switch (rolId) {
            case 1: return getString(R.string.donante);
            case 2: return getString(R.string.receptor);
            case 3: return getString(R.string.rol_ambos);
            default: return getString(R.string.desconocido);
        }
    }

    /**
     * Configura la navegación inferior de la aplicación
     * Define las acciones para cada ítem del menú de navegación
     */
    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation_bar);

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
     * Carga los chats del usuario desde la API
     * Aplica filtros si están activos para mostrar chats específicos
     */
    private void loadChats() {
        if (usuarioActual == null) {
            return;
        }

        ApiService.getChatsByUsuario(usuarioActual.getUsuarioid(), new ApiService.ListCallback<Chat>() {
            @Override
            public void onSuccess(List<Chat> chats) {
                runOnUiThread(() -> {
                    if (chats != null && !chats.isEmpty()) {
                        List<Chat> filteredChats = chats;
                        if (filterBySolicitud && filteredSolicitudId != -1) {
                            filteredChats = new ArrayList<>();
                            for (Chat chat : chats) {
                                if (chat.getSolicitudid() == filteredSolicitudId) {
                                    filteredChats.add(chat);
                                }
                            }
                        }

                        chatsAdapter.actualizarChats(filteredChats);
                        showChatsList();
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
     * Actualiza el título de la actividad según el contexto
     * Configura diferentes títulos y comportamientos según los filtros activos
     */
    private void updateTitle() {
        TextView textTitle = findViewById(R.id.text_title_mensajeria);
        ImageView btnBack = findViewById(R.id.btn_back);

        if (filterBySolicitud && filteredSolicitudId != -1) {
            textTitle.setText(R.string.chats_solicitud);
            btnBack.setOnClickListener(v -> {
                Intent intent = new Intent(Mensajeria.this, HistorialDonaciones.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        } else {
            textTitle.setText(R.string.mensajeria);
            btnBack.setOnClickListener(v -> {
                Intent intent = new Intent(Mensajeria.this, HistorialDonaciones.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }
    }

    /**
     * Actualiza los mensajes para todos los chats existentes
     * Verifica si hay mensajes nuevos en cada chat activo
     */
    private void refreshMessagesForAllChats() {
        if (chatsList == null || chatsList.isEmpty()) {
            return;
        }

        for (Chat chat : chatsList) {
            refreshMessagesForChat(chat.getChatid());
        }
    }

    /**
     * Actualiza los mensajes para un chat específico
     * @param chatId ID del chat a actualizar
     */
    private void refreshMessagesForChat(int chatId) {
        ApiService.getMensajesByChat(chatId, new ApiService.ListCallback<Mensaje>() {
            @Override
            public void onSuccess(List<Mensaje> mensajes) {
                runOnUiThread(() -> {
                    if (mensajes != null && !mensajes.isEmpty()) {
                        Mensaje ultimoMensaje = mensajes.get(0);
                        for (Mensaje mensaje : mensajes) {
                            if (mensaje.getMensajeid() > ultimoMensaje.getMensajeid()) {
                                ultimoMensaje = mensaje;
                            }
                        }

                        String contenido = ultimoMensaje.getContenido();
                        String fechaEnvio = ultimoMensaje.getFechaEnvio();
                        int ultimoMensajeId = ultimoMensaje.getMensajeid();

                        chatsAdapter.actualizarChatSiEsNecesario(chatId, contenido, fechaEnvio, ultimoMensajeId);
                    }
                });
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    /**
     * Inicia la actualización automática de tiempos en los chats
     * Actualiza las marcas de tiempo (hace x minutos/horas) periódicamente
     */
    private void startTimeRefresh() {
        autoRefreshHandler = new Handler();
        autoRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (chatsAdapter != null) {
                    chatsAdapter.actualizarTiempos();
                }
                autoRefreshHandler.postDelayed(this, AUTO_REFRESH_INTERVAL);
            }
        };
        autoRefreshHandler.post(autoRefreshRunnable);
    }

    /**
     * Inicia el polling para verificar mensajes nuevos
     * Consulta periódicamente si hay nuevos mensajes en los chats activos
     */
    private void startMessagesPolling() {
        messagesPollingHandler = new Handler();
        messagesPollingRunnable = new Runnable() {
            @Override
            public void run() {
                refreshMessagesForAllChats();
                messagesPollingHandler.postDelayed(this, MESSAGES_POLLING_INTERVAL);
            }
        };
        messagesPollingHandler.postDelayed(messagesPollingRunnable, MESSAGES_POLLING_INTERVAL);
    }

    /**
     * Detiene todos los handlers de actualización automática
     * Previene fugas de memoria al eliminar callbacks pendientes
     */
    private void stopAllHandlers() {
        if (autoRefreshHandler != null && autoRefreshRunnable != null) {
            autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        }
        if (messagesPollingHandler != null && messagesPollingRunnable != null) {
            messagesPollingHandler.removeCallbacks(messagesPollingRunnable);
        }
    }

    /**
     * Muestra la lista de chats (oculta estado vacío)
     */
    private void showChatsList() {
        recyclerViewChats.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
    }

    /**
     * Muestra el estado vacío (oculta lista de chats)
     */
    private void showEmptyState() {
        recyclerViewChats.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);
    }

    /**
     * Método del ciclo de vida que se ejecuta al reanudar la actividad
     * Reactiva las actualizaciones automáticas y recarga los chats
     */
    @Override
    protected void onResume() {
        super.onResume();

        updateTitle();
        loadChats();
        startTimeRefresh();
        startMessagesPolling();
    }

    /**
     * Método del ciclo de vida que se ejecuta al pausar la actividad
     * Detiene las actualizaciones automáticas para ahorrar recursos
     */
    @Override
    protected void onPause() {
        super.onPause();
        stopAllHandlers();
    }

    /**
     * Método del ciclo de vida que se ejecuta al destruir la actividad
     * Limpia recursos y detiene todos los handlers
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAllHandlers();
    }

    /**
     * Método del ciclo de vida que se ejecuta al detener la actividad
     * Detiene las actualizaciones automáticas
     */
    @Override
    protected void onStop() {
        super.onStop();
        stopAllHandlers();
    }

    /**
     * Maneja la navegación hacia atrás desde la mensajería
     * @param view Vista que disparó el evento
     */
    public void back(View view) {
        Intent intent = new Intent(Mensajeria.this, HistorialDonaciones.class);
        startActivity(intent);
        finish();
    }
}