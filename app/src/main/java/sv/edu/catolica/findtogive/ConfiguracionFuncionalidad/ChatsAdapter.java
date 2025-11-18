package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import sv.edu.catolica.findtogive.ClasesDiseño.ChatC;
import sv.edu.catolica.findtogive.Modelado.Chat;
import sv.edu.catolica.findtogive.Modelado.Mensaje;
import sv.edu.catolica.findtogive.Modelado.Usuario;
import sv.edu.catolica.findtogive.R;

public class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ChatViewHolder> {

    private List<Chat> chatsList;
    private Context context;
    private Map<Integer, Usuario> usuariosMap;
    private Map<Integer, List<Mensaje>> mensajesMap;
    private Map<Integer, String> ultimoMensajeMap;
    private Map<Integer, String> ultimaFechaEnvioMap;
    private Map<Integer, Integer> ultimoMensajeIdMap;
    private Usuario usuarioActual;

    public ChatsAdapter(List<Chat> chatsList, Context context) {
        this.chatsList = chatsList;
        this.context = context;
        this.usuariosMap = new HashMap<>();
        this.mensajesMap = new HashMap<>();
        this.ultimoMensajeMap = new HashMap<>();
        this.ultimaFechaEnvioMap = new HashMap<>();
        this.ultimoMensajeIdMap = new HashMap<>();
        this.usuarioActual = SharedPreferencesManager.getCurrentUser(context);
    }

    /**
     * Crea y retorna una nueva instancia de ChatViewHolder inflando el layout del item del chat
     */
    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.desing_item_chat_list, parent, false);
        return new ChatViewHolder(view);
    }

    // Interface para manejar el click en la foto de perfil
    public interface OnFotoPerfilClickListener {
        void onFotoPerfilClick(Usuario usuario);
    }

    private OnFotoPerfilClickListener listener;

    /**
     * Establece el listener para manejar clicks en la foto de perfil
     */
    public void setOnFotoPerfilClickListener(OnFotoPerfilClickListener listener) {
        this.listener = listener;
    }

    /**
     * Vincula los datos del chat en la posición especificada con el ViewHolder
     */
    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chatsList.get(position);
        holder.bind(chat);
    }

    /**
     * Retorna el número total de chats en la lista
     */
    @Override
    public int getItemCount() {
        return chatsList.size();
    }

    /**
     * Ordena los chats inicialmente separándolos en dos grupos: con mensajes y sin mensajes.
     * Los chats con mensajes se ordenan por ID de mensaje descendente y se colocan primero,
     * seguidos de los chats sin mensajes.
     */
    private void ordenarChatsInicial() {
        List<Chat> chatsConMensajes = new ArrayList<>();
        List<Chat> chatsSinMensajes = new ArrayList<>();

        for (Chat chat : chatsList) {
            Integer ultimoMensajeId = ultimoMensajeIdMap.get(chat.getChatid());
            if (ultimoMensajeId != null && ultimoMensajeId > 0) {
                chatsConMensajes.add(chat);
            } else {
                chatsSinMensajes.add(chat);
            }
        }

        Collections.sort(chatsConMensajes, new Comparator<Chat>() {
            @Override
            public int compare(Chat chat1, Chat chat2) {
                Integer id1 = ultimoMensajeIdMap.get(chat1.getChatid());
                Integer id2 = ultimoMensajeIdMap.get(chat2.getChatid());
                return id2.compareTo(id1);
            }
        });

        chatsList.clear();
        chatsList.addAll(chatsConMensajes);
        chatsList.addAll(chatsSinMensajes);
    }

    /**
     * Actualiza completamente la lista de chats, limpiando todas las cachés existentes
     * y cargando nuevamente usuarios y mensajes para cada chat
     */
    public void actualizarChats(List<Chat> nuevosChats) {
        chatsList.clear();
        usuariosMap.clear();
        mensajesMap.clear();
        ultimoMensajeMap.clear();
        ultimaFechaEnvioMap.clear();
        ultimoMensajeIdMap.clear();

        chatsList.addAll(nuevosChats);
        notifyDataSetChanged();

        for (int i = 0; i < chatsList.size(); i++) {
            Chat chat = chatsList.get(i);
            int otroUsuarioId = (chat.getUsuario1id() == usuarioActual.getUsuarioid()) ?
                    chat.getUsuario2id() : chat.getUsuario1id();

            cargarUsuario(otroUsuarioId, i);
            cargarMensajes(chat.getChatid(), i);
        }
    }

    /**
     * Carga la información de un usuario específico desde el servidor y la almacena en caché.
     * Si el usuario ya está en caché, solo actualiza la vista correspondiente
     */
    private void cargarUsuario(int usuarioId, int position) {
        if (usuariosMap.containsKey(usuarioId)) {
            runOnUiThread(() -> {
                if (position >= 0 && position < chatsList.size()) {
                    notifyItemChanged(position);
                }
            });
            return;
        }

        ApiService.getUsuarioById(usuarioId, new ApiService.ApiCallback<Usuario>() {
            @Override
            public void onSuccess(Usuario usuario) {
                usuariosMap.put(usuarioId, usuario);
                runOnUiThread(() -> {
                    if (position >= 0 && position < chatsList.size()) {
                        notifyItemChanged(position);
                    }
                });
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    /**
     * Carga todos los mensajes de un chat específico desde el servidor, identifica el último mensaje
     * y actualiza las cachés correspondientes con su contenido, fecha e ID
     */
    private void cargarMensajes(int chatId, int position) {
        ApiService.getMensajesByChat(chatId, new ApiService.ListCallback<Mensaje>() {
            @Override
            public void onSuccess(List<Mensaje> mensajes) {
                mensajesMap.put(chatId, mensajes);

                if (mensajes != null && !mensajes.isEmpty()) {
                    Mensaje ultimoMensaje = mensajes.get(0);
                    for (Mensaje mensaje : mensajes) {
                        if (mensaje.getMensajeid() > ultimoMensaje.getMensajeid()) {
                            ultimoMensaje = mensaje;
                        }
                    }

                    ultimoMensajeMap.put(chatId, ultimoMensaje.getContenido());
                    ultimaFechaEnvioMap.put(chatId, ultimoMensaje.getFechaEnvio());
                    ultimoMensajeIdMap.put(chatId, ultimoMensaje.getMensajeid());

                    if (todosLosMensajesCargados()) {
                        runOnUiThread(() -> {
                            ordenarChatsInicial();
                            notifyDataSetChanged();
                        });
                    } else {
                        runOnUiThread(() -> {
                            if (position >= 0 && position < chatsList.size()) {
                                notifyItemChanged(position);
                            }
                        });
                    }

                } else {
                    ultimoMensajeMap.put(chatId, context.getString(R.string.iniciar_conversacion));
                    ultimaFechaEnvioMap.put(chatId, null);
                    ultimoMensajeIdMap.put(chatId, -1);

                    runOnUiThread(() -> {
                        if (position >= 0 && position < chatsList.size()) {
                            notifyItemChanged(position);
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    /**
     * Verifica si todos los mensajes de todos los chats han sido cargados completamente
     */
    private boolean todosLosMensajesCargados() {
        for (Chat chat : chatsList) {
            if (!ultimoMensajeIdMap.containsKey(chat.getChatid())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Actualiza un chat específico solo si se detecta un mensaje nuevo (con ID mayor al actual).
     * Si es un mensaje nuevo, mueve el chat a la primera posición y actualiza las cachés
     */
    public void actualizarChatSiEsNecesario(int chatId, String nuevoMensaje, String fechaEnvio, int nuevoMensajeId) {
        Integer mensajeIdActual = ultimoMensajeIdMap.get(chatId);

        if (mensajeIdActual == null || nuevoMensajeId > mensajeIdActual) {
            ultimoMensajeMap.put(chatId, nuevoMensaje);
            ultimaFechaEnvioMap.put(chatId, fechaEnvio);
            ultimoMensajeIdMap.put(chatId, nuevoMensajeId);

            int posicionActual = -1;
            Chat chatActualizado = null;

            for (int i = 0; i < chatsList.size(); i++) {
                if (chatsList.get(i).getChatid() == chatId) {
                    posicionActual = i;
                    chatActualizado = chatsList.get(i);
                    break;
                }
            }

            if (chatActualizado != null && posicionActual > 0) {
                chatsList.remove(posicionActual);
                chatsList.add(0, chatActualizado);
                notifyItemMoved(posicionActual, 0);
                notifyItemChanged(0);
            } else if (chatActualizado != null) {
                notifyItemChanged(0);
            }
        }
    }

    /**
     * Actualiza los tiempos de los mensajes en todas las vistas sin reordenar los chats
     */
    public void actualizarTiempos() {
        for (int i = 0; i < chatsList.size(); i++) {
            notifyItemChanged(i);
        }
    }

    /**
     * Método obsoleto para actualizar chat, mantenido por compatibilidad
     */
    public void actualizarChat(int chatId, String nuevoMensaje, String horaMensaje) {
    }

    /**
     * Calcula y formatea el tiempo transcurrido desde que se envió un mensaje.
     * Retorna cadenas como "Ahora", "hace X min", "hace X h", "Ayer", etc.
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
            long dias = horas / 24;

            if (dias == 0 && horas == 0 && minutos == 0) {
                return context.getString(R.string.ahora);
            } else if (dias == 0 && horas == 0) {
                return context.getString(R.string.hace_minutos, minutos);
            } else if (dias == 0) {
                return context.getString(R.string.hace_horas, horas);
            } else if (dias == 1) {
                return context.getString(R.string.ayer);
            } else if (dias < 7) {
                return context.getString(R.string.hace_dias, dias);
            } else {
                return fechaLimpia.substring(0, 10);
            }

        } catch (Exception e) {
            return "";
        }
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

    public class ChatViewHolder extends RecyclerView.ViewHolder {

        private ImageView imgProfile;
        private TextView textUserName;
        private TextView textLastMessage;
        private TextView textLastMessageTime;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);

            imgProfile = itemView.findViewById(R.id.img_profile_chat);
            textUserName = itemView.findViewById(R.id.text_chat_user_name);
            textLastMessage = itemView.findViewById(R.id.text_last_message_content);
            textLastMessageTime = itemView.findViewById(R.id.text_last_message_time);
        }

        /**
         * Vincula los datos del chat a los elementos de la vista y configura los listeners de click
         */
        public void bind(Chat chat) {
            int position = getAdapterPosition();

            int otroUsuarioId = (chat.getUsuario1id() == usuarioActual.getUsuarioid()) ?
                    chat.getUsuario2id() : chat.getUsuario1id();

            Usuario usuario = usuariosMap.get(otroUsuarioId);
            if (usuario != null) {
                mostrarDatosUsuario(usuario);
            } else {
                textUserName.setText(context.getString(R.string.cargando));
                imgProfile.setImageResource(R.drawable.ico_logo_findtogive);
            }

            String ultimoMensaje = ultimoMensajeMap.get(chat.getChatid());
            String fechaEnvio = ultimaFechaEnvioMap.get(chat.getChatid());

            if (ultimoMensaje != null) {
                textLastMessage.setText(ultimoMensaje);
                if (fechaEnvio != null) {
                    String tiempoActualizado = calcularTiempoMensaje(fechaEnvio);
                    textLastMessageTime.setText(tiempoActualizado);
                } else {
                    textLastMessageTime.setText("");
                }
            } else {
                textLastMessage.setText(context.getString(R.string.cargando_mensajes));
                textLastMessageTime.setText("");
            }

            imgProfile.setOnClickListener(v -> {
                if (usuario != null && listener != null) {
                    listener.onFotoPerfilClick(usuario);
                }
            });

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ChatC.class);
                intent.putExtra("chat_id", chat.getChatid());
                intent.putExtra("solicitud_id", chat.getSolicitudid());
                intent.putExtra("otro_usuario_id", otroUsuarioId);

                Usuario usuarioParaChat = usuariosMap.get(otroUsuarioId);
                String nombreChat = usuarioParaChat != null ?
                        usuarioParaChat.getNombre() + " " + usuarioParaChat.getApellido() : context.getString(R.string.usuario);
                intent.putExtra("chat_nombre", nombreChat);

                context.startActivity(intent);
            });
        }

        /**
         * Muestra los datos del usuario en la vista, incluyendo nombre y foto de perfil
         */
        private void mostrarDatosUsuario(Usuario usuario) {
            String nombreCompleto = usuario.getNombre() + " " + usuario.getApellido();
            textUserName.setText(nombreCompleto);

            if (usuario.getFotoUrl() != null && !usuario.getFotoUrl().isEmpty()) {
                Glide.with(context)
                        .load(usuario.getFotoUrl())
                        .placeholder(R.drawable.ico_logo_findtogive)
                        .error(R.drawable.ico_logo_findtogive)
                        .apply(RequestOptions.circleCropTransform())
                        .into(imgProfile);
            } else {
                imgProfile.setImageResource(R.drawable.ico_logo_findtogive);
            }
        }
    }
}