package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import android.os.Handler;
import java.util.ArrayList;
import java.util.List;

import sv.edu.catolica.findtogive.Modelado.SolicitudDonacion;

public class HistorialRealtimeService {

    private static HistorialRealtimeService instance;
    private List<HistorialListener> listeners = new ArrayList<>();
    private boolean isPolling = false;
    private Handler pollingHandler;
    private static final long POLLING_INTERVAL = 30000; // 30 segundos para no ser intrusivo

    // Cache para evitar recargas innecesarias
    private List<SolicitudDonacion> ultimasSolicitudesCache = new ArrayList<>();
    private long ultimaActualizacion = 0;
    private static final long CACHE_DURATION = 30000; // 30 segundos

    public interface HistorialListener {
        void onSolicitudActualizada(SolicitudDonacion solicitud);
        void onSolicitudEstadoCambiado(int solicitudId, String nuevoEstado);
        void onNuevosMensajes(int solicitudId, boolean tieneMensajesNoLeidos);
        void onError(String error);
    }

    public static HistorialRealtimeService getInstance() {
        if (instance == null) {
            instance = new HistorialRealtimeService();
        }
        return instance;
    }

    public void agregarListener(HistorialListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }

        if (!isPolling && !listeners.isEmpty()) {
            startPolling();
        }
    }

    public void removerListener(HistorialListener listener) {
        listeners.remove(listener);

        if (listeners.isEmpty()) {
            stopPolling();
        }
    }

    private void startPolling() {
        if (isPolling) return;

        isPolling = true;
        pollingHandler = new Handler();

        pollingHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isPolling || listeners.isEmpty()) return;

                verificarCambiosEnHistorial();

                if (isPolling) {
                    pollingHandler.postDelayed(this, POLLING_INTERVAL);
                }
            }
        }, POLLING_INTERVAL);
    }

    private void stopPolling() {
        isPolling = false;
        if (pollingHandler != null) {
            pollingHandler.removeCallbacksAndMessages(null);
        }
    }

    private void verificarCambiosEnHistorial() {
        // Solo verificar si ha pasado suficiente tiempo desde la última actualización
        long tiempoActual = System.currentTimeMillis();
        if (tiempoActual - ultimaActualizacion < CACHE_DURATION) {
            return;
        }

        ApiService.getSolicitudesActivas(new ApiService.ListCallback<SolicitudDonacion>() {
            @Override
            public void onSuccess(List<SolicitudDonacion> solicitudes) {
                ultimaActualizacion = System.currentTimeMillis();

                if (solicitudes != null) {
                    // Solo notificar cambios reales
                    for (SolicitudDonacion nuevaSolicitud : solicitudes) {
                        boolean encontrada = false;
                        for (SolicitudDonacion cachedSolicitud : ultimasSolicitudesCache) {
                            if (cachedSolicitud.getSolicitudid() == nuevaSolicitud.getSolicitudid()) {
                                if (!cachedSolicitud.getEstado().equals(nuevaSolicitud.getEstado())) {
                                    // Estado cambió, notificar
                                    for (HistorialListener listener : new ArrayList<>(listeners)) {
                                        listener.onSolicitudEstadoCambiado(nuevaSolicitud.getSolicitudid(), nuevaSolicitud.getEstado());
                                    }
                                }
                                encontrada = true;
                                break;
                            }
                        }

                        if (!encontrada) {
                            // Nueva solicitud
                            for (HistorialListener listener : new ArrayList<>(listeners)) {
                                listener.onSolicitudActualizada(nuevaSolicitud);
                            }
                        }
                    }

                    // Actualizar cache
                    ultimasSolicitudesCache = new ArrayList<>(solicitudes);
                }
            }

            @Override
            public void onError(String error) {
                for (HistorialListener listener : new ArrayList<>(listeners)) {
                    listener.onError("Error verificando historial: " + error);
                }
            }
        });
    }

    public void forzarActualizacion() {
        ultimaActualizacion = 0; // Forzar actualización ignorando cache
        verificarCambiosEnHistorial();
    }

    // Método para notificar cambios específicos de estado
    public void notificarCambioEstado(int solicitudId, String nuevoEstado) {
        for (HistorialListener listener : new ArrayList<>(listeners)) {
            listener.onSolicitudEstadoCambiado(solicitudId, nuevoEstado);
        }

        // Actualizar cache local
        for (SolicitudDonacion solicitud : ultimasSolicitudesCache) {
            if (solicitud.getSolicitudid() == solicitudId) {
                solicitud.setEstado(nuevoEstado);
                break;
            }
        }
    }

    // Método para notificar nuevos mensajes
    public void notificarNuevosMensajes(int solicitudId, boolean tieneMensajesNoLeidos) {
        for (HistorialListener listener : new ArrayList<>(listeners)) {
            listener.onNuevosMensajes(solicitudId, tieneMensajesNoLeidos);
        }
    }
}