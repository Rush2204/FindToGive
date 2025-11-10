package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class AppNotificationManager {
    private static final String TAG = "AppNotificationManager";
    private static final String PREFS_NAME = "notification_prefs";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";

    public static void startNotificationService(Context context) {
        try {
            // VERIFICAR PERMISOS ANTES DE INICIAR EL SERVICIO
            if (!NotificationPermissionManager.areNotificationsEnabled(context)) {
                Log.w(TAG, "Permisos de notificación no concedidos - Servicio no iniciado");
                return;
            }

            Log.d(TAG, "Iniciando servicio de notificaciones");

            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, true).apply();

            Intent serviceIntent = new Intent(context, ChatNotificationService.class);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

            Log.d(TAG, "Servicio de notificaciones iniciado exitosamente");

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

            Log.d(TAG, "Servicio de notificaciones detenido");

        } catch (Exception e) {
            Log.e(TAG, "Error deteniendo servicio: " + e.getMessage(), e);
        }
    }

    public static boolean areNotificationsEnabled(Context context) {
        try {
            // VERIFICAR TANTO LOS PERMISOS COMO LA CONFIGURACIÓN LOCAL
            boolean hasPermissions = NotificationPermissionManager.areNotificationsEnabled(context);
            boolean localEnabled = getLocalNotificationSetting(context);

            Log.d(TAG, "Estado notificaciones - Permisos: " + hasPermissions + ", Local: " + localEnabled);

            return hasPermissions && localEnabled;

        } catch (Exception e) {
            Log.e(TAG, "Error verificando notificaciones: " + e.getMessage(), e);
            return false; // Por defecto deshabilitadas si hay error
        }
    }

    // Método auxiliar para obtener solo la configuración local
    private static boolean getLocalNotificationSetting(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true); // Por defecto habilitadas
        } catch (Exception e) {
            Log.e(TAG, "Error obteniendo configuración local: " + e.getMessage(), e);
            return true;
        }
    }

    // NUEVO MÉTODO: Verificar solo permisos (sin configuración local)
    public static boolean hasNotificationPermissions(Context context) {
        return NotificationPermissionManager.areNotificationsEnabled(context);
    }

    // NUEVO MÉTODO: Forzar inicio del servicio (útil después de conceder permisos)
    public static void startServiceIfPermissionsGranted(Context context) {
        try {
            if (NotificationPermissionManager.areNotificationsEnabled(context)) {
                startNotificationService(context);
            } else {
                Log.w(TAG, "No se puede forzar inicio - Permisos no concedidos");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error forzando inicio de servicio: " + e.getMessage(), e);
        }
    }

    // NUEVO MÉTODO: Habilitar/deshabilitar notificaciones localmente
    public static void setLocalNotificationsEnabled(Context context, boolean enabled) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();

            Log.d(TAG, "Configuración local de notificaciones: " + enabled);

            // Si se deshabilitan, detener el servicio
            if (!enabled) {
                stopNotificationService(context);
            }
            // Si se habilitan, solo iniciar si tiene permisos
            else if (NotificationPermissionManager.areNotificationsEnabled(context)) {
                startNotificationService(context);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error cambiando configuración local: " + e.getMessage(), e);
        }
    }

    public static void clearNotificationPreferences(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("chat_notifications", Context.MODE_PRIVATE);
            prefs.edit().clear().apply();

            // También limpiar configuración local
            SharedPreferences mainPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            mainPrefs.edit().clear().apply();

            Log.d(TAG, "Todas las preferencias de notificaciones limpiadas");

        } catch (Exception e) {
            Log.e(TAG, "Error limpiando preferencias: " + e.getMessage(), e);
        }
    }

    // NUEVO MÉTODO: Estado detallado de notificaciones (para debugging)
    public static String getNotificationStatus(Context context) {
        boolean hasPermissions = NotificationPermissionManager.areNotificationsEnabled(context);
        boolean localEnabled = getLocalNotificationSetting(context);
        boolean overallEnabled = areNotificationsEnabled(context);

        return String.format("Notificaciones - Permisos: %s, Local: %s, General: %s",
                hasPermissions, localEnabled, overallEnabled);
    }
}