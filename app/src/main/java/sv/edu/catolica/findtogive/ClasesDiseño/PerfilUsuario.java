package sv.edu.catolica.findtogive.ClasesDiseño;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
                    Toast.makeText(this, "Solo receptores pueden crear solicitudes", Toast.LENGTH_SHORT).show();
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
                // Ya estamos en perfil
                return true;
            }
            return false;
        });

        if (usuarioActual != null && usuarioActual.getRolid() == 1) {
            bottomNavigation.getMenu().findItem(R.id.nav_crear).setVisible(false);
        }

        bottomNavigation.setSelectedItemId(R.id.nav_perfil);
    }

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

    private void loadUserData() {
        usuarioActual = SharedPreferencesManager.getCurrentUser(this);

        if (usuarioActual != null) {
            displayUserData();
            actualizarNavegacionSegunRol();

            // Actualizar visibilidad del ítem "Crear" en la navegación
            if (usuarioActual.getRolid() == 1) {
                bottomNavigation.getMenu().findItem(R.id.nav_crear).setVisible(false);
            }
        } else {
            Toast.makeText(this, "Error: No se pudo cargar la información del usuario", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void displayUserData() {
        // Nombre completo
        textUserName.setText(usuarioActual.getNombreCompleto());

        // Email
        textDisplayEmail.setText(usuarioActual.getEmail());

        // Edad
        textDisplayAge.setText(usuarioActual.getEdad() + " años");

        // Teléfono
        textDisplayPhone.setText(usuarioActual.getTelefono() != null ?
                usuarioActual.getTelefono() : "No especificado");

        // Ubicación
        textDisplayLocation.setText(usuarioActual.getUbicacion() != null ?
                usuarioActual.getUbicacion() : "No especificada");

        // Tipo de sangre
        textBloodTypeBadge.setText(obtenerTipoSangreCompleto(usuarioActual.getTiposangreid()));

        // Rol
        textDisplayRol.setText(obtenerRolCompleto(usuarioActual.getRolid()));

        // Foto de perfil
        loadProfileImage();
    }

    private void loadProfileImage() {
        System.out.println("🔄 Cargando imagen de perfil. URL: " +
                (usuarioActual != null ? usuarioActual.getFotoUrl() : "usuario null"));

        if (usuarioActual != null && usuarioActual.getFotoUrl() != null && !usuarioActual.getFotoUrl().isEmpty()) {
            System.out.println("✅ URL de imagen válida, cargando con Glide: " + usuarioActual.getFotoUrl());

            Glide.with(this)
                    .load(usuarioActual.getFotoUrl())
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.logo_findtogive)
                    .error(R.drawable.logo_findtogive)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                    Target<Drawable> target, boolean isFirstResource) {
                            System.out.println("❌ Error Glide al cargar imagen: " +
                                    (e != null ? e.getMessage() : "Desconocido"));
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model,
                                                       Target<Drawable> target, DataSource dataSource,
                                                       boolean isFirstResource) {
                            System.out.println("✅ Imagen cargada exitosamente con Glide");
                            return false;
                        }
                    })
                    .into(imgProfile);
        } else {
            System.out.println("ℹ️ Usando imagen por defecto");
            imgProfile.setImageResource(R.drawable.logo_findtogive);
        }
    }
    private void setupClickListeners() {
        // Botón para editar foto
        findViewById(R.id.btn_edit_photo).setOnClickListener(v -> {
            pickImageLauncher.launch("image/*");
        });

        // Botón para editar perfil
        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(PerfilUsuario.this, EdicionPerfil.class);
            startActivity(intent);
        });

        // Botón para cerrar sesión
        btnLogout.setOnClickListener(v -> {
            logoutUser();
        });
    }

    private void uploadProfileImage(Uri imageUri) {
        // Mostrar indicador de carga en la imagen
        imgProfile.setImageResource(R.drawable.logo_findtogive);

        StorageService.uploadProfileImage(this, imageUri, usuarioActual.getUsuarioid(),
                new StorageService.UploadCallback() {
                    @Override
                    public void onSuccess(String imageUrl) {
                        System.out.println("✅ URL de imagen obtenida: " + imageUrl);

                        // En lugar de usar toJsonForUpdate, crearemos el JSON manualmente
                        // para asegurarnos que solo actualizamos foto_url
                        updateOnlyPhotoUrl(imageUrl);
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            loadProfileImage(); // Recargar imagen original
                            System.out.println("❌ Error al subir imagen: " + error);
                            Toast.makeText(PerfilUsuario.this, "Error al subir imagen: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void updateOnlyPhotoUrl(String imageUrl) {
        CompletableFuture.runAsync(() -> {
            try {
                // Crear JSON manualmente SOLO con foto_url
                String json = "{\"foto_url\":\"" + imageUrl + "\"}";
                System.out.println("📤 JSON específico para foto_url: " + json);

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
                    System.out.println("📨 Response code: " + response.code());

                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        System.out.println("✅ UPDATE foto_url exitoso: " + responseBody);

                        runOnUiThread(() -> {
                            // Actualizar el usuario local con la nueva foto
                            usuarioActual.setFotoUrl(imageUrl);

                            // Actualizar SharedPreferences
                            SharedPreferencesManager.saveUser(PerfilUsuario.this, usuarioActual);

                            // Actualizar la imagen en la UI
                            loadProfileImage();
                            Toast.makeText(PerfilUsuario.this, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                        System.out.println("❌ Error en UPDATE foto_url: " + response.code() + " - " + errorBody);

                        runOnUiThread(() -> {
                            loadProfileImage(); // Recargar imagen original
                            Toast.makeText(PerfilUsuario.this, "Error al actualizar foto: " + response.code(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }

            } catch (Exception e) {
                System.out.println("❌ Exception en updateOnlyPhotoUrl: " + e.getMessage());
                runOnUiThread(() -> {
                    loadProfileImage(); // Recargar imagen original
                    Toast.makeText(PerfilUsuario.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // NUEVO MÉTODO: Actualizar la navegación según el rol
    private void actualizarNavegacionSegunRol() {
        if (usuarioActual != null) {
            boolean esDonante = usuarioActual.getRolid() == 1;
            bottomNavigation.getMenu().findItem(R.id.nav_crear).setVisible(!esDonante);

            System.out.println("🔄 Navegación actualizada - Rol: " + usuarioActual.getRolid() +
                    ", Mostrar 'Crear': " + !esDonante);
        }
    }

    private void logoutUser() {
        SharedPreferencesManager.logout(this);
        Intent intent = new Intent(this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

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
            default: return "Desconocido";
        }
    }

    private String obtenerRolCompleto(int rolid) {
        switch (rolid) {
            case 1: return "Donante";
            case 2: return "Receptor";
            case 3: return "Donante y Receptor";
            default: return "Usuario";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar datos del usuario actualizado
        Usuario usuarioActualizado = SharedPreferencesManager.getCurrentUser(this);
        if (usuarioActualizado != null) {
            usuarioActual = usuarioActualizado;
            displayUserData();

            actualizarNavegacionSegunRol();
        }
        bottomNavigation.setSelectedItemId(R.id.nav_perfil);
    }
}