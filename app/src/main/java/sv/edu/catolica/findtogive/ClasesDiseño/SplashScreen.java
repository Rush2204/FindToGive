package sv.edu.catolica.findtogive.ClasesDiseño;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.NotificationPermissionManager;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.SharedPreferencesManager;
import sv.edu.catolica.findtogive.R;

public class SplashScreen extends Activity {

    private static final String TAG = "SplashScreen";
    private boolean permissionRequested = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.desing_splash_screen);

        // INICIAR ANIMACIÓN (igual que antes)
        ImageView imagen = findViewById(R.id.img_logo);
        Animation MiAnimacion = AnimationUtils.loadAnimation(this, R.anim.rotacion);
        imagen.startAnimation(MiAnimacion);

        Log.d(TAG, "SplashScreen iniciado con animación");

        Handler manejador = new Handler();
        manejador.postDelayed(new Runnable() {
            @Override
            public void run() {
                // DESPUÉS DE LA ANIMACIÓN (4 segundos), VERIFICAR PERMISOS
                checkPermissionsAndNavigate();
            }
        }, 4000); // Manteniendo tus 4 segundos de animación
    }

    private void checkPermissionsAndNavigate() {
        Log.d(TAG, "Verificando permisos después de la animación");

        if (NotificationPermissionManager.areNotificationsEnabled(this)) {
            Log.d(TAG, "Permisos ya concedidos - Navegando inmediatamente");
            // Si ya tiene permisos, navegar inmediatamente
            navigateToNextScreen();
        } else {
            Log.d(TAG, "Solicitando permisos de notificación");
            // Solicitar permisos
            permissionRequested = true;
            NotificationPermissionManager.requestNotificationPermission(this);
            // La navegación se manejará en onRequestPermissionsResult
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (permissionRequested) {
            Log.d(TAG, "Resultado de permisos recibido: " + requestCode);

            if (NotificationPermissionManager.handlePermissionResult(requestCode, grantResults)) {
                Log.d(TAG, "Permisos de notificación concedidos");
            } else {
                Log.d(TAG, "Permisos de notificación denegados");
            }

            // Navegar después de recibir el resultado de permisos
            navigateToNextScreen();
        }
    }

    private void navigateToNextScreen() {
        Log.d(TAG, "Navegando a la siguiente pantalla");

        // Verificar si ya está logueado (como en tu código original)
        if (SharedPreferencesManager.isLoggedIn(this)) {
            Log.d(TAG, "Usuario ya logueado, navegando a FeedDonacion");
            Intent ventana = new Intent(SplashScreen.this, FeedDonacion.class);
            startActivity(ventana);
        } else {
            Log.d(TAG, "Usuario no logueado, navegando a Login");
            Intent ventana = new Intent(SplashScreen.this, Login.class);
            startActivity(ventana);
        }
        finish();
    }

    // Manejar el caso donde el usuario cierra el diálogo de permisos sin responder
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "SplashScreen onResume");

        // Si se solicitó permisos pero no se recibió respuesta (usuario cerró el diálogo)
        if (permissionRequested && !NotificationPermissionManager.areNotificationsEnabled(this)) {
            Log.d(TAG, "Usuario cerró diálogo de permisos, navegando...");
            navigateToNextScreen();
        }
    }
}