package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
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
    private Map<Integer, Usuario> usuariosMap; // Cache de usuarios
    private Map<Integer, HospitalUbicacion> hospitalesMap; // Cache de hospitales

    public SolicitudesAdapter(List<SolicitudDonacion> solicitudesList, Context context) {
        this.solicitudesList = solicitudesList;
        this.context = context;
        this.usuariosMap = new HashMap<>();
        this.hospitalesMap = new HashMap<>(); // Inicializar cache de hospitales
    }

    @NonNull
    @Override
    public SolicitudViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.desing_item_solicitud, parent, false);
        return new SolicitudViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SolicitudViewHolder holder, int position) {
        SolicitudDonacion solicitud = solicitudesList.get(position);
        holder.bind(solicitud);
    }

    @Override
    public int getItemCount() {
        return solicitudesList.size();
    }

    // ========== MÉTODOS PARA REALTIME ==========

    // Método para forzar la actualización de todas las vistas
    public void forzarActualizacionCompleta() {
        notifyDataSetChanged();
        System.out.println("🔄 FEED: Actualización forzada de todas las solicitudes");
    }

    // Método para forzar la carga de usuarios
    public void forzarCargaUsuarios() {
        System.out.println("👥 FEED: Forzando carga de usuarios para todas las solicitudes");

        for (int i = 0; i < solicitudesList.size(); i++) {
            SolicitudDonacion solicitud = solicitudesList.get(i);
            int usuarioId = solicitud.getUsuarioid();

            // Si el usuario no está en cache, forzar su carga
            if (!usuariosMap.containsKey(usuarioId)) {
                System.out.println("🔍 FEED: Forzando carga de usuario: " + usuarioId);
                cargarUsuarioForzado(usuarioId, i);
            } else {
                System.out.println("✅ FEED: Usuario " + usuarioId + " ya está en cache");
            }
        }
    }

    // Método para agregar una nueva solicitud (Realtime)
    public void agregarSolicitud(SolicitudDonacion solicitud) {
        System.out.println("🆕 FEED: Agregando nueva solicitud - " + solicitud.getTitulo());

        // Agregar al principio de la lista
        solicitudesList.add(0, solicitud);
        notifyItemInserted(0);

        // Forzar carga del usuario inmediatamente
        cargarUsuarioForzado(solicitud.getUsuarioid(), 0);

        System.out.println("✅ FEED: Nueva solicitud agregada en posición 0");
    }

    // Método para actualizar una solicitud existente (Realtime)
    public void actualizarSolicitud(SolicitudDonacion solicitud) {
        for (int i = 0; i < solicitudesList.size(); i++) {
            if (solicitudesList.get(i).getSolicitudid() == solicitud.getSolicitudid()) {
                solicitudesList.set(i, solicitud);
                notifyItemChanged(i);
                System.out.println("✏️ FEED: Solicitud actualizada en posición " + i);

                // Recargar datos del usuario por si cambió
                cargarUsuarioForzado(solicitud.getUsuarioid(), i);
                break;
            }
        }
    }

    // Método para eliminar una solicitud (Realtime)
    public void eliminarSolicitud(int solicitudId) {
        for (int i = 0; i < solicitudesList.size(); i++) {
            if (solicitudesList.get(i).getSolicitudid() == solicitudId) {
                solicitudesList.remove(i);
                notifyItemRemoved(i);
                System.out.println("🗑️ FEED: Solicitud eliminada de posición " + i);
                break;
            }
        }
    }

    // Método para actualizar la lista completa de solicitudes
    public void updateData(List<SolicitudDonacion> nuevasSolicitudes) {
        System.out.println("💥 FEED: ACTUALIZACIÓN AGRESIVA DE SOLICITUDES: " + nuevasSolicitudes.size() + " solicitudes");

        // Limpiar solo las solicitudes, mantener caches
        this.solicitudesList.clear();
        // NO limpiar usuariosMap ni hospitalesMap para mantener el cache

        // Agregar nuevas solicitudes
        this.solicitudesList.addAll(nuevasSolicitudes);

        // Notificar cambio INMEDIATO
        notifyDataSetChanged();

        System.out.println("✅ FEED: Lista de solicitudes actualizada - " + solicitudesList.size() + " solicitudes");

        // Forzar carga de usuarios inmediatamente
        forzarCargaUsuarios();

        // Cargar datos de hospitales
        cargarHospitalesParaSolicitudes();

        // Cargar datos de usuarios para las nuevas solicitudes
        cargarDatosUsuarios();
    }

    public void setUsuariosData(Map<Integer, Usuario> usuariosMap) {
        this.usuariosMap.clear();
        this.usuariosMap.putAll(usuariosMap);
        notifyDataSetChanged();
    }

    // ========== MÉTODOS PARA CARGAR HOSPITALES ==========

    private void cargarHospitalesParaSolicitudes() {
        Set<Integer> hospitalIds = new HashSet<>();

        // Identificar hospitales que necesitan ser cargados
        for (SolicitudDonacion solicitud : solicitudesList) {
            int hospitalId = solicitud.getHospitalid();
            if (!hospitalesMap.containsKey(hospitalId)) {
                hospitalIds.add(hospitalId);
            }
        }

        System.out.println("🏥 FEED: Cargando " + hospitalIds.size() + " hospitales desde API");

        // Cargar cada hospital individualmente
        for (Integer hospitalId : hospitalIds) {
            cargarHospitalIndividual(hospitalId);
        }
    }

    private void cargarHospitalIndividual(int hospitalId) {
        ApiService.getHospitalById(hospitalId, new ApiService.ApiCallback<HospitalUbicacion>() {
            @Override
            public void onSuccess(HospitalUbicacion hospital) {
                hospitalesMap.put(hospitalId, hospital);
                System.out.println("✅ FEED: Hospital cargado: " + hospital.getNombre());

                // Actualizar todas las solicitudes que usen este hospital
                actualizarSolicitudesConHospital(hospitalId, hospital);
            }

            @Override
            public void onError(String error) {
                System.out.println("❌ FEED: Error cargando hospital " + hospitalId + ": " + error);

                // Reintentar después de 2 segundos
                new Handler().postDelayed(() -> {
                    cargarHospitalIndividual(hospitalId);
                }, 2000);
            }
        });
    }

    private void actualizarSolicitudesConHospital(int hospitalId, HospitalUbicacion hospital) {
        for (int i = 0; i < solicitudesList.size(); i++) {
            if (solicitudesList.get(i).getHospitalid() == hospitalId) {
                solicitudesList.get(i).setHospital(hospital);

                // Actualizar la vista específica
                final int position = i;
                runOnUiThread(() -> {
                    if (position >= 0 && position < solicitudesList.size()) {
                        notifyItemChanged(position);
                    }
                });
            }
        }
    }

    // ========== MÉTODOS PRIVADOS ==========

    private void cargarUsuarioForzado(int usuarioId, int position) {
        ApiService.getUsuarioById(usuarioId, new ApiService.ApiCallback<Usuario>() {
            @Override
            public void onSuccess(Usuario usuario) {
                usuariosMap.put(usuarioId, usuario);
                System.out.println("✅ FEED: Usuario forzado cargado: " + usuario.getNombre());

                // Actualizar la vista específica
                if (position >= 0 && position < solicitudesList.size()) {
                    runOnUiThread(() -> notifyItemChanged(position));
                }
            }

            @Override
            public void onError(String error) {
                System.out.println("❌ FEED: Error forzando carga de usuario " + usuarioId + ": " + error);

                // Reintentar después de 2 segundos
                new Handler().postDelayed(() -> {
                    if (position < solicitudesList.size() && solicitudesList.get(position).getUsuarioid() == usuarioId) {
                        cargarUsuarioForzado(usuarioId, position);
                    }
                }, 2000);
            }
        });
    }

    // NUEVO MÉTODO: Para cargar usuarios individuales desde la clase principal
    private void cargarUsuarioIndividual(int usuarioId, int position) {
        ApiService.getUsuarioById(usuarioId, new ApiService.ApiCallback<Usuario>() {
            @Override
            public void onSuccess(Usuario usuario) {
                usuariosMap.put(usuarioId, usuario);
                System.out.println("✅ FEED: Usuario individual cargado: " + usuario.getNombre());

                // Actualizar específicamente este item INMEDIATAMENTE
                if (position >= 0 && position < solicitudesList.size()) {
                    runOnUiThread(() -> notifyItemChanged(position));
                }
            }

            @Override
            public void onError(String error) {
                System.out.println("❌ FEED: Error cargando usuario individual " + usuarioId);
                // Intentar de nuevo después de 1 segundo
                new Handler().postDelayed(() -> {
                    if (position < solicitudesList.size() && solicitudesList.get(position).getUsuarioid() == usuarioId) {
                        cargarUsuarioIndividual(usuarioId, position);
                    }
                }, 1000);
            }
        });
    }

    private void cargarDatosUsuarios() {
        if (solicitudesList.isEmpty()) {
            System.out.println("📭 FEED: No hay solicitudes para cargar usuarios");
            return;
        }

        // Obtener IDs únicos de usuarios
        List<Integer> usuarioIds = new ArrayList<>();
        for (SolicitudDonacion solicitud : solicitudesList) {
            if (!usuariosMap.containsKey(solicitud.getUsuarioid())) {
                usuarioIds.add(solicitud.getUsuarioid());
            }
        }

        if (usuarioIds.isEmpty()) {
            System.out.println("✅ FEED: Todos los usuarios ya están en cache");
            return;
        }

        System.out.println("👥 FEED: Cargando " + usuarioIds.size() + " usuarios desde API");

        // Cargar usuarios desde la API
        ApiService.getUsuariosByIds(usuarioIds, new ApiService.ListCallback<Usuario>() {
            @Override
            public void onSuccess(List<Usuario> usuarios) {
                for (Usuario usuario : usuarios) {
                    usuariosMap.put(usuario.getUsuarioid(), usuario);
                }
                System.out.println("✅ FEED: " + usuarios.size() + " usuarios cargados exitosamente");

                // Forzar actualización completa
                runOnUiThread(() -> {
                    notifyDataSetChanged();
                    System.out.println("🔄 FEED: Vistas actualizadas después de carga de usuarios");
                });
            }

            @Override
            public void onError(String error) {
                System.out.println("❌ FEED: Error cargando usuarios: " + error);

                // Intentar carga individual como fallback
                for (Integer usuarioId : usuarioIds) {
                    for (int i = 0; i < solicitudesList.size(); i++) {
                        if (solicitudesList.get(i).getUsuarioid() == usuarioId) {
                            // Usar el nuevo método de la clase principal
                            cargarUsuarioIndividual(usuarioId, i);
                            break;
                        }
                    }
                }
            }
        });
    }

    // Agrega este método helper para ejecutar en UI thread
    private void runOnUiThread(Runnable action) {
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).runOnUiThread(action);
        } else {
            new Handler(android.os.Looper.getMainLooper()).post(action);
        }
    }

    // ========== VIEW HOLDER ==========

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

        private void cargarDatosHospital(int hospitalId, int position) {
            // Si ya está en cache, no cargar de nuevo
            if (hospitalesMap.containsKey(hospitalId)) {
                return;
            }

            ApiService.getHospitalById(hospitalId, new ApiService.ApiCallback<HospitalUbicacion>() {
                @Override
                public void onSuccess(HospitalUbicacion hospital) {
                    hospitalesMap.put(hospitalId, hospital);

                    // Actualizar todas las solicitudes que usen este hospital
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
                    System.out.println("❌ Error cargando hospital: " + error);
                }
            });
        }

        public void bind(SolicitudDonacion solicitud) {
            int position = getAdapterPosition();
            System.out.println("🎯 FEED: RENDERIZANDO solicitud " + solicitud.getSolicitudid() + " en posición " + position);

            // Título y descripción
            textTitle.setText(solicitud.getTitulo() != null ? solicitud.getTitulo() : "Sin título");
            textDescription.setText(solicitud.getDescripcion() != null ? solicitud.getDescripcion() : "Sin descripción");

            // UBICACIÓN - Usar el cache de hospitales
            configurarUbicacion(solicitud, position);

            // Tipo de sangre
            textBloodTypeBadge.setText(obtenerTipoSangreCompleto(solicitud.getTiposangreid()));

            // Tiempo de publicación
            textTimeAgo.setText(calcularTiempoTranscurrido(solicitud.getFechaPublicacion()));

            // ✅ Cargar datos REALES del usuario - VERSIÓN AGRESIVA
            cargarDatosUsuarioAgresivo(solicitud.getUsuarioid(), position);

            // ✅ Cargar imagen adjunta
            cargarImagenAdjunta(solicitud);

            // Click listeners
            btnChatear.setOnClickListener(v -> abrirChat(solicitud));
            itemView.setOnClickListener(v -> {
                Toast.makeText(context, "Abriendo detalles de: " + solicitud.getTitulo(), Toast.LENGTH_SHORT).show();
            });
        }

        private void configurarUbicacion(SolicitudDonacion solicitud, int position) {
            HospitalUbicacion hospital = hospitalesMap.get(solicitud.getHospitalid());

            if (hospital != null) {
                // Tenemos datos del hospital en cache
                solicitud.setHospital(hospital);
                textLocationDetails.setText(hospital.getNombre());

                // Hacer clickeable si tiene link
                if (hospital.getLink() != null && !hospital.getLink().isEmpty()) {
                    textLocationDetails.setTextColor(context.getResources().getColor(android.R.color.holo_blue_dark));
                    textLocationDetails.setClickable(true);
                    textLocationDetails.setFocusable(true);

                    textLocationDetails.setOnClickListener(v -> {
                        abrirGoogleMaps(hospital);
                    });
                } else {
                    // No tiene link, mostrar normal
                    textLocationDetails.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
                    textLocationDetails.setClickable(false);
                    textLocationDetails.setFocusable(false);
                }
            } else {
                // No tenemos datos del hospital, mostrar loading
                textLocationDetails.setText("Cargando hospital...");
                textLocationDetails.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
                textLocationDetails.setClickable(false);
                textLocationDetails.setFocusable(false);

                // Forzar carga del hospital
                cargarDatosHospital(solicitud.getHospitalid(), position);
            }
        }

        // Método para abrir Google Maps
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
                if (intent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(intent);
                } else {
                    // Si Google Maps no está instalado, abrir en navegador
                    intent.setPackage(null);
                    context.startActivity(intent);
                }

            } catch (Exception e) {
                Toast.makeText(context, "Error al abrir la ubicación", Toast.LENGTH_SHORT).show();
                Log.e("Maps", "Error al abrir Google Maps: " + e.getMessage());
            }
        }

        private void cargarDatosUsuarioAgresivo(int usuarioId, int position) {
            Usuario usuario = usuariosMap.get(usuarioId);

            if (usuario != null) {
                // ✅ DATOS REALES del usuario - MOSTRAR INMEDIATAMENTE
                String nombreCompleto = usuario.getNombre() + " " + usuario.getApellido();
                textUserName.setText(nombreCompleto);

                // ✅ FOTO DE PERFIL REAL - MOSTRAR INMEDIATAMENTE
                if (usuario.getFotoUrl() != null && !usuario.getFotoUrl().isEmpty()) {
                    Glide.with(context)
                            .load(usuario.getFotoUrl())
                            .placeholder(R.drawable.logo_findtogive)
                            .error(R.drawable.logo_findtogive)
                            .apply(RequestOptions.circleCropTransform())
                            .into(imgProfile);
                } else {
                    // Foto por defecto si no tiene
                    imgProfile.setImageResource(R.drawable.logo_findtogive);
                }

                System.out.println("✅ FEED: Usuario " + usuarioId + " mostrado desde cache");
            } else {
                // Datos temporales mientras se carga
                textUserName.setText("Cargando...");
                imgProfile.setImageResource(R.drawable.logo_findtogive);
                System.out.println("⏳ FEED: Cargando usuario " + usuarioId + " desde API...");

                // Cargar usuario individual si no está en el cache - VERSIÓN AGRESIVA
                cargarUsuarioIndividualAgresivo(usuarioId, position);
            }
        }

        private void cargarUsuarioIndividualAgresivo(int usuarioId, int position) {
            ApiService.getUsuarioById(usuarioId, new ApiService.ApiCallback<Usuario>() {
                @Override
                public void onSuccess(Usuario usuario) {
                    usuariosMap.put(usuarioId, usuario);
                    System.out.println("✅ FEED: Usuario individual cargado: " + usuario.getNombre());

                    // Actualizar específicamente este item INMEDIATAMENTE
                    if (position >= 0 && position < solicitudesList.size()) {
                        runOnUiThread(() -> notifyItemChanged(position));
                    }
                }

                @Override
                public void onError(String error) {
                    System.out.println("❌ FEED: Error cargando usuario individual " + usuarioId);
                    // Intentar de nuevo después de 1 segundo
                    new Handler().postDelayed(() -> {
                        if (position < solicitudesList.size() && solicitudesList.get(position).getUsuarioid() == usuarioId) {
                            cargarUsuarioIndividualAgresivo(usuarioId, position);
                        }
                    }, 1000);
                }
            });
        }

        private void cargarImagenAdjunta(SolicitudDonacion solicitud) {
            Log.d("SolicitudesAdapter", "🖼️ Cargando imagen para solicitud: " + solicitud.getSolicitudid());
            Log.d("SolicitudesAdapter", "📸 URL: " + solicitud.getImagenUrl());

            if (solicitud.getImagenUrl() != null && !solicitud.getImagenUrl().isEmpty()) {
                Log.d("SolicitudesAdapter", "✅ URL válida, mostrando imagen");

                // Asegurar visibilidad
                imgAdjunta.setVisibility(View.VISIBLE);

                // Cargar con Glide - VERSIÓN MÁS ROBUSTA
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
                                Log.e("SolicitudesAdapter", "❌ Error Glide: " +
                                        (e != null ? e.getMessage() : "Desconocido"));
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model,
                                                           Target<Drawable> target, DataSource dataSource,
                                                           boolean isFirstResource) {
                                Log.d("SolicitudesAdapter", "✅ Imagen cargada exitosamente - Tamaño: " +
                                        resource.getIntrinsicWidth() + "x" + resource.getIntrinsicHeight());
                                return false;
                            }
                        })
                        .into(imgAdjunta);

            } else {
                Log.d("SolicitudesAdapter", "❌ No hay imagen, ocultando contenedor");
                imgAdjunta.setVisibility(View.GONE);
            }
        }

        private void expandirImagen(String imageUrl) {
            Toast.makeText(context, "Ver imagen completa", Toast.LENGTH_SHORT).show();
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

                long segundos = Math.abs(duracion.getSeconds()); // Usar valor absoluto
                long minutos = segundos / 60;
                long horas = minutos / 60;
                long dias = horas / 24;

                // Si la fecha es futura, mostrar "Próximamente"
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

                // Si la fecha es pasada
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

        private void abrirChat(SolicitudDonacion solicitud) {
            Usuario usuarioActual = SharedPreferencesManager.getCurrentUser(context);

            if (usuarioActual == null) {
                Toast.makeText(context, "Error: No se pudo obtener información del usuario", Toast.LENGTH_SHORT).show();
                return;
            }

            // Verificar rol del usuario - solo donantes (1) o ambos (3) pueden iniciar chats
            // Receptores (2) NO pueden iniciar chats
            if (usuarioActual.getRolid() == 2) {
                Toast.makeText(context, "Los receptores no pueden iniciar chats", Toast.LENGTH_LONG).show();
                return;
            }

            if (usuarioActual.getUsuarioid() == solicitud.getUsuarioid()) {
                Toast.makeText(context, "No puedes chatear contigo mismo", Toast.LENGTH_SHORT).show();
                return;
            }

            // Buscar si ya existe un chat
            ApiService.getChatByUsuariosAndSolicitud(
                    usuarioActual.getUsuarioid(),
                    solicitud.getUsuarioid(),
                    solicitud.getSolicitudid(),
                    new ApiService.ApiCallback<Chat>() {
                        @Override
                        public void onSuccess(Chat chatExistente) {
                            // Chat existe, abrir directamente
                            abrirActivityChat(chatExistente, solicitud);
                        }

                        @Override
                        public void onError(String error) {
                            // No existe chat, crear uno nuevo
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
                                    Toast.makeText(context, "Error al crear chat: " + error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
            );
        }

        private void abrirActivityChat(Chat chat, SolicitudDonacion solicitud) {
            // Obtener información del otro usuario para el título del chat
            Usuario otroUsuario = usuariosMap.get(solicitud.getUsuarioid());
            String nombreChat = otroUsuario != null ?
                    otroUsuario.getNombre() + " " + otroUsuario.getApellido() : "Usuario";

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