package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.annotation.NonNull;

import sv.edu.catolica.findtogive.R;

public class HistorialFiltroDialog extends Dialog {

    public interface HistorialFiltroListener {
        void onAplicarFiltros(String estado, String rol);
        void onLimpiarFiltros();
    }

    private HistorialFiltroListener listener;
    private Spinner spinnerEstado, spinnerRol;
    private Button btnAplicar, btnLimpiar;

    private String estadoActual = "activa";
    private String rolActual = "todas";

    public HistorialFiltroDialog(@NonNull Context context, HistorialFiltroListener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_historial_filtro);

        inicializarVistas();
        configurarSpinners();
        configurarListeners();
    }

    private void inicializarVistas() {
        spinnerEstado = findViewById(R.id.spinner_estado);
        spinnerRol = findViewById(R.id.spinner_rol);
        btnAplicar = findViewById(R.id.btn_aplicar);
        btnLimpiar = findViewById(R.id.btn_limpiar);
    }

    private void configurarSpinners() {
        // Configurar spinner de estado - MODIFICADO: Texto más descriptivo
        String[] estados = {
                "Todas las solicitudes",
                "Solo activas",  // MODIFICADO: Agregar indicador
                "Solo completadas",
                "Solo canceladas"
        };

        ArrayAdapter<String> estadoAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                estados
        );
        estadoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEstado.setAdapter(estadoAdapter);

        // Configurar spinner de rol (sin cambios)
        String[] roles = {
                "Todos los roles",
                "Solicitudes creadas",
                "Solicitudes iniciadas"
        };

        ArrayAdapter<String> rolAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                roles
        );
        rolAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRol.setAdapter(rolAdapter);

        // Establecer selecciones actuales
        establecerSeleccionActual();
    }

    private void establecerSeleccionActual() {
        // Establecer selección de estado - MODIFICADO: Por defecto "activa"
        switch (estadoActual) {
            case "todas":
                spinnerEstado.setSelection(0);
                break;
            case "activa":
                spinnerEstado.setSelection(1); // Esta es la selección por defecto ahora
                break;
            case "completada":
                spinnerEstado.setSelection(2);
                break;
            case "cancelada":
                spinnerEstado.setSelection(3);
                break;
            default:
                spinnerEstado.setSelection(1); // Por defecto "activa"
                break;
        }

        // Establecer selección de rol (sin cambios)
        switch (rolActual) {
            case "todas":
                spinnerRol.setSelection(0);
                break;
            case "receptor":
                spinnerRol.setSelection(1);
                break;
            case "donante":
                spinnerRol.setSelection(2);
                break;
            default:
                spinnerRol.setSelection(0);
                break;
        }
    }

    private void configurarListeners() {
        btnAplicar.setOnClickListener(v -> aplicarFiltros());
        btnLimpiar.setOnClickListener(v -> limpiarFiltros());
    }

    private void aplicarFiltros() {
        String estadoSeleccionado = obtenerEstadoDeSpinner();
        String rolSeleccionado = obtenerRolDeSpinner();

        if (listener != null) {
            listener.onAplicarFiltros(estadoSeleccionado, rolSeleccionado);
        }

        dismiss();
    }

    private void limpiarFiltros() {
        // MODIFICADO: Establecer estado como "activa" en lugar de "todas"
        spinnerEstado.setSelection(1); // "Solo activas" está en posición 1

        // Rol sigue siendo "todas"
        spinnerRol.setSelection(0);

        if (listener != null) {
            // MODIFICADO: Enviar "activa" en lugar de "todas"
            listener.onAplicarFiltros("activa", "todas");
        }

        dismiss();
    }

    private String obtenerEstadoDeSpinner() {
        int position = spinnerEstado.getSelectedItemPosition();
        switch (position) {
            case 0: return "todas";
            case 1: return "activa";
            case 2: return "completada";
            case 3: return "cancelada";
            default: return "todas";
        }
    }

    private String obtenerRolDeSpinner() {
        int position = spinnerRol.getSelectedItemPosition();
        switch (position) {
            case 0: return "todas";
            case 1: return "receptor";
            case 2: return "donante";
            default: return "todas";
        }
    }

    // Métodos para establecer valores iniciales
    public void setFiltrosActuales(String estado, String rol) {
        this.estadoActual = estado != null ? estado : "activa";  // CAMBIADO de "todas" a "activa"
        this.rolActual = rol != null ? rol : "todas";
    }
}
