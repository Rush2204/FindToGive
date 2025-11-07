package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class AppNotificationManager {  // ⬅️ CAMBIAR NOMBRE
    private static final String TAG = "AppNotificationManager";  // ⬅️ CAMBIAR TAG
    private static final String PREFS_NAME = "notification_prefs";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";

    public static void startNotificationService(Context context) {
        try {
            Log.d(TAG, "Iniciando servicio de notificaciones");

            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, true).apply();

            Intent serviceIntent = new Intent(context, ChatNotificationService.class);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error iniciando servicio: " + e.getMessage(), e);
        }
    }

    public static void stopNotificationService(Context context) {
        try {
            Log.d(TAG, "Deteniendo servicio de notificaciones");

            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, false).apply();

            Intent serviceIntent = new Intent(context, ChatNotificationService.class);
            context.stopService(serviceIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error deteniendo servicio: " + e.getMessage(), e);
        }
    }

    public static boolean areNotificationsEnabled(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
        } catch (Exception e) {
            Log.e(TAG, "Error verificando notificaciones: " + e.getMessage(), e);
            return true; // Por defecto habilitadas
        }
    }

    public static void clearNotificationPreferences(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("chat_notifications", Context.MODE_PRIVATE);
            prefs.edit().clear().apply();
            Log.d(TAG, "Preferencias de notificaciones limpiadas");
        } catch (Exception e) {
            Log.e(TAG, "Error limpiando preferencias: " + e.getMessage(), e);
        }
    }
}