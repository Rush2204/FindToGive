package sv.edu.catolica.findtogive.ClasesDiseño;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.ApiService;
import sv.edu.catolica.findtogive.Modelado.Usuario;
import sv.edu.catolica.findtogive.R;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.SharedPreferencesManager;

public class Registro extends AppCompatActivity {

    private TextInputEditText etNombre, etApellido, etEmail, etPassword, etEdad, etTelefono;
    private TextInputLayout tilNombre, tilApellido, tilEmail, tilPassword, tilEdad, tilTelefono;
    private Spinner spinnerRol, spinnerTipoSangre;
    private Button btnRegistrar;
    private TextView tvLoginLink;

    private int selectedRolId = 3; // "ambos" por defecto
    private int selectedTipoSangreId = 1; // "A+" por defecto

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.desing_registro);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupSpinners();
        setupClickListeners();
    }

    private void initializeViews() {
        // TextInputLayouts
        tilNombre = findViewById(R.id.til_nombre);
        tilApellido = findViewById(R.id.til_apellido);
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        tilEdad = findViewById(R.id.til_edad);
        tilTelefono = findViewById(R.id.til_telefono);

        // TextInputEditTexts
        etNombre = findViewById(R.id.et_nombre);
        etApellido = findViewById(R.id.et_apellido);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etEdad = findViewById(R.id.et_edad);
        etTelefono = findViewById(R.id.et_telefono);

        // Spinners
        spinnerRol = findViewById(R.id.spinner_rol);
        spinnerTipoSangre = findViewById(R.id.spinner_tipo_sangre);

        // Botón y enlace
        btnRegistrar = findViewById(R.id.btn_registrar);
        tvLoginLink = findViewById(R.id.tv_login_link);
    }

    private void setupSpinners() {
        // Configurar spinner de roles
        ArrayAdapter<CharSequence> rolAdapter = ArrayAdapter.createFromResource(this,
                R.array.roles_array, android.R.layout.simple_spinner_item);
        rolAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRol.setAdapter(rolAdapter);

        spinnerRol.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Mapear posición a rolid: 1=donante, 2=receptor, 3=ambos
                selectedRolId = position + 1;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedRolId = 3; // ambos por defecto
            }
        });

        // Configurar spinner de tipos de sangre
        ArrayAdapter<CharSequence> sangreAdapter = ArrayAdapter.createFromResource(this,
                R.array.tipos_sangre_array, android.R.layout.simple_spinner_item);
        sangreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipoSangre.setAdapter(sangreAdapter);

        spinnerTipoSangre.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Mapear posición a tiposangreid: 1=A+, 2=A-, etc.
                selectedTipoSangreId = position + 1;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedTipoSangreId = 1; // A+ por defecto
            }
        });
    }

    private void setupClickListeners() {
        btnRegistrar.setOnClickListener(v -> registerUser());

        tvLoginLink.setOnClickListener(v -> {
            Intent intent = new Intent(Registro.this, Login.class);
            startActivity(intent);
            finish();
        });

        // Configurar validaciones y formateo
        setupInputValidations();
        setupTelefonoFormatting();

        // Limpiar errores al escribir
        setupErrorClearingListeners();
    }

    private void setupInputValidations() {
        // Filtro para nombre - solo letras y espacios
        etNombre.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                if (!input.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]*$")) {
                    // Remover caracteres no válidos
                    String filtered = input.replaceAll("[^a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]", "");
                    if (!filtered.equals(input)) {
                        etNombre.setText(filtered);
                        etNombre.setSelection(filtered.length());
                        tilNombre.setError("Solo se permiten letras y espacios");
                    }
                } else {
                    tilNombre.setError(null);
                }
            }
        });

        // Filtro para apellido - solo letras y espacios
        etApellido.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                if (!input.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]*$")) {
                    // Remover caracteres no válidos
                    String filtered = input.replaceAll("[^a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]", "");
                    if (!filtered.equals(input)) {
                        etApellido.setText(filtered);
                        etApellido.setSelection(filtered.length());
                        tilApellido.setError("Solo se permiten letras y espacios");
                    }
                } else {
                    tilApellido.setError(null);
                }
            }
        });
    }

    private void setupTelefonoFormatting() {
        etTelefono.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting = false;
            private boolean deletingHyphen = false;
            private int lastLength = 0;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                lastLength = s.length();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Detectar si se está eliminando el guión
                if (before == 1 && count == 0 && lastLength > 0) {
                    String text = s.toString();
                    if (start < text.length() && text.charAt(start) == '-') {
                        deletingHyphen = true;
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;

                isFormatting = true;

                String phone = s.toString().replaceAll("[^\\d]", "");

                // Si se está eliminando el guión, eliminar también el dígito anterior
                if (deletingHyphen && phone.length() == 4 && lastLength == 6) {
                    phone = phone.substring(0, phone.length() - 1);
                    deletingHyphen = false;
                }

                StringBuilder formatted = new StringBuilder();

                if (phone.length() >= 4) {
                    formatted.append(phone.substring(0, 4));
                    if (phone.length() > 4) {
                        formatted.append("-").append(phone.substring(4, Math.min(8, phone.length())));
                    }
                } else {
                    formatted.append(phone);
                }

                // Solo actualizar si hay cambios
                if (!s.toString().equals(formatted.toString())) {
                    etTelefono.setText(formatted.toString());

                    // Posicionar el cursor correctamente
                    int cursorPos = formatted.length();
                    if (phone.length() <= 4) {
                        cursorPos = phone.length();
                    } else if (phone.length() < 8) {
                        cursorPos = phone.length() + 1; // +1 por el guión
                    }
                    etTelefono.setSelection(Math.min(cursorPos, formatted.length()));
                }

                isFormatting = false;
            }
        });

        // Mejorar la experiencia de edición permitiendo selección
        etTelefono.setOnKeyListener((v, keyCode, event) -> {
            // Permitir navegación y selección sin interferencias
            return false;
        });
    }

    private void setupErrorClearingListeners() {
        etNombre.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) tilNombre.setError(null);
        });

        etApellido.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) tilApellido.setError(null);
        });

        etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) tilEmail.setError(null);
        });

        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) tilPassword.setError(null);
        });

        etEdad.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) tilEdad.setError(null);
        });

        etTelefono.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) tilTelefono.setError(null);
        });
    }

    private void registerUser() {
        String nombre = etNombre.getText().toString().trim();
        String apellido = etApellido.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String edadStr = etEdad.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();

        if (!validateInputs(nombre, apellido, email, password, edadStr, telefono)) {
            return;
        }

        int edad = Integer.parseInt(edadStr);

        // Crear usuario con nombre y apellido separados
        Usuario nuevoUsuario = new Usuario(nombre, apellido, email, password,
                edad, telefono, selectedRolId, selectedTipoSangreId);

        setRegisterButtonState(false, "Registrando...");

        ApiService.registerUser(nuevoUsuario, new ApiService.ApiCallback<Usuario>() {
            @Override
            public void onSuccess(Usuario usuario) {
                runOnUiThread(() -> {
                    onRegisterSuccess(usuario);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    onRegisterError(error);
                });
            }
        });
    }

    private boolean validateInputs(String nombre, String apellido, String email, String password,
                                   String edadStr, String telefono) {
        boolean isValid = true;

        // Validar nombre
        if (nombre.isEmpty()) {
            tilNombre.setError("El nombre es requerido");
            isValid = false;
        } else if (!isValidNombreApellido(nombre)) {
            tilNombre.setError("El nombre solo puede contener letras y espacios");
            isValid = false;
        } else if (nombre.length() < 2) {
            tilNombre.setError("El nombre debe tener al menos 2 caracteres");
            isValid = false;
        } else {
            tilNombre.setError(null);
        }

        // Validar apellido
        if (apellido.isEmpty()) {
            tilApellido.setError("El apellido es requerido");
            isValid = false;
        } else if (!isValidNombreApellido(apellido)) {
            tilApellido.setError("El apellido solo puede contener letras y espacios");
            isValid = false;
        } else if (apellido.length() < 2) {
            tilApellido.setError("El apellido debe tener al menos 2 caracteres");
            isValid = false;
        } else {
            tilApellido.setError(null);
        }

        // Validar email
        if (email.isEmpty()) {
            tilEmail.setError("El correo electrónico es requerido");
            isValid = false;
        } else if (!isValidEmail(email)) {
            tilEmail.setError("Ingresa un correo electrónico válido");
            isValid = false;
        } else {
            tilEmail.setError(null);
        }

        // Validar contraseña
        if (password.isEmpty()) {
            tilPassword.setError("La contraseña es requerida");
            isValid = false;
        } else if (password.length() < 8) {
            tilPassword.setError("La contraseña debe tener al menos 8 caracteres");
            isValid = false;
        } else if (!isValidPassword(password)) {
            tilPassword.setError("La contraseña debe incluir mayúsculas, minúsculas y números");
            isValid = false;
        } else {
            tilPassword.setError(null);
        }

        // Validar edad
        if (edadStr.isEmpty()) {
            tilEdad.setError("La edad es requerida");
            isValid = false;
        } else {
            try {
                int edad = Integer.parseInt(edadStr);
                if (edad < 18 || edad > 65) {
                    tilEdad.setError("La edad debe estar entre 18 y 65 años");
                    isValid = false;
                } else {
                    tilEdad.setError(null);
                }
            } catch (NumberFormatException e) {
                tilEdad.setError("Ingresa una edad válida");
                isValid = false;
            }
        }

        // Validar teléfono con formato XXXX-XXXX
        if (telefono.isEmpty()) {
            tilTelefono.setError("El teléfono es requerido");
            isValid = false;
        } else if (!isValidTelefono(telefono)) {
            tilTelefono.setError("Formato: XXXX-XXXX (8 dígitos)");
            isValid = false;
        } else {
            tilTelefono.setError(null);
        }

        return isValid;
    }

    private boolean isValidNombreApellido(String text) {
        // Solo letras (incluyendo acentos) y espacios
        String nombrePattern = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$";
        return text.matches(nombrePattern);
    }

    private boolean isValidEmail(String email) {
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
        return email.matches(emailPattern);
    }

    private boolean isValidPassword(String password) {
        // Al menos una mayúscula, una minúscula, un número y mínimo 8 caracteres
        String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$";
        return password.matches(passwordPattern);
    }

    private boolean isValidTelefono(String telefono) {
        // Formato: 4 dígitos + guión + 4 dígitos
        String telefonoPattern = "\\d{4}-\\d{4}";
        return telefono.matches(telefonoPattern);
    }

    private void onRegisterSuccess(Usuario usuario) {
        Toast.makeText(this, "¡Registro exitoso! Bienvenido " + usuario.getNombre(), Toast.LENGTH_SHORT).show();

        // Guardar usuario y navegar al feed de donaciones
        SharedPreferencesManager.saveUser(this, usuario);
        navigateToFeedDonacion();
    }

    private void onRegisterError(String error) {
        setRegisterButtonState(true, "Registrarse");

        if (error.contains("El email ya está registrado")) {
            tilEmail.setError("Este email ya está registrado");
            Toast.makeText(this, "El email ya está en uso", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Error en registro: " + error, Toast.LENGTH_LONG).show();
        }
    }

    private void setRegisterButtonState(boolean enabled, String text) {
        btnRegistrar.setEnabled(enabled);
        btnRegistrar.setText(text);
    }

    private void navigateToFeedDonacion() {
        Intent intent = new Intent(Registro.this, FeedDonacion.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Restaurar estado del botón si vuelve a esta actividad
        setRegisterButtonState(true, "Registrarse");
    }
}