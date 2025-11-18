package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;

import sv.edu.catolica.findtogive.ClasesDiseño.ChatC;
import sv.edu.catolica.findtogive.ClasesDiseño.FeedDonacion;
import sv.edu.catolica.findtogive.ClasesDiseño.Mensajeria;
import sv.edu.catolica.findtogive.Modelado.Chat;
import sv.edu.catolica.findtogive.Modelado.Mensaje;
import sv.edu.catolica.findtogive.Modelado.Usuario;
import sv.edu.catolica.findtogive.R;

public class ChatNotificationService extends Service {

    private static final String CHANNEL_ID = "chat_notifications_channel";
    private static final String CHANNEL_NAME = "Chat Notifications";
    private static final int SERVICE_NOTIFICATION_ID = 1002;
    private static final long POLLING_INTERVAL = 10000; // 10 segundos

    private Handler pollingHandler;
    private Runnable pollingRunnable;
    private Usuario usuarioActual;
    private SharedPreferences notificationPrefs;
    private List<Chat> cachedChats = new ArrayList<>();

    /**
     * Método que se ejecuta cuando se crea el servicio
     * Inicializa componentes y configura el servicio foreground
     */
    @SuppressLint("ForegroundServiceType")
    @Override
    public void onCreate() {
        super.onCreate();

        usuarioActual = SharedPreferencesManager.getCurrentUser(this);
        notificationPrefs = getSharedPreferences("chat_notifications", Context.MODE_PRIVATE);
        createNotificationChannel();

        startForeground(SERVICE_NOTIFICATION_ID, createServiceNotification());
    }

    /**
     * Método que se ejecuta cuando se inicia el servicio
     * @param intent Intent que inició el servicio
     * @param flags Flags de inicio
     * @param startId ID de inicio
     * @return Modo de comportamiento del servicio
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startPolling();
        return START_STICKY;
    }

    /**
     * Método para binding del servicio (no utilizado)
     * @param intent Intent de binding
     * @return null ya que no se soporta binding
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Crea el canal de notificaciones para Android 8+
     * Configura las propiedades del canal como luces y vibración
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    android.app.NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(getString(R.string.notificaciones_chats_mensajes_desc));
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});

            android.app.NotificationManager manager =
                    (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * Crea la notificación del servicio foreground
     * @return Notificación configurada para el servicio
     */
    private Notification createServiceNotification() {
        Intent notificationIntent = new Intent(this, FeedDonacion.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.emergencias_no_esperan))
                .setSmallIcon(R.drawable.ico_logo_findtogive)
                .setContentIntent(pendingIntent)
                .setOngoing(false)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
    }

    /**
     * Inicia el polling periódico para verificar nuevos chats y mensajes
     * Ejecuta las verificaciones cada 10 segundos
     */
    private void startPolling() {
        pollingHandler = new Handler();
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                checkForNewChats();
                checkForNewMessages();
                pollingHandler.postDelayed(this, POLLING_INTERVAL);
            }
        };
        pollingHandler.post(pollingRunnable);
    }

    /**
     * Verifica si hay nuevos chats para el usuario actual
     * Actualiza la cache y verifica chats nuevos
     */
    private void checkForNewChats() {
        if (usuarioActual == null) return;

        ApiService.getChatsByUsuario(usuarioActual.getUsuarioid(), new ApiService.ListCallback<Chat>() {
            @Override
            public void onSuccess(List<Chat> chats) {
                if (chats != null) {
                    updateCachedChats(chats);

                    for (Chat chat : chats) {
                        checkIfNewChat(chat);
                    }
                }
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    /**
     * Actualiza la cache local de chats con la lista más reciente
     * @param chats Lista actualizada de chats
     */
    private void updateCachedChats(List<Chat> chats) {
        if (chats != null) {
            cachedChats.clear();
            cachedChats.addAll(chats);

            for (Chat chat : chats) {
                saveChatToPreferences(chat);
            }
        }
    }

    /**
     * Verifica si un chat es nuevo y no ha sido notificado
     * @param chat Chat a verificar
     */
    private void checkIfNewChat(Chat chat) {
        String key = "chat_notified_" + chat.getChatid();
        boolean alreadyNotified = notificationPrefs.getBoolean(key, false);

        if (!alreadyNotified) {
            ApiService.getMensajesByChat(chat.getChatid(), new ApiService.ListCallback<Mensaje>() {
                @Override
                public void onSuccess(List<Mensaje> mensajes) {
                    if (mensajes != null && !mensajes.isEmpty()) {
                        notificationPrefs.edit().putBoolean(key, true).apply();
                    } else {
                        notificationPrefs.edit().putBoolean(key, true).apply();
                    }
                }

                @Override
                public void onError(String error) {
                }
            });
        }
    }

    /**
     * Verifica si hay mensajes nuevos en todos los chats del usuario
     */
    private void checkForNewMessages() {
        if (usuarioActual == null) return;

        ApiService.getChatsByUsuario(usuarioActual.getUsuarioid(), new ApiService.ListCallback<Chat>() {
            @Override
            public void onSuccess(List<Chat> chats) {
                if (chats != null) {
                    for (Chat chat : chats) {
                        checkForNewMessagesInChat(chat);
                    }
                }
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    /**
     * Verifica si hay mensajes nuevos en un chat específico
     * @param chat Chat a verificar
     */
    private void checkForNewMessagesInChat(Chat chat) {
        ApiService.getMensajesByChat(chat.getChatid(), new ApiService.ListCallback<Mensaje>() {
            @Override
            public void onSuccess(List<Mensaje> mensajes) {
                if (mensajes != null && !mensajes.isEmpty()) {
                    Mensaje ultimoMensaje = mensajes.get(0);
                    for (Mensaje mensaje : mensajes) {
                        if (mensaje.getMensajeid() > ultimoMensaje.getMensajeid()) {
                            ultimoMensaje = mensaje;
                        }
                    }

                    if (ultimoMensaje.getEmisorioid() != usuarioActual.getUsuarioid()) {
                        String key = "last_message_" + chat.getChatid();
                        int lastNotifiedMessageId = notificationPrefs.getInt(key, -1);

                        if (ultimoMensaje.getMensajeid() > lastNotifiedMessageId) {
                            showNewMessageNotification(chat, ultimoMensaje);
                            notificationPrefs.edit().putInt(key, ultimoMensaje.getMensajeid()).apply();
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
     * Muestra una notificación para un nuevo chat
     * @param chat Chat que generó la notificación
     */
    private void showNewChatNotification(Chat chat) {
        int otroUsuarioId = getOtroUsuarioId(chat.getChatid());

        if (otroUsuarioId == -1) {
            createNotification(getString(R.string.nueva_conversacion),
                    getString(R.string.tienes_nuevo_chat), chat.getChatid(), true);
            return;
        }

        ApiService.getUsuarioById(otroUsuarioId, new ApiService.ApiCallback<Usuario>() {
            @Override
            public void onSuccess(Usuario usuario) {
                String nombreUsuario = usuario != null ?
                        usuario.getNombre() + " " + usuario.getApellido() : getString(R.string.alguien);

                String title = getString(R.string.nueva_conversacion);
                String message = nombreUsuario + " " + getString(R.string.quiere_hablar_contigo);

                createNotification(title, message, chat.getChatid(), true);
            }

            @Override
            public void onError(String error) {
                createNotification(getString(R.string.nueva_conversacion),
                        getString(R.string.tienes_nuevo_chat), chat.getChatid(), true);
            }
        });
    }

    /**
     * Muestra una notificación para un nuevo mensaje
     * @param chat Chat donde se recibió el mensaje
     * @param mensaje Mensaje recibido
     */
    private void showNewMessageNotification(Chat chat, Mensaje mensaje) {
        int otroUsuarioId = getOtroUsuarioId(chat.getChatid());

        if (otroUsuarioId == -1) {
            createNotification(getString(R.string.nuevo_mensaje),
                    mensaje.getContenido(), chat.getChatid(), false);
            return;
        }

        ApiService.getUsuarioById(otroUsuarioId, new ApiService.ApiCallback<Usuario>() {
            @Override
            public void onSuccess(Usuario usuario) {
                String nombreUsuario = usuario != null ?
                        usuario.getNombre() + " " + usuario.getApellido() : getString(R.string.alguien);

                String title = getString(R.string.mensaje_de, nombreUsuario);
                String message = mensaje.getContenido();

                if (message.length() > 50) {
                    message = message.substring(0, 47) + "...";
                }

                createNotification(title, message, chat.getChatid(), false);
            }

            @Override
            public void onError(String error) {
                createNotification(getString(R.string.nuevo_mensaje),
                        mensaje.getContenido(), chat.getChatid(), false);
            }
        });
    }

    /**
     * Obtiene el ID del otro usuario en un chat
     * @param chatId ID del chat
     * @return ID del otro usuario o -1 si no se encuentra
     */
    private int getOtroUsuarioId(int chatId) {
        if (usuarioActual == null) {
            return -1;
        }

        for (Chat chat : cachedChats) {
            if (chat.getChatid() == chatId) {
                int otroUsuarioId = (chat.getUsuario1id() == usuarioActual.getUsuarioid()) ?
                        chat.getUsuario2id() : chat.getUsuario1id();
                return otroUsuarioId;
            }
        }

        String chatKey = "chat_info_" + chatId;
        int savedOtroUsuarioId = notificationPrefs.getInt(chatKey, -1);
        if (savedOtroUsuarioId != -1) {
            return savedOtroUsuarioId;
        }

        return fetchOtroUsuarioIdSynchronous(chatId);
    }

    /**
     * Obtiene el ID del otro usuario de forma síncrona desde la API
     * @param chatId ID del chat
     * @return ID del otro usuario o -1 si no se encuentra
     */
    private int fetchOtroUsuarioIdSynchronous(int chatId) {
        final int[] resultado = {-1};
        final Object lock = new Object();

        ApiService.getChatById(chatId, new ApiService.ApiCallback<Chat>() {
            @Override
            public void onSuccess(Chat chat) {
                synchronized (lock) {
                    if (chat != null && usuarioActual != null) {
                        resultado[0] = (chat.getUsuario1id() == usuarioActual.getUsuarioid()) ?
                                chat.getUsuario2id() : chat.getUsuario1id();

                        saveChatToCache(chat);
                        saveChatToPreferences(chat);
                    }
                    lock.notify();
                }
            }

            @Override
            public void onError(String error) {
                synchronized (lock) {
                    lock.notify();
                }
            }
        });

        synchronized (lock) {
            try {
                lock.wait(5000);
            } catch (InterruptedException e) {
            }
        }

        return resultado[0];
    }

    /**
     * Guarda un chat en la cache local
     * @param chat Chat a guardar
     */
    private void saveChatToCache(Chat chat) {
        for (int i = 0; i < cachedChats.size(); i++) {
            if (cachedChats.get(i).getChatid() == chat.getChatid()) {
                cachedChats.remove(i);
                break;
            }
        }
        cachedChats.add(chat);
    }

    /**
     * Guarda información del chat en SharedPreferences
     * @param chat Chat a guardar
     */
    private void saveChatToPreferences(Chat chat) {
        if (usuarioActual != null) {
            int otroUsuarioId = (chat.getUsuario1id() == usuarioActual.getUsuarioid()) ?
                    chat.getUsuario2id() : chat.getUsuario1id();

            String chatKey = "chat_info_" + chat.getChatid();
            notificationPrefs.edit()
                    .putInt(chatKey, otroUsuarioId)
                    .putInt("chat_solicitud_" + chat.getChatid(), chat.getSolicitudid())
                    .apply();
        }
    }

    /**
     * Crea y muestra una notificación
     * @param title Título de la notificación
     * @param message Mensaje de la notificación
     * @param chatId ID del chat relacionado
     * @param isNewChat Indica si es un chat nuevo
     */
    private void createNotification(String title, String message, int chatId, boolean isNewChat) {
        int notificationId = generateUniqueNotificationId(chatId);

        Intent intent;
        if (isNewChat) {
            intent = new Intent(this, Mensajeria.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        } else {
            intent = new Intent(this, ChatC.class);
            intent.putExtra("chat_id", chatId);
            intent.putExtra("solicitud_id", -1);
            intent.putExtra("otro_usuario_id", getOtroUsuarioId(chatId));

            int solicitudId = getSolicitudIdFromChat(chatId);
            intent.putExtra("solicitud_id", solicitudId);

            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                notificationId,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ico_logo_findtogive)
                .setColor(getResources().getColor(R.color.dark_gray))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL);

        if (!isNewChat) {
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
        }

        android.app.NotificationManager notificationManager =
                (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(notificationId, builder.build());
    }

    /**
     * Obtiene el ID de solicitud asociado a un chat
     * @param chatId ID del chat
     * @return ID de la solicitud o -1 si no se encuentra
     */
    private int getSolicitudIdFromChat(int chatId) {
        for (Chat chat : cachedChats) {
            if (chat.getChatid() == chatId) {
                return chat.getSolicitudid();
            }
        }

        String chatKey = "chat_solicitud_" + chatId;
        return notificationPrefs.getInt(chatKey, -1);
    }

    /**
     * Genera un ID único para la notificación
     * @param chatId ID del chat
     * @return ID único para la notificación
     */
    private int generateUniqueNotificationId(int chatId) {
        return (chatId * 10000) + (int) (System.currentTimeMillis() % 10000);
    }

    /**
     * Método que se ejecuta cuando se destruye el servicio
     * Limpia los recursos y detiene el polling
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (pollingHandler != null && pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
        }
    }
}