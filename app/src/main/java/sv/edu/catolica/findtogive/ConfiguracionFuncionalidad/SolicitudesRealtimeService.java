package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import android.os.Handler;
import java.util.ArrayList;
import java.util.List;

import sv.edu.catolica.findtogive.Modelado.SolicitudDonacion;

public class SolicitudesRealtimeService {

    private static SolicitudesRealtimeService instance;
    private List<SolicitudListener> listeners = new ArrayList<>();
    private boolean isPolling = false;
    private Handler pollingHandler;
    private static final long POLLING_INTERVAL = 1000;

    public interface SolicitudListener {
        void onNuevaSolicitud(SolicitudDonacion solicitud);
        void onSolicitudActualizada(SolicitudDonacion solicitud);
        void onSolicitudEliminada(int solicitudId);
        void onError(String error);
    }

    /**
     * Retorna la instancia singleton del servicio de tiempo real para solicitudes
     */
    public static SolicitudesRealtimeService getInstance() {
        if (instance == null) {
            instance = new SolicitudesRealtimeService();
        }
        return instance;
    }

    /**
     * Agrega un listener para recibir actualizaciones en tiempo real de solicitudes.
     * Inicia automáticamente el polling si es el primer listener agregado
     */
    public void agregarListener(SolicitudListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }

        if (!isPolling && !listeners.isEmpty()) {
            startPolling();
        }
    }

    /**
     * Remueve un listener y detiene el polling si no quedan listeners activos
     */
    public void removerListener(SolicitudListener listener) {
        listeners.remove(listener);

        if (listeners.isEmpty()) {
            stopPolling();
        }
    }

    /**
     * Inicia el proceso de polling que verifica periódicamente nuevas solicitudes.
     * Se ejecuta cada POLLING_INTERVAL milisegundos mientras haya listeners activos
     */
    private void startPolling() {
        if (isPolling) return;

        isPolling = true;
        pollingHandler = new Handler();

        pollingHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isPolling || listeners.isEmpty()) return;

                verificarNuevasSolicitudes();

                if (isPolling) {
                    pollingHandler.postDelayed(this, POLLING_INTERVAL);
                }
            }
        }, POLLING_INTERVAL);
    }

    /**
     * Detiene el proceso de polling y limpia los handlers
     */
    private void stopPolling() {
        isPolling = false;
        if (pollingHandler != null) {
            pollingHandler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * Verifica nuevas solicitudes consultando la API y notifica a todos los listeners
     * con los resultados obtenidos
     */
    private void verificarNuevasSolicitudes() {
        ApiService.getSolicitudesActivas(new ApiService.ListCallback<SolicitudDonacion>() {
            @Override
            public void onSuccess(List<SolicitudDonacion> solicitudes) {
                for (SolicitudListener listener : new ArrayList<>(listeners)) {
                    if (solicitudes != null && !solicitudes.isEmpty()) {
                        listener.onNuevaSolicitud(solicitudes.get(0));
                    }
                }
            }

            @Override
            public void onError(String error) {
                for (SolicitudListener listener : new ArrayList<>(listeners)) {
                    listener.onError("Error verificando solicitudes: " + error);
                }
            }
        });
    }

    /**
     * Fuerza una actualización manual inmediata sin esperar al próximo intervalo de polling
     */
    public void forzarActualizacion() {
        if (!listeners.isEmpty()) {
            verificarNuevasSolicitudes();
        }
    }
}