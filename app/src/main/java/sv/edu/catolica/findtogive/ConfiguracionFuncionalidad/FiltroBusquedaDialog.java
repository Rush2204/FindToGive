package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;

import sv.edu.catolica.findtogive.R;

public class FiltroBusquedaDialog extends Dialog {

    public interface FiltroBusquedaListener {
        void onAplicarFiltros(String query, int tipoSangreId);
        void onLimpiarFiltros();
    }

    private FiltroBusquedaListener listener;
    private EditText etBuscar;
    private Spinner spinnerTipoSangre;
    private Button btnAplicar, btnLimpiar;

    private String queryActual = "";
    private int tipoSangreIdActual = -1;

    public FiltroBusquedaDialog(@NonNull Context context, FiltroBusquedaListener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_filtro_busqueda);

        inicializarVistas();
        configurarSpinnerTipoSangre();
        configurarListeners();
    }

    private void inicializarVistas() {
        etBuscar = findViewById(R.id.et_buscar);
        spinnerTipoSangre = findViewById(R.id.spinner_tipo_sangre);
        btnAplicar = findViewById(R.id.btn_aplicar);
        btnLimpiar = findViewById(R.id.btn_limpiar);

        // Restaurar valores actuales si existen
        if (!queryActual.isEmpty()) {
            etBuscar.setText(queryActual);
        }
    }

    private void configurarSpinnerTipoSangre() {
        // Obtener array de tipos de sangre desde strings.xml
        String[] tiposSangre = getContext().getResources().getStringArray(R.array.tipos_sangre_array);

        // Crear array con opción "Todos"
        String[] opcionesSpinner = new String[tiposSangre.length + 1];
        opcionesSpinner[0] = "Todos los tipos";
        System.arraycopy(tiposSangre, 0, opcionesSpinner, 1, tiposSangre.length);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                opcionesSpinner
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerTipoSangre.setAdapter(adapter);

        // Establecer selección actual si existe
        if (tipoSangreIdActual != -1) {
            String tipoSangre = convertirTipoSangreIdANombre(tipoSangreIdActual);
            for (int i = 0; i < opcionesSpinner.length; i++) {
                if (opcionesSpinner[i].equals(tipoSangre)) {
                    spinnerTipoSangre.setSelection(i);
                    break;
                }
            }
        } else {
            spinnerTipoSangre.setSelection(0); // Seleccionar "Todos los tipos" por defecto
        }
    }

    private void configurarListeners() {
        btnAplicar.setOnClickListener(v -> aplicarFiltros());
        btnLimpiar.setOnClickListener(v -> limpiarFiltros());

        // Buscar en tiempo real mientras se escribe
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                queryActual = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void aplicarFiltros() {
        String query = etBuscar.getText().toString().trim();
        String tipoSangreSeleccionado = spinnerTipoSangre.getSelectedItem().toString();

        int tipoSangreId = -1;
        if (!tipoSangreSeleccionado.equals("Todos los tipos")) {
            tipoSangreId = convertirTipoSangreNombreAId(tipoSangreSeleccionado);
        }

        if (listener != null) {
            listener.onAplicarFiltros(query, tipoSangreId);
        }

        dismiss();
    }

    private void limpiarFiltros() {
        etBuscar.setText("");
        spinnerTipoSangre.setSelection(0);
        queryActual = "";
        tipoSangreIdActual = -1;

        if (listener != null) {
            listener.onLimpiarFiltros();
        }

        dismiss();
    }

    // Métodos para convertir entre ID y nombre del tipo de sangre
    private String convertirTipoSangreIdANombre(int tipoSangreId) {
        switch (tipoSangreId) {
            case 1: return "A+";
            case 2: return "A-";
            case 3: return "B+";
            case 4: return "B-";
            case 5: return "AB+";
            case 6: return "AB-";
            case 7: return "O+";
            case 8: return "O-";
            default: return "";
        }
    }

    private int convertirTipoSangreNombreAId(String tipoSangreNombre) {
        switch (tipoSangreNombre) {
            case "A+": return 1;
            case "A-": return 2;
            case "B+": return 3;
            case "B-": return 4;
            case "AB+": return 5;
            case "AB-": return 6;
            case "O+": return 7;
            case "O-": return 8;
            default: return -1;
        }
    }

    // Métodos para establecer valores iniciales
    public void setFiltrosActuales(String query, int tipoSangreId) {
        this.queryActual = query != null ? query : "";
        this.tipoSangreIdActual = tipoSangreId;
    }
}