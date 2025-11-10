package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sv.edu.catolica.findtogive.Modelado.Mensaje;
import sv.edu.catolica.findtogive.Modelado.Usuario;
import sv.edu.catolica.findtogive.R;

public class MensajesAdapter extends RecyclerView.Adapter<MensajesAdapter.MensajeViewHolder> {

    private static final int TYPE_SEND = 1;
    private static final int TYPE_RECEIVE = 2;

    private List<Mensaje> mensajesList;
    private int usuarioActualId;
    private int otroUsuarioId;
    private Map<Integer, Usuario> usuariosMap;

    public MensajesAdapter(List<Mensaje> mensajesList, int usuarioActualId, int otroUsuarioId) {
        this.mensajesList = mensajesList;
        this.usuarioActualId = usuarioActualId;
        this.otroUsuarioId = otroUsuarioId;
        this.usuariosMap = new HashMap<>();
    }

    @Override
    public int getItemViewType(int position) {
        Mensaje mensaje = mensajesList.get(position);
        return mensaje.getEmisorioid() == usuarioActualId ? TYPE_SEND : TYPE_RECEIVE;
    }

    @NonNull
    @Override
    public MensajeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;

        if (viewType == TYPE_SEND) {
            view = inflater.inflate(R.layout.desing_item_chat_send, parent, false);
        } else {
            view = inflater.inflate(R.layout.desing_item_chat_receive, parent, false);
        }

        return new MensajeViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull MensajeViewHolder holder, int position) {
        Mensaje mensaje = mensajesList.get(position);
        holder.bind(mensaje);
    }

    @Override
    public int getItemCount() {
        return mensajesList.size();
    }

    // Método para agregar un nuevo mensaje
    public void agregarMensaje(Mensaje mensaje) {
        // Verificar que el mensaje no exista ya
        for (Mensaje existing : mensajesList) {
            if (existing.getMensajeid() == mensaje.getMensajeid()) {
                return; // Ya existe, no agregar
            }
        }

        mensajesList.add(mensaje);
        notifyItemInserted(mensajesList.size() - 1);
    }

    // Método para verificar si un mensaje ya existe
    public boolean contieneMensaje(int mensajeId) {
        for (Mensaje mensaje : mensajesList) {
            if (mensaje.getMensajeid() == mensajeId) {
                return true;
            }
        }
        return false;
    }

    // Método para actualizar toda la lista
    public void actualizarMensajes(List<Mensaje> nuevosMensajes) {
        mensajesList.clear();
        mensajesList.addAll(nuevosMensajes);
        notifyDataSetChanged();
    }

    // Método para precargar el usuario
    public void precargarUsuario(Usuario usuario) {
        if (usuario != null) {
            usuariosMap.put(usuario.getUsuarioid(), usuario);
        }
    }

    // NUEVO MÉTODO: Actualizar estado de leído de un mensaje específico
    public void actualizarEstadoLeido(int mensajeId, boolean leido) {
        for (int i = 0; i < mensajesList.size(); i++) {
            Mensaje mensaje = mensajesList.get(i);
            if (mensaje.getMensajeid() == mensajeId) {
                mensaje.setLeido(leido);
                notifyItemChanged(i);
                break;
            }
        }
    }

    // NUEVO MÉTODO: Actualizar estado de leído para todos los mensajes del otro usuario
    public void marcarTodosComoLeidos() {
        boolean changed = false;
        for (int i = 0; i < mensajesList.size(); i++) {
            Mensaje mensaje = mensajesList.get(i);
            // Solo marcar mensajes del otro usuario que no estén leídos
            if (mensaje.getEmisorioid() != usuarioActualId && !mensaje.isLeido()) {
                mensaje.setLeido(true);
                changed = true;
            }
        }
        if (changed) {
            notifyDataSetChanged();
        }
    }

    public class MensajeViewHolder extends RecyclerView.ViewHolder {

        private TextView textMessageContent;
        private TextView textMessageTime;
        private TextView textLeidoStatus; // NUEVO: Para mostrar estado de leído
        private ImageView imgProfile;
        private int viewType;

        public MensajeViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            this.viewType = viewType;

            textMessageContent = itemView.findViewById(R.id.text_message_content);
            textMessageTime = itemView.findViewById(R.id.text_message_time);

            // NUEVO: Inicializar TextView para estado de leído (solo en mensajes enviados)
            if (viewType == TYPE_SEND) {
                textLeidoStatus = itemView.findViewById(R.id.text_leido_status);
            }

            if (viewType == TYPE_RECEIVE) {
                imgProfile = itemView.findViewById(R.id.img_profile);
            }
        }

        public void bind(Mensaje mensaje) {
            textMessageContent.setText(mensaje.getContenido());

            // Usar el mismo método para formatear la hora en los mensajes del chat
            if (mensaje.getFechaEnvio() != null) {
                String hora = calcularTiempoMensaje(mensaje.getFechaEnvio());
                textMessageTime.setText(hora);
            } else {
                textMessageTime.setText("");
            }

            // NUEVO: Mostrar estado de leído para mensajes enviados
            if (viewType == TYPE_SEND && textLeidoStatus != null) {
                if (mensaje.isLeido()) {
                    textLeidoStatus.setText("✓✓"); // Doble check para leído
                    textLeidoStatus.setTextColor(Color.parseColor("#4CAF50")); // Verde
                    textLeidoStatus.setAlpha(1.0f); // Totalmente visible
                } else {
                    textLeidoStatus.setText("✓"); // Check simple para enviado
                    textLeidoStatus.setTextColor(Color.parseColor("#CCFFFFFF")); // Blanco semitransparente
                    textLeidoStatus.setAlpha(0.7f); // Semitransparente
                }
                textLeidoStatus.setVisibility(View.VISIBLE);
            }

            // Para mensajes recibidos, cargar la foto del OTRO usuario
            if (viewType == TYPE_RECEIVE && imgProfile != null) {
                cargarFotoPerfil();
            }
        }

        private String calcularTiempoMensaje(String fechaMensaje) {
            if (fechaMensaje == null || fechaMensaje.isEmpty()) {
                return "";
            }

            try {
                // Formato de Supabase: "2025-10-21 06:05:51.520241" o "2025-10-21T06:05:51.520241"
                String fechaLimpia = fechaMensaje.replace(" ", "T");
                java.time.LocalDateTime fecha = java.time.LocalDateTime.parse(fechaLimpia);
                java.time.LocalDateTime ahora = java.time.LocalDateTime.now();

                java.time.Duration duracion = java.time.Duration.between(fecha, ahora);

                long segundos = Math.abs(duracion.getSeconds());
                long minutos = segundos / 60;
                long horas = minutos / 60;

                // Para mensajes en el chat, mostrar solo la hora (HH:mm)
                if (duracion.isNegative() || minutos < 1) {
                    return "Ahora";
                } else if (minutos < 60) {
                    return fechaLimpia.substring(11, 16); // HH:mm
                } else if (horas < 24) {
                    return fechaLimpia.substring(11, 16); // HH:mm
                } else {
                    return fechaLimpia.substring(5, 10); // MM-DD
                }

            } catch (Exception e) {
                return "";
            }
        }

        private void cargarFotoPerfil() {
            // Siempre cargar la foto del OTRO usuario del chat
            Usuario usuario = usuariosMap.get(otroUsuarioId);

            if (usuario != null && usuario.getFotoUrl() != null && !usuario.getFotoUrl().isEmpty()) {
                // Cargar la foto real
                Glide.with(itemView.getContext())
                        .load(usuario.getFotoUrl())
                        .placeholder(R.drawable.logo_findtogive)
                        .error(R.drawable.logo_findtogive)
                        .apply(RequestOptions.circleCropTransform())
                        .into(imgProfile);
            } else {
                // Usar placeholder
                imgProfile.setImageResource(R.drawable.logo_findtogive);

                // Si no tenemos el usuario en cache, cargarlo
                if (usuario == null) {
                    cargarUsuario(otroUsuarioId);
                }
            }
        }

        private void cargarUsuario(int usuarioId) {
            ApiService.getUsuarioById(usuarioId, new ApiService.ApiCallback<Usuario>() {
                @Override
                public void onSuccess(Usuario usuario) {
                    usuariosMap.put(usuarioId, usuario);

                    // Actualizar la imagen si este ViewHolder todavía es válido
                    if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                        if (usuario.getFotoUrl() != null && !usuario.getFotoUrl().isEmpty()) {
                            Glide.with(itemView.getContext())
                                    .load(usuario.getFotoUrl())
                                    .placeholder(R.drawable.logo_findtogive)
                                    .error(R.drawable.logo_findtogive)
                                    .apply(RequestOptions.circleCropTransform())
                                    .into(imgProfile);
                        }
                    }
                }

                @Override
                public void onError(String error) {
                    // Mantener el placeholder
                    imgProfile.setImageResource(R.drawable.logo_findtogive);
                }
            });
        }
    }
}