package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

    /**
     * Determina el tipo de vista para cada posición basado en si el mensaje
     * fue enviado por el usuario actual o recibido de otro usuario
     */
    @Override
    public int getItemViewType(int position) {
        Mensaje mensaje = mensajesList.get(position);
        return mensaje.getEmisorioid() == usuarioActualId ? TYPE_SEND : TYPE_RECEIVE;
    }

    /**
     * Crea y retorna una nueva instancia de MensajeViewHolder inflando
     * el layout correspondiente según el tipo de mensaje (enviado/recibido)
     */
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

    /**
     * Vincula los datos del mensaje en la posición especificada con el ViewHolder
     */
    @Override
    public void onBindViewHolder(@NonNull MensajeViewHolder holder, int position) {
        Mensaje mensaje = mensajesList.get(position);
        holder.bind(mensaje);
    }

    /**
     * Retorna el número total de mensajes en la lista
     */
    @Override
    public int getItemCount() {
        return mensajesList.size();
    }

    /**
     * Agrega un nuevo mensaje a la lista verificando primero que no exista
     * para evitar duplicados. Notifica la inserción para actualizar la vista
     */
    public void agregarMensaje(Mensaje mensaje) {
        for (Mensaje existing : mensajesList) {
            if (existing.getMensajeid() == mensaje.getMensajeid()) {
                return; // Ya existe, no agregar
            }
        }

        mensajesList.add(mensaje);
        notifyItemInserted(mensajesList.size() - 1);
    }

    /**
     * Verifica si un mensaje con el ID especificado ya existe en la lista
     */
    public boolean contieneMensaje(int mensajeId) {
        for (Mensaje mensaje : mensajesList) {
            if (mensaje.getMensajeid() == mensajeId) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reemplaza completamente la lista de mensajes con una nueva lista
     * y notifica el cambio para actualizar toda la vista
     */
    public void actualizarMensajes(List<Mensaje> nuevosMensajes) {
        mensajesList.clear();
        mensajesList.addAll(nuevosMensajes);
        notifyDataSetChanged();
    }

    /**
     * Precarga la información de un usuario en el mapa de caché para
     * uso posterior en la visualización de fotos de perfil
     */
    public void precargarUsuario(Usuario usuario) {
        if (usuario != null) {
            usuariosMap.put(usuario.getUsuarioid(), usuario);
        }
    }

    /**
     * Actualiza el estado de leído de un mensaje específico identificado por su ID.
     * Busca el mensaje en la lista y actualiza su vista si es encontrado
     */
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

    /**
     * Marca todos los mensajes del otro usuario como leídos.
     * Solo afecta a mensajes recibidos que no estaban previamente marcados como leídos
     */
    public void marcarTodosComoLeidos() {
        boolean changed = false;
        for (int i = 0; i < mensajesList.size(); i++) {
            Mensaje mensaje = mensajesList.get(i);
            if (mensaje.getEmisorioid() != usuarioActualId && !mensaje.isLeido()) {
                mensaje.setLeido(true);
                changed = true;
            }
        }
        if (changed) {
            notifyDataSetChanged();
        }
    }

    /**
     * Actualiza el estado de leído de múltiples mensajes
     */
    public void actualizarEstadosLeido(Map<Integer, Boolean> estadosLeido) {
        boolean cambios = false;

        for (int i = 0; i < mensajesList.size(); i++) {
            Mensaje mensaje = mensajesList.get(i);
            Boolean nuevoEstado = estadosLeido.get(mensaje.getMensajeid());

            if (nuevoEstado != null && mensaje.isLeido() != nuevoEstado) {
                mensaje.setLeido(nuevoEstado);
                notifyItemChanged(i);
                cambios = true;
            }
        }

        if (cambios) {
            notifyDataSetChanged();
        }
    }

    /**
     * Sincroniza la lista local con una lista actualizada del servidor
     */
    public void sincronizarMensajes(List<Mensaje> mensajesActualizados) {
        Map<Integer, Mensaje> mensajesMap = new HashMap<>();
        for (Mensaje mensaje : mensajesActualizados) {
            mensajesMap.put(mensaje.getMensajeid(), mensaje);
        }

        // Actualizar mensajes existentes
        for (int i = 0; i < mensajesList.size(); i++) {
            Mensaje mensajeLocal = mensajesList.get(i);
            Mensaje mensajeActualizado = mensajesMap.get(mensajeLocal.getMensajeid());

            if (mensajeActualizado != null) {
                // Actualizar solo si hay cambios
                if (mensajeLocal.isLeido() != mensajeActualizado.isLeido()) {
                    mensajeLocal.setLeido(mensajeActualizado.isLeido());
                    notifyItemChanged(i);
                }
                // Remover del mapa para saber cuáles quedan por agregar
                mensajesMap.remove(mensajeLocal.getMensajeid());
            }
        }

        // Agregar nuevos mensajes (si los hay)
        for (Mensaje nuevoMensaje : mensajesMap.values()) {
            agregarMensaje(nuevoMensaje);
        }
    }

    public class MensajeViewHolder extends RecyclerView.ViewHolder {

        private TextView textMessageContent;
        private TextView textMessageTime;
        private TextView textLeidoStatus;
        private ImageView imgProfile;
        private int viewType;
        private View itemView;

        public MensajeViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            this.viewType = viewType;
            this.itemView = itemView;

            textMessageContent = itemView.findViewById(R.id.text_message_content);
            textMessageTime = itemView.findViewById(R.id.text_message_time);

            if (viewType == TYPE_SEND) {
                textLeidoStatus = itemView.findViewById(R.id.text_leido_status);
            }

            if (viewType == TYPE_RECEIVE) {
                imgProfile = itemView.findViewById(R.id.img_profile);
            }

            setupLongClick();
        }

        /**
         * Configura el listener de long click para permitir copiar el contenido
         * del mensaje al portapapeles del dispositivo
         */
        private void setupLongClick() {
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Mensaje mensaje = mensajesList.get(position);
                        copiarTexto(mensaje.getContenido());
                        return true;
                    }
                    return false;
                }
            });
        }

        /**
         * Copia el texto especificado al portapapeles del sistema y muestra
         * un mensaje toast de confirmación o error
         */
        private void copiarTexto(String texto) {
            try {
                android.content.ClipboardManager clipboard =
                        (android.content.ClipboardManager) itemView.getContext()
                                .getSystemService(Context.CLIPBOARD_SERVICE);

                android.content.ClipData clip = android.content.ClipData
                        .newPlainText(itemView.getContext().getString(R.string.mensaje_chat_label), texto);

                clipboard.setPrimaryClip(clip);

                Toast.makeText(itemView.getContext(),
                        itemView.getContext().getString(R.string.mensaje_copiado),
                        Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                Toast.makeText(itemView.getContext(),
                        itemView.getContext().getString(R.string.error_copiar),
                        Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * Vincula los datos del mensaje a los elementos de la vista,
         * incluyendo contenido, hora, estado de leído y foto de perfil
         */
        public void bind(Mensaje mensaje) {
            textMessageContent.setText(mensaje.getContenido());

            if (mensaje.getFechaEnvio() != null) {
                String hora = calcularTiempoMensaje(mensaje.getFechaEnvio());
                textMessageTime.setText(hora);
            } else {
                textMessageTime.setText("");
            }

            if (viewType == TYPE_SEND && textLeidoStatus != null) {
                if (mensaje.isLeido()) {
                    textLeidoStatus.setText(itemView.getContext().getString(R.string.mensaje_leido));
                    textLeidoStatus.setTextColor(Color.parseColor("#4CAF50"));
                    textLeidoStatus.setAlpha(1.0f);
                } else {
                    textLeidoStatus.setText(itemView.getContext().getString(R.string.mensaje_enviado));
                    textLeidoStatus.setTextColor(Color.parseColor("#CCFFFFFF"));
                    textLeidoStatus.setAlpha(0.7f);
                }
                textLeidoStatus.setVisibility(View.VISIBLE);
            }

            if (viewType == TYPE_RECEIVE && imgProfile != null) {
                cargarFotoPerfil();
            }
        }

        /**
         * Calcula y formatea el tiempo del mensaje para mostrar en la interfaz.
         * Muestra "Ahora" para mensajes recientes, hora (HH:mm) para mensajes del mismo día,
         * o fecha (MM-DD) para mensajes más antiguos
         */
        private String calcularTiempoMensaje(String fechaMensaje) {
            if (fechaMensaje == null || fechaMensaje.isEmpty()) {
                return "";
            }

            try {
                String fechaLimpia = fechaMensaje.replace(" ", "T");
                java.time.LocalDateTime fecha = java.time.LocalDateTime.parse(fechaLimpia);
                java.time.LocalDateTime ahora = java.time.LocalDateTime.now();

                java.time.Duration duracion = java.time.Duration.between(fecha, ahora);

                long segundos = Math.abs(duracion.getSeconds());
                long minutos = segundos / 60;
                long horas = minutos / 60;

                if (duracion.isNegative() || minutos < 1) {
                    return itemView.getContext().getString(R.string.ahora);
                } else if (minutos < 60) {
                    return fechaLimpia.substring(11, 16);
                } else if (horas < 24) {
                    return fechaLimpia.substring(11, 16);
                } else {
                    return fechaLimpia.substring(5, 10);
                }

            } catch (Exception e) {
                return "";
            }
        }

        /**
         * Carga la foto de perfil del otro usuario en el ImageView correspondiente.
         * Usa caché de usuarios y realiza una petición a la API si no está disponible
         */
        private void cargarFotoPerfil() {
            Usuario usuario = usuariosMap.get(otroUsuarioId);

            if (usuario != null && usuario.getFotoUrl() != null && !usuario.getFotoUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(usuario.getFotoUrl())
                        .placeholder(R.drawable.logo_findtogive)
                        .error(R.drawable.logo_findtogive)
                        .apply(RequestOptions.circleCropTransform())
                        .into(imgProfile);
            } else {
                imgProfile.setImageResource(R.drawable.logo_findtogive);

                if (usuario == null) {
                    cargarUsuario(otroUsuarioId);
                }
            }
        }

        /**
         * Carga la información de un usuario específico desde la API y actualiza
         * la foto de perfil una vez obtenidos los datos
         */
        private void cargarUsuario(int usuarioId) {
            ApiService.getUsuarioById(usuarioId, new ApiService.ApiCallback<Usuario>() {
                @Override
                public void onSuccess(Usuario usuario) {
                    usuariosMap.put(usuarioId, usuario);

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
                    imgProfile.setImageResource(R.drawable.logo_findtogive);
                }
            });
        }
    }
}