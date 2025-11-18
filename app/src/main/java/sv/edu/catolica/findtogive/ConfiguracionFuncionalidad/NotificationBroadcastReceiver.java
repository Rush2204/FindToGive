package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationBroadcastReceiver extends BroadcastReceiver {

    /**
     * Se ejecuta cuando se recibe un broadcast.
     * Inicia el servicio de notificaciones de chat, usando startForegroundService
     * para versiones de Android Oreo (API 26) y superiores, o startService para versiones anteriores.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, ChatNotificationService.class);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}