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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.ApiService;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.AppNotificationManager;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.NotificationPermissionManager;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.SecurityHelper;
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

    /**
     * Método principal que inicializa la actividad de registro
     * Configura la vista, spinners y validaciones de entrada
     */
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

    /**
     * Inicializa todos los componentes visuales de la interfaz
     * Obtiene referencias a los views del layout
     */
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

    /**
     * Configura los spinners de rol y tipo de sangre
     * Carga las opciones desde los arrays de recursos y establece listeners
     */
    private void setupSpinners() {
        // Configurar spinner de roles
        ArrayAdapter<CharSequence> rolAdapter = ArrayAdapter.createFromResource(this,
                R.array.roles_array, android.R.layout.simple_spinner_item);
        rolAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRol.setAdapter(rolAdapter);

        spinnerRol.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
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
                selectedTipoSangreId = position + 1;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedTipoSangreId = 1; // A+ por defecto
            }
        });
    }

    /**
     * Configura los listeners de clic para los botones
     * Asigna las acciones a realizar cuando se presionan los botones
     */
    private void setupClickListeners() {
        btnRegistrar.setOnClickListener(v -> registerUser());

        tvLoginLink.setOnClickListener(v -> {
            Intent intent = new Intent(Registro.this, Login.class);
            startActivity(intent);
            finish();
        });

        setupInputValidations();
        setupTelefonoFormatting();
        setupErrorClearingListeners();
    }

    /**
     * Configura las validaciones en tiempo real para los campos de entrada
     * Aplica filtros para nombre y apellido mientras el usuario escribe
     */
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
                    String filtered = input.replaceAll("[^a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]", "");
                    if (!filtered.equals(input)) {
                        etNombre.setText(filtered);
                        etNombre.setSelection(filtered.length());
                        tilNombre.setError(getString(R.string.solo_se_permiten_letras_y_espacios));
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
                    String filtered = input.replaceAll("[^a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]", "");
                    if (!filtered.equals(input)) {
                        etApellido.setText(filtered);
                        etApellido.setSelection(filtered.length());
                        tilApellido.setError(getString(R.string.solo_se_permiten_letras_y_espacios));
                    }
                } else {
                    tilApellido.setError(null);
                }
            }
        });
    }

    /**
     * Configura el formateo automático del número de teléfono
     * Aplica el formato XXXX-XXXX mientras el usuario escribe
     */
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

                if (!s.toString().equals(formatted.toString())) {
                    etTelefono.setText(formatted.toString());

                    int cursorPos = formatted.length();
                    if (phone.length() <= 4) {
                        cursorPos = phone.length();
                    } else if (phone.length() < 8) {
                        cursorPos = phone.length() + 1;
                    }
                    etTelefono.setSelection(Math.min(cursorPos, formatted.length()));
                }

                isFormatting = false;
            }
        });

        etTelefono.setOnKeyListener((v, keyCode, event) -> {
            return false;
        });
    }

    /**
     * Configura listeners para limpiar mensajes de error cuando el campo recibe foco
     * Mejora la experiencia de usuario eliminando errores al intentar corregirlos
     */
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

    /**
     * Procesa el registro del usuario
     * Valida los datos, crea el usuario y lo registra en la API
     */
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

        Usuario nuevoUsuario = new Usuario(nombre, apellido, email, password,
                edad, telefono, selectedRolId, selectedTipoSangreId);

        setRegisterButtonState(false, getString(R.string.registrando));

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

    /**
     * Valida todos los campos de entrada antes del registro
     * @param nombre Nombre a validar
     * @param apellido Apellido a validar
     * @param email Email a validar
     * @param password Contraseña a validar
     * @param edadStr Edad a validar
     * @param telefono Teléfono a validar
     * @return true si todos los campos son válidos, false en caso contrario
     */
    private boolean validateInputs(String nombre, String apellido, String email, String password,
                                   String edadStr, String telefono) {
        boolean isValid = true;

        // Validar nombre
        if (nombre.isEmpty()) {
            tilNombre.setError(getString(R.string.nombre_requerido));
            isValid = false;
        } else if (!isValidNombreApellido(nombre)) {
            tilNombre.setError(getString(R.string.nombre_solo_letras_espacios));
            isValid = false;
        } else if (nombre.length() < 2) {
            tilNombre.setError(getString(R.string.nombre_min_caracteres));
            isValid = false;
        } else {
            tilNombre.setError(null);
        }

        // Validar apellido
        if (apellido.isEmpty()) {
            tilApellido.setError(getString(R.string.apellido_requerido));
            isValid = false;
        } else if (!isValidNombreApellido(apellido)) {
            tilApellido.setError(getString(R.string.apellido_solo_letras_espacios));
            isValid = false;
        } else if (apellido.length() < 2) {
            tilApellido.setError(getString(R.string.apellido_min_caracteres));
            isValid = false;
        } else {
            tilApellido.setError(null);
        }

        // Validar email
        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.email_requerido));
            isValid = false;
        } else if (!isValidEmail(email)) {
            tilEmail.setError(getString(R.string.email_valido_requerido));
            isValid = false;
        } else {
            tilEmail.setError(null);
        }

        // Validar contraseña
        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.password_requerido));
            isValid = false;
        } else if (password.length() < 8) {
            tilPassword.setError(getString(R.string.password_min_caracteres));
            isValid = false;
        } else if (!SecurityHelper.isPasswordStrong(password)) {
            tilPassword.setError(getString(R.string.password_fuerte_requerida));
            isValid = false;
        } else {
            tilPassword.setError(null);
        }

        // Validar edad
        if (edadStr.isEmpty()) {
            tilEdad.setError(getString(R.string.edad_requerida));
            isValid = false;
        } else {
            try {
                int edad = Integer.parseInt(edadStr);
                if (edad < 18 || edad > 65) {
                    tilEdad.setError(getString(R.string.edad_rango_valido));
                    isValid = false;
                } else {
                    tilEdad.setError(null);
                }
            } catch (NumberFormatException e) {
                tilEdad.setError(getString(R.string.edad_valida_requerida));
                isValid = false;
            }
        }

        // Validar teléfono con formato XXXX-XXXX
        if (telefono.isEmpty()) {
            tilTelefono.setError(getString(R.string.telefono_requerido));
            isValid = false;
        } else if (!isValidTelefono(telefono)) {
            tilTelefono.setError(getString(R.string.formato_telefono));
            isValid = false;
        } else {
            tilTelefono.setError(null);
        }

        return isValid;
    }

    /**
     * Valida que el nombre o apellido contenga solo letras y espacios
     * @param text Texto a validar
     * @return true si es válido, false en caso contrario
     */
    private boolean isValidNombreApellido(String text) {
        String nombrePattern = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$";
        return text.matches(nombrePattern);
    }

    /**
     * Valida el formato de email usando expresión regular
     * @param email Email a validar
     * @return true si el formato es válido, false en caso contrario
     */
    private boolean isValidEmail(String email) {
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
        return email.matches(emailPattern);
    }

    /**
     * Valida el formato de la contraseña
     * @param password Contraseña a validar
     * @return true si la contraseña cumple con los requisitos de seguridad
     */
    private boolean isValidPassword(String password) {
        String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$";
        return password.matches(passwordPattern);
    }

    /**
     * Valida el formato del teléfono (XXXX-XXXX)
     * @param telefono Número de teléfono a validar
     * @return true si el formato es válido, false en caso contrario
     */
    private boolean isValidTelefono(String telefono) {
        String telefonoPattern = "\\d{4}-\\d{4}";
        return telefono.matches(telefonoPattern);
    }

    /**
     * Se ejecuta cuando el registro es exitoso
     * Guarda el usuario, solicita permisos y navega al feed principal
     * @param usuario Usuario registrado exitosamente
     */
    private void onRegisterSuccess(Usuario usuario) {
        String mensajeBienvenida = getString(R.string.registro_exitoso_bienvenida, usuario.getNombre());
        Toast.makeText(this, mensajeBienvenida, Toast.LENGTH_SHORT).show();

        SharedPreferencesManager.saveUser(this, usuario);

        if (!NotificationPermissionManager.areNotificationsEnabled(this)) {
            NotificationPermissionManager.requestNotificationPermission(this);
        } else {
            if (AppNotificationManager.areNotificationsEnabled(this)) {
                AppNotificationManager.startNotificationService(this);
            }
            navigateToFeedDonacion();
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

        if (NotificationPermissionManager.handlePermissionResult(requestCode, grantResults)) {
            if (AppNotificationManager.areNotificationsEnabled(this)) {
                AppNotificationManager.startNotificationService(this);
            }
        }
        navigateToFeedDonacion();
    }

    /**
     * Se ejecuta cuando ocurre un error en el registro
     * Muestra mensajes de error específicos según el tipo de error
     * @param error Mensaje de error recibido
     */
    private void onRegisterError(String error) {
        setRegisterButtonState(true, getString(R.string.registrarse));

        if (error.contains("El email ya está registrado")) {
            tilEmail.setError(getString(R.string.email_ya_registrado));
            Toast.makeText(this, R.string.email_en_uso, Toast.LENGTH_LONG).show();
        } else {
            String errorRegistro = getString(R.string.error_registro, error);
            Toast.makeText(this, errorRegistro, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Cambia el estado del botón de registro (habilitado/deshabilitado)
     * @param enabled true para habilitar, false para deshabilitar
     * @param text Texto a mostrar en el botón
     */
    private void setRegisterButtonState(boolean enabled, String text) {
        btnRegistrar.setEnabled(enabled);
        btnRegistrar.setText(text);
    }

    /**
     * Navega a la actividad principal del feed de donaciones
     * Limpia el stack de actividades
     */
    private void navigateToFeedDonacion() {
        Intent intent = new Intent(Registro.this, FeedDonacion.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Método del ciclo de vida que se ejecuta al reanudar la actividad
     * Restaura el estado del botón de registro
     */
    @Override
    protected void onResume() {
        super.onResume();
        setRegisterButtonState(true, getString(R.string.registrarse));
    }
}