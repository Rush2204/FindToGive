package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class AppNotificationManager {
    private static final String PREFS_NAME = "notification_prefs";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";

    /**
     * Inicia el servicio de notificaciones si los permisos están concedidos
     * @param context Contexto de la aplicación
     */
    public static void startNotificationService(Context context) {
        try {
            if (!NotificationPermissionManager.areNotificationsEnabled(context)) {
                return;
            }

            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, true).apply();

            Intent serviceIntent = new Intent(context, ChatNotificationService.class);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

        } catch (Exception e) {
        }
    }

    /**
     * Detiene el servicio de notificaciones
     * @param context Contexto de la aplicación
     */
    public static void stopNotificationService(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, false).apply();

            Intent serviceIntent = new Intent(context, ChatNotificationService.class);
            context.stopService(serviceIntent);

        } catch (Exception e) {
        }
    }

    /**
     * Verifica si las notificaciones están habilitadas (permisos y configuración local)
     * @param context Contexto de la aplicación
     * @return true si las notificaciones están habilitadas, false en caso contrario
     */
    public static boolean areNotificationsEnabled(Context context) {
        try {
            boolean hasPermissions = NotificationPermissionManager.areNotificationsEnabled(context);
            boolean localEnabled = getLocalNotificationSetting(context);

            return hasPermissions && localEnabled;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Obtiene la configuración local de notificaciones
     * @param context Contexto de la aplicación
     * @return true si las notificaciones están habilitadas localmente
     */
    private static boolean getLocalNotificationSetting(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Verifica solo los permisos de notificación sin considerar la configuración local
     * @param context Contexto de la aplicación
     * @return true si los permisos están concedidos
     */
    public static boolean hasNotificationPermissions(Context context) {
        return NotificationPermissionManager.areNotificationsEnabled(context);
    }

    /**
     * Inicia el servicio solo si los permisos están concedidos
     * @param context Contexto de la aplicación
     */
    public static void startServiceIfPermissionsGranted(Context context) {
        try {
            if (NotificationPermissionManager.areNotificationsEnabled(context)) {
                startNotificationService(context);
            }
        } catch (Exception e) {
        }
    }

    /**
     * Habilita o deshabilita las notificaciones localmente
     * @param context Contexto de la aplicación
     * @param enabled true para habilitar, false para deshabilitar
     */
    public static void setLocalNotificationsEnabled(Context context, boolean enabled) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();

            if (!enabled) {
                stopNotificationService(context);
            } else if (NotificationPermissionManager.areNotificationsEnabled(context)) {
                startNotificationService(context);
            }

        } catch (Exception e) {
        }
    }

    /**
     * Limpia todas las preferencias de notificaciones almacenadas
     * @param context Contexto de la aplicación
     */
    public static void clearNotificationPreferences(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("chat_notifications", Context.MODE_PRIVATE);
            prefs.edit().clear().apply();

            SharedPreferences mainPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            mainPrefs.edit().clear().apply();

        } catch (Exception e) {
        }
    }

    /**
     * Obtiene el estado detallado de las notificaciones
     * @param context Contexto de la aplicación
     * @return String con el estado de permisos y configuración local
     */
    public static String getNotificationStatus(Context context) {
        boolean hasPermissions = NotificationPermissionManager.areNotificationsEnabled(context);
        boolean localEnabled = getLocalNotificationSetting(context);
        boolean overallEnabled = areNotificationsEnabled(context);

        return String.format("Notificaciones - Permisos: %s, Local: %s, General: %s",
                hasPermissions, localEnabled, overallEnabled);
    }
}