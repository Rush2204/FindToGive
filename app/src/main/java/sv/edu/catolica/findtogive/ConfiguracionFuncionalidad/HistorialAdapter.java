package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import sv.edu.catolica.findtogive.Modelado.HospitalUbicacion;
import sv.edu.catolica.findtogive.R;

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
    private Context context;

    private Map<Integer, Boolean> solicitudTieneMensajesNoLeidos;
    private Map<Integer, List<Chat>> chatsPorSolicitud;
    private Map<Integer, Boolean> esDonantePorSolicitud;

    private static Map<Integer, HospitalUbicacion> hospitalesCache = new HashMap<>();

    public HistorialAdapter(List<SolicitudDonacion> solicitudList,
                            OnItemDeleteListener deleteListener,
                            OnItemCompleteListener completeListener,
                            Usuario usuarioActual) {
        this.solicitudList = solicitudList != null ? solicitudList : new ArrayList<>();
        this.deleteListener = deleteListener;
        this.completeListener = completeListener;
        this.usuarioActual = usuarioActual;

        this.solicitudTieneMensajesNoLeidos = new HashMap<>();
        this.chatsPorSolicitud = new HashMap<>();
        this.esDonantePorSolicitud = new HashMap<>();
    }

    /**
     * Limpia el cache estático de hospitales, útil para liberar memoria
     * o forzar recarga de datos
     */
    public static void limpiarCacheHospitales() {
        hospitalesCache.clear();
    }

    /**
     * Retorna estadísticas del cache de hospitales para monitoreo
     */
    public static String obtenerEstadisticasCache() {
        return "Hospitales en cache: " + hospitalesCache.size();
    }

    /**
     * Establece el listener para manejar clicks en los items de la lista
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    /**
     * Actualiza la información de chats y mensajes no leídos para las solicitudes.
     * Organiza los chats por solicitud y determina el rol del usuario (donante/receptor)
     * en cada una de ellas
     */
    public void actualizarInfoChats(List<Chat> chatsDelUsuario, Map<Integer, Boolean> mensajesNoLeidosPorSolicitud) {
        this.chatsPorSolicitud.clear();
        this.esDonantePorSolicitud.clear();

        this.solicitudTieneMensajesNoLeidos.clear();
        if (mensajesNoLeidosPorSolicitud != null) {
            this.solicitudTieneMensajesNoLeidos.putAll(mensajesNoLeidosPorSolicitud);
        }

        for (Chat chat : chatsDelUsuario) {
            int solicitudId = chat.getSolicitudid();

            if (!chatsPorSolicitud.containsKey(solicitudId)) {
                chatsPorSolicitud.put(solicitudId, new ArrayList<>());
            }
            chatsPorSolicitud.get(solicitudId).add(chat);

            boolean esDonante = chat.getUsuario1id() == usuarioActual.getUsuarioid();
            esDonantePorSolicitud.put(solicitudId, esDonante);
        }

        notifyDataSetChanged();
    }

    /**
     * Carga todos los hospitales necesarios para las solicitudes en la lista.
     * Optimiza las peticiones agrupando hospitales únicos y usando cache
     */
    public void cargarTodosLosHospitales() {
        if (solicitudList == null || solicitudList.isEmpty()) {
            return;
        }

        Map<Integer, List<Integer>> hospitalIdsMap = new HashMap<>();
        for (int i = 0; i < solicitudList.size(); i++) {
            SolicitudDonacion solicitud = solicitudList.get(i);
            int hospitalId = solicitud.getHospitalid();

            if (solicitud.getHospital() == null && !hospitalesCache.containsKey(hospitalId)) {
                if (!hospitalIdsMap.containsKey(hospitalId)) {
                    hospitalIdsMap.put(hospitalId, new ArrayList<>());
                }
                hospitalIdsMap.get(hospitalId).add(i);
            } else if (solicitud.getHospital() == null && hospitalesCache.containsKey(hospitalId)) {
                solicitud.setHospital(hospitalesCache.get(hospitalId));
            }
        }

        if (hospitalIdsMap.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        for (Map.Entry<Integer, List<Integer>> entry : hospitalIdsMap.entrySet()) {
            int hospitalId = entry.getKey();
            List<Integer> posiciones = entry.getValue();
            cargarHospitalUnico(hospitalId, posiciones);
        }
    }

    /**
     * Carga un hospital específico desde la API y actualiza todas las solicitudes
     * que lo referencian. Guarda el resultado en cache para uso futuro
     */
    private void cargarHospitalUnico(int hospitalId, List<Integer> posiciones) {
        ApiService.getHospitalById(hospitalId, new ApiService.ApiCallback<HospitalUbicacion>() {
            @Override
            public void onSuccess(HospitalUbicacion hospital) {
                hospitalesCache.put(hospitalId, hospital);

                for (int position : posiciones) {
                    if (position >= 0 && position < solicitudList.size()) {
                        solicitudList.get(position).setHospital(hospital);
                        notifyItemChanged(position);
                    }
                }
            }

            @Override
            public void onError(String error) {
                HospitalUbicacion hospitalGenerico = new HospitalUbicacion();
                hospitalGenerico.setNombre(context.getString(R.string.hospital));
                hospitalesCache.put(hospitalId, hospitalGenerico);

                for (int position : posiciones) {
                    if (position >= 0 && position < solicitudList.size()) {
                        solicitudList.get(position).setHospital(hospitalGenerico);
                        notifyItemChanged(position);
                    }
                }
            }
        });
    }

    /**
     * Verifica si una solicitud específica tiene mensajes no leídos
     */
    private boolean tieneMensajesNoLeidos(int solicitudId) {
        return solicitudTieneMensajesNoLeidos.containsKey(solicitudId) &&
                solicitudTieneMensajesNoLeidos.get(solicitudId);
    }

    /**
     * Determina si el usuario actual es donante en una solicitud específica.
     * Primero verifica si es el creador, luego consulta la información de chats
     */
    private boolean esDonanteEnSolicitud(int solicitudId) {
        for (SolicitudDonacion solicitud : solicitudList) {
            if (solicitud.getSolicitudid() == solicitudId) {
                if (solicitud.getUsuarioid() == usuarioActual.getUsuarioid()) {
                    return false;
                }
                break;
            }
        }

        return esDonantePorSolicitud.containsKey(solicitudId) &&
                esDonantePorSolicitud.get(solicitudId);
    }

    /**
     * Crea y retorna una nueva instancia de HistorialViewHolder inflando el layout del item
     */
    @NonNull
    @Override
    public HistorialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.desing_item_historial, parent, false);
        this.context = parent.getContext();
        return new HistorialViewHolder(view);
    }

    /**
     * Vincula los datos de la solicitud en la posición especificada con el ViewHolder.
     * Incluye manejo de errores robusto para prevenir crashes
     */
    @Override
    public void onBindViewHolder(@NonNull HistorialViewHolder holder, int position) {
        if (position < 0 || position >= solicitudList.size()) {
            return;
        }

        try {
            SolicitudDonacion solicitud = solicitudList.get(position);

            if (solicitud.getHospital() == null && hospitalesCache.containsKey(solicitud.getHospitalid())) {
                solicitud.setHospital(hospitalesCache.get(solicitud.getHospitalid()));
            }

            holder.bind(solicitud, position);
        } catch (Exception e) {
        }
    }

    /**
     * Retorna el número total de solicitudes en la lista
     */
    @Override
    public int getItemCount() {
        return solicitudList != null ? solicitudList.size() : 0;
    }

    /**
     * Notifica cambios en el dataset de manera segura con manejo de excepciones
     */
    public void notifyDataChanged() {
        try {
            notifyDataSetChanged();
        } catch (Exception e) {
        }
    }

    /**
     * Actualiza completamente la lista de solicitudes, asigna hospitales desde cache
     * y programa la carga de hospitales faltantes
     */
    public void actualizarListaSolicitudes(List<SolicitudDonacion> nuevasSolicitudes) {
        try {
            this.solicitudList.clear();
            if (nuevasSolicitudes != null) {
                this.solicitudList.addAll(nuevasSolicitudes);

                for (SolicitudDonacion solicitud : this.solicitudList) {
                    if (solicitud.getHospital() == null && hospitalesCache.containsKey(solicitud.getHospitalid())) {
                        solicitud.setHospital(hospitalesCache.get(solicitud.getHospitalid()));
                    }
                }
            }

            notifyDataChanged();

            new android.os.Handler().postDelayed(() -> {
                cargarTodosLosHospitales();
            }, 100);

        } catch (Exception e) {
        }
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

        /**
         * Vincula todos los datos de la solicitud a los elementos de la vista,
         * configurando la información básica, iconos, mensajes y botones
         */
        public void bind(SolicitudDonacion solicitud, int position) {
            textTypeDonation.setText(solicitud.getTitulo() != null ? solicitud.getTitulo() : context.getString(R.string.sin_titulo));
            textDateDonation.setText(context.getString(R.string.fecha_prefix) + formatearFecha(solicitud.getFechaPublicacion()));

            configurarUbicacionHospital(solicitud, position);

            textTypebDonation.setText(context.getString(R.string.tipo_sangre_prefix) + obtenerTipoSangre(solicitud.getTiposangreid()));

            configurarIconoRol(solicitud.getSolicitudid());

            configurarIndicadorMensajes(solicitud.getSolicitudid());

            aplicarEstiloSegunEstado(solicitud.getEstado());

            configurarBotones(solicitud, position);

            configurarClickListeners(solicitud, position);
        }

        /**
         * Configura la visualización de la ubicación del hospital, haciendo el texto
         * clickeable si hay un enlace disponible para abrir en Google Maps
         */
        private void configurarUbicacionHospital(SolicitudDonacion solicitud, int position) {
            HospitalUbicacion hospital = solicitud.getHospital();

            if (hospital != null && hospital.getNombre() != null && !hospital.getNombre().equals(context.getString(R.string.hospital))) {
                textPlaceDonation.setText(context.getString(R.string.hospital_prefix) + hospital.getNombre());

                if (hospital.getLink() != null && !hospital.getLink().isEmpty()) {
                    textPlaceDonation.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_blue_dark));
                    textPlaceDonation.setClickable(true);
                    textPlaceDonation.setFocusable(true);

                    textPlaceDonation.setOnClickListener(v -> {
                        abrirGoogleMaps(hospital);
                    });
                } else {
                    textPlaceDonation.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.darker_gray));
                    textPlaceDonation.setClickable(false);
                    textPlaceDonation.setFocusable(false);
                }
            } else {
                textPlaceDonation.setText(context.getString(R.string.cargando_hospital));
                textPlaceDonation.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.darker_gray));
                textPlaceDonation.setClickable(false);
                textPlaceDonation.setFocusable(false);

                if (hospital == null || hospital.getNombre() == null || hospital.getNombre().equals(context.getString(R.string.hospital))) {
                    cargarHospitalIndividual(solicitud.getHospitalid(), position);
                }
            }
        }

        /**
         * Carga un hospital individual desde la API o cache para una posición específica
         */
        private void cargarHospitalIndividual(int hospitalId, int position) {
            if (hospitalesCache.containsKey(hospitalId)) {
                HospitalUbicacion hospital = hospitalesCache.get(hospitalId);
                if (position >= 0 && position < solicitudList.size()) {
                    solicitudList.get(position).setHospital(hospital);
                    notifyItemChanged(position);
                }
                return;
            }

            ApiService.getHospitalById(hospitalId, new ApiService.ApiCallback<HospitalUbicacion>() {
                @Override
                public void onSuccess(HospitalUbicacion hospital) {
                    hospitalesCache.put(hospitalId, hospital);

                    if (position >= 0 && position < solicitudList.size()) {
                        solicitudList.get(position).setHospital(hospital);
                        notifyItemChanged(position);
                    }
                }

                @Override
                public void onError(String error) {
                    HospitalUbicacion hospitalGenerico = new HospitalUbicacion();
                    hospitalGenerico.setNombre(context.getString(R.string.hospital));
                    hospitalesCache.put(hospitalId, hospitalGenerico);

                    if (position >= 0 && position < solicitudList.size()) {
                        solicitudList.get(position).setHospital(hospitalGenerico);
                        notifyItemChanged(position);
                    }
                }
            });
        }

        /**
         * Abre Google Maps con la ubicación del hospital, usando el enlace disponible
         * o creando uno con las coordenadas. Maneja fallbacks si Google Maps no está instalado
         */
        private void abrirGoogleMaps(HospitalUbicacion hospital) {
            try {
                String mapsUrl = hospital.getLink();

                if (mapsUrl == null || mapsUrl.isEmpty()) {
                    mapsUrl = "https://www.google.com/maps/search/?api=1&query=" +
                            hospital.getLatitud() + "," + hospital.getLongitud() +
                            "&query=" + Uri.encode(hospital.getNombre());
                }

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl));
                intent.setPackage("com.google.android.apps.maps");

                if (intent.resolveActivity(itemView.getContext().getPackageManager()) != null) {
                    itemView.getContext().startActivity(intent);
                } else {
                    intent.setPackage(null);
                    itemView.getContext().startActivity(intent);
                }

            } catch (Exception e) {
                Toast.makeText(itemView.getContext(), context.getString(R.string.error_abrir_ubicacion), Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * Aplica estilos visuales diferentes según el estado de la solicitud:
         * activa (normal), completada o cancelada (atenuadas)
         */
        private void aplicarEstiloSegunEstado(String estado) {
            try {
                com.google.android.material.card.MaterialCardView cardView =
                        (com.google.android.material.card.MaterialCardView) itemView;

                Context context = itemView.getContext();

                switch (estado) {
                    case "activa":
                        cardView.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.white));
                        cardView.setAlpha(1.0f);
                        cardView.setCardElevation(4f);
                        break;

                    case "completada":
                    case "cancelada":
                        cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.light_gray));
                        cardView.setAlpha(0.8f);
                        cardView.setCardElevation(2f);
                        break;

                    default:
                        cardView.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.white));
                        cardView.setAlpha(1.0f);
                        break;
                }
            } catch (Exception e) {
            }
        }

        /**
         * Configura el icono que indica si el usuario es donante o receptor en esta solicitud
         */
        private void configurarIconoRol(int solicitudId) {
            try {
                boolean esDonante = esDonanteEnSolicitud(solicitudId);

                if (esDonante) {
                    imgRolIcon.setImageResource(R.drawable.ico_donante);
                    imgRolIcon.setContentDescription(context.getString(R.string.donante));
                } else {
                    imgRolIcon.setImageResource(R.drawable.ico_receptor);
                    imgRolIcon.setContentDescription(context.getString(R.string.receptor));
                }
            } catch (Exception e) {
            }
        }

        /**
         * Muestra u oculta el indicador de mensajes nuevos según si la solicitud
         * tiene mensajes no leídos
         */
        private void configurarIndicadorMensajes(int solicitudId) {
            try {
                boolean tieneMensajesNoLeidos = tieneMensajesNoLeidos(solicitudId);

                if (tieneMensajesNoLeidos) {
                    textNewMessages.setVisibility(View.VISIBLE);
                } else {
                    textNewMessages.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                textNewMessages.setVisibility(View.GONE);
            }
        }

        /**
         * Configura la visibilidad y comportamiento de los botones de completar y eliminar
         * según el estado de la solicitud y si el usuario es el creador
         */
        private void configurarBotones(SolicitudDonacion solicitud, int position) {
            try {
                boolean esCreador = solicitud.getUsuarioid() == usuarioActual.getUsuarioid();
                boolean estaActiva = "activa".equals(solicitud.getEstado());

                if (esCreador && estaActiva) {
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
                    btnCheckHistory.setVisibility(View.GONE);
                    btnDeleteHistory.setVisibility(View.GONE);
                }

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
            } catch (Exception e) {
            }
        }

        /**
         * Configura los listeners de click para el área principal del item
         */
        private void configurarClickListeners(SolicitudDonacion solicitud, int position) {
            try {
                clickableArea.setOnClickListener(v -> {
                    if (itemClickListener != null) {
                        itemClickListener.onItemClick(solicitud);
                    }
                });
            } catch (Exception e) {
            }
        }

        /**
         * Formatea una fecha en formato ISO a un formato legible en español (día/mes/año)
         */
        private String formatearFecha(String fecha) {
            if (fecha == null || fecha.isEmpty()) {
                return context.getString(R.string.fecha_no_disponible);
            }

            try {
                String fechaLimpia = fecha.replace(" ", "T");
                String[] partesFecha = fechaLimpia.split("T")[0].split("-");

                if (partesFecha.length >= 3) {
                    String año = partesFecha[0];
                    String mes = obtenerNombreMes(Integer.parseInt(partesFecha[1]));
                    String dia = partesFecha[2];
                    return dia + "/" + mes + "/" + año;
                }
            } catch (Exception e) {
            }

            return context.getString(R.string.fecha_invalida);
        }

        /**
         * Convierte un número de mes (1-12) a su nombre en español
         */
        private String obtenerNombreMes(int numeroMes) {
            String[] meses = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
            return (numeroMes >= 1 && numeroMes <= 12) ? meses[numeroMes - 1] : String.valueOf(numeroMes);
        }

        /**
         * Convierte un ID de tipo de sangre a su representación textual (A+, B-, etc.)
         */
        private String obtenerTipoSangre(int tiposangreid) {
            Map<Integer, String> tiposSangre = new HashMap<>();
            tiposSangre.put(1, "A+");
            tiposSangre.put(2, "A-");
            tiposSangre.put(3, "B+");
            tiposSangre.put(4, "B-");
            tiposSangre.put(5, "AB+");
            tiposSangre.put(6, "AB-");
            tiposSangre.put(7, "O+");
            tiposSangre.put(8, "O-");

            return tiposSangre.getOrDefault(tiposangreid, context.getString(R.string.desconocido));
        }
    }
}