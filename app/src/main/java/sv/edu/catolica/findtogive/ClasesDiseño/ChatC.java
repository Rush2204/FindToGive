package sv.edu.catolica.findtogive.ClasesDiseño;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.ApiService;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.MensajesAdapter;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.SharedPreferencesManager;
import sv.edu.catolica.findtogive.Modelado.Mensaje;
import sv.edu.catolica.findtogive.Modelado.Notificacion;
import sv.edu.catolica.findtogive.Modelado.SolicitudDonacion;
import sv.edu.catolica.findtogive.Modelado.Usuario;
import sv.edu.catolica.findtogive.R;

public class ChatC extends AppCompatActivity {

    private TextView textChatName;

    private RecyclerView recyclerViewChat;
    private EditText editTextMessage;
    private ImageButton btnSend;

    private int chatId;
    private int solicitudId;
    private int otroUsuarioId;
    private String chatNombre;
    private String estadoSolicitud;

    private List<Mensaje> mensajesList;
    private MensajesAdapter mensajesAdapter;
    private Usuario usuarioActual;

    private Handler pollingHandler;
    private Runnable pollingRunnable;
    private static final long POLLING_INTERVAL = 1000;
    private SharedPreferences notificacionPrefs;

    private boolean mensajesMarcadosComoLeidos = false;

    /**
     * Método principal que inicializa la actividad del chat
     * Configura la vista, inicializa componentes y comienza la funcionalidad del chat
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.desing_chat);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        notificacionPrefs = getSharedPreferences("chat_notifications", Context.MODE_PRIVATE);

        initializeViews();

        if (!mensajesMarcadosComoLeidos) {
            marcarMensajesComoLeidos();
            mensajesMarcadosComoLeidos = true;
        }

        verificarEstadoSolicitud();
        setupRecyclerView();
        loadMensajes();
        startPolling();
    }

    /**
     * Inicializa todos los componentes visuales de la interfaz
     * Obtiene referencias a los views y configura los listeners de eventos
     */
    private void initializeViews() {
        textChatName = findViewById(R.id.text_chat_name);

        recyclerViewChat = findViewById(R.id.recycler_view_chat);
        editTextMessage = findViewById(R.id.edit_text_message);
        btnSend = findViewById(R.id.btn_send);

        chatId = getIntent().getIntExtra("chat_id", -1);
        solicitudId = getIntent().getIntExtra("solicitud_id", -1);
        otroUsuarioId = getIntent().getIntExtra("otro_usuario_id", -1);
        chatNombre = getIntent().getStringExtra("chat_nombre");
        estadoSolicitud = getIntent().getStringExtra("solicitud_estado");

        usuarioActual = SharedPreferencesManager.getCurrentUser(this);

        textChatName.setText(chatNombre != null ? chatNombre : "Chat");

        btnSend.setOnClickListener(v -> enviarMensaje());

        setupEditTextPaste();
    }

    /**
     * Configura el EditText para permitir funcionalidades de pegar texto
     * Habilita el menú contextual nativo de Android para copiar y pegar
     */
    private void setupEditTextPaste() {
        editTextMessage.setCustomSelectionActionModeCallback(new android.view.ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                return true;
            }

            @Override
            public boolean onPrepareActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                return true;
            }

            @Override
            public boolean onActionItemClicked(android.view.ActionMode mode, android.view.MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(android.view.ActionMode mode) {
            }
        });

        editTextMessage.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (editTextMessage.getText().toString().isEmpty()) {
                    Toast.makeText(ChatC.this, R.string.pegar_texto, Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });
    }

    /**
     * Verifica el estado actual de la solicitud de donación
     * Bloquea el envío de mensajes si la solicitud no está activa
     */
    private void verificarEstadoSolicitud() {
        if (estadoSolicitud != null && !"activa".equals(estadoSolicitud)) {
            bloquearEnvioMensajes();
            return;
        }

        if (solicitudId != -1) {
            consultarEstadoSolicitud();
        }
    }

    /**
     * Consulta el estado de la solicitud desde la API
     * Obtiene información actualizada sobre el estado de la solicitud de donación
     */
    private void consultarEstadoSolicitud() {
        ApiService.getSolicitudById(solicitudId, new ApiService.ApiCallback<SolicitudDonacion>() {
            @Override
            public void onSuccess(SolicitudDonacion solicitud) {
                if (solicitud != null) {
                    estadoSolicitud = solicitud.getEstado();
                    runOnUiThread(() -> {
                        if (!"activa".equals(estadoSolicitud)) {
                            bloquearEnvioMensajes();
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                consultarEstadoSolicitudAlternativo();
            }
        });
    }

    /**
     * Método alternativo para consultar el estado de la solicitud
     * Busca entre todas las solicitudes del usuario cuando el método principal falla
     */
    private void consultarEstadoSolicitudAlternativo() {
        ApiService.getSolicitudesByUsuarioId(usuarioActual.getUsuarioid(), new ApiService.ListCallback<SolicitudDonacion>() {
            @Override
            public void onSuccess(List<SolicitudDonacion> solicitudes) {
                if (solicitudes != null) {
                    for (SolicitudDonacion solicitud : solicitudes) {
                        if (solicitud.getSolicitudid() == solicitudId) {
                            estadoSolicitud = solicitud.getEstado();
                            runOnUiThread(() -> {
                                if (!"activa".equals(estadoSolicitud)) {
                                    bloquearEnvioMensajes();
                                }
                            });
                            break;
                        }
                    }
                }
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    /**
     * Bloquea la interfaz para enviar mensajes cuando la solicitud no está activa
     * Deshabilita el EditText y botón de enviar, mostrando el estado actual
     */
    private void bloquearEnvioMensajes() {
        runOnUiThread(() -> {
            editTextMessage.setEnabled(false);
            editTextMessage.setHint(getString(R.string.chat_cerrado_solicitud) +
                    ("completada".equals(estadoSolicitud) ? "completada ✅" : "cancelada ❌"));
            editTextMessage.setBackgroundColor(0xFFF0F0F0);

            btnSend.setEnabled(false);
            btnSend.setAlpha(0.3f);
            btnSend.setClickable(false);

            String mensajeEstado = "completada".equals(estadoSolicitud) ?
                    "completada ✅" : "cancelada ❌";
            Toast.makeText(this,
                    getString(R.string.esta_solicitud) + mensajeEstado + getString(R.string.solo_puedes_ver_los_mensajes),
                    Toast.LENGTH_LONG).show();

            if (textChatName != null) {
                String tituloActual = textChatName.getText().toString();
                if (!tituloActual.contains("(" + estadoSolicitud + ")")) {
                    textChatName.setText(tituloActual + " (" + estadoSolicitud + ")");
                }
            }
        });
    }

    /**
     * Configura el RecyclerView para mostrar la lista de mensajes
     * Inicializa el adapter y establece el layout manager
     */
    private void setupRecyclerView() {
        mensajesList = new ArrayList<>();
        mensajesAdapter = new MensajesAdapter(mensajesList, usuarioActual.getUsuarioid(), otroUsuarioId);
        recyclerViewChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChat.setAdapter(mensajesAdapter);

        precargarOtroUsuario();

        mensajesAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                recyclerViewChat.smoothScrollToPosition(mensajesAdapter.getItemCount() - 1);
            }
        });
    }

    /**
     * Precarga la información del otro usuario participante en el chat
     * Actualiza el título del chat con el nombre real del usuario
     */
    private void precargarOtroUsuario() {
        if (otroUsuarioId == -1) return;

        ApiService.getUsuarioById(otroUsuarioId, new ApiService.ApiCallback<Usuario>() {
            @Override
            public void onSuccess(Usuario usuario) {
                mensajesAdapter.precargarUsuario(usuario);
                if (usuario != null) {
                    String nombreCompleto = usuario.getNombre() + " " + usuario.getApellido();
                    textChatName.setText(nombreCompleto);

                    if (estadoSolicitud != null && !"activa".equals(estadoSolicitud)) {
                        textChatName.setText(nombreCompleto + " (" + estadoSolicitud + ")");
                    }
                }
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    /**
     * Carga los mensajes del chat desde la API
     * Actualiza la lista de mensajes en el adapter
     */
    private void loadMensajes() {
        if (chatId == -1) {
            Toast.makeText(this, R.string.error_id_chat_invalido, Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService.getMensajesByChat(chatId, new ApiService.ListCallback<Mensaje>() {
            @Override
            public void onSuccess(List<Mensaje> mensajes) {
                runOnUiThread(() -> {
                    if (mensajes != null) {
                        if (hayMensajesNuevos(mensajes)) {
                            mensajesAdapter.actualizarMensajes(mensajes);
                            if (!mensajes.isEmpty()) {
                                recyclerViewChat.scrollToPosition(mensajes.size() - 1);
                            }
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                });
            }
        });
    }

    /**
     * Verifica si hay mensajes nuevos comparando con la lista actual
     * @param nuevosMensajes Lista de mensajes recién obtenidos
     * @return true si hay mensajes nuevos, false en caso contrario
     */
    private boolean hayMensajesNuevos(List<Mensaje> nuevosMensajes) {
        if (mensajesList.size() != nuevosMensajes.size()) {
            return true;
        }

        if (!mensajesList.isEmpty() && !nuevosMensajes.isEmpty()) {
            Mensaje ultimoActual = mensajesList.get(mensajesList.size() - 1);
            Mensaje ultimoNuevo = nuevosMensajes.get(nuevosMensajes.size() - 1);
            return ultimoActual.getMensajeid() != ultimoNuevo.getMensajeid();
        }

        return !mensajesList.isEmpty() || !nuevosMensajes.isEmpty();
    }

    /**
     * Inicia el polling para actualizar mensajes periódicamente
     * Consulta la API cada cierto intervalo para obtener nuevos mensajes
     */
    private void startPolling() {
        pollingHandler = new Handler();
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                loadMensajes();
                pollingHandler.postDelayed(this, POLLING_INTERVAL);
            }
        };
        pollingHandler.postDelayed(pollingRunnable, POLLING_INTERVAL);
    }

    /**
     * Detiene el polling de mensajes
     * Elimina los callbacks pendientes para evitar fugas de memoria
     */
    private void stopPolling() {
        if (pollingHandler != null && pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
        }
    }

    /**
     * Envía un nuevo mensaje al chat
     * Valida el contenido y estado antes de enviar a la API
     */
    private void enviarMensaje() {
        if (estadoSolicitud != null && !"activa".equals(estadoSolicitud)) {
            Toast.makeText(this,
                    getString(R.string.no_puedes_enviar_mensajes_en_solicitudes) + estadoSolicitud,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String contenido = editTextMessage.getText().toString().trim();
        if (contenido.isEmpty()) {
            Toast.makeText(this, R.string.escribe_un_mensaje, Toast.LENGTH_SHORT).show();
            return;
        }

        if (chatId == -1) {
            Toast.makeText(this, R.string.error_no_se_puede_enviar_el_mensaje, Toast.LENGTH_SHORT).show();
            return;
        }

        Mensaje nuevoMensaje = new Mensaje(chatId, usuarioActual.getUsuarioid(), contenido);

        ApiService.createMensaje(nuevoMensaje, new ApiService.ApiCallback<Mensaje>() {
            @Override
            public void onSuccess(Mensaje mensaje) {
                runOnUiThread(() -> {
                    editTextMessage.setText("");
                    mensajesAdapter.agregarMensaje(mensaje);
                    recyclerViewChat.smoothScrollToPosition(mensajesAdapter.getItemCount() - 1);
                    verificarSiEsPrimerMensaje(mensaje);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(ChatC.this, getString(R.string.error_enviando_mensaje) + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Verifica si el mensaje actual es el primero del usuario en este chat
     * Crea una notificación si es el primer mensaje
     * @param mensaje Mensaje recién enviado
     */
    private void verificarSiEsPrimerMensaje(Mensaje mensaje) {
        boolean yaNotificado = notificacionPrefs.getBoolean("chat_" + chatId, false);

        if (yaNotificado) {
            return;
        }

        ApiService.getMensajesByChat(chatId, new ApiService.ListCallback<Mensaje>() {
            @Override
            public void onSuccess(List<Mensaje> mensajes) {
                if (mensajes != null) {
                    long mensajesDelUsuario = mensajes.stream()
                            .filter(m -> m.getEmisorioid() == usuarioActual.getUsuarioid())
                            .count();

                    if (mensajesDelUsuario == 1) {
                        crearNotificacionNuevaConversacion();
                    } else {
                        notificacionPrefs.edit().putBoolean("chat_" + chatId, true).apply();
                    }
                }
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    /**
     * Marca todos los mensajes del chat como leídos
     * Actualiza el estado en la API y localmente
     */
    private void marcarMensajesComoLeidos() {
        if (chatId == -1 || usuarioActual == null) {
            return;
        }

        ApiService.updateMensajesLeidos(chatId, usuarioActual.getUsuarioid(), new ApiService.ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                actualizarEstadoLeidoLocalmente();
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    /**
     * Actualiza localmente el estado de los mensajes a "leído"
     * Sincroniza la interfaz con el estado actualizado de la API
     */
    private void actualizarEstadoLeidoLocalmente() {
        if (mensajesList != null && mensajesAdapter != null) {
            mensajesAdapter.marcarTodosComoLeidos();
        }
    }

    /**
     * Crea una notificación para el otro usuario indicando nueva conversación
     * Solo se ejecuta una vez por chat para evitar notificaciones duplicadas
     */
    private void crearNotificacionNuevaConversacion() {
        boolean yaNotificado = notificacionPrefs.getBoolean("chat_" + chatId, false);
        if (yaNotificado) {
            return;
        }

        String nombreUsuarioActual = usuarioActual.getNombre() + " " + usuarioActual.getApellido();
        String titulo = getString(R.string.tienes_un_nuevo_mensaje);
        String mensajeNotificacion = nombreUsuarioActual + getString(R.string.quiere_hablar_contigo);

        Notificacion notificacion = new Notificacion(otroUsuarioId, titulo, mensajeNotificacion);

        ApiService.createNotificacion(notificacion, new ApiService.ApiCallback<Notificacion>() {
            @Override
            public void onSuccess(Notificacion result) {
                SharedPreferences.Editor editor = notificacionPrefs.edit();
                editor.putBoolean("chat_" + chatId, true);
                editor.apply();
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    /**
     * Método del ciclo de vida que se ejecuta al destruir la actividad
     * Limpia recursos y detiene el polling
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPolling();
    }

    /**
     * Método del ciclo de vida que se ejecuta al pausar la actividad
     * Detiene temporalmente el polling para ahorrar recursos
     */
    @Override
    protected void onPause() {
        super.onPause();
        stopPolling();
    }

    /**
     * Método del ciclo de vida que se ejecuta al reanudar la actividad
     * Reinicia el polling y carga los mensajes actualizados
     */
    @Override
    protected void onResume() {
        super.onResume();
        startPolling();
        loadMensajes();
    }

    /**
     * Maneja la navegación hacia atrás desde el chat
     * Preserva información de filtrado para la actividad de mensajería
     * @param view Vista que disparó el evento
     */
    public void back(View view) {
        Intent intent = new Intent(ChatC.this, Mensajeria.class);

        int solicitudId = getIntent().getIntExtra("solicitud_id", -1);

        if (solicitudId != -1) {
            intent.putExtra("filter_by_solicitud", true);
            intent.putExtra("solicitud_id", solicitudId);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}