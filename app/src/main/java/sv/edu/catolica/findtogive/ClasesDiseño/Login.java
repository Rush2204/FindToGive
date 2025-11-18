package sv.edu.catolica.findtogive.ClasesDiseño;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.ApiService;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.AppNotificationManager;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.NotificationPermissionManager;
import sv.edu.catolica.findtogive.Modelado.Usuario;
import sv.edu.catolica.findtogive.R;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.SharedPreferencesManager;

public class Login extends AppCompatActivity {

    private TextInputEditText editTextEmail, editTextPassword;
    private TextInputLayout inputLayoutEmail, inputLayoutPassword;
    private Button btnLogin;
    private TextView textRegisterLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.desing_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Verificar si ya está logueado
        if (SharedPreferencesManager.isLoggedIn(this)) {
            // Solicitar permisos si no los tiene
            if (!NotificationPermissionManager.areNotificationsEnabled(this)) {
                NotificationPermissionManager.requestNotificationPermission(this);
            } else {
                // Ya tiene permisos, iniciar servicio
                if (AppNotificationManager.areNotificationsEnabled(this)) {
                    AppNotificationManager.startNotificationService(this);
                }
            }
            navigateToFeedDonacion();
            return;
        }

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        inputLayoutEmail = findViewById(R.id.input_layout_email);
        inputLayoutPassword = findViewById(R.id.input_layout_password);
        editTextEmail = findViewById(R.id.edit_text_email);
        editTextPassword = findViewById(R.id.edit_text_password);
        btnLogin = findViewById(R.id.btn_login);
        textRegisterLink = findViewById(R.id.text_register_link);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> loginUser());

        textRegisterLink.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, Registro.class);
            startActivity(intent);
        });

        editTextEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) inputLayoutEmail.setError(null);
        });

        editTextPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) inputLayoutPassword.setError(null);
        });

        editTextPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                loginUser();
                return true;
            }
            return false;
        });
    }

    private void loginUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (!validateInputs(email, password)) {
            return;
        }

        setLoginButtonState(false, getString(R.string.iniciando_sesion));

        ApiService.loginUser(email, password, new ApiService.ApiCallback<Usuario>() {
            @Override
            public void onSuccess(Usuario usuario) {
                runOnUiThread(() -> {
                    onLoginSuccess(usuario);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    onLoginError(error);
                });
            }
        });
    }

    private boolean validateInputs(String email, String password) {
        boolean isValid = true;

        if (email.isEmpty()) {
            inputLayoutEmail.setError(getString(R.string.el_correo_electronico_es_requerido));
            isValid = false;
        } else if (!isValidEmail(email)) {
            inputLayoutEmail.setError(getString(R.string.ingresa_un_correo_electronico_valido));
            isValid = false;
        } else {
            inputLayoutEmail.setError(null);
        }

        if (password.isEmpty()) {
            inputLayoutPassword.setError(getString(R.string.la_contrasena_es_requerida));
            isValid = false;
        } else if (password.length() < 6) {
            inputLayoutPassword.setError(getString(R.string.la_contrasena_debe_tener_al_menos_6_caracteres));
            isValid = false;
        } else {
            inputLayoutPassword.setError(null);
        }

        return isValid;
    }

    private boolean isValidEmail(String email) {
        String emailPattern = "[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}";
        return email.matches(emailPattern);
    }

    private void onLoginSuccess(Usuario usuario) {
        SharedPreferencesManager.saveUser(Login.this, usuario);

        String mensajeBienvenida = getString(R.string.bienvenida_usuario, usuario.getNombre());
        Toast.makeText(Login.this, mensajeBienvenida, Toast.LENGTH_SHORT).show();

        // 🔥 AGREGAR ESTA LÍNEA - Iniciar servicio de notificaciones
        if (AppNotificationManager.areNotificationsEnabled(Login.this)) {
            AppNotificationManager.startNotificationService(Login.this);
        }

        navigateToFeedDonacion();
    }

    private void onLoginError(String errorMessage) {
        setLoginButtonState(true, getString(R.string.iniciar_sesion));

        if (errorMessage.contains("No se encontraron datos") ||
                errorMessage.contains("Error: 404") ||
                errorMessage.contains("Error: 400")) {
            inputLayoutEmail.setError(getString(R.string.correo_o_contrasena_incorrectos));
            inputLayoutPassword.setError(getString(R.string.correo_o_contrasena_incorrectos));
            Toast.makeText(Login.this, R.string.credenciales_incorrectas, Toast.LENGTH_LONG).show();
        } else if (errorMessage.contains("Error de red")) {
            Toast.makeText(Login.this, R.string.error_de_conexion_verifica_tu_internet, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(Login.this, getString(R.string.error) + errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void setLoginButtonState(boolean enabled, String text) {
        btnLogin.setEnabled(enabled);
        btnLogin.setText(text);
    }

    private void navigateToFeedDonacion() {
        Intent intent = new Intent(Login.this, FeedDonacion.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Agregar método para manejar resultado de permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (NotificationPermissionManager.handlePermissionResult(requestCode, grantResults)) {
            // Permiso concedido, iniciar servicio si está logueado
            if (SharedPreferencesManager.isLoggedIn(this) &&
                    AppNotificationManager.areNotificationsEnabled(this)) {
                AppNotificationManager.startNotificationService(this);
            }
        }
        // Si se deniegan los permisos, continuar sin notificaciones
    }

    @Override
    protected void onResume() {
        super.onResume();
        setLoginButtonState(true, getString(R.string.iniciar_sesion));
        inputLayoutEmail.setError(null);
        inputLayoutPassword.setError(null);
    }
}