package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import sv.edu.catolica.findtogive.Modelado.SolicitudDonacion;
import sv.edu.catolica.findtogive.Modelado.Usuario;
import sv.edu.catolica.findtogive.Modelado.Chat;
import sv.edu.catolica.findtogive.Modelado.Mensaje;
import sv.edu.catolica.findtogive.R;
import android.content.Context;

public class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.HistorialViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(SolicitudDonacion solicitud);
    }

    public interface OnItemDeleteListener {
        void onDeleteClick(SolicitudDonacion solicitud, int position);
    }

    public interface OnItemCompleteListener {
        void onCompleteClick(SolicitudDonacion solicitud, int position);
    }

    private List<SolicitudDonacion> solicitudList;
    private OnItemClickListener itemClickListener;
    private OnItemDeleteListener deleteListener;
    private OnItemCompleteListener completeListener;
    private Usuario usuarioActual;

    // Nuevas variables para manejar mensajes no leídos
    private Map<Integer, Boolean> solicitudTieneMensajesNoLeidos;
    private Map<Integer, List<Chat>> chatsPorSolicitud;
    private Map<Integer, Boolean> esDonantePorSolicitud;

    public HistorialAdapter(List<SolicitudDonacion> solicitudList,
                            OnItemDeleteListener deleteListener,
                            OnItemCompleteListener completeListener,
                            Usuario usuarioActual) {
        this.solicitudList = solicitudList != null ? solicitudList : new ArrayList<>();
        this.deleteListener = deleteListener;
        this.completeListener = completeListener;
        this.usuarioActual = usuarioActual;

        // Inicializar las nuevas estructuras de datos
        this.solicitudTieneMensajesNoLeidos = new HashMap<>();
        this.chatsPorSolicitud = new HashMap<>();
        this.esDonantePorSolicitud = new HashMap<>();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    // NUEVO MÉTODO: Actualizar información de chats y mensajes
    public void actualizarInfoChats(List<Chat> chatsDelUsuario, Map<Integer, Boolean> mensajesNoLeidosPorSolicitud) {
        this.chatsPorSolicitud.clear();
        this.esDonantePorSolicitud.clear();
        this.solicitudTieneMensajesNoLeidos.clear();

        // Organizar chats por solicitud y determinar rol
        for (Chat chat : chatsDelUsuario) {
            int solicitudId = chat.getSolicitudid();

            // Agregar chat a la lista por solicitud
            if (!chatsPorSolicitud.containsKey(solicitudId)) {
                chatsPorSolicitud.put(solicitudId, new ArrayList<>());
            }
            chatsPorSolicitud.get(solicitudId).add(chat);

            // Determinar si es donante en esta solicitud
            boolean esDonante = chat.getUsuario1id() == usuarioActual.getUsuarioid();
            esDonantePorSolicitud.put(solicitudId, esDonante);
        }

        // Actualizar información de mensajes no leídos
        if (mensajesNoLeidosPorSolicitud != null) {
            this.solicitudTieneMensajesNoLeidos.putAll(mensajesNoLeidosPorSolicitud);
        }

        notifyDataSetChanged();
    }

    // NUEVO MÉTODO: Verificar si una solicitud tiene mensajes no leídos
    private boolean tieneMensajesNoLeidos(int solicitudId) {
        return solicitudTieneMensajesNoLeidos.containsKey(solicitudId) &&
                solicitudTieneMensajesNoLeidos.get(solicitudId);
    }

    // NUEVO MÉTODO: Determinar el rol del usuario en la solicitud
    private boolean esDonanteEnSolicitud(int solicitudId) {
        // Primero verificar si es el creador de la solicitud
        for (SolicitudDonacion solicitud : solicitudList) {
            if (solicitud.getSolicitudid() == solicitudId) {
                if (solicitud.getUsuarioid() == usuarioActual.getUsuarioid()) {
                    return false; // Es receptor (creador de la solicitud)
                }
                break;
            }
        }

        // Si no es el creador, verificar en los chats
        return esDonantePorSolicitud.containsKey(solicitudId) &&
                esDonantePorSolicitud.get(solicitudId);
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

    public class HistorialViewHolder extends RecyclerView.ViewHolder {
        private ImageView imgRolIcon;
        private TextView textTypeDonation;
        private TextView textNewMessages;
        private TextView textDateDonation;
        private TextView textPlaceDonation;
        private TextView textTypebDonation;
        private ImageButton btnCheckHistory;
        private ImageButton btnDeleteHistory;
        private View clickableArea;

        public HistorialViewHolder(@NonNull View itemView) {
            super(itemView);

            imgRolIcon = itemView.findViewById(R.id.img_rol_icon);
            textTypeDonation = itemView.findViewById(R.id.text_type_donation);
            textNewMessages = itemView.findViewById(R.id.text_new_messages);
            textDateDonation = itemView.findViewById(R.id.text_date_donation);
            textPlaceDonation = itemView.findViewById(R.id.text_place_donation);
            textTypebDonation = itemView.findViewById(R.id.text_typeb_donation);
            btnCheckHistory = itemView.findViewById(R.id.btn_check_history);
            btnDeleteHistory = itemView.findViewById(R.id.btn_delete_history);
            clickableArea = itemView.findViewById(R.id.clickable_area);
        }

        public void bind(SolicitudDonacion solicitud, int position) {
            // Configurar información básica
            textTypeDonation.setText(solicitud.getTitulo());
            textDateDonation.setText("Fecha: " + formatearFecha(solicitud.getFechaPublicacion()));
            textPlaceDonation.setText("Lugar: " + solicitud.getUbicacion());
            textTypebDonation.setText("Tipo de sangre: " + obtenerTipoSangre(solicitud.getTiposangreid()));

            // NUEVO: Configurar icono de rol
            configurarIconoRol(solicitud.getSolicitudid());

            // NUEVO: Mostrar indicador de mensajes nuevos
            configurarIndicadorMensajes(solicitud.getSolicitudid());

            // NUEVO: Aplicar estilo según el estado (fondo gris para completadas/canceladas)
            aplicarEstiloSegunEstado(solicitud.getEstado());

            // Configurar botones según el estado y rol
            configurarBotones(solicitud, position);

            // Configurar click listeners
            configurarClickListeners(solicitud, position);
        }

        // NUEVO MÉTODO: Aplicar estilo visual según el estado de la solicitud
        // NUEVO MÉTODO: Aplicar estilo visual según el estado de la solicitud (versión sutil)
        private void aplicarEstiloSegunEstado(String estado) {
            // Obtener la tarjeta principal
            com.google.android.material.card.MaterialCardView cardView =
                    (com.google.android.material.card.MaterialCardView) itemView;

            // Obtener el contexto
            Context context = itemView.getContext();

            switch (estado) {
                case "activa":
                    // Estado activo - aspecto normal
                    cardView.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.white));
                    cardView.setAlpha(1.0f);
                    cardView.setCardElevation(4f); // Elevación normal
                    break;

                case "completada":
                case "cancelada":
                    // Estados no activos - aspecto deshabilitado
                    cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.light_gray));
                    cardView.setAlpha(0.8f);
                    cardView.setCardElevation(2f); // Menos elevación
                    break;

                default:
                    cardView.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.white));
                    cardView.setAlpha(1.0f);
                    break;
            }
        }

        // NUEVO MÉTODO: Configurar icono de rol (donante/receptor)
        private void configurarIconoRol(int solicitudId) {
            boolean esDonante = esDonanteEnSolicitud(solicitudId);

            if (esDonante) {
                imgRolIcon.setImageResource(R.drawable.ico_donante);
                imgRolIcon.setContentDescription("Donante");
            } else {
                imgRolIcon.setImageResource(R.drawable.ico_receptor);
                imgRolIcon.setContentDescription("Receptor");
            }
        }

        // NUEVO MÉTODO: Configurar indicador de mensajes nuevos
        private void configurarIndicadorMensajes(int solicitudId) {
            if (tieneMensajesNoLeidos(solicitudId)) {
                textNewMessages.setVisibility(View.VISIBLE);
            } else {
                textNewMessages.setVisibility(View.GONE);
            }
        }

        private void configurarBotones(SolicitudDonacion solicitud, int position) {
            // Mostrar/ocultar botones según el estado y si el usuario es el creador
            boolean esCreador = solicitud.getUsuarioid() == usuarioActual.getUsuarioid();
            boolean estaActiva = "activa".equals(solicitud.getEstado());

            if (esCreador && estaActiva) {
                // El usuario es el creador y la solicitud está activa
                btnCheckHistory.setVisibility(View.VISIBLE);
                btnDeleteHistory.setVisibility(View.VISIBLE);

                btnCheckHistory.setOnClickListener(v -> {
                    if (completeListener != null) {
                        completeListener.onCompleteClick(solicitud, position);
                    }
                });

                btnDeleteHistory.setOnClickListener(v -> {
                    if (deleteListener != null) {
                        deleteListener.onDeleteClick(solicitud, position);
                    }
                });
            } else {
                // Ocultar botones para solicitudes completadas/canceladas o cuando no es el creador
                btnCheckHistory.setVisibility(View.GONE);
                btnDeleteHistory.setVisibility(View.GONE);
            }

            // Cambiar icono y color según el estado
            switch (solicitud.getEstado()) {
                case "activa":
                    btnCheckHistory.setImageResource(android.R.drawable.arrow_down_float);
                    btnCheckHistory.setColorFilter(itemView.getContext().getColor(android.R.color.holo_green_dark));
                    break;
                case "completada":
                    btnCheckHistory.setImageResource(android.R.drawable.checkbox_on_background);
                    btnCheckHistory.setColorFilter(itemView.getContext().getColor(android.R.color.holo_green_dark));
                    break;
                case "cancelada":
                    btnDeleteHistory.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                    btnDeleteHistory.setColorFilter(itemView.getContext().getColor(android.R.color.holo_red_dark));
                    break;
            }
        }

        private void configurarClickListeners(SolicitudDonacion solicitud, int position) {
            clickableArea.setOnClickListener(v -> {
                if (itemClickListener != null) {
                    itemClickListener.onItemClick(solicitud);
                }
            });
        }

        private String formatearFecha(String fecha) {
            if (fecha == null || fecha.isEmpty()) {
                return "Fecha no disponible";
            }

            try {
                // Formato de Supabase: "2024-01-15T14:30:00.000000" o "2024-01-15 14:30:00.000000"
                String fechaLimpia = fecha.replace(" ", "T");
                String[] partesFecha = fechaLimpia.split("T")[0].split("-");

                if (partesFecha.length >= 3) {
                    String año = partesFecha[0];
                    String mes = obtenerNombreMes(Integer.parseInt(partesFecha[1]));
                    String dia = partesFecha[2];
                    return dia + "/" + mes + "/" + año;
                }
            } catch (Exception e) {
                Log.e("HistorialAdapter", "Error formateando fecha: " + fecha, e);
            }

            return "Fecha inválida";
        }

        private String obtenerNombreMes(int numeroMes) {
            String[] meses = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
            return (numeroMes >= 1 && numeroMes <= 12) ? meses[numeroMes - 1] : String.valueOf(numeroMes);
        }

        private String obtenerTipoSangre(int tiposangreid) {
            // Mapeo simple de IDs a tipos de sangre (ajusta según tu base de datos)
            Map<Integer, String> tiposSangre = new HashMap<>();
            tiposSangre.put(1, "A+");
            tiposSangre.put(2, "A-");
            tiposSangre.put(3, "B+");
            tiposSangre.put(4, "B-");
            tiposSangre.put(5, "AB+");
            tiposSangre.put(6, "AB-");
            tiposSangre.put(7, "O+");
            tiposSangre.put(8, "O-");

            return tiposSangre.getOrDefault(tiposangreid, "Desconocido");
        }
    }
}