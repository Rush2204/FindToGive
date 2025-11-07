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
            // 🔥 AGREGAR ESTO - Iniciar servicio si ya está logueado
            if (AppNotificationManager.areNotificationsEnabled(this)) {
                AppNotificationManager.startNotificationService(this);
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

        setLoginButtonState(false, "Iniciando sesión...");

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
            inputLayoutEmail.setError("El correo electrónico es requerido");
            isValid = false;
        } else if (!isValidEmail(email)) {
            inputLayoutEmail.setError("Ingresa un correo electrónico válido");
            isValid = false;
        } else {
            inputLayoutEmail.setError(null);
        }

        if (password.isEmpty()) {
            inputLayoutPassword.setError("La contraseña es requerida");
            isValid = false;
        } else if (password.length() < 6) {
            inputLayoutPassword.setError("La contraseña debe tener al menos 6 caracteres");
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

        Toast.makeText(Login.this, "¡Bienvenido " + usuario.getNombre() + "!", Toast.LENGTH_SHORT).show();

        // 🔥 AGREGAR ESTA LÍNEA - Iniciar servicio de notificaciones
        if (AppNotificationManager.areNotificationsEnabled(Login.this)) {
            AppNotificationManager.startNotificationService(Login.this);
        }

        navigateToFeedDonacion();
    }

    private void onLoginError(String errorMessage) {
        setLoginButtonState(true, "Iniciar sesión");

        if (errorMessage.contains("No se encontraron datos") ||
                errorMessage.contains("Error: 404") ||
                errorMessage.contains("Error: 400")) {
            inputLayoutEmail.setError("Correo o contraseña incorrectos");
            inputLayoutPassword.setError("Correo o contraseña incorrectos");
            Toast.makeText(Login.this, "Credenciales incorrectas", Toast.LENGTH_LONG).show();
        } else if (errorMessage.contains("Error de red")) {
            Toast.makeText(Login.this, "Error de conexión. Verifica tu internet", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(Login.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
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

    @Override
    protected void onResume() {
        super.onResume();
        setLoginButtonState(true, "Iniciar sesión");
        inputLayoutEmail.setError(null);
        inputLayoutPassword.setError(null);
    }
}