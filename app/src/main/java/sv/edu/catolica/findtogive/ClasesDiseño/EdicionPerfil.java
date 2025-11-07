package sv.edu.catolica.findtogive.ClasesDiseño;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.ApiService;
import sv.edu.catolica.findtogive.Modelado.Notificacion;
import sv.edu.catolica.findtogive.Modelado.Usuario;
import sv.edu.catolica.findtogive.R;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.SharedPreferencesManager;

public class EdicionPerfil extends AppCompatActivity {

    private Usuario usuarioActual;
    private TextInputEditText editTextNombre, editTextApellido, editTextMail, editTextTelefono, editTextUbicacion;
    private TextInputLayout inputLayoutNombre, inputLayoutApellido, inputLayoutMail, inputLayoutTelefono, inputLayoutUbicacion;
    private Spinner spinnerRol, spinnerTipoSangre;
    private Button btnGuardarCambios;

    private SharedPreferences cambiosPrefs;

    // Variables para guardar los valores originales
    private String nombreOriginal, apellidoOriginal, emailOriginal, telefonoOriginal;
    private int rolOriginal, tipoSangreOriginal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.desing_edicion_perfil);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        loadUserData();
        guardarValoresOriginales();
        setupSpinners();
        setupInputValidations();
        setupClickListeners();
    }

    private void initViews() {
        // TextInputLayouts
        inputLayoutNombre = findViewById(R.id.input_layout_nombre);
        inputLayoutApellido = findViewById(R.id.input_layout_apellido);
        inputLayoutMail = findViewById(R.id.input_layout_mail);
        inputLayoutTelefono = findViewById(R.id.input_layout_telefono);
        inputLayoutUbicacion = findViewById(R.id.input_layout_ubicacion);

        // TextInputEditTexts
        editTextNombre = findViewById(R.id.edit_text_nombre);
        editTextApellido = findViewById(R.id.edit_text_apellido);
        editTextMail = findViewById(R.id.edit_text_mail);
        editTextTelefono = findViewById(R.id.edit_text_telefono);
        editTextUbicacion = findViewById(R.id.edit_text_ubicacion);

        // Spinners
        spinnerRol = findViewById(R.id.spinner_rol);
        spinnerTipoSangre = findViewById(R.id.spinner_tipo_sangre);

        // Botón
        btnGuardarCambios = findViewById(R.id.btn_guardar_cambios);
    }

    private void loadUserData() {
        usuarioActual = SharedPreferencesManager.getCurrentUser(this);

        if (usuarioActual != null) {
            // Cargar datos en los campos
            editTextNombre.setText(usuarioActual.getNombre());
            editTextApellido.setText(usuarioActual.getApellido());
            editTextMail.setText(usuarioActual.getEmail());
            editTextTelefono.setText(usuarioActual.getTelefono());

            // Ubicación fija - Santa Ana, El Salvador
            editTextUbicacion.setText("Santa Ana, El Salvador");
            editTextUbicacion.setEnabled(false); // Hacer el campo no editable
            inputLayoutUbicacion.setHint("Ubicación (Fija)");

        } else {
            Toast.makeText(this, "Error: No se pudo cargar la información del usuario", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void guardarValoresOriginales() {
        if (usuarioActual != null) {
            nombreOriginal = usuarioActual.getNombre();
            apellidoOriginal = usuarioActual.getApellido();
            emailOriginal = usuarioActual.getEmail();
            telefonoOriginal = usuarioActual.getTelefono();
            rolOriginal = usuarioActual.getRolid();
            tipoSangreOriginal = usuarioActual.getTiposangreid();
        }
    }

    private void setupSpinners() {
        // Configurar spinner de roles
        ArrayAdapter<CharSequence> rolAdapter = ArrayAdapter.createFromResource(this,
                R.array.roles_array, android.R.layout.simple_spinner_item);
        rolAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRol.setAdapter(rolAdapter);

        // Configurar spinner de tipos de sangre
        ArrayAdapter<CharSequence> sangreAdapter = ArrayAdapter.createFromResource(this,
                R.array.tipos_sangre_array, android.R.layout.simple_spinner_item);
        sangreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipoSangre.setAdapter(sangreAdapter);

        // Establecer selecciones actuales
        if (usuarioActual != null) {
            spinnerRol.setSelection(usuarioActual.getRolid() - 1); // Asumiendo que los IDs empiezan en 1
            spinnerTipoSangre.setSelection(usuarioActual.getTiposangreid() - 1); // Asumiendo que los IDs empiezan en 1
        }
    }

    private void setupInputValidations() {
        // Filtro para nombre - solo letras y espacios
        editTextNombre.addTextChangedListener(new TextWatcher() {
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
                        editTextNombre.setText(filtered);
                        editTextNombre.setSelection(filtered.length());
                        inputLayoutNombre.setError("Solo se permiten letras y espacios");
                    }
                } else {
                    inputLayoutNombre.setError(null);
                }
            }
        });

        // Filtro para apellido - solo letras y espacios
        editTextApellido.addTextChangedListener(new TextWatcher() {
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
                        editTextApellido.setText(filtered);
                        editTextApellido.setSelection(filtered.length());
                        inputLayoutApellido.setError("Solo se permiten letras y espacios");
                    }
                } else {
                    inputLayoutApellido.setError(null);
                }
            }
        });

        // Validación de email en tiempo real
        editTextMail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String email = s.toString().trim();
                if (!email.isEmpty() && !isValidEmail(email)) {
                    inputLayoutMail.setError("Ingresa un correo electrónico válido");
                } else {
                    inputLayoutMail.setError(null);
                }
            }
        });

        // Configurar formateo del teléfono (igual que en Registro)
        setupTelefonoFormatting();

        // Limpiar errores al enfocar campos
        setupErrorClearingListeners();
    }

    private void setupTelefonoFormatting() {
        editTextTelefono.addTextChangedListener(new TextWatcher() {
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
                    editTextTelefono.setText(formatted.toString());

                    // Posicionar el cursor correctamente
                    int cursorPos = formatted.length();
                    if (phone.length() <= 4) {
                        cursorPos = phone.length();
                    } else if (phone.length() < 8) {
                        cursorPos = phone.length() + 1; // +1 por el guión
                    }
                    editTextTelefono.setSelection(Math.min(cursorPos, formatted.length()));
                }

                // Validar formato en tiempo real
                String currentText = formatted.toString();
                if (!currentText.isEmpty() && !isValidTelefono(currentText)) {
                    inputLayoutTelefono.setError("Formato: XXXX-XXXX (8 dígitos)");
                } else {
                    inputLayoutTelefono.setError(null);
                }

                isFormatting = false;
            }
        });

        // Mejorar la experiencia de edición permitiendo selección
        editTextTelefono.setOnKeyListener((v, keyCode, event) -> {
            // Permitir navegación y selección sin interferencias
            return false;
        });
    }

    private void setupErrorClearingListeners() {
        editTextNombre.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) inputLayoutNombre.setError(null);
        });

        editTextApellido.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) inputLayoutApellido.setError(null);
        });

        editTextMail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) inputLayoutMail.setError(null);
        });

        editTextTelefono.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) inputLayoutTelefono.setError(null);
        });
    }

    private void setupClickListeners() {
        btnGuardarCambios.setOnClickListener(v -> {
            guardarCambios();
        });
    }

    private void guardarCambios() {
        String nombre = editTextNombre.getText().toString().trim();
        String apellido = editTextApellido.getText().toString().trim();
        String email = editTextMail.getText().toString().trim();
        String telefono = editTextTelefono.getText().toString().trim();
        String ubicacion = editTextUbicacion.getText().toString().trim();

        if (!validateInputs(nombre, apellido, email, telefono)) {
            return;
        }

        // Detectar qué campos cambiaron
        List<String> camposCambiados = detectarCambios(nombre, apellido, email, telefono);

        // Si no hay cambios, mostrar mensaje y salir
        if (camposCambiados.isEmpty()) {
            Toast.makeText(this, "No se detectaron cambios para guardar", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear usuario actualizado
        Usuario usuarioActualizado = new Usuario();
        usuarioActualizado.setUsuarioid(usuarioActual.getUsuarioid());
        usuarioActualizado.setNombre(nombre);
        usuarioActualizado.setApellido(apellido);
        usuarioActualizado.setEmail(email); // Incluir el email actualizado
        usuarioActualizado.setTelefono(telefono);
        usuarioActualizado.setUbicacion(ubicacion);
        usuarioActualizado.setEdad(usuarioActual.getEdad()); // Mantener edad actual
        usuarioActualizado.setFotoUrl(usuarioActual.getFotoUrl()); // Mantener foto

        // Obtener nuevos valores de los spinners
        usuarioActualizado.setRolid(spinnerRol.getSelectedItemPosition() + 1);
        usuarioActualizado.setTiposangreid(spinnerTipoSangre.getSelectedItemPosition() + 1);

        // Mostrar loading
        setGuardarButtonState(false, "Guardando...");

        // Actualizar en la base de datos
        ApiService.updateUser(usuarioActualizado, new ApiService.ApiCallback<Usuario>() {
            @Override
            public void onSuccess(Usuario usuarioActualizado) {
                runOnUiThread(() -> {
                    // Actualizar SharedPreferences con todos los datos, incluyendo el email
                    SharedPreferencesManager.saveUser(EdicionPerfil.this, usuarioActualizado);

                    // Crear notificaciones para los campos cambiados
                    crearNotificacionesCambios(camposCambiados);

                    Toast.makeText(EdicionPerfil.this, "Perfil actualizado exitosamente", Toast.LENGTH_SHORT).show();

                    // Actualizar valores originales
                    guardarValoresOriginales();

                    // Notificar a PerfilUsuario que los datos cambiaron
                    setResult(RESULT_OK);
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    setGuardarButtonState(true, "Guardar Cambios");

                    // Detectar específicamente errores de email duplicado
                    if (isEmailDuplicateError(error)) {
                        inputLayoutMail.setError("Este email ya está registrado");
                        Toast.makeText(EdicionPerfil.this, "El email ya está en uso por otro usuario", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(EdicionPerfil.this, "Error al actualizar perfil: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private List<String> detectarCambios(String nombre, String apellido, String email, String telefono) {
        List<String> cambios = new ArrayList<>();

        // Verificar cambios en cada campo
        if (!nombre.equals(nombreOriginal)) {
            cambios.add("nombre");
        }
        if (!apellido.equals(apellidoOriginal)) {
            cambios.add("apellido");
        }
        if (!email.equals(emailOriginal)) {
            cambios.add("email");
        }
        if (!telefono.equals(telefonoOriginal)) {
            cambios.add("teléfono");
        }

        // Verificar cambios en spinners
        int nuevoRol = spinnerRol.getSelectedItemPosition() + 1;
        int nuevoTipoSangre = spinnerTipoSangre.getSelectedItemPosition() + 1;

        if (nuevoRol != rolOriginal) {
            cambios.add("rol");
        }
        if (nuevoTipoSangre != tipoSangreOriginal) {
            cambios.add("tipo de sangre");
        }

        System.out.println("🔍 Campos cambiados detectados: " + cambios);
        return cambios;
    }

    private void crearNotificacionesCambios(List<String> camposCambiados) {
        if (camposCambiados.isEmpty() || usuarioActual == null) {
            return;
        }

        System.out.println("🎯 Creando notificaciones para cambios en perfil");

        // Crear una notificación por cada campo cambiado
        for (String campo : camposCambiados) {
            String titulo = "Cambios en el perfil";
            String mensaje = generarMensajeCambio(campo);

            Notificacion notificacion = new Notificacion(usuarioActual.getUsuarioid(), titulo, mensaje);

            ApiService.createNotificacion(notificacion, new ApiService.ApiCallback<Notificacion>() {
                @Override
                public void onSuccess(Notificacion result) {
                    System.out.println("✅ Notificación creada para cambio en: " + campo);
                }

                @Override
                public void onError(String error) {
                    System.out.println("❌ Error creando notificación para " + campo + ": " + error);
                }
            });
        }
    }

    private String generarMensajeCambio(String campo) {
        switch (campo) {
            case "nombre":
                return "Tu nombre ha sido actualizado";
            case "apellido":
                return "Tu apellido ha sido actualizado";
            case "email":
                return "Tu email ha sido actualizado";
            case "teléfono":
                return "Tu teléfono ha sido actualizado";
            case "rol":
                return "Tu rol ha sido actualizado";
            case "tipo de sangre":
                return "Tu tipo de sangre ha sido actualizado";
            default:
                return "Un campo de tu perfil ha sido actualizado";
        }
    }

    private boolean isEmailDuplicateError(String error) {
        if (error == null) return false;

        String lowerError = error.toLowerCase();

        // Buscar patrones comunes de errores de duplicación
        return lowerError.contains("duplicate") ||
                lowerError.contains("unique constraint") ||
                lowerError.contains("already exists") ||
                lowerError.contains("email ya está") ||
                lowerError.contains("email already") ||
                lowerError.contains("23505") || // Código de error PostgreSQL para unique violation
                lowerError.contains("violates unique constraint") ||
                (lowerError.contains("email") && lowerError.contains("already"));
    }

    private boolean validateInputs(String nombre, String apellido, String email, String telefono) {
        boolean isValid = true;

        // Validar nombre
        if (nombre.isEmpty()) {
            inputLayoutNombre.setError("El nombre es requerido");
            isValid = false;
        } else if (!isValidNombreApellido(nombre)) {
            inputLayoutNombre.setError("El nombre solo puede contener letras y espacios");
            isValid = false;
        } else if (nombre.length() < 2) {
            inputLayoutNombre.setError("El nombre debe tener al menos 2 caracteres");
            isValid = false;
        } else {
            inputLayoutNombre.setError(null);
        }

        // Validar apellido
        if (apellido.isEmpty()) {
            inputLayoutApellido.setError("El apellido es requerido");
            isValid = false;
        } else if (!isValidNombreApellido(apellido)) {
            inputLayoutApellido.setError("El apellido solo puede contener letras y espacios");
            isValid = false;
        } else if (apellido.length() < 2) {
            inputLayoutApellido.setError("El apellido debe tener al menos 2 caracteres");
            isValid = false;
        } else {
            inputLayoutApellido.setError(null);
        }

        // Validar email
        if (email.isEmpty()) {
            inputLayoutMail.setError("El correo electrónico es requerido");
            isValid = false;
        } else if (!isValidEmail(email)) {
            inputLayoutMail.setError("Ingresa un correo electrónico válido");
            isValid = false;
        } else {
            inputLayoutMail.setError(null);
        }

        // Validar teléfono con formato XXXX-XXXX
        if (telefono.isEmpty()) {
            inputLayoutTelefono.setError("El teléfono es requerido");
            isValid = false;
        } else if (!isValidTelefono(telefono)) {
            inputLayoutTelefono.setError("Formato: XXXX-XXXX (8 dígitos)");
            isValid = false;
        } else {
            inputLayoutTelefono.setError(null);
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

    private boolean isValidTelefono(String telefono) {
        // Formato: 4 dígitos + guión + 4 dígitos
        String telefonoPattern = "\\d{4}-\\d{4}";
        return telefono.matches(telefonoPattern);
    }

    private void setGuardarButtonState(boolean enabled, String text) {
        btnGuardarCambios.setEnabled(enabled);
        btnGuardarCambios.setText(text);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Restaurar estado del botón si vuelve a esta actividad
        setGuardarButtonState(true, "Guardar Cambios");
    }
}