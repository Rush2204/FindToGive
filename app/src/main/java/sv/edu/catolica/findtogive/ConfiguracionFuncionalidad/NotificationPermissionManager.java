// NotificationPermissionManager.java
package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class NotificationPermissionManager {

    public static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

    /**
     * Verifica si los permisos de notificación están concedidos.
     * En Android 13 (TIRAMISU) y superiores, verifica el permiso POST_NOTIFICATIONS.
     * En versiones anteriores, retorna true ya que los permisos se conceden automáticamente.
     */
    public static boolean areNotificationsEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        // En versiones anteriores a Android 13, los permisos se conceden automáticamente
        return true;
    }

    /**
     * Solicita los permisos de notificación al usuario si es necesario.
     * Solo aplica para Android 13 (TIRAMISU) y superiores.
     * En versiones anteriores no realiza ninguna acción.
     */
    public static void requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!areNotificationsEnabled(activity)) {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    /**
     * Maneja el resultado de la solicitud de permisos.
     * Verifica si el código de solicitud coincide y si el permiso fue concedido.
     * Retorna true si el permiso fue concedido, false en caso contrario.
     */
    public static boolean handlePermissionResult(int requestCode, int[] grantResults) {
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            return grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }
}