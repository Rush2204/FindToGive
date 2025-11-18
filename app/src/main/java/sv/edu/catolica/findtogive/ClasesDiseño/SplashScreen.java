package sv.edu.catolica.findtogive.ClasesDiseño;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.NotificationPermissionManager;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.SharedPreferencesManager;
import sv.edu.catolica.findtogive.R;

public class SplashScreen extends Activity {

    private boolean permissionRequested = false;

    /**
     * Método principal que inicializa la pantalla de presentación
     * Configura la animación y verifica permisos después de un delay
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.desing_splash_screen);

        ImageView imagen = findViewById(R.id.img_logo);
        Animation MiAnimacion = AnimationUtils.loadAnimation(this, R.anim.rotacion);
        imagen.startAnimation(MiAnimacion);

        Handler manejador = new Handler();
        manejador.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkPermissionsAndNavigate();
            }
        }, 4000);
    }

    /**
     * Verifica los permisos de notificación y navega a la siguiente pantalla
     * Solicita permisos si no están concedidos, de lo contrario navega inmediatamente
     */
    private void checkPermissionsAndNavigate() {
        if (NotificationPermissionManager.areNotificationsEnabled(this)) {
            navigateToNextScreen();
        } else {
            permissionRequested = true;
            NotificationPermissionManager.requestNotificationPermission(this);
        }
    }

    /**
     * Maneja los resultados de las solicitudes de permisos
     * @param requestCode Código de la solicitud de permisos
     * @param permissions Permisos solicitados
     * @param grantResults Resultados de la concesión de permisos
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (permissionRequested) {
            NotificationPermissionManager.handlePermissionResult(requestCode, grantResults);
            navigateToNextScreen();
        }
    }

    /**
     * Navega a la siguiente pantalla según el estado de autenticación
     * Redirige al feed principal si el usuario está logueado, de lo contrario al login
     */
    private void navigateToNextScreen() {
        if (SharedPreferencesManager.isLoggedIn(this)) {
            Intent ventana = new Intent(SplashScreen.this, FeedDonacion.class);
            startActivity(ventana);
        } else {
            Intent ventana = new Intent(SplashScreen.this, Login.class);
            startActivity(ventana);
        }
        finish();
    }

    /**
     * Método del ciclo de vida que se ejecuta al reanudar la actividad
     * Maneja el caso donde el usuario cierra el diálogo de permisos sin responder
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (permissionRequested && !NotificationPermissionManager.areNotificationsEnabled(this)) {
            navigateToNextScreen();
        }
    }
}