package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sv.edu.catolica.findtogive.ClasesDiseño.ChatC;
import sv.edu.catolica.findtogive.Modelado.Chat;
import sv.edu.catolica.findtogive.Modelado.HospitalUbicacion;
import sv.edu.catolica.findtogive.Modelado.SolicitudDonacion;
import sv.edu.catolica.findtogive.Modelado.Usuario;
import sv.edu.catolica.findtogive.R;

public class SolicitudesAdapter extends RecyclerView.Adapter<SolicitudesAdapter.SolicitudViewHolder> {

    private List<SolicitudDonacion> solicitudesList;
    private Context context;
    private Map<Integer, Usuario> usuariosMap;
    private Map<Integer, HospitalUbicacion> hospitalesMap;

    public SolicitudesAdapter(List<SolicitudDonacion> solicitudesList, Context context) {
        this.solicitudesList = solicitudesList;
        this.context = context;
        this.usuariosMap = new HashMap<>();
        this.hospitalesMap = new HashMap<>();
    }

    /**
     * Crea y retorna una nueva instancia de SolicitudViewHolder inflando el layout del item
     */
    @NonNull
    @Override
    public SolicitudViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.desing_item_solicitud, parent, false);
        return new SolicitudViewHolder(view);
    }

    /**
     * Vincula los datos de la solicitud en la posición especificada con el ViewHolder
     */
    @Override
    public void onBindViewHolder(@NonNull SolicitudViewHolder holder, int position) {
        SolicitudDonacion solicitud = solicitudesList.get(position);
        holder.bind(solicitud);
    }

    /**
     * Retorna el número total de solicitudes en la lista
     */
    @Override
    public int getItemCount() {
        return solicitudesList.size();
    }

    /**
     * Fuerza la actualización completa de todas las vistas del RecyclerView
     */
    public void forzarActualizacionCompleta() {
        notifyDataSetChanged();
    }

    /**
     * Fuerza la carga de usuarios para todas las solicitudes que no estén en caché
     */
    public void forzarCargaUsuarios() {
        for (int i = 0; i < solicitudesList.size(); i++) {
            SolicitudDonacion solicitud = solicitudesList.get(i);
            int usuarioId = solicitud.getUsuarioid();

            if (!usuariosMap.containsKey(usuarioId)) {
                cargarUsuarioForzado(usuarioId, i);
            }
        }
    }

    /**
     * Agrega una nueva solicitud al principio de la lista y fuerza la carga de su usuario
     */
    public void agregarSolicitud(SolicitudDonacion solicitud) {
        solicitudesList.add(0, solicitud);
        notifyItemInserted(0);
        cargarUsuarioForzado(solicitud.getUsuarioid(), 0);
    }

    /**
     * Actualiza una solicitud existente en la lista y recarga los datos del usuario
     */
    public void actualizarSolicitud(SolicitudDonacion solicitud) {
        for (int i = 0; i < solicitudesList.size(); i++) {
            if (solicitudesList.get(i).getSolicitudid() == solicitud.getSolicitudid()) {
                solicitudesList.set(i, solicitud);
                notifyItemChanged(i);
                cargarUsuarioForzado(solicitud.getUsuarioid(), i);
                break;
            }
        }
    }

    /**
     * Elimina una solicitud de la lista basándose en su ID
     */
    public void eliminarSolicitud(int solicitudId) {
        for (int i = 0; i < solicitudesList.size(); i++) {
            if (solicitudesList.get(i).getSolicitudid() == solicitudId) {
                solicitudesList.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    /**
     * Actualiza completamente la lista de solicitudes manteniendo los caches existentes
     * y forzando la carga de usuarios y hospitales para las nuevas solicitudes
     */
    public void updateData(List<SolicitudDonacion> nuevasSolicitudes) {
        this.solicitudesList.clear();
        this.solicitudesList.addAll(nuevasSolicitudes);
        notifyDataSetChanged();
        forzarCargaUsuarios();
        cargarHospitalesParaSolicitudes();
        cargarDatosUsuarios();
    }

    /**
     * Establece el mapa de usuarios y actualiza las vistas
     */
    public void setUsuariosData(Map<Integer, Usuario> usuariosMap) {
        this.usuariosMap.clear();
        this.usuariosMap.putAll(usuariosMap);
        notifyDataSetChanged();
    }

    /**
     * Carga los hospitales necesarios para las solicitudes que no estén en caché
     */
    private void cargarHospitalesParaSolicitudes() {
        Set<Integer> hospitalIds = new HashSet<>();

        for (SolicitudDonacion solicitud : solicitudesList) {
            int hospitalId = solicitud.getHospitalid();
            if (!hospitalesMap.containsKey(hospitalId)) {
                hospitalIds.add(hospitalId);
            }
        }

        for (Integer hospitalId : hospitalIds) {
            cargarHospitalIndividual(hospitalId);
        }
    }

    /**
     * Carga un hospital individual desde la API y lo almacena en caché
     */
    private void cargarHospitalIndividual(int hospitalId) {
        ApiService.getHospitalById(hospitalId, new ApiService.ApiCallback<HospitalUbicacion>() {
            @Override
            public void onSuccess(HospitalUbicacion hospital) {
                hospitalesMap.put(hospitalId, hospital);
                actualizarSolicitudesConHospital(hospitalId, hospital);
            }

            @Override
            public void onError(String error) {
                new Handler().postDelayed(() -> {
                    cargarHospitalIndividual(hospitalId);
                }, 2000);
            }
        });
    }

    /**
     * Actualiza todas las solicitudes que referencian un hospital específico
     */
    private void actualizarSolicitudesConHospital(int hospitalId, HospitalUbicacion hospital) {
        for (int i = 0; i < solicitudesList.size(); i++) {
            if (solicitudesList.get(i).getHospitalid() == hospitalId) {
                solicitudesList.get(i).setHospital(hospital);
                final int position = i;
                runOnUiThread(() -> {
                    if (position >= 0 && position < solicitudesList.size()) {
                        notifyItemChanged(position);
                    }
                });
            }
        }
    }

    /**
     * Carga forzadamente un usuario específico y actualiza su vista correspondiente
     */
    private void cargarUsuarioForzado(int usuarioId, int position) {
        ApiService.getUsuarioById(usuarioId, new ApiService.ApiCallback<Usuario>() {
            @Override
            public void onSuccess(Usuario usuario) {
                usuariosMap.put(usuarioId, usuario);
                if (position >= 0 && position < solicitudesList.size()) {
                    runOnUiThread(() -> notifyItemChanged(position));
                }
            }

            @Override
            public void onError(String error) {
                new Handler().postDelayed(() -> {
                    if (position < solicitudesList.size() && solicitudesList.get(position).getUsuarioid() == usuarioId) {
                        cargarUsuarioForzado(usuarioId, position);
                    }
                }, 2000);
            }
        });
    }

    /**
     * Carga un usuario individual y actualiza su vista específica
     */
    private void cargarUsuarioIndividual(int usuarioId, int position) {
        ApiService.getUsuarioById(usuarioId, new ApiService.ApiCallback<Usuario>() {
            @Override
            public void onSuccess(Usuario usuario) {
                usuariosMap.put(usuarioId, usuario);
                if (position >= 0 && position < solicitudesList.size()) {
                    runOnUiThread(() -> notifyItemChanged(position));
                }
            }

            @Override
            public void onError(String error) {
                new Handler().postDelayed(() -> {
                    if (position < solicitudesList.size() && solicitudesList.get(position).getUsuarioid() == usuarioId) {
                        cargarUsuarioIndividual(usuarioId, position);
                    }
                }, 1000);
            }
        });
    }

    /**
     * Carga los datos de usuarios para todas las solicitudes que no estén en caché
     */
    private void cargarDatosUsuarios() {
        if (solicitudesList.isEmpty()) {
            return;
        }

        List<Integer> usuarioIds = new ArrayList<>();
        for (SolicitudDonacion solicitud : solicitudesList) {
            if (!usuariosMap.containsKey(solicitud.getUsuarioid())) {
                usuarioIds.add(solicitud.getUsuarioid());
            }
        }

        if (usuarioIds.isEmpty()) {
            return;
        }

        ApiService.getUsuariosByIds(usuarioIds, new ApiService.ListCallback<Usuario>() {
            @Override
            public void onSuccess(List<Usuario> usuarios) {
                for (Usuario usuario : usuarios) {
                    usuariosMap.put(usuario.getUsuarioid(), usuario);
                }
                runOnUiThread(() -> {
                    notifyDataSetChanged();
                });
            }

            @Override
            public void onError(String error) {
                for (Integer usuarioId : usuarioIds) {
                    for (int i = 0; i < solicitudesList.size(); i++) {
                        if (solicitudesList.get(i).getUsuarioid() == usuarioId) {
                            cargarUsuarioIndividual(usuarioId, i);
                            break;
                        }
                    }
                }
            }
        });
    }

    /**
     * Ejecuta una acción en el hilo principal de la UI
     */
    private void runOnUiThread(Runnable action) {
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).runOnUiThread(action);
        } else {
            new Handler(android.os.Looper.getMainLooper()).post(action);
        }
    }

    public class SolicitudViewHolder extends RecyclerView.ViewHolder {

        private ImageView imgProfile, imgAdjunta;
        private TextView textUserName, textTimeAgo, textTitle, textDescription,
                textBloodTypeBadge, textLocationDetails;
        private Button btnChatear;

        public SolicitudViewHolder(@NonNull View itemView) {
            super(itemView);

            imgProfile = itemView.findViewById(R.id.img_profile);
            imgAdjunta = itemView.findViewById(R.id.img_adjunta);
            textUserName = itemView.findViewById(R.id.text_user_name);
            textTimeAgo = itemView.findViewById(R.id.text_time_ago);
            textTitle = itemView.findViewById(R.id.text_title);
            textDescription = itemView.findViewById(R.id.text_description);
            textBloodTypeBadge = itemView.findViewById(R.id.text_blood_type_badge);
            textLocationDetails = itemView.findViewById(R.id.text_location_details);
            btnChatear = itemView.findViewById(R.id.btnchatear);
        }

        /**
         * Carga los datos de un hospital específico si no está en caché
         */
        private void cargarDatosHospital(int hospitalId, int position) {
            if (hospitalesMap.containsKey(hospitalId)) {
                return;
            }

            ApiService.getHospitalById(hospitalId, new ApiService.ApiCallback<HospitalUbicacion>() {
                @Override
                public void onSuccess(HospitalUbicacion hospital) {
                    hospitalesMap.put(hospitalId, hospital);
                    for (int i = 0; i < solicitudesList.size(); i++) {
                        if (solicitudesList.get(i).getHospitalid() == hospitalId) {
                            solicitudesList.get(i).setHospital(hospital);
                            final int pos = i;
                            runOnUiThread(() -> {
                                if (pos >= 0 && pos < solicitudesList.size()) {
                                    notifyItemChanged(pos);
                                }
                            });
                        }
                    }
                }

                @Override
                public void onError(String error) {
                }
            });
        }

        /**
         * Vincula todos los datos de la solicitud a los elementos de la vista,
         * incluyendo información del usuario, hospital, imagen adjunta y configurando los listeners
         */
        public void bind(SolicitudDonacion solicitud) {
            int position = getAdapterPosition();

            textTitle.setText(solicitud.getTitulo() != null ? solicitud.getTitulo() : context.getString(R.string.sin_titulo));
            textDescription.setText(solicitud.getDescripcion() != null ? solicitud.getDescripcion() : context.getString(R.string.sin_descripcion));

            configurarUbicacion(solicitud, position);

            textBloodTypeBadge.setText(obtenerTipoSangreCompleto(solicitud.getTiposangreid()));

            textTimeAgo.setText(calcularTiempoTranscurrido(solicitud.getFechaPublicacion()));

            cargarDatosUsuarioAgresivo(solicitud.getUsuarioid(), position);

            cargarImagenAdjunta(solicitud);

            btnChatear.setOnClickListener(v -> abrirChat(solicitud));
            itemView.setOnClickListener(v -> {
                Toast.makeText(context, "Abriendo detalles de: " + solicitud.getTitulo(), Toast.LENGTH_SHORT).show();
            });
        }

        /**
         * Configura la visualización de la ubicación del hospital, haciendo el texto
         * clickeable si hay un enlace disponible para abrir en Google Maps
         */
        private void configurarUbicacion(SolicitudDonacion solicitud, int position) {
            HospitalUbicacion hospital = hospitalesMap.get(solicitud.getHospitalid());

            if (hospital != null) {
                solicitud.setHospital(hospital);
                textLocationDetails.setText(hospital.getNombre());

                if (hospital.getLink() != null && !hospital.getLink().isEmpty()) {
                    textLocationDetails.setTextColor(context.getResources().getColor(android.R.color.holo_blue_dark));
                    textLocationDetails.setClickable(true);
                    textLocationDetails.setFocusable(true);

                    textLocationDetails.setOnClickListener(v -> {
                        abrirGoogleMaps(hospital);
                    });
                } else {
                    textLocationDetails.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
                    textLocationDetails.setClickable(false);
                    textLocationDetails.setFocusable(false);
                }
            } else {
                textLocationDetails.setText(context.getString(R.string.cargando_hospital));
                textLocationDetails.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
                textLocationDetails.setClickable(false);
                textLocationDetails.setFocusable(false);
                cargarDatosHospital(solicitud.getHospitalid(), position);
            }
        }

        /**
         * Abre Google Maps con la ubicación del hospital usando el enlace disponible
         * o creando uno con las coordenadas
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

                if (intent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(intent);
                } else {
                    intent.setPackage(null);
                    context.startActivity(intent);
                }

            } catch (Exception e) {
                Toast.makeText(context, context.getString(R.string.error_abrir_ubicacion), Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * Carga agresivamente los datos del usuario, mostrando información real si está en caché
         * o forzando la carga desde la API si no está disponible
         */
        private void cargarDatosUsuarioAgresivo(int usuarioId, int position) {
            Usuario usuario = usuariosMap.get(usuarioId);

            if (usuario != null) {
                String nombreCompleto = usuario.getNombre() + " " + usuario.getApellido();
                textUserName.setText(nombreCompleto);

                if (usuario.getFotoUrl() != null && !usuario.getFotoUrl().isEmpty()) {
                    Glide.with(context)
                            .load(usuario.getFotoUrl())
                            .placeholder(R.drawable.logo_findtogive)
                            .error(R.drawable.logo_findtogive)
                            .apply(RequestOptions.circleCropTransform())
                            .into(imgProfile);
                } else {
                    imgProfile.setImageResource(R.drawable.logo_findtogive);
                }
            } else {
                textUserName.setText(context.getString(R.string.cargando));
                imgProfile.setImageResource(R.drawable.logo_findtogive);
                cargarUsuarioIndividualAgresivo(usuarioId, position);
            }
        }

        /**
         * Carga agresivamente un usuario individual desde la API con reintentos automáticos
         */
        private void cargarUsuarioIndividualAgresivo(int usuarioId, int position) {
            ApiService.getUsuarioById(usuarioId, new ApiService.ApiCallback<Usuario>() {
                @Override
                public void onSuccess(Usuario usuario) {
                    usuariosMap.put(usuarioId, usuario);
                    if (position >= 0 && position < solicitudesList.size()) {
                        runOnUiThread(() -> notifyItemChanged(position));
                    }
                }

                @Override
                public void onError(String error) {
                    new Handler().postDelayed(() -> {
                        if (position < solicitudesList.size() && solicitudesList.get(position).getUsuarioid() == usuarioId) {
                            cargarUsuarioIndividualAgresivo(usuarioId, position);
                        }
                    }, 1000);
                }
            });
        }

        /**
         * Carga la imagen adjunta de la solicitud usando Glide con manejo de errores
         * y transformaciones para mejor visualización
         */
        private void cargarImagenAdjunta(SolicitudDonacion solicitud) {
            if (solicitud.getImagenUrl() != null && !solicitud.getImagenUrl().isEmpty()) {
                imgAdjunta.setVisibility(View.VISIBLE);

                Glide.with(context)
                        .load(solicitud.getImagenUrl())
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.logo_findtogive)
                                .error(R.drawable.logo_findtogive)
                                .transform(new CenterCrop(), new RoundedCorners(16))
                                .override(600, 400)
                        )
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                        Target<Drawable> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model,
                                                           Target<Drawable> target, DataSource dataSource,
                                                           boolean isFirstResource) {
                                return false;
                            }
                        })
                        .into(imgAdjunta);

            } else {
                imgAdjunta.setVisibility(View.GONE);
            }
        }

        /**
         * Convierte un ID de tipo de sangre a su representación textual completa
         */
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
                default: return context.getString(R.string.desconocido);
            }
        }

        /**
         * Calcula y formatea el tiempo transcurrido desde la publicación de la solicitud
         * mostrando tiempos relativos como "hace X minutos", "hace X horas", etc.
         */
        private String calcularTiempoTranscurrido(String fechaPublicacion) {
            if (fechaPublicacion == null || fechaPublicacion.isEmpty()) {
                return context.getString(R.string.reciente);
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
                        return context.getResources().getQuantityString(R.plurals.en_dias, (int) dias, dias);
                    } else if (horas > 0) {
                        return context.getResources().getQuantityString(R.plurals.en_horas, (int) horas, horas);
                    } else if (minutos > 0) {
                        return context.getResources().getQuantityString(R.plurals.en_minutos, (int) minutos, minutos);
                    } else {
                        return context.getString(R.string.proximamente);
                    }
                }

                if (dias > 0) {
                    return context.getResources().getQuantityString(R.plurals.hace_dias, (int) dias, dias);
                } else if (horas > 0) {
                    return context.getResources().getQuantityString(R.plurals.hace_horas, (int) horas, horas);
                } else if (minutos > 0) {
                    return context.getResources().getQuantityString(R.plurals.hace_minutos, (int) minutos, minutos);
                } else if (segundos > 30) {
                    return context.getString(R.string.hace_segundos, segundos);
                } else {
                    return context.getString(R.string.hace_unos_segundos);
                }

            } catch (Exception e) {
                return context.getString(R.string.reciente);
            }
        }

        /**
         * Abre un chat para la solicitud actual, verificando primero los permisos del usuario
         * y creando un nuevo chat si no existe uno previo
         */
        private void abrirChat(SolicitudDonacion solicitud) {
            Usuario usuarioActual = SharedPreferencesManager.getCurrentUser(context);

            if (usuarioActual == null) {
                Toast.makeText(context, context.getString(R.string.error_obtener_info_usuario), Toast.LENGTH_SHORT).show();
                return;
            }

            if (usuarioActual.getRolid() == 2) {
                Toast.makeText(context, context.getString(R.string.receptores_no_pueden_chatear), Toast.LENGTH_LONG).show();
                return;
            }

            if (usuarioActual.getUsuarioid() == solicitud.getUsuarioid()) {
                Toast.makeText(context, context.getString(R.string.no_chatear_contigo_mismo), Toast.LENGTH_SHORT).show();
                return;
            }

            ApiService.getChatByUsuariosAndSolicitud(
                    usuarioActual.getUsuarioid(),
                    solicitud.getUsuarioid(),
                    solicitud.getSolicitudid(),
                    new ApiService.ApiCallback<Chat>() {
                        @Override
                        public void onSuccess(Chat chatExistente) {
                            abrirActivityChat(chatExistente, solicitud);
                        }

                        @Override
                        public void onError(String error) {
                            Chat nuevoChat = new Chat(
                                    usuarioActual.getUsuarioid(),
                                    solicitud.getUsuarioid(),
                                    solicitud.getSolicitudid()
                            );

                            ApiService.createChat(nuevoChat, new ApiService.ApiCallback<Chat>() {
                                @Override
                                public void onSuccess(Chat chatCreado) {
                                    abrirActivityChat(chatCreado, solicitud);
                                }

                                @Override
                                public void onError(String error) {
                                    Toast.makeText(context, context.getString(R.string.error_crear_chat, error), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
            );
        }

        /**
         * Abre la actividad de chat con toda la información necesaria
         */
        private void abrirActivityChat(Chat chat, SolicitudDonacion solicitud) {
            Usuario otroUsuario = usuariosMap.get(solicitud.getUsuarioid());
            String nombreChat = otroUsuario != null ?
                    otroUsuario.getNombre() + " " + otroUsuario.getApellido() : context.getString(R.string.usuario);

            Intent intent = new Intent(context, ChatC.class);
            intent.putExtra("chat_id", chat.getChatid());
            intent.putExtra("solicitud_id", solicitud.getSolicitudid());
            intent.putExtra("solicitud_titulo", solicitud.getTitulo());
            intent.putExtra("otro_usuario_id", solicitud.getUsuarioid());
            intent.putExtra("chat_nombre", nombreChat);
            context.startActivity(intent);
        }
    }
}