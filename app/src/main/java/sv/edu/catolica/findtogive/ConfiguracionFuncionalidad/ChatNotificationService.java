package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

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

    private static final String TAG = "ChatNotificationService";
    private static final String CHANNEL_ID = "chat_notifications_channel";
    private static final String CHANNEL_NAME = "Chat Notifications";
    private static final int NOTIFICATION_ID = 1001;
    private static final int SERVICE_NOTIFICATION_ID = 1002;

    private static final long POLLING_INTERVAL = 10000; // 10 segundos

    private Handler pollingHandler;
    private Runnable pollingRunnable;
    private Usuario usuarioActual;
    private SharedPreferences notificationPrefs;
    private List<Chat> cachedChats = new ArrayList<>();

    @SuppressLint("ForegroundServiceType")
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Servicio de notificaciones creado");

        usuarioActual = SharedPreferencesManager.getCurrentUser(this);
        notificationPrefs = getSharedPreferences("chat_notifications", Context.MODE_PRIVATE);
        createNotificationChannel();

        // Iniciar como servicio foreground para Android 8+
        startForeground(SERVICE_NOTIFICATION_ID, createServiceNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Servicio iniciado");
        startPolling();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    android.app.NotificationManager.IMPORTANCE_HIGH  // ⬅️ Agregar android.app.
            );
            channel.setDescription("Notificaciones de chats y mensajes");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});

            android.app.NotificationManager manager =  // ⬅️ Agregar android.app.
                    (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createServiceNotification() {
        Intent notificationIntent = new Intent(this, FeedDonacion.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("¡Las emergencias no esperan!")
                .setSmallIcon(R.drawable.ico_logo_findtogive)
                .setContentIntent(pendingIntent)
                .setOngoing(false)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
    }

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

    private void checkForNewChats() {
        if (usuarioActual == null) return;

        ApiService.getChatsByUsuario(usuarioActual.getUsuarioid(), new ApiService.ListCallback<Chat>() {
            @Override
            public void onSuccess(List<Chat> chats) {
                if (chats != null) {
                    // Actualizar el cache de chats
                    updateCachedChats(chats);

                    for (Chat chat : chats) {
                        checkIfNewChat(chat);
                    }
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error checking chats: " + error);
            }
        });
    }

    private void updateCachedChats(List<Chat> chats) {
        if (chats != null) {
            cachedChats.clear();
            cachedChats.addAll(chats);

            // Guardar también en SharedPreferences
            for (Chat chat : chats) {
                saveChatToPreferences(chat);
            }
        }
    }

    private void checkIfNewChat(Chat chat) {
        String key = "chat_notified_" + chat.getChatid();
        boolean alreadyNotified = notificationPrefs.getBoolean(key, false);

        if (!alreadyNotified) {
            // Verificar si el chat tiene mensajes
            ApiService.getMensajesByChat(chat.getChatid(), new ApiService.ListCallback<Mensaje>() {
                @Override
                public void onSuccess(List<Mensaje> mensajes) {
                    if (mensajes != null && !mensajes.isEmpty()) {
                        // Hay mensajes, es un chat existente
                        notificationPrefs.edit().putBoolean(key, true).apply();
                    } else {
                        // No hay mensajes, es un chat nuevo
                        showNewChatNotification(chat);
                        notificationPrefs.edit().putBoolean(key, true).apply();
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error checking messages for chat: " + error);
                }
            });
        }
    }

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
                Log.e(TAG, "Error checking chats for messages: " + error);
            }
        });
    }

    private void checkForNewMessagesInChat(Chat chat) {
        ApiService.getMensajesByChat(chat.getChatid(), new ApiService.ListCallback<Mensaje>() {
            @Override
            public void onSuccess(List<Mensaje> mensajes) {
                if (mensajes != null && !mensajes.isEmpty()) {
                    // Encontrar el mensaje más reciente
                    Mensaje ultimoMensaje = mensajes.get(0);
                    for (Mensaje mensaje : mensajes) {
                        if (mensaje.getMensajeid() > ultimoMensaje.getMensajeid()) {
                            ultimoMensaje = mensaje;
                        }
                    }

                    // Verificar si es un mensaje nuevo y no es del usuario actual
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
                Log.e(TAG, "Error checking messages in chat: " + error);
            }
        });
    }

    private void showNewChatNotification(Chat chat) {
        // Obtener información del otro usuario
        int otroUsuarioId = getOtroUsuarioId(chat.getChatid());

        if (otroUsuarioId == -1) {
            createNotification("Nueva conversación",
                    "Tienes un nuevo chat", chat.getChatid(), true);
            return;
        }

        ApiService.getUsuarioById(otroUsuarioId, new ApiService.ApiCallback<Usuario>() {
            @Override
            public void onSuccess(Usuario usuario) {
                String nombreUsuario = usuario != null ?
                        usuario.getNombre() + " " + usuario.getApellido() : "Alguien";

                String title = "Nueva conversación";
                String message = nombreUsuario + " quiere hablar contigo";

                createNotification(title, message, chat.getChatid(), true);
            }

            @Override
            public void onError(String error) {
                createNotification("Nueva conversación",
                        "Tienes un nuevo chat", chat.getChatid(), true);
            }
        });
    }

    private void showNewMessageNotification(Chat chat, Mensaje mensaje) {
        // Obtener información del otro usuario
        int otroUsuarioId = getOtroUsuarioId(chat.getChatid());

        if (otroUsuarioId == -1) {
            Log.e(TAG, "No se pudo obtener el otro usuario para el chat: " + chat.getChatid());
            // Usar notificación genérica
            createNotification("Nuevo mensaje",
                    mensaje.getContenido(), chat.getChatid(), false);
            return;
        }

        ApiService.getUsuarioById(otroUsuarioId, new ApiService.ApiCallback<Usuario>() {
            @Override
            public void onSuccess(Usuario usuario) {
                String nombreUsuario = usuario != null ?
                        usuario.getNombre() + " " + usuario.getApellido() : "Alguien";

                String title = "Mensaje de " + nombreUsuario;
                String message = mensaje.getContenido();

                if (message.length() > 50) {
                    message = message.substring(0, 47) + "...";
                }

                createNotification(title, message, chat.getChatid(), false);
            }

            @Override
            public void onError(String error) {
                // Notificación genérica si falla
                createNotification("Nuevo mensaje",
                        mensaje.getContenido(), chat.getChatid(), false);
            }
        });
    }

    private int getOtroUsuarioId(int chatId) {
        if (usuarioActual == null) {
            Log.e(TAG, "Usuario actual es null");
            return -1;
        }

        Log.d(TAG, "Buscando otro usuario para chat: " + chatId + ", usuario actual: " + usuarioActual.getUsuarioid());

        // Método 1: Buscar en cache local
        for (Chat chat : cachedChats) {
            if (chat.getChatid() == chatId) {
                int otroUsuarioId = (chat.getUsuario1id() == usuarioActual.getUsuarioid()) ?
                        chat.getUsuario2id() : chat.getUsuario1id();
                Log.d(TAG, "Encontrado en cache: " + otroUsuarioId);
                return otroUsuarioId;
            }
        }

        Log.d(TAG, "Chat no encontrado en cache, buscando en SharedPreferences...");

        // Método 2: Buscar en SharedPreferences
        String chatKey = "chat_info_" + chatId;
        int savedOtroUsuarioId = notificationPrefs.getInt(chatKey, -1);
        if (savedOtroUsuarioId != -1) {
            Log.d(TAG, "Encontrado en SharedPreferences: " + savedOtroUsuarioId);
            return savedOtroUsuarioId;
        }

        Log.d(TAG, "No encontrado localmente, consultando API...");

        // Método 3: Consultar API
        return fetchOtroUsuarioIdSynchronous(chatId);
    }

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

                        // Guardar en cache y SharedPreferences para futuras referencias
                        saveChatToCache(chat);
                        saveChatToPreferences(chat);

                        Log.d(TAG, "Obtenido de API: " + resultado[0]);
                    }
                    lock.notify();
                }
            }

            @Override
            public void onError(String error) {
                synchronized (lock) {
                    Log.e(TAG, "Error obteniendo chat de API: " + error);
                    lock.notify();
                }
            }
        });

        // Esperar máximo 5 segundos por la respuesta
        synchronized (lock) {
            try {
                lock.wait(5000);
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupción esperando respuesta de API", e);
            }
        }

        return resultado[0];
    }

    private void saveChatToCache(Chat chat) {
        // Remover si ya existe
        for (int i = 0; i < cachedChats.size(); i++) {
            if (cachedChats.get(i).getChatid() == chat.getChatid()) {
                cachedChats.remove(i);
                break;
            }
        }
        // Agregar el nuevo
        cachedChats.add(chat);
    }

    private void saveChatToPreferences(Chat chat) {
        if (usuarioActual != null) {
            int otroUsuarioId = (chat.getUsuario1id() == usuarioActual.getUsuarioid()) ?
                    chat.getUsuario2id() : chat.getUsuario1id();

            String chatKey = "chat_info_" + chat.getChatid();
            notificationPrefs.edit().putInt(chatKey, otroUsuarioId).apply();
            Log.d(TAG, "Chat guardado en preferences: " + chat.getChatid() + " -> " + otroUsuarioId);
        }
    }

    private void createNotification(String title, String message, int chatId, boolean isNewChat) {
        // Generar un ID único para cada notificación
        int notificationId = generateUniqueNotificationId(chatId);

        Intent intent;
        if (isNewChat) {
            intent = new Intent(this, Mensajeria.class);
        } else {
            intent = new Intent(this, ChatC.class);
            intent.putExtra("chat_id", chatId);
            intent.putExtra("solicitud_id", -1);
            intent.putExtra("otro_usuario_id", getOtroUsuarioId(chatId));
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                notificationId, // Usar ID único como requestCode
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

        // ⬇️⬇️⬇️ ESTA ES LA LÍNEA CRÍTICA - CAMBIAR POR:
        android.app.NotificationManager notificationManager =
                (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(notificationId, builder.build());

        Log.d(TAG, "Notificación creada - ID: " + notificationId + ", Chat: " + chatId);
    }

    private int generateUniqueNotificationId(int chatId) {
        // Combinar chatId con timestamp para crear un ID único
        return (chatId * 10000) + (int) (System.currentTimeMillis() % 10000);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Servicio destruido");
        if (pollingHandler != null && pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
        }
    }
}