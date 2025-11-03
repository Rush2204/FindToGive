package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sv.edu.catolica.findtogive.Modelado.SolicitudDonacion;
import sv.edu.catolica.findtogive.Modelado.Usuario;
import sv.edu.catolica.findtogive.R;

public class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.HistorialViewHolder> {

    private List<SolicitudDonacion> solicitudList;
    private OnItemDeleteListener deleteListener;
    private Map<Integer, Usuario> usuariosMap; // Cache para usuarios

    public interface OnItemDeleteListener {
        void onDeleteClick(SolicitudDonacion solicitud, int position);
    }

    public HistorialAdapter(List<SolicitudDonacion> solicitudList, OnItemDeleteListener deleteListener) {
        this.solicitudList = solicitudList;
        this.deleteListener = deleteListener;
        this.usuariosMap = new HashMap<>();
    }

    @NonNull
    @Override
    public HistorialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.desing_item_historial, parent, false);
        return new HistorialViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistorialViewHolder holder, int position) {
        SolicitudDonacion solicitud = solicitudList.get(position);
        holder.bind(solicitud, position);
    }

    @Override
    public int getItemCount() {
        return solicitudList.size();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < solicitudList.size()) {
            solicitudList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void updateData(List<SolicitudDonacion> nuevasSolicitudes) {
        this.solicitudList.clear();
        this.solicitudList.addAll(nuevasSolicitudes);
        notifyDataSetChanged();
    }

    // Método para cargar datos de usuario
    public void cargarUsuario(int usuarioId, Usuario usuario) {
        usuariosMap.put(usuarioId, usuario);
        notifyDataSetChanged();
    }

    // VIEW HOLDER
    class HistorialViewHolder extends RecyclerView.ViewHolder {

        private ImageView iconDonation; // ImageView para la foto de perfil
        private TextView textTypeDonation, textDateDonation, textPlaceDonation, textTypebDonation;
        private ImageButton btnDeleteHistory;

        public HistorialViewHolder(@NonNull View itemView) {
            super(itemView);

            iconDonation = itemView.findViewById(R.id.icon_donation); // Este es el ImageView
            textTypeDonation = itemView.findViewById(R.id.text_type_donation);
            textDateDonation = itemView.findViewById(R.id.text_date_donation);
            textPlaceDonation = itemView.findViewById(R.id.text_place_donation);
            textTypebDonation = itemView.findViewById(R.id.text_typeb_donation);
            btnDeleteHistory = itemView.findViewById(R.id.btn_delete_history);
        }

        public void bind(SolicitudDonacion solicitud, int position) {
            // Configurar datos de la solicitud
            textTypeDonation.setText(solicitud.getTitulo() != null ? solicitud.getTitulo() : "Sin título");
            textDateDonation.setText("Fecha: " + calcularTiempoTranscurrido(solicitud.getFechaPublicacion()));
            textPlaceDonation.setText("Lugar: " + (solicitud.getUbicacion() != null ? solicitud.getUbicacion() : "Ubicación no especificada"));
            textTypebDonation.setText("Tipo sangre: " + obtenerTipoSangreCompleto(solicitud.getTiposangreid()));

            // Cargar foto de perfil del usuario
            cargarFotoPerfil(solicitud.getUsuarioid());

            // Configurar botón de eliminar
            btnDeleteHistory.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDeleteClick(solicitud, position);
                }
            });

            // Click en el item para ver detalles
            itemView.setOnClickListener(v -> {
                // Aquí puedes abrir detalles de la solicitud si lo deseas
            });
        }

        private void cargarFotoPerfil(int usuarioId) {
            // Verificar si tenemos el usuario en cache
            Usuario usuario = usuariosMap.get(usuarioId);
            if (usuario != null && usuario.getFotoUrl() != null && !usuario.getFotoUrl().isEmpty()) {
                // Cargar foto de perfil real
                Glide.with(itemView.getContext())
                        .load(usuario.getFotoUrl())
                        .placeholder(R.drawable.ico_logo_findtogive)
                        .error(R.drawable.ico_logo_findtogive)
                        .apply(RequestOptions.circleCropTransform())
                        .into(iconDonation);
            } else {
                // Foto por defecto
                iconDonation.setImageResource(R.drawable.ico_logo_findtogive);

                // Si no tenemos el usuario, cargarlo
                if (usuario == null) {
                    cargarUsuarioDesdeApi(usuarioId);
                }
            }
        }

        private void cargarUsuarioDesdeApi(int usuarioId) {
            ApiService.getUsuarioById(usuarioId, new ApiService.ApiCallback<Usuario>() {
                @Override
                public void onSuccess(Usuario usuario) {
                    usuariosMap.put(usuarioId, usuario);
                    // Actualizar la foto si este item todavía muestra el mismo usuario
                    if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                        SolicitudDonacion currentSolicitud = solicitudList.get(getAdapterPosition());
                        if (currentSolicitud.getUsuarioid() == usuarioId) {
                            cargarFotoPerfil(usuarioId);
                        }
                    }
                }

                @Override
                public void onError(String error) {
                    // Mantener la foto por defecto
                }
            });
        }

        private String obtenerTipoSangreCompleto(int tiposangreid) {
            switch (tiposangreid) {
                case 1: return "A+";
                case 2: return "A-";
                case 3: return "B+";
                case 4: return "B-";
                case 5: return "AB+";
                case 6: return "AB-";
                case 7: return "O+";
                case 8: return "O-";
                default: return "Desconocido";
            }
        }

        private String calcularTiempoTranscurrido(String fechaPublicacion) {
            if (fechaPublicacion == null || fechaPublicacion.isEmpty()) {
                return "Reciente";
            }

            try {
                java.time.LocalDateTime fecha = java.time.LocalDateTime.parse(fechaPublicacion);
                java.time.LocalDateTime ahora = java.time.LocalDateTime.now();

                java.time.Duration duracion = java.time.Duration.between(fecha, ahora);

                long segundos = Math.abs(duracion.getSeconds());
                long minutos = segundos / 60;
                long horas = minutos / 60;
                long dias = horas / 24;

                if (duracion.isNegative()) {
                    if (dias > 0) {
                        return "en " + dias + (dias == 1 ? " día" : " días");
                    } else if (horas > 0) {
                        return "en " + horas + (horas == 1 ? " hora" : " horas");
                    } else if (minutos > 0) {
                        return "en " + minutos + (minutos == 1 ? " minuto" : " minutos");
                    } else {
                        return "próximamente";
                    }
                }

                if (dias > 0) {
                    return "hace " + dias + (dias == 1 ? " día" : " días");
                } else if (horas > 0) {
                    return "hace " + horas + (horas == 1 ? " hora" : " horas");
                } else if (minutos > 0) {
                    return "hace " + minutos + (minutos == 1 ? " minuto" : " minutos");
                } else if (segundos > 30) {
                    return "hace " + segundos + " segundos";
                } else {
                    return "hace unos segundos";
                }

            } catch (Exception e) {
                return "Reciente";
            }
        }
    }
}