package sv.edu.catolica.findtogive.ClasesDiseño;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import sv.edu.catolica.findtogive.R;

public class SplashScreen extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.desing_splash_screen);

        ImageView imagen = findViewById(R.id.img_logo);

        Animation MiAnimacion = AnimationUtils.loadAnimation(this, R.anim.rotacion);

        imagen.startAnimation(MiAnimacion);

        Handler manejador = new Handler();
        manejador.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent ventana = new Intent(SplashScreen.this, Login.class);
                startActivity(ventana);
                finish();
            }
        }, 4000);
    }
}