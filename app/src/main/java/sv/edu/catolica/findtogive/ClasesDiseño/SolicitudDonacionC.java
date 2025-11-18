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

    /**
     * Activity result launcher para seleccionar imágenes de la galería
     */
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        if (imagePreview != null) {
                            imagePreview.setImageURI(selectedImageUri);
                            imagePreview.setVisibility(View.VISIBLE);
                        }
                        TextView uploadText = layoutUploadImage.findViewById(R.id.upload_text);
                        if (uploadText != null) {
                            uploadText.setText(getString(R.string.imagen_seleccionada_cambiar));
                        }
                    }
                }
            }
    );

    /**
     * Obtiene el nombre del rol del usuario actual
     * @return Nombre del rol (Donante, Receptor, Ambos)
     */
    private String obtenerNombreRol() {
        if (usuarioActual == null) return getString(R.string.desconocido);

        switch (usuarioActual.getRolid()) {
            case 1: return getString(R.string.rol_donante);
            case 2: return getString(R.string.rol_receptor);
            case 3: return getString(R.string.rol_ambos);
            default: return getString(R.string.desconocido);
        }
    }

    /**
     * Método principal que inicializa la actividad de creación de solicitud
     * Configura la vista, verifica permisos y carga los datos necesarios
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        setContentView(R.layout.desing_solicitud_donacion);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        usuarioActual = SharedPreferencesManager.getCurrentUser(this);

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

    /**
     * Verifica si el usuario actual puede crear solicitudes de donación
     * @return true si el usuario es receptor o ambos, false si es donante
     */
    private boolean puedeCrearSolicitudes() {
        if (usuarioActual == null) {
            return false;
        }

        return usuarioActual.getRolid() == 2 || usuarioActual.getRolid() == 3;
    }

    /**
     * Muestra un diálogo de error cuando el usuario no tiene permisos para crear solicitudes
     * Redirige al feed principal después de mostrar el mensaje
     */
    private void mostrarErrorRol() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.acceso_restringido)
                .setMessage(getString(R.string.donantes_no_pueden_crear_solicitudes, obtenerNombreRol()))
                .setPositiveButton(R.string.ir_al_feed, (dialog, which) -> {
                    Intent intent = new Intent(this, FeedDonacion.class);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton(R.string.cerrar, (dialog, which) -> {
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Inicializa todos los componentes visuales de la interfaz
     * Obtiene referencias a los views del layout
     */
    private void initializeViews() {
        inputLayoutTitulo = findViewById(R.id.input_layout_titulo);
        inputLayoutDescripcion = findViewById(R.id.input_layout_descripcion);
        editTextTitulo = findViewById(R.id.edit_text_titulo);
        editTextDescripcion = findViewById(R.id.edit_text_descripcion);

        spinnerTipoSangre = findViewById(R.id.spinner_tipo_sangre);
        spinnerHospital = findViewById(R.id.spinner_hospital);

        layoutUploadImage = findViewById(R.id.layout_upload_image);
        imagePreview = findViewById(R.id.image_preview);

        btnEnviarSolicitud = findViewById(R.id.btn_enviar_solicitud);
        bottomNavigation = findViewById(R.id.bottom_navigation_bar);
    }

    /**
     * Configura los spinners de tipo de sangre y hospitales
     * Establece los adapters y listeners para cada spinner
     */
    private void setupSpinners() {
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
                selectedTipoSangreId = 1;
            }
        });

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

    /**
     * Carga la lista de hospitales desde la API
     * Actualiza el spinner de hospitales con los datos obtenidos
     */
    private void loadHospitales() {
        ApiService.getHospitales(new ApiService.ListCallback<HospitalUbicacion>() {
            @Override
            public void onSuccess(List<HospitalUbicacion> hospitales) {
                runOnUiThread(() -> {
                    if (hospitales != null && !hospitales.isEmpty()) {
                        hospitalesList.clear();
                        hospitalesList.addAll(hospitales);
                        hospitalAdapter.notifyDataSetChanged();

                        if (!hospitalesList.isEmpty()) {
                            selectedHospitalId = hospitalesList.get(0).getHospitalid();
                        }
                    } else {
                        Toast.makeText(SolicitudDonacionC.this,
                                R.string.error_cargar_hospitales, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(SolicitudDonacionC.this,
                            getString(R.string.error_cargar_hospitales_detalle, error), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Configura los listeners de clic para los botones y campos
     * Asigna las acciones a realizar cuando se interactúa con los elementos
     */
    private void setupClickListeners() {
        btnEnviarSolicitud.setOnClickListener(v -> crearSolicitud());

        layoutUploadImage.setOnClickListener(v -> seleccionarImagen());

        editTextTitulo.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) inputLayoutTitulo.setError(null);
        });

        editTextDescripcion.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) inputLayoutDescripcion.setError(null);
        });
    }

    /**
     * Abre el selector de imágenes para que el usuario seleccione una foto
     * Utiliza el launcher de activity result para manejar la selección
     */
    private void seleccionarImagen() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    /**
     * Crea una nueva solicitud de donación
     * Valida los datos, crea el objeto solicitud y lo envía a la API
     */
    private void crearSolicitud() {
        String titulo = editTextTitulo.getText().toString().trim();
        String descripcion = editTextDescripcion.getText().toString().trim();

        if (!validarInputs(titulo, descripcion)) {
            return;
        }

        Usuario usuarioActual = SharedPreferencesManager.getCurrentUser(this);
        if (usuarioActual == null) {
            Toast.makeText(this, R.string.error_obtener_info_usuario, Toast.LENGTH_SHORT).show();
            return;
        }

        SolicitudDonacion solicitud = new SolicitudDonacion(
                usuarioActual.getUsuarioid(),
                titulo,
                descripcion,
                selectedTipoSangreId,
                selectedHospitalId
        );

        btnEnviarSolicitud.setEnabled(false);
        btnEnviarSolicitud.setText(getString(R.string.creando_solicitud));

        ApiService.createSolicitud(solicitud, new ApiService.ApiCallback<SolicitudDonacion>() {
            @Override
            public void onSuccess(SolicitudDonacion solicitudCreada) {
                if (selectedImageUri != null) {
                    subirImagenSolicitud(solicitudCreada);
                } else {
                    runOnUiThread(() -> {
                        onSolicitudCreadaExitosamente(solicitudCreada);
                    });
                }
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnEnviarSolicitud.setEnabled(true);
                    btnEnviarSolicitud.setText(getString(R.string.enviar_solicitud));
                    Toast.makeText(SolicitudDonacionC.this,
                            getString(R.string.error_crear_solicitud, error), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Sube la imagen de la solicitud al servidor de almacenamiento
     * @param solicitud Solicitud a la que se asociará la imagen
     */
    private void subirImagenSolicitud(SolicitudDonacion solicitud) {
        if (solicitud.getSolicitudid() <= 0) {
            runOnUiThread(() -> {
                Toast.makeText(SolicitudDonacionC.this,
                        R.string.error_id_solicitud_invalido, Toast.LENGTH_LONG).show();
                onSolicitudCreadaExitosamente(solicitud);
            });
            return;
        }

        StorageService.uploadSolicitudImage(this, selectedImageUri, solicitud.getSolicitudid(),
                new StorageService.UploadCallback() {
                    @Override
                    public void onSuccess(String imageUrl) {
                        solicitud.setImagenUrl(imageUrl);
                        actualizarSolicitudConImagen(solicitud);
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(SolicitudDonacionC.this,
                                    getString(R.string.error_subir_imagen_solicitud, error),
                                    Toast.LENGTH_LONG).show();
                            onSolicitudCreadaExitosamente(solicitud);
                        });
                    }
                });
    }

    /**
     * Actualiza la solicitud con la URL de la imagen subida
     * @param solicitud Solicitud a actualizar con la URL de la imagen
     */
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
                    Toast.makeText(SolicitudDonacionC.this,
                            getString(R.string.error_actualizar_imagen_solicitud, error),
                            Toast.LENGTH_SHORT).show();
                    onSolicitudCreadaExitosamente(solicitud);
                });
            }
        });
    }

    /**
     * Se ejecuta cuando la solicitud se crea exitosamente
     * Muestra mensaje de éxito y navega al feed principal
     * @param solicitud Solicitud creada exitosamente
     */
    private void onSolicitudCreadaExitosamente(SolicitudDonacion solicitud) {
        Toast.makeText(this, R.string.solicitud_creada_exitosamente, Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, FeedDonacion.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    /**
     * Valida todos los campos de entrada antes de crear la solicitud
     * @param titulo Título de la solicitud
     * @param descripcion Descripción de la solicitud
     * @return true si todos los campos son válidos, false en caso contrario
     */
    private boolean validarInputs(String titulo, String descripcion) {
        boolean isValid = true;

        if (titulo.isEmpty()) {
            inputLayoutTitulo.setError(getString(R.string.titulo_requerido));
            isValid = false;
        } else if (titulo.length() < 5) {
            inputLayoutTitulo.setError(getString(R.string.titulo_min_caracteres));
            isValid = false;
        } else {
            inputLayoutTitulo.setError(null);
        }

        if (descripcion.isEmpty()) {
            inputLayoutDescripcion.setError(getString(R.string.descripcion_requerida));
            isValid = false;
        } else if (descripcion.length() < 10) {
            inputLayoutDescripcion.setError(getString(R.string.descripcion_min_caracteres));
            isValid = false;
        } else {
            inputLayoutDescripcion.setError(null);
        }

        if (hospitalesList.isEmpty()) {
            Toast.makeText(this, R.string.no_hospitales_disponibles, Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    /**
     * Configura la navegación inferior de la aplicación
     * Define las acciones para cada ítem del menú de navegación
     */
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

    /**
     * Método del ciclo de vida que se ejecuta al reanudar la actividad
     * Actualiza la selección de la navegación inferior
     */
    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigation.setSelectedItemId(R.id.nav_crear);
    }

    /**
     * Método del ciclo de vida que se ejecuta al destruir la actividad
     * Limpia recursos si es necesario
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}