package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import android.os.Handler;
import android.util.Log;
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

    public static SolicitudesRealtimeService getInstance() {
        if (instance == null) {
            instance = new SolicitudesRealtimeService();
        }
        return instance;
    }

    public void agregarListener(SolicitudListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            Log.d("SolicitudesRealtime", "✅ Listener agregado, total: " + listeners.size());
        }

        // Iniciar polling si es el primer listener
        if (!isPolling && !listeners.isEmpty()) {
            startPolling();
        }
    }

    public void removerListener(SolicitudListener listener) {
        listeners.remove(listener);
        Log.d("SolicitudesRealtime", "❌ Listener removido, total: " + listeners.size());

        // Detener polling si no hay listeners
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

                Log.d("SolicitudesRealtime", "🔄 Polling: Verificando nuevas solicitudes");
                verificarNuevasSolicitudes();

                if (isPolling) {
                    pollingHandler.postDelayed(this, POLLING_INTERVAL);
                }
            }
        }, POLLING_INTERVAL);

        Log.d("SolicitudesRealtime", "🚀 Polling iniciado");
    }

    private void stopPolling() {
        isPolling = false;
        if (pollingHandler != null) {
            pollingHandler.removeCallbacksAndMessages(null);
        }
        Log.d("SolicitudesRealtime", "⏹️ Polling detenido");
    }

    private void verificarNuevasSolicitudes() {
        ApiService.getSolicitudesActivas(new ApiService.ListCallback<SolicitudDonacion>() {
            @Override
            public void onSuccess(List<SolicitudDonacion> solicitudes) {
                Log.d("SolicitudesRealtime", "📡 Polling exitoso: " + solicitudes.size() + " solicitudes");

                // Notificar a todos los listeners
                for (SolicitudListener listener : new ArrayList<>(listeners)) {
                    // Podrías implementar lógica más inteligente aquí para detectar cambios
                    if (solicitudes != null && !solicitudes.isEmpty()) {
                        // Por simplicidad, notificamos sobre la primera solicitud
                        // En una implementación real, compararías con el estado anterior
                        listener.onNuevaSolicitud(solicitudes.get(0));
                    }
                }
            }

            @Override
            public void onError(String error) {
                Log.e("SolicitudesRealtime", "❌ Error en polling: " + error);
                for (SolicitudListener listener : new ArrayList<>(listeners)) {
                    listener.onError("Error verificando solicitudes: " + error);
                }
            }
        });
    }

    // Método para forzar una actualización manual
    public void forzarActualizacion() {
        if (!listeners.isEmpty()) {
            verificarNuevasSolicitudes();
        }
    }
}
