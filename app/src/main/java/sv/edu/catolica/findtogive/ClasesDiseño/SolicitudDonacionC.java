package sv.edu.catolica.findtogive.ClasesDiseño;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.ApiService;
import sv.edu.catolica.findtogive.Modelado.HospitalUbicacion;
import sv.edu.catolica.findtogive.Modelado.SolicitudDonacion;
import sv.edu.catolica.findtogive.Modelado.Usuario;
import sv.edu.catolica.findtogive.R;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.SharedPreferencesManager;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.StorageService;

public class SolicitudDonacionC extends AppCompatActivity {

    private TextInputEditText editTextTitulo, editTextDescripcion;
    private TextInputLayout inputLayoutTitulo, inputLayoutDescripcion;
    private Spinner spinnerTipoSangre, spinnerHospital;
    private Button btnEnviarSolicitud;
    private View layoutUploadImage;
    private ImageView imagePreview;

    private int selectedTipoSangreId = 1; // A+ por defecto
    private int selectedHospitalId = 1; // Primer hospital por defecto
    private Uri selectedImageUri = null;
    private Usuario usuarioActual;
    private List<HospitalUbicacion> hospitalesList;
    private ArrayAdapter<HospitalUbicacion> hospitalAdapter;

    private BottomNavigationView bottomNavigation;

    // Activity result launcher para seleccionar imágenes
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        // Mostrar preview de la imagen
                        if (imagePreview != null) {
                            imagePreview.setImageURI(selectedImageUri);
                            imagePreview.setVisibility(View.VISIBLE);
                        }
                        // Cambiar el texto del layout para indicar que hay imagen seleccionada
                        TextView uploadText = layoutUploadImage.findViewById(R.id.upload_text);
                        if (uploadText != null) {
                            uploadText.setText("Imagen seleccionada - Toca para cambiar");
                        }
                    }
                }
            }
    );

    private String obtenerNombreRol() {
        if (usuarioActual == null) return "Desconocido";

        switch (usuarioActual.getRolid()) {
            case 1: return "Donante";
            case 2: return "Receptor";
            case 3: return "Ambos";
            default: return "Desconocido";
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Configurar para que el teclado no empuje el contenido
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        setContentView(R.layout.desing_solicitud_donacion);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Obtener usuario actual
        usuarioActual = SharedPreferencesManager.getCurrentUser(this);

        // Verificar si puede crear solicitudes (solo rolid=2 y 3)
        if (!puedeCrearSolicitudes()) {
            mostrarErrorRol();
            return;
        }

        initializeViews();
        setupSpinners();
        loadHospitales();
        setupClickListeners();
        setupBottomNavigation();
    }

    private boolean puedeCrearSolicitudes() {
        if (usuarioActual == null) {
            return false;
        }

        // Solo receptores (rolid=2) y ambos (rolid=3) pueden crear solicitudes
        // Donantes (rolid=1) NO pueden crear solicitudes
        return usuarioActual.getRolid() == 2 || usuarioActual.getRolid() == 3;
    }

    private void mostrarErrorRol() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Acceso restringido")
                .setMessage("Los donantes no pueden crear solicitudes de donación. Tu rol actual es: " + obtenerNombreRol())
                .setPositiveButton("Ir al Feed", (dialog, which) -> {
                    Intent intent = new Intent(this, FeedDonacion.class);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cerrar", (dialog, which) -> {
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void initializeViews() {
        // Text inputs
        inputLayoutTitulo = findViewById(R.id.input_layout_titulo);
        inputLayoutDescripcion = findViewById(R.id.input_layout_descripcion);
        editTextTitulo = findViewById(R.id.edit_text_titulo);
        editTextDescripcion = findViewById(R.id.edit_text_descripcion);

        // Spinners
        spinnerTipoSangre = findViewById(R.id.spinner_tipo_sangre);
        spinnerHospital = findViewById(R.id.spinner_hospital);

        // Upload image section
        layoutUploadImage = findViewById(R.id.layout_upload_image);

        // Image preview
        imagePreview = findViewById(R.id.image_preview);

        // Button
        btnEnviarSolicitud = findViewById(R.id.btn_enviar_solicitud);

        bottomNavigation = findViewById(R.id.bottom_navigation_bar);
    }

    private void setupSpinners() {
        // Configurar spinner de tipos de sangre
        ArrayAdapter<CharSequence> sangreAdapter = ArrayAdapter.createFromResource(this,
                R.array.tipos_sangre_array, android.R.layout.simple_spinner_item);
        sangreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipoSangre.setAdapter(sangreAdapter);

        spinnerTipoSangre.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedTipoSangreId = position + 1; // 1=A+, 2=A-, etc.
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedTipoSangreId = 1; // A+ por defecto
            }
        });

        // Configurar spinner de hospitales (inicialmente vacío)
        hospitalesList = new ArrayList<>();
        hospitalAdapter = new ArrayAdapter<HospitalUbicacion>(this,
                android.R.layout.simple_spinner_item, hospitalesList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                if (position >= 0 && position < hospitalesList.size()) {
                    textView.setText(hospitalesList.get(position).getNombre());
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                if (position >= 0 && position < hospitalesList.size()) {
                    textView.setText(hospitalesList.get(position).getNombre());
                }
                return view;
            }
        };
        hospitalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHospital.setAdapter(hospitalAdapter);

        spinnerHospital.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < hospitalesList.size()) {
                    selectedHospitalId = hospitalesList.get(position).getHospitalid();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (!hospitalesList.isEmpty()) {
                    selectedHospitalId = hospitalesList.get(0).getHospitalid();
                }
            }
        });
    }

    private void loadHospitales() {
        ApiService.getHospitales(new ApiService.ListCallback<HospitalUbicacion>() {
            @Override
            public void onSuccess(List<HospitalUbicacion> hospitales) {
                runOnUiThread(() -> {
                    if (hospitales != null && !hospitales.isEmpty()) {
                        hospitalesList.clear();
                        hospitalesList.addAll(hospitales);
                        hospitalAdapter.notifyDataSetChanged();

                        // Seleccionar el primer hospital por defecto
                        if (!hospitalesList.isEmpty()) {
                            selectedHospitalId = hospitalesList.get(0).getHospitalid();
                        }
                    } else {
                        Toast.makeText(SolicitudDonacionC.this,
                                "No se pudieron cargar los hospitales", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(SolicitudDonacionC.this,
                            "Error al cargar hospitales: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setupClickListeners() {
        // Botón de enviar solicitud
        btnEnviarSolicitud.setOnClickListener(v -> crearSolicitud());

        // Sección de upload de imagen
        layoutUploadImage.setOnClickListener(v -> seleccionarImagen());

        // Limpiar errores al escribir
        editTextTitulo.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) inputLayoutTitulo.setError(null);
        });

        editTextDescripcion.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) inputLayoutDescripcion.setError(null);
        });
    }

    private void seleccionarImagen() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void crearSolicitud() {
        String titulo = editTextTitulo.getText().toString().trim();
        String descripcion = editTextDescripcion.getText().toString().trim();

        if (!validarInputs(titulo, descripcion)) {
            return;
        }

        // Obtener usuario actual
        Usuario usuarioActual = SharedPreferencesManager.getCurrentUser(this);
        if (usuarioActual == null) {
            Toast.makeText(this, "Error: No se pudo obtener información del usuario", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear objeto solicitud
        SolicitudDonacion solicitud = new SolicitudDonacion(
                usuarioActual.getUsuarioid(),
                titulo,
                descripcion,
                selectedTipoSangreId,
                selectedHospitalId
        );

        // Deshabilitar botón mientras se procesa
        btnEnviarSolicitud.setEnabled(false);
        btnEnviarSolicitud.setText("Creando solicitud...");

        // Primero crear la solicitud en la base de datos
        ApiService.createSolicitud(solicitud, new ApiService.ApiCallback<SolicitudDonacion>() {
            @Override
            public void onSuccess(SolicitudDonacion solicitudCreada) {
                // Si hay imagen seleccionada, subirla
                if (selectedImageUri != null) {
                    subirImagenSolicitud(solicitudCreada);
                } else {
                    // Si no hay imagen, finalizar
                    runOnUiThread(() -> {
                        onSolicitudCreadaExitosamente(solicitudCreada);
                    });
                }
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnEnviarSolicitud.setEnabled(true);
                    btnEnviarSolicitud.setText("Enviar solicitud");
                    Toast.makeText(SolicitudDonacionC.this,
                            "Error al crear solicitud: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void subirImagenSolicitud(SolicitudDonacion solicitud) {
        // Verificar que tenemos un ID válido
        if (solicitud.getSolicitudid() <= 0) {
            runOnUiThread(() -> {
                Toast.makeText(SolicitudDonacionC.this,
                        "Error: ID de solicitud inválido", Toast.LENGTH_LONG).show();
                onSolicitudCreadaExitosamente(solicitud);
            });
            return;
        }

        StorageService.uploadSolicitudImage(this, selectedImageUri, solicitud.getSolicitudid(),
                new StorageService.UploadCallback() {
                    @Override
                    public void onSuccess(String imageUrl) {
                        System.out.println("✅ Imagen subida: " + imageUrl);

                        // Actualizar la solicitud con la URL de la imagen
                        solicitud.setImagenUrl(imageUrl);
                        actualizarSolicitudConImagen(solicitud);
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            System.out.println("❌ Error subiendo imagen: " + error);
                            // Aún así la solicitud se creó, solo que sin imagen
                            Toast.makeText(SolicitudDonacionC.this,
                                    "Solicitud creada pero error al subir imagen: " + error,
                                    Toast.LENGTH_LONG).show();
                            onSolicitudCreadaExitosamente(solicitud);
                        });
                    }
                });
    }

    private void actualizarSolicitudConImagen(SolicitudDonacion solicitud) {
        ApiService.updateSolicitud(solicitud, new ApiService.ApiCallback<SolicitudDonacion>() {
            @Override
            public void onSuccess(SolicitudDonacion solicitudActualizada) {
                runOnUiThread(() -> {
                    onSolicitudCreadaExitosamente(solicitudActualizada);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    // Aún así la solicitud se creó
                    Toast.makeText(SolicitudDonacionC.this,
                            "Solicitud creada pero error al actualizar imagen: " + error,
                            Toast.LENGTH_SHORT).show();
                    onSolicitudCreadaExitosamente(solicitud);
                });
            }
        });
    }

    private void onSolicitudCreadaExitosamente(SolicitudDonacion solicitud) {
        Toast.makeText(this, "¡Solicitud creada exitosamente!", Toast.LENGTH_SHORT).show();

        // Regresar al feed de donaciones
        Intent intent = new Intent(this, FeedDonacion.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private boolean validarInputs(String titulo, String descripcion) {
        boolean isValid = true;

        // Validar título
        if (titulo.isEmpty()) {
            inputLayoutTitulo.setError("El título es requerido");
            isValid = false;
        } else if (titulo.length() < 5) {
            inputLayoutTitulo.setError("El título debe tener al menos 5 caracteres");
            isValid = false;
        } else {
            inputLayoutTitulo.setError(null);
        }

        // Validar descripción
        if (descripcion.isEmpty()) {
            inputLayoutDescripcion.setError("La descripción es requerida");
            isValid = false;
        } else if (descripcion.length() < 10) {
            inputLayoutDescripcion.setError("La descripción debe tener al menos 10 caracteres");
            isValid = false;
        } else {
            inputLayoutDescripcion.setError(null);
        }

        // Validar que se haya seleccionado un hospital
        if (hospitalesList.isEmpty()) {
            Toast.makeText(this, "No hay hospitales disponibles", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_inicio) {
                Intent intent = new Intent(this, FeedDonacion.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_crear) {
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
                Intent intent = new Intent(this, PerfilUsuario.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            }
            return false;
        });

        bottomNavigation.setSelectedItemId(R.id.nav_crear);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigation.setSelectedItemId(R.id.nav_crear);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Limpiar recursos si es necesario
    }
}