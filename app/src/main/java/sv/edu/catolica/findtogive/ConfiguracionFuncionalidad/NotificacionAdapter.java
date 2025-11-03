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

    @NonNull
    @Override
    public NotificacionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.desing_item_notification, parent, false);
        return new NotificacionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificacionViewHolder holder, int position) {
        Notificacion notificacion = notificaciones.get(position);
        holder.bind(notificacion);
    }

    @Override
    public int getItemCount() {
        return notificaciones != null ? notificaciones.size() : 0;
    }

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

        public void bind(Notificacion notificacion) {
            textNotificationTitle.setText(notificacion.getTitulo());
            textNotificationContent.setText(notificacion.getMensaje());

            // Formatear fecha
            if (notificacion.getFechaEnvio() != null) {
                String fechaFormateada = formatFecha(notificacion.getFechaEnvio());
                textNotificationDate.setText(fechaFormateada);
            }

            // Mostrar indicador de no leído
            noLeido.setVisibility(!notificacion.isLeida() ? View.VISIBLE : View.INVISIBLE);

            // Cargar foto de perfil del usuario que generó la notificación
            imgNotificationIcon.setImageResource(R.drawable.ico_logo_findtogive);

            // Marcar como leída al hacer clic
            itemView.setOnClickListener(v -> {
                if (!notificacion.isLeida()) {
                    marcarComoLeida(notificacion);
                }
            });
        }

        private String formatFecha(String fecha) {
            try {
                if (fecha == null || fecha.isEmpty()) {
                    return "Reciente";
                }

                // Limpiar formato de Supabase
                String fechaLimpia = fecha.replace(" ", "T");
                java.time.LocalDateTime fechaNotif = java.time.LocalDateTime.parse(fechaLimpia);
                java.time.LocalDateTime ahora = java.time.LocalDateTime.now();

                java.time.Duration duracion = java.time.Duration.between(fechaNotif, ahora);

                long segundos = Math.abs(duracion.getSeconds());
                long minutos = segundos / 60;
                long horas = minutos / 60;
                long dias = horas / 24;

                if (minutos < 1) {
                    return "Ahora";
                } else if (minutos < 60) {
                    return "hace " + minutos + " min";
                } else if (horas < 24) {
                    return "hace " + horas + " h";
                } else if (dias == 1) {
                    return "Ayer";
                } else if (dias < 7) {
                    return "hace " + dias + " d";
                } else {
                    return fechaLimpia.substring(0, 10); // YYYY-MM-DD
                }

            } catch (Exception e) {
                return "Reciente";
            }
        }

        private void marcarComoLeida(Notificacion notificacion) {
            ApiService.updateNotificacionLeida(notificacion.getNotificacionid(),
                    new ApiService.ApiCallback<Notificacion>() {
                        @Override
                        public void onSuccess(Notificacion result) {
                            // Actualizar UI
                            notificacion.setLeida(true);
                            noLeido.setVisibility(View.INVISIBLE);
                            notifyItemChanged(getAdapterPosition());
                        }

                        @Override
                        public void onError(String error) {
                            System.out.println("❌ Error marcando notificación como leída: " + error);
                        }
                    });
        }
    }
}