package sv.edu.catolica.findtogive.ClasesDiseño;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
    private ImageView btnBack;
    private RecyclerView recyclerViewChat;
    private EditText editTextMessage;
    private ImageButton btnSend;

    private int chatId;
    private int solicitudId;
    private int otroUsuarioId;
    private String chatNombre;
    private String estadoSolicitud; // Para controlar el estado

    private List<Mensaje> mensajesList;
    private MensajesAdapter mensajesAdapter;
    private Usuario usuarioActual;

    private Handler pollingHandler;
    private Runnable pollingRunnable;
    private static final long POLLING_INTERVAL = 1000;
    private SharedPreferences notificacionPrefs;

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

        // MARCAR MENSAJES COMO LEÍDOS AL ENTRAR AL CHAT
        marcarMensajesComoLeidos();

        // VERIFICAR ESTADO DE LA SOLICITUD INMEDIATAMENTE - MEJORADO
        verificarEstadoSolicitud();

        setupRecyclerView();
        loadMensajes();
        startPolling();
    }

    private void initializeViews() {
        textChatName = findViewById(R.id.text_chat_name);
        btnBack = findViewById(R.id.btn_back);
        recyclerViewChat = findViewById(R.id.recycler_view_chat);
        editTextMessage = findViewById(R.id.edit_text_message);
        btnSend = findViewById(R.id.btn_send);

        // Obtener datos del intent
        chatId = getIntent().getIntExtra("chat_id", -1);
        solicitudId = getIntent().getIntExtra("solicitud_id", -1);
        otroUsuarioId = getIntent().getIntExtra("otro_usuario_id", -1);
        chatNombre = getIntent().getStringExtra("chat_nombre");
        estadoSolicitud = getIntent().getStringExtra("solicitud_estado"); // NUEVO

        usuarioActual = SharedPreferencesManager.getCurrentUser(this);

        textChatName.setText(chatNombre != null ? chatNombre : "Chat");

        btnSend.setOnClickListener(v -> enviarMensaje());

        setupEditTextPaste();
    }

    // NUEVO MÉTODO: Configurar el EditText para permitir pegar
    private void setupEditTextPaste() {
        // Esto habilita el menú contextual nativo de Android (copiar, pegar, etc.)
        editTextMessage.setCustomSelectionActionModeCallback(new android.view.ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                // El menú contextual se crea automáticamente con las opciones estándar
                return true;
            }

            @Override
            public boolean onPrepareActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                // Permitir que todas las opciones del menú estén disponibles
                return true;
            }

            @Override
            public boolean onActionItemClicked(android.view.ActionMode mode, android.view.MenuItem item) {
                return false; // Dejar que el sistema maneje las acciones
            }

            @Override
            public void onDestroyActionMode(android.view.ActionMode mode) {
                // Limpieza si es necesaria
            }
        });

        // También puedes agregar un LongClickListener para mostrar un mensaje personalizado
        editTextMessage.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // Mostrar un Toast informativo (opcional)
                if (editTextMessage.getText().toString().isEmpty()) {
                    Toast.makeText(ChatC.this, "Mantén presionado para pegar", Toast.LENGTH_SHORT).show();
                }
                return false; // Dejar que el sistema maneje el long click normalmente
            }
        });
    }

    // NUEVO MÉTODO: Verificar estado de la solicitud - MEJORADO
    private void verificarEstadoSolicitud() {
        Log.d("ChatC", "🔍 Verificando estado de solicitud: " + estadoSolicitud);

        // Si ya viene el estado del intent, usarlo inmediatamente
        if (estadoSolicitud != null && !"activa".equals(estadoSolicitud)) {
            Log.d("ChatC", "🚫 Estado no activo detectado desde intent: " + estadoSolicitud);
            bloquearEnvioMensajes();
            return;
        }

        // Si no viene el estado o viene como activa, consultar a la API para confirmar
        if (solicitudId != -1) {
            consultarEstadoSolicitud();
        }
    }

    // NUEVO MÉTODO: Consultar estado de la solicitud desde la API - MEJORADO
    private void consultarEstadoSolicitud() {
        Log.d("ChatC", "🔍 Consultando estado de solicitud ID: " + solicitudId);

        // Usar el método específico para obtener la solicitud por ID
        ApiService.getSolicitudById(solicitudId, new ApiService.ApiCallback<SolicitudDonacion>() {
            @Override
            public void onSuccess(SolicitudDonacion solicitud) {
                if (solicitud != null) {
                    estadoSolicitud = solicitud.getEstado();
                    Log.d("ChatC", "✅ Estado obtenido de API: " + estadoSolicitud);

                    runOnUiThread(() -> {
                        if (!"activa".equals(estadoSolicitud)) {
                            Log.d("ChatC", "🚫 Bloqueando mensajes - Estado: " + estadoSolicitud);
                            bloquearEnvioMensajes();
                        } else {
                            Log.d("ChatC", "✅ Estado activo - Mensajes permitidos");
                        }
                    });
                } else {
                    Log.e("ChatC", "❌ Solicitud no encontrada");
                }
            }

            @Override
            public void onError(String error) {
                Log.e("ChatC", "❌ Error consultando estado de solicitud: " + error);
                // Si hay error, intentar con el método alternativo
                consultarEstadoSolicitudAlternativo();
            }
        });
    }

    // MÉTODO ALTERNATIVO: Consultar estado desde todas las solicitudes del usuario
    private void consultarEstadoSolicitudAlternativo() {
        ApiService.getSolicitudesByUsuarioId(usuarioActual.getUsuarioid(), new ApiService.ListCallback<SolicitudDonacion>() {
            @Override
            public void onSuccess(List<SolicitudDonacion> solicitudes) {
                if (solicitudes != null) {
                    for (SolicitudDonacion solicitud : solicitudes) {
                        if (solicitud.getSolicitudid() == solicitudId) {
                            estadoSolicitud = solicitud.getEstado();
                            Log.d("ChatC", "✅ Estado obtenido (alternativo): " + estadoSolicitud);

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
                Log.e("ChatC", "❌ Error en consulta alternativa: " + error);
            }
        });
    }

    // MÉTODO: Bloquear envío de mensajes - MEJORADO
    private void bloquearEnvioMensajes() {
        runOnUiThread(() -> {
            Log.d("ChatC", "🔒 Bloqueando interfaz de mensajes");

            editTextMessage.setEnabled(false);
            editTextMessage.setHint("Chat cerrado - Solicitud " +
                    ("completada".equals(estadoSolicitud) ? "completada ✅" : "cancelada ❌"));
            editTextMessage.setBackgroundColor(0xFFF0F0F0);

            btnSend.setEnabled(false);
            btnSend.setAlpha(0.3f);
            btnSend.setClickable(false);

            // Mostrar mensaje informativo
            String mensajeEstado = "completada".equals(estadoSolicitud) ?
                    "completada ✅" : "cancelada ❌";
            Toast.makeText(this,
                    "Esta solicitud está " + mensajeEstado + ". Solo puedes ver los mensajes.",
                    Toast.LENGTH_LONG).show();

            // Actualizar título para mostrar estado
            if (textChatName != null) {
                String tituloActual = textChatName.getText().toString();
                // Solo agregar el estado si no está ya en el título
                if (!tituloActual.contains("(" + estadoSolicitud + ")")) {
                    textChatName.setText(tituloActual + " (" + estadoSolicitud + ")");
                }
            }
        });
    }

    private void setupRecyclerView() {
        mensajesList = new ArrayList<>();
        // Pasar el otroUsuarioId al adapter
        mensajesAdapter = new MensajesAdapter(mensajesList, usuarioActual.getUsuarioid(), otroUsuarioId);
        recyclerViewChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChat.setAdapter(mensajesAdapter);

        // Precargar la información del otro usuario ANTES de cargar los mensajes
        precargarOtroUsuario();

        mensajesAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                recyclerViewChat.smoothScrollToPosition(mensajesAdapter.getItemCount() - 1);
            }
        });
    }

    private void precargarOtroUsuario() {
        if (otroUsuarioId == -1) return;

        ApiService.getUsuarioById(otroUsuarioId, new ApiService.ApiCallback<Usuario>() {
            @Override
            public void onSuccess(Usuario usuario) {
                // Precargar el usuario en el adapter
                mensajesAdapter.precargarUsuario(usuario);
                // Actualizar el título del chat con el nombre real
                if (usuario != null) {
                    String nombreCompleto = usuario.getNombre() + " " + usuario.getApellido();
                    textChatName.setText(nombreCompleto);

                    // Si está bloqueado, actualizar título con estado
                    if (estadoSolicitud != null && !"activa".equals(estadoSolicitud)) {
                        textChatName.setText(nombreCompleto + " (" + estadoSolicitud + ")");
                    }
                }
                System.out.println("✅ Usuario precargado en chat: " + usuario.getNombre());
            }

            @Override
            public void onError(String error) {
                System.out.println("❌ Error precargando usuario: " + error);
            }
        });
    }

    private void loadMensajes() {
        if (chatId == -1) {
            Toast.makeText(this, "Error: ID de chat inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService.getMensajesByChat(chatId, new ApiService.ListCallback<Mensaje>() {
            @Override
            public void onSuccess(List<Mensaje> mensajes) {
                runOnUiThread(() -> {
                    if (mensajes != null) {
                        // Verificar si hay mensajes nuevos
                        if (hayMensajesNuevos(mensajes)) {
                            mensajesAdapter.actualizarMensajes(mensajes);
                            if (!mensajes.isEmpty()) {
                                recyclerViewChat.scrollToPosition(mensajes.size() - 1);
                            }
                            System.out.println("🔄 Mensajes actualizados: " + mensajes.size());

                            // Marcar mensajes nuevos como leídos
                            marcarMensajesNuevosComoLeidos(mensajes);
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    System.out.println("❌ Error cargando mensajes: " + error);
                });
            }
        });
    }

    // NUEVO MÉTODO: Marcar mensajes nuevos como leídos
    private void marcarMensajesNuevosComoLeidos(List<Mensaje> nuevosMensajes) {
        boolean hayMensajesNoLeidos = false;

        for (Mensaje mensaje : nuevosMensajes) {
            if (!mensaje.isLeido() && mensaje.getEmisorioid() != usuarioActual.getUsuarioid()) {
                hayMensajesNoLeidos = true;
                break;
            }
        }

        if (hayMensajesNoLeidos) {
            marcarMensajesComoLeidos();
        }
    }



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

    private void startPolling() {
        pollingHandler = new Handler();
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                loadMensajes();

                // Verificar si hay mensajes no leídos y marcarlos
                verificarYMarcarMensajesNoLeidos();

                pollingHandler.postDelayed(this, POLLING_INTERVAL);
            }
        };
        pollingHandler.postDelayed(pollingRunnable, POLLING_INTERVAL);
    }

    // NUEVO MÉTODO: Verificar y marcar mensajes no leídos
    private void verificarYMarcarMensajesNoLeidos() {
        if (mensajesList != null) {
            for (Mensaje mensaje : mensajesList) {
                if (!mensaje.isLeido() && mensaje.getEmisorioid() != usuarioActual.getUsuarioid()) {
                    marcarMensajesComoLeidos();
                    break;
                }
            }
        }
    }

    private void stopPolling() {
        if (pollingHandler != null && pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
        }
    }

    private void enviarMensaje() {
        // VERIFICAR SI ESTÁ BLOQUEADO ANTES DE ENVIAR - CORRECCIÓN CRÍTICA
        if (estadoSolicitud != null && !"activa".equals(estadoSolicitud)) {
            Log.d("ChatC", "🚫 Intento de enviar mensaje bloqueado - Estado: " + estadoSolicitud);
            Toast.makeText(this,
                    "No puedes enviar mensajes en solicitudes " + estadoSolicitud,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String contenido = editTextMessage.getText().toString().trim();
        if (contenido.isEmpty()) {
            Toast.makeText(this, "Escribe un mensaje", Toast.LENGTH_SHORT).show();
            return;
        }

        if (chatId == -1) {
            Toast.makeText(this, "Error: No se puede enviar el mensaje", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(ChatC.this, "Error enviando mensaje: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void verificarSiEsPrimerMensaje(Mensaje mensaje) {
        boolean yaNotificado = notificacionPrefs.getBoolean("chat_" + chatId, false);

        if (yaNotificado) {
            System.out.println("⏭️ Chat " + chatId + " ya fue notificado, omitiendo...");
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
                        System.out.println("🎉 ¡Es el primer mensaje del usuario! Creando notificación...");
                        crearNotificacionNuevaConversacion();
                    } else {
                        System.out.println("ℹ️ No es el primer mensaje del usuario (" + mensajesDelUsuario + " mensajes), no se crea notificación");
                        notificacionPrefs.edit().putBoolean("chat_" + chatId, true).apply();
                    }
                }
            }

            @Override
            public void onError(String error) {
                System.out.println("❌ Error verificando mensajes: " + error);
            }
        });
    }

    // NUEVO MÉTODO: Marcar mensajes como leídos
    private void marcarMensajesComoLeidos() {
        if (chatId == -1 || usuarioActual == null) {
            return;
        }

        ApiService.updateMensajesLeidos(chatId, usuarioActual.getUsuarioid(), new ApiService.ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d("ChatC", "✅ Mensajes marcados como leídos para chat: " + chatId);

                // Actualizar la lista local de mensajes para reflejar el cambio
                actualizarEstadoLeidoLocalmente();
            }

            @Override
            public void onError(String error) {
                Log.e("ChatC", "❌ Error marcando mensajes como leídos: " + error);
            }
        });
    }

    // NUEVO MÉTODO: Actualizar estado leído en la lista local
    private void actualizarEstadoLeidoLocalmente() {
        if (mensajesList != null && mensajesAdapter != null) {
            // Usar el nuevo método del adapter
            mensajesAdapter.marcarTodosComoLeidos();
        }
    }

    private void crearNotificacionNuevaConversacion() {
        boolean yaNotificado = notificacionPrefs.getBoolean("chat_" + chatId, false);
        if (yaNotificado) {
            System.out.println("🚫 Notificación cancelada - chat " + chatId + " ya notificado");
            return;
        }

        String nombreUsuarioActual = usuarioActual.getNombre() + " " + usuarioActual.getApellido();
        String titulo = "Tienes un nuevo mensaje";
        String mensajeNotificacion = nombreUsuarioActual + " quiere hablar contigo";

        System.out.println("🎯 Creando notificación ÚNICA para chat: " + chatId);

        Notificacion notificacion = new Notificacion(otroUsuarioId, titulo, mensajeNotificacion);

        ApiService.createNotificacion(notificacion, new ApiService.ApiCallback<Notificacion>() {
            @Override
            public void onSuccess(Notificacion result) {
                System.out.println("✅ Notificación ÚNICA creada para chat: " + chatId);

                SharedPreferences.Editor editor = notificacionPrefs.edit();
                editor.putBoolean("chat_" + chatId, true);
                editor.apply();

                System.out.println("📝 Chat " + chatId + " marcado como notificado");
            }

            @Override
            public void onError(String error) {
                System.out.println("❌ Error creando notificación: " + error);
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPolling();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPolling();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startPolling();
        loadMensajes();

        // MARCAR MENSAJES COMO LEÍDOS AL VOLVER AL CHAT
        marcarMensajesComoLeidos();
    }

    public void back(View view) {
        Intent intent = new Intent(ChatC.this, Mensajeria.class);

        int solicitudId = getIntent().getIntExtra("solicitud_id", -1);
        if (solicitudId != -1) {
            intent.putExtra("filter_by_solicitud", true);
            intent.putExtra("solicitud_id", solicitudId);
        }

        startActivity(intent);
        finish();
    }
}