package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import sv.edu.catolica.findtogive.Modelado.Notificacion;
import sv.edu.catolica.findtogive.Modelado.Usuario;
import sv.edu.catolica.findtogive.R;

public class NotificacionAdapter extends RecyclerView.Adapter<NotificacionAdapter.NotificacionViewHolder> {

    private List<Notificacion> notificaciones;
    private Context context;

    public NotificacionAdapter(List<Notificacion> notificaciones, Context context) {
        this.notificaciones = notificaciones;
        this.context = context;
    }

    /**
     * Crea y retorna una nueva instancia de NotificacionViewHolder inflando el layout del item
     */
    @NonNull
    @Override
    public NotificacionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.desing_item_notification, parent, false);
        return new NotificacionViewHolder(view);
    }

    /**
     * Vincula los datos de la notificación en la posición especificada con el ViewHolder
     */
    @Override
    public void onBindViewHolder(@NonNull NotificacionViewHolder holder, int position) {
        Notificacion notificacion = notificaciones.get(position);
        holder.bind(notificacion);
    }

    /**
     * Retorna el número total de notificaciones en la lista
     */
    @Override
    public int getItemCount() {
        return notificaciones != null ? notificaciones.size() : 0;
    }

    /**
     * Reemplaza completamente la lista de notificaciones y notifica el cambio
     */
    public void setNotificaciones(List<Notificacion> notificaciones) {
        this.notificaciones = notificaciones;
        notifyDataSetChanged();
    }

    class NotificacionViewHolder extends RecyclerView.ViewHolder {
        private TextView textNotificationTitle;
        private TextView textNotificationContent;
        private TextView textNotificationDate;
        private View noLeido;
        private ImageView imgNotificationIcon;

        public NotificacionViewHolder(@NonNull View itemView) {
            super(itemView);
            textNotificationTitle = itemView.findViewById(R.id.text_notification_title);
            textNotificationContent = itemView.findViewById(R.id.text_notification_content);
            textNotificationDate = itemView.findViewById(R.id.text_notification_date);
            noLeido = itemView.findViewById(R.id.noLeido);
            imgNotificationIcon = itemView.findViewById(R.id.img_notification_icon);
        }

        /**
         * Vincula todos los datos de la notificación a los elementos de la vista,
         * incluyendo título, mensaje, fecha, estado de leído y manejo de clicks
         */
        public void bind(Notificacion notificacion) {
            textNotificationTitle.setText(notificacion.getTitulo());
            textNotificationContent.setText(notificacion.getMensaje());

            if (notificacion.getFechaEnvio() != null) {
                String fechaFormateada = formatFecha(notificacion.getFechaEnvio());
                textNotificationDate.setText(fechaFormateada);
            }

            noLeido.setVisibility(!notificacion.isLeida() ? View.VISIBLE : View.INVISIBLE);

            imgNotificationIcon.setImageResource(R.drawable.ico_logo_findtogive);

            itemView.setOnClickListener(v -> {
                if (!notificacion.isLeida()) {
                    marcarComoLeida(notificacion);
                }
            });
        }

        /**
         * Formatea la fecha de la notificación a un formato legible y relativo.
         * Convierte fechas ISO a textos como "Ahora", "hace X min", "hace X h",
         * "Ayer", "hace X d" o fecha completa para notificaciones antiguas
         */
        private String formatFecha(String fecha) {
            try {
                if (fecha == null || fecha.isEmpty()) {
                    return context.getString(R.string.reciente);
                }

                String fechaLimpia = fecha.replace(" ", "T");
                java.time.LocalDateTime fechaNotif = java.time.LocalDateTime.parse(fechaLimpia);
                java.time.LocalDateTime ahora = java.time.LocalDateTime.now();

                java.time.Duration duracion = java.time.Duration.between(fechaNotif, ahora);

                long segundos = Math.abs(duracion.getSeconds());
                long minutos = segundos / 60;
                long horas = minutos / 60;
                long dias = horas / 24;

                if (minutos < 1) {
                    return context.getString(R.string.ahora);
                } else if (minutos < 60) {
                    return context.getString(R.string.hace_minutos_notif, minutos);
                } else if (horas < 24) {
                    return context.getString(R.string.hace_horas_notif, horas);
                } else if (dias == 1) {
                    return context.getString(R.string.ayer);
                } else if (dias < 7) {
                    return context.getString(R.string.hace_dias_notif, dias);
                } else {
                    return fechaLimpia.substring(0, 10);
                }

            } catch (Exception e) {
                return context.getString(R.string.reciente);
            }
        }

        /**
         * Marca una notificación como leída mediante una petición a la API.
         * Actualiza la interfaz visualmente ocultando el indicador de no leído
         * una vez que la operación se completa exitosamente
         */
        private void marcarComoLeida(Notificacion notificacion) {
            ApiService.updateNotificacionLeida(notificacion.getNotificacionid(),
                    new ApiService.ApiCallback<Notificacion>() {
                        @Override
                        public void onSuccess(Notificacion result) {
                            notificacion.setLeida(true);
                            noLeido.setVisibility(View.INVISIBLE);
                            notifyItemChanged(getAdapterPosition());
                        }

                        @Override
                        public void onError(String error) {
                        }
                    });
        }
    }
}