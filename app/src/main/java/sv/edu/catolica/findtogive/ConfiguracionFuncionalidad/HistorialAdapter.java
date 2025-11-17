package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
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

    // Nuevas variables para manejar mensajes no leídos
    private Map<Integer, Boolean> solicitudTieneMensajesNoLeidos;
    private Map<Integer, List<Chat>> chatsPorSolicitud;
    private Map<Integer, Boolean> esDonantePorSolicitud;

    // Mapa para cache de hospitales - NUEVO: Hacerlo estático para persistir entre recreaciones
    private static Map<Integer, HospitalUbicacion> hospitalesCache = new HashMap<>();

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

        // NUEVO: El cache de hospitales ya es estático, no necesita reinicialización
        Log.d("HistorialAdapter", "🏥 Cache de hospitales inicializado con " + hospitalesCache.size() + " hospitales");
    }

    // NUEVO MÉTODO: Limpiar cache de hospitales (opcional, para cuando sea necesario)
    public static void limpiarCacheHospitales() {
        hospitalesCache.clear();
        Log.d("HistorialAdapter", "🗑️ Cache de hospitales limpiado");
    }

    // NUEVO MÉTODO: Obtener estadísticas del cache
    public static String obtenerEstadisticasCache() {
        return "Hospitales en cache: " + hospitalesCache.size();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    // NUEVO MÉTODO MEJORADO: Actualizar información de chats y mensajes
    public void actualizarInfoChats(List<Chat> chatsDelUsuario, Map<Integer, Boolean> mensajesNoLeidosPorSolicitud) {
        this.chatsPorSolicitud.clear();
        this.esDonantePorSolicitud.clear();

        // NUEVO: Limpiar y actualizar completamente el mapa de mensajes no leídos
        this.solicitudTieneMensajesNoLeidos.clear();
        if (mensajesNoLeidosPorSolicitud != null) {
            this.solicitudTieneMensajesNoLeidos.putAll(mensajesNoLeidosPorSolicitud);
        }

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

        // Log para debugging
        System.out.println("🔄 Adapter: " + this.solicitudTieneMensajesNoLeidos.size() +
                " solicitudes con info de mensajes no leídos");

        notifyDataSetChanged();
    }

    // NUEVO MÉTODO: Cargar TODOS los hospitales de una vez
    public void cargarTodosLosHospitales() {
        if (solicitudList == null || solicitudList.isEmpty()) {
            Log.d("HistorialAdapter", "📭 No hay solicitudes para cargar hospitales");
            return;
        }

        // Obtener IDs únicos de hospitales
        Map<Integer, List<Integer>> hospitalIdsMap = new HashMap<>(); // hospitalId -> lista de posiciones
        for (int i = 0; i < solicitudList.size(); i++) {
            SolicitudDonacion solicitud = solicitudList.get(i);
            int hospitalId = solicitud.getHospitalid();

            // NUEVO: Verificar si el hospital ya está cargado en el objeto solicitud
            if (solicitud.getHospital() == null && !hospitalesCache.containsKey(hospitalId)) {
                if (!hospitalIdsMap.containsKey(hospitalId)) {
                    hospitalIdsMap.put(hospitalId, new ArrayList<>());
                }
                hospitalIdsMap.get(hospitalId).add(i);
            } else if (solicitud.getHospital() == null && hospitalesCache.containsKey(hospitalId)) {
                // NUEVO: Si está en cache pero no en la solicitud, asignarlo inmediatamente
                solicitud.setHospital(hospitalesCache.get(hospitalId));
                Log.d("HistorialAdapter", "✅ Hospital asignado desde cache: " + hospitalId);
            }
        }

        if (hospitalIdsMap.isEmpty()) {
            Log.d("HistorialAdapter", "✅ Todos los hospitales ya están cargados");
            // NUEVO: Forzar actualización para mostrar hospitales desde cache
            notifyDataSetChanged();
            return;
        }

        Log.d("HistorialAdapter", "🏥 Cargando " + hospitalIdsMap.size() + " hospitales únicos desde API");

        // Cargar cada hospital único
        for (Map.Entry<Integer, List<Integer>> entry : hospitalIdsMap.entrySet()) {
            int hospitalId = entry.getKey();
            List<Integer> posiciones = entry.getValue();

            cargarHospitalUnico(hospitalId, posiciones);
        }
    }

    // NUEVO MÉTODO: Cargar un hospital único y actualizar todas sus solicitudes
    private void cargarHospitalUnico(int hospitalId, List<Integer> posiciones) {
        ApiService.getHospitalById(hospitalId, new ApiService.ApiCallback<HospitalUbicacion>() {
            @Override
            public void onSuccess(HospitalUbicacion hospital) {
                // Guardar en cache estático
                hospitalesCache.put(hospitalId, hospital);

                Log.d("HistorialAdapter", "✅ Hospital cargado: " + hospital.getNombre() + " (ID: " + hospitalId + ")");

                // Actualizar todas las solicitudes que usen este hospital
                for (int position : posiciones) {
                    if (position >= 0 && position < solicitudList.size()) {
                        solicitudList.get(position).setHospital(hospital);
                        notifyItemChanged(position);
                    }
                }
            }

            @Override
            public void onError(String error) {
                Log.e("HistorialAdapter", "❌ Error cargando hospital " + hospitalId + ": " + error);

                // Mostrar nombre genérico en caso de error
                HospitalUbicacion hospitalGenerico = new HospitalUbicacion();
                hospitalGenerico.setNombre("Hospital");
                hospitalesCache.put(hospitalId, hospitalGenerico);

                // Actualizar todas las solicitudes con nombre genérico
                for (int position : posiciones) {
                    if (position >= 0 && position < solicitudList.size()) {
                        solicitudList.get(position).setHospital(hospitalGenerico);
                        notifyItemChanged(position);
                    }
                }
            }
        });
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
        this.context = parent.getContext(); // Guardar contexto
        return new HistorialViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistorialViewHolder holder, int position) {
        if (position < 0 || position >= solicitudList.size()) {
            Log.e("HistorialAdapter", "❌ Posición inválida: " + position + ", tamaño: " + solicitudList.size());
            return;
        }

        try {
            SolicitudDonacion solicitud = solicitudList.get(position);

            // NUEVO: Verificar y cargar hospital si falta
            if (solicitud.getHospital() == null && hospitalesCache.containsKey(solicitud.getHospitalid())) {
                solicitud.setHospital(hospitalesCache.get(solicitud.getHospitalid()));
                Log.d("HistorialAdapter", "🏥 Hospital recuperado desde cache para posición " + position);
            }

            holder.bind(solicitud, position);
        } catch (Exception e) {
            Log.e("HistorialAdapter", "💥 Error crítico en onBindViewHolder: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return solicitudList != null ? solicitudList.size() : 0;
    }

    // ✅ MÉTODO SIMPLIFICADO: Solo notificar cambios
    public void notifyDataChanged() {
        try {
            notifyDataSetChanged();
        } catch (Exception e) {
            Log.e("HistorialAdapter", "💥 Error en notifyDataChanged: " + e.getMessage());
        }
    }

    // NUEVO MÉTODO MODIFICADO: Actualizar la lista de solicitudes de manera segura
    public void actualizarListaSolicitudes(List<SolicitudDonacion> nuevasSolicitudes) {
        try {
            this.solicitudList.clear();
            if (nuevasSolicitudes != null) {
                this.solicitudList.addAll(nuevasSolicitudes);

                // NUEVO: Asignar hospitales desde cache inmediatamente
                for (SolicitudDonacion solicitud : this.solicitudList) {
                    if (solicitud.getHospital() == null && hospitalesCache.containsKey(solicitud.getHospitalid())) {
                        solicitud.setHospital(hospitalesCache.get(solicitud.getHospitalid()));
                        Log.d("HistorialAdapter", "✅ Hospital asignado desde cache al actualizar lista");
                    }
                }
            }

            notifyDataChanged();

            // NUEVO: Forzar carga inmediata de hospitales que falten
            new android.os.Handler().postDelayed(() -> {
                cargarTodosLosHospitales();
            }, 100);

        } catch (Exception e) {
            Log.e("HistorialAdapter", "💥 Error actualizando lista: " + e.getMessage());
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

        public void bind(SolicitudDonacion solicitud, int position) {
            // Configurar información básica
            textTypeDonation.setText(solicitud.getTitulo() != null ? solicitud.getTitulo() : "Sin título");
            textDateDonation.setText("Fecha: " + formatearFecha(solicitud.getFechaPublicacion()));

            // MOSTRAR HOSPITAL - VERSIÓN MEJORADA
            configurarUbicacionHospital(solicitud, position);

            textTypebDonation.setText("Tipo de sangre: " + obtenerTipoSangre(solicitud.getTiposangreid()));

            // NUEVO: Configurar icono de rol
            configurarIconoRol(solicitud.getSolicitudid());

            // NUEVO MEJORADO: Configurar indicador de mensajes nuevos con logging
            configurarIndicadorMensajes(solicitud.getSolicitudid());

            // NUEVO: Aplicar estilo según el estado (fondo gris para completadas/canceladas)
            aplicarEstiloSegunEstado(solicitud.getEstado());

            // Configurar botones según el estado y rol
            configurarBotones(solicitud, position);

            // Configurar click listeners
            configurarClickListeners(solicitud, position);
        }

        // NUEVO MÉTODO MEJORADO: Configurar ubicación del hospital
        private void configurarUbicacionHospital(SolicitudDonacion solicitud, int position) {
            HospitalUbicacion hospital = solicitud.getHospital();

            if (hospital != null && hospital.getNombre() != null && !hospital.getNombre().equals("Hospital")) {
                // Tenemos datos reales del hospital
                textPlaceDonation.setText("Hospital: " + hospital.getNombre());

                // Hacer clickeable si tiene link
                if (hospital.getLink() != null && !hospital.getLink().isEmpty()) {
                    textPlaceDonation.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_blue_dark));
                    textPlaceDonation.setClickable(true);
                    textPlaceDonation.setFocusable(true);

                    textPlaceDonation.setOnClickListener(v -> {
                        abrirGoogleMaps(hospital);
                    });
                } else {
                    // No tiene link, mostrar normal
                    textPlaceDonation.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.darker_gray));
                    textPlaceDonation.setClickable(false);
                    textPlaceDonation.setFocusable(false);
                }
            } else {
                // No tenemos datos del hospital o son genéricos
                textPlaceDonation.setText("Cargando hospital...");
                textPlaceDonation.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.darker_gray));
                textPlaceDonation.setClickable(false);
                textPlaceDonation.setFocusable(false);

                // NUEVO: Intentar cargar el hospital si no está disponible
                if (hospital == null || hospital.getNombre() == null || hospital.getNombre().equals("Hospital")) {
                    cargarHospitalIndividual(solicitud.getHospitalid(), position);
                }
            }
        }

        // NUEVO MÉTODO: Cargar hospital individual para esta posición
        private void cargarHospitalIndividual(int hospitalId, int position) {
            // Verificar si ya está en cache
            if (hospitalesCache.containsKey(hospitalId)) {
                HospitalUbicacion hospital = hospitalesCache.get(hospitalId);
                if (position >= 0 && position < solicitudList.size()) {
                    solicitudList.get(position).setHospital(hospital);
                    notifyItemChanged(position);
                }
                return;
            }

            // Cargar desde API
            ApiService.getHospitalById(hospitalId, new ApiService.ApiCallback<HospitalUbicacion>() {
                @Override
                public void onSuccess(HospitalUbicacion hospital) {
                    // Guardar en cache
                    hospitalesCache.put(hospitalId, hospital);

                    // Actualizar la solicitud específica
                    if (position >= 0 && position < solicitudList.size()) {
                        solicitudList.get(position).setHospital(hospital);
                        notifyItemChanged(position);
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e("HistorialAdapter", "❌ Error cargando hospital individual: " + error);
                    // En caso de error, asignar hospital genérico
                    HospitalUbicacion hospitalGenerico = new HospitalUbicacion();
                    hospitalGenerico.setNombre("Hospital");
                    hospitalesCache.put(hospitalId, hospitalGenerico);

                    if (position >= 0 && position < solicitudList.size()) {
                        solicitudList.get(position).setHospital(hospitalGenerico);
                        notifyItemChanged(position);
                    }
                }
            });
        }

        // NUEVO MÉTODO: Abrir Google Maps
        private void abrirGoogleMaps(HospitalUbicacion hospital) {
            try {
                String mapsUrl = hospital.getLink();

                // Si no hay link específico, crear uno con las coordenadas
                if (mapsUrl == null || mapsUrl.isEmpty()) {
                    mapsUrl = "https://www.google.com/maps/search/?api=1&query=" +
                            hospital.getLatitud() + "," + hospital.getLongitud() +
                            "&query=" + Uri.encode(hospital.getNombre());
                }

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl));
                intent.setPackage("com.google.android.apps.maps");

                // Verificar si Google Maps está instalado
                if (intent.resolveActivity(itemView.getContext().getPackageManager()) != null) {
                    itemView.getContext().startActivity(intent);
                } else {
                    // Si Google Maps no está instalado, abrir en navegador
                    intent.setPackage(null);
                    itemView.getContext().startActivity(intent);
                }

            } catch (Exception e) {
                Toast.makeText(itemView.getContext(), "Error al abrir la ubicación", Toast.LENGTH_SHORT).show();
                Log.e("HistorialMaps", "Error al abrir Google Maps: " + e.getMessage());
            }
        }

        // NUEVO MÉTODO: Aplicar estilo visual según el estado de la solicitud (versión sutil)
        private void aplicarEstiloSegunEstado(String estado) {
            try {
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
            } catch (Exception e) {
                Log.e("HistorialAdapter", "Error aplicando estilo: " + e.getMessage());
            }
        }

        // NUEVO MÉTODO: Configurar icono de rol (donante/receptor)
        private void configurarIconoRol(int solicitudId) {
            try {
                boolean esDonante = esDonanteEnSolicitud(solicitudId);

                if (esDonante) {
                    imgRolIcon.setImageResource(R.drawable.ico_donante);
                    imgRolIcon.setContentDescription("Donante");
                } else {
                    imgRolIcon.setImageResource(R.drawable.ico_receptor);
                    imgRolIcon.setContentDescription("Receptor");
                }
            } catch (Exception e) {
                Log.e("HistorialAdapter", "Error configurando icono de rol: " + e.getMessage());
            }
        }

        // NUEVO MÉTODO MEJORADO: Configurar indicador de mensajes nuevos con logging
        private void configurarIndicadorMensajes(int solicitudId) {
            try {
                boolean tieneMensajesNoLeidos = tieneMensajesNoLeidos(solicitudId);

                if (tieneMensajesNoLeidos) {
                    textNewMessages.setVisibility(View.VISIBLE);
                    System.out.println("🔴 Mostrando indicador para solicitud " + solicitudId);
                } else {
                    textNewMessages.setVisibility(View.GONE);
                }

                // Log para debugging
                System.out.println("💬 Solicitud " + solicitudId +
                        " - Mensajes no leídos: " + tieneMensajesNoLeidos);

            } catch (Exception e) {
                Log.e("HistorialAdapter", "Error configurando indicador de mensajes: " + e.getMessage());
                textNewMessages.setVisibility(View.GONE); // Por seguridad, ocultar en caso de error
            }
        }

        private void configurarBotones(SolicitudDonacion solicitud, int position) {
            try {
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
            } catch (Exception e) {
                Log.e("HistorialAdapter", "Error configurando botones: " + e.getMessage());
            }
        }

        private void configurarClickListeners(SolicitudDonacion solicitud, int position) {
            try {
                clickableArea.setOnClickListener(v -> {
                    if (itemClickListener != null) {
                        itemClickListener.onItemClick(solicitud);
                    }
                });
            } catch (Exception e) {
                Log.e("HistorialAdapter", "Error configurando click listeners: " + e.getMessage());
            }
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