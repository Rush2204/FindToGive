package sv.edu.catolica.findtogive.ClasesDiseño;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import androidx.annotation.Nullable;
import android.graphics.drawable.Drawable;

import java.util.concurrent.CompletableFuture;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sv.edu.catolica.findtogive.Modelado.Usuario;
import sv.edu.catolica.findtogive.R;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.SharedPreferencesManager;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.StorageService;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.SupabaseClient;

public class PerfilUsuario extends AppCompatActivity {

    private Usuario usuarioActual;
    private ImageView imgProfile;
    private TextView textUserName, textBloodTypeBadge, textDisplayEmail,
            textDisplayAge, textDisplayPhone, textDisplayLocation, textDisplayRol;
    private Button btnEditProfile, btnLogout;
    private BottomNavigationView bottomNavigation;

    private ActivityResultLauncher<String> pickImageLauncher;

    /**
     * Método principal que inicializa la actividad del perfil de usuario
     * Configura la vista, navegación y carga los datos del usuario
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.design_perfil_usuario);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupBottomNavigation();
        setupImagePicker();
        loadUserData();
        setupClickListeners();
    }

    /**
     * Inicializa todos los componentes visuales de la interfaz
     * Obtiene referencias a los views del layout
     */
    private void initViews() {
        imgProfile = findViewById(R.id.img_profile);
        textUserName = findViewById(R.id.text_user_name);
        textBloodTypeBadge = findViewById(R.id.text_blood_type_badge);
        textDisplayEmail = findViewById(R.id.text_display_email);
        textDisplayAge = findViewById(R.id.text_display_age);
        textDisplayPhone = findViewById(R.id.text_display_phone);
        textDisplayLocation = findViewById(R.id.text_display_location);
        textDisplayRol = findViewById(R.id.text_display_rol);
        btnEditProfile = findViewById(R.id.btn_edit_profile);
        btnLogout = findViewById(R.id.btn_logout);
        bottomNavigation = findViewById(R.id.bottom_navigation_bar);
    }

    /**
     * Configura la navegación inferior de la aplicación
     * Define las acciones para cada ítem del menú de navegación
     */
    private void setupBottomNavigation() {
        actualizarNavegacionSegunRol();

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_inicio) {
                Intent intent = new Intent(this, FeedDonacion.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_crear) {
                if (usuarioActual != null && (usuarioActual.getRolid() == 2 || usuarioActual.getRolid() == 3)) {
                    Intent intent = new Intent(this, SolicitudDonacionC.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, R.string.solo_receptores_pueden_crear_solicitudes, Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            } else if (itemId == R.id.nav_notificaciones) {
                Intent intent = new Intent(this, Notificaciones.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_historial) {
                Intent intent = new Intent(this, HistorialDonaciones.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_perfil) {
                return true;
            }
            return false;
        });

        if (usuarioActual != null && usuarioActual.getRolid() == 1) {
            bottomNavigation.getMenu().findItem(R.id.nav_crear).setVisible(false);
        }

        bottomNavigation.setSelectedItemId(R.id.nav_perfil);
    }

    /**
     * Configura el selector de imágenes para cambiar la foto de perfil
     * Utiliza Activity Result API para manejar la selección de imágenes
     */
    private void setupImagePicker() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        uploadProfileImage(uri);
                    }
                }
        );
    }

    /**
     * Carga los datos del usuario desde SharedPreferences
     * Verifica que el usuario esté autenticado antes de mostrar los datos
     */
    private void loadUserData() {
        usuarioActual = SharedPreferencesManager.getCurrentUser(this);

        if (usuarioActual != null) {
            displayUserData();
            actualizarNavegacionSegunRol();

            if (usuarioActual.getRolid() == 1) {
                bottomNavigation.getMenu().findItem(R.id.nav_crear).setVisible(false);
            }
        } else {
            Toast.makeText(this, R.string.error_cargar_info_usuario, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Muestra los datos del usuario en la interfaz
     * Rellena todos los campos con la información del usuario actual
     */
    private void displayUserData() {
        textUserName.setText(usuarioActual.getNombreCompleto());
        textDisplayEmail.setText(usuarioActual.getEmail());
        textDisplayAge.setText(getString(R.string.edad_formato_perfil, usuarioActual.getEdad()));
        textDisplayPhone.setText(usuarioActual.getTelefono() != null ?
                usuarioActual.getTelefono() : getString(R.string.no_especificado));
        textDisplayLocation.setText(usuarioActual.getUbicacion() != null ?
                usuarioActual.getUbicacion() : getString(R.string.no_especificada));
        textBloodTypeBadge.setText(obtenerTipoSangreCompleto(usuarioActual.getTiposangreid()));
        textDisplayRol.setText(obtenerRolCompleto(usuarioActual.getRolid()));

        loadProfileImage();
    }

    /**
     * Carga la imagen de perfil del usuario usando Glide
     * Muestra imagen por defecto si no hay URL de imagen disponible
     */
    private void loadProfileImage() {
        if (usuarioActual != null && usuarioActual.getFotoUrl() != null && !usuarioActual.getFotoUrl().isEmpty()) {
            Glide.with(this)
                    .load(usuarioActual.getFotoUrl())
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.logo_findtogive)
                    .error(R.drawable.logo_findtogive)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                    Target<Drawable> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model,
                                                       Target<Drawable> target, DataSource dataSource,
                                                       boolean isFirstResource) {
                            return false;
                        }
                    })
                    .into(imgProfile);
        } else {
            imgProfile.setImageResource(R.drawable.logo_findtogive);
        }
    }

    /**
     * Configura los listeners de clic para los botones
     * Asigna las acciones a realizar cuando se presionan los botones
     */
    private void setupClickListeners() {
        findViewById(R.id.btn_edit_photo).setOnClickListener(v -> {
            pickImageLauncher.launch("image/*");
        });

        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(PerfilUsuario.this, EdicionPerfil.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            logoutUser();
        });
    }

    /**
     * Sube la imagen de perfil seleccionada al servidor
     * @param imageUri URI de la imagen seleccionada
     */
    private void uploadProfileImage(Uri imageUri) {
        imgProfile.setImageResource(R.drawable.logo_findtogive);

        StorageService.uploadProfileImage(this, imageUri, usuarioActual.getUsuarioid(),
                new StorageService.UploadCallback() {
                    @Override
                    public void onSuccess(String imageUrl) {
                        updateOnlyPhotoUrl(imageUrl);
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            loadProfileImage();
                            Toast.makeText(PerfilUsuario.this, getString(R.string.error_subir_imagen, error), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    /**
     * Actualiza solo la URL de la foto de perfil en la base de datos
     * @param imageUrl Nueva URL de la imagen de perfil
     */
    private void updateOnlyPhotoUrl(String imageUrl) {
        CompletableFuture.runAsync(() -> {
            try {
                String json = "{\"foto_url\":\"" + imageUrl + "\"}";

                RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

                String url = SupabaseClient.URLs.usuario() + "?usuarioid=eq." + usuarioActual.getUsuarioid();

                Request request = new Request.Builder()
                        .url(url)
                        .patch(body)
                        .addHeader("Authorization", SupabaseClient.Headers.getAuthHeader())
                        .addHeader("apikey", SupabaseClient.Headers.getApiKeyHeader())
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=representation")
                        .build();

                try (Response response = SupabaseClient.getHttpClient().newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        runOnUiThread(() -> {
                            usuarioActual.setFotoUrl(imageUrl);
                            SharedPreferencesManager.saveUser(PerfilUsuario.this, usuarioActual);
                            loadProfileImage();
                            Toast.makeText(PerfilUsuario.this, R.string.foto_perfil_actualizada, Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                        runOnUiThread(() -> {
                            loadProfileImage();
                            Toast.makeText(PerfilUsuario.this, getString(R.string.error_al_actualizar_foto) + response.code(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }

            } catch (Exception e) {
                runOnUiThread(() -> {
                    loadProfileImage();
                    Toast.makeText(PerfilUsuario.this, getString(R.string.error_generico, e.getMessage()), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Actualiza la navegación inferior según el rol del usuario
     * Oculta el ítem de creación para usuarios donantes
     */
    private void actualizarNavegacionSegunRol() {
        if (usuarioActual != null) {
            boolean esDonante = usuarioActual.getRolid() == 1;
            bottomNavigation.getMenu().findItem(R.id.nav_crear).setVisible(!esDonante);
        }
    }

    /**
     * Cierra la sesión del usuario y redirige al login
     * Limpia todos los datos de sesión almacenados
     */
    private void logoutUser() {
        SharedPreferencesManager.logout(this);
        Intent intent = new Intent(this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Convierte un ID de tipo de sangre a su nombre correspondiente
     * @param tiposangreid ID del tipo de sangre
     * @return Nombre completo del tipo de sangre
     */
    private String obtenerTipoSangreCompleto(int tiposangreid) {
        switch (tiposangreid) {
            case 1: return "A+";
            case 2: return "A-";
            case 3: return "B+";
            case 4: return "B-";
            case 5: return "AB+";
            case 6: return "AB-";
            case 7: return "O+";
            case 8: return "O-";
            default: return getString(R.string.desconocido);
        }
    }

    /**
     * Convierte un ID de rol a su nombre correspondiente
     * @param rolid ID del rol
     * @return Nombre completo del rol
     */
    private String obtenerRolCompleto(int rolid) {
        switch (rolid) {
            case 1: return getString(R.string.donante);
            case 2: return getString(R.string.receptor);
            case 3: return getString(R.string.rol_donante_receptor);
            default: return getString(R.string.rol_usuario);
        }
    }

    /**
     * Método del ciclo de vida que se ejecuta al reanudar la actividad
     * Recarga los datos del usuario y actualiza la navegación
     */
    @Override
    protected void onResume() {
        super.onResume();
        Usuario usuarioActualizado = SharedPreferencesManager.getCurrentUser(this);
        if (usuarioActualizado != null) {
            usuarioActual = usuarioActualizado;
            displayUserData();
            actualizarNavegacionSegunRol();
        }
        bottomNavigation.setSelectedItemId(R.id.nav_perfil);
    }
}