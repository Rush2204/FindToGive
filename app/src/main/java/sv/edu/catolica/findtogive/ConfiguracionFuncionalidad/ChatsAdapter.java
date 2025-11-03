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
    private Map<Integer, String> ultimaFechaEnvioMap; // NUEVO: Guardar fecha original
    private Map<Integer, Integer> ultimoMensajeIdMap;
    private Usuario usuarioActual;

    public ChatsAdapter(List<Chat> chatsList, Context context) {
        this.chatsList = chatsList;
        this.context = context;
        this.usuariosMap = new HashMap<>();
        this.mensajesMap = new HashMap<>();
        this.ultimoMensajeMap = new HashMap<>();
        this.ultimaFechaEnvioMap = new HashMap<>(); // NUEVO
        this.ultimoMensajeIdMap = new HashMap<>();
        this.usuarioActual = SharedPreferencesManager.getCurrentUser(context);
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.desing_item_chat_list, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chatsList.get(position);
        holder.bind(chat);
    }

    @Override
    public int getItemCount() {
        return chatsList.size();
    }

    // NUEVO MÉTODO: Ordenar chats por ID del último mensaje (solo una vez)
    private void ordenarChatsInicial() {
        System.out.println("🔄 Aplicando orden inicial por ID de mensaje");

        List<Chat> chatsConMensajes = new ArrayList<>();
        List<Chat> chatsSinMensajes = new ArrayList<>();

        // Separar chats con y sin mensajes
        for (Chat chat : chatsList) {
            Integer ultimoMensajeId = ultimoMensajeIdMap.get(chat.getChatid());
            if (ultimoMensajeId != null && ultimoMensajeId > 0) {
                chatsConMensajes.add(chat);
            } else {
                chatsSinMensajes.add(chat);
            }
        }

        // Ordenar chats con mensajes por ID descendente
        Collections.sort(chatsConMensajes, new Comparator<Chat>() {
            @Override
            public int compare(Chat chat1, Chat chat2) {
                Integer id1 = ultimoMensajeIdMap.get(chat1.getChatid());
                Integer id2 = ultimoMensajeIdMap.get(chat2.getChatid());
                return id2.compareTo(id1); // Orden descendente
            }
        });

        // Combinar listas
        chatsList.clear();
        chatsList.addAll(chatsConMensajes);
        chatsList.addAll(chatsSinMensajes);

        System.out.println("✅ Orden inicial aplicado - " + chatsConMensajes.size() + " con mensajes, " + chatsSinMensajes.size() + " sin mensajes");
    }

    // Método para actualizar la lista completa de chats
    public void actualizarChats(List<Chat> nuevosChats) {
        System.out.println("💥 ACTUALIZACIÓN DE CHATS: " + nuevosChats.size() + " chats");

        // Limpiar TODO
        chatsList.clear();
        usuariosMap.clear();
        mensajesMap.clear();
        ultimoMensajeMap.clear();
        ultimaFechaEnvioMap.clear();
        ultimoMensajeIdMap.clear();

        // Agregar nuevos chats
        chatsList.addAll(nuevosChats);

        // Notificar cambio
        notifyDataSetChanged();

        System.out.println("✅ Lista de chats actualizada - " + chatsList.size() + " chats");

        // Cargar usuarios y mensajes
        for (int i = 0; i < chatsList.size(); i++) {
            Chat chat = chatsList.get(i);
            int otroUsuarioId = (chat.getUsuario1id() == usuarioActual.getUsuarioid()) ?
                    chat.getUsuario2id() : chat.getUsuario1id();

            cargarUsuario(otroUsuarioId, i);
            cargarMensajes(chat.getChatid(), i);
        }
    }

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
                System.out.println("❌ Error cargando usuario " + usuarioId);
            }
        });
    }

    private void cargarMensajes(int chatId, int position) {
        ApiService.getMensajesByChat(chatId, new ApiService.ListCallback<Mensaje>() {
            @Override
            public void onSuccess(List<Mensaje> mensajes) {
                mensajesMap.put(chatId, mensajes);

                if (mensajes != null && !mensajes.isEmpty()) {
                    // Encontrar el mensaje con ID más alto
                    Mensaje ultimoMensaje = mensajes.get(0);
                    for (Mensaje mensaje : mensajes) {
                        if (mensaje.getMensajeid() > ultimoMensaje.getMensajeid()) {
                            ultimoMensaje = mensaje;
                        }
                    }

                    ultimoMensajeMap.put(chatId, ultimoMensaje.getContenido());
                    ultimaFechaEnvioMap.put(chatId, ultimoMensaje.getFechaEnvio()); // Guardar fecha original
                    ultimoMensajeIdMap.put(chatId, ultimoMensaje.getMensajeid());

                    System.out.println("✅ Mensajes cargados para chat " + chatId + " - ID: " + ultimoMensaje.getMensajeid());

                    // Verificar si ya cargamos todos los mensajes para aplicar orden inicial
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
                    // No hay mensajes
                    ultimoMensajeMap.put(chatId, "Iniciar conversación");
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
                System.out.println("❌ Error cargando mensajes para chat " + chatId);
            }
        });
    }

    // NUEVO MÉTODO: Verificar si todos los mensajes están cargados
    private boolean todosLosMensajesCargados() {
        for (Chat chat : chatsList) {
            if (!ultimoMensajeIdMap.containsKey(chat.getChatid())) {
                return false;
            }
        }
        return true;
    }

    // MÉTODO CLAVE: Solo actualizar si hay un mensaje nuevo
    public void actualizarChatSiEsNecesario(int chatId, String nuevoMensaje, String fechaEnvio, int nuevoMensajeId) {
        Integer mensajeIdActual = ultimoMensajeIdMap.get(chatId);

        // SOLO actualizar si el ID del mensaje es mayor (mensaje nuevo)
        if (mensajeIdActual == null || nuevoMensajeId > mensajeIdActual) {
            System.out.println("💬 MENSAJE NUEVO detectado - Chat: " + chatId +
                    " - ID anterior: " + mensajeIdActual + ", ID nuevo: " + nuevoMensajeId);

            // Actualizar cache
            ultimoMensajeMap.put(chatId, nuevoMensaje);
            ultimaFechaEnvioMap.put(chatId, fechaEnvio);
            ultimoMensajeIdMap.put(chatId, nuevoMensajeId);

            // Mover el chat a la posición 0 solo si hay un mensaje nuevo
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
                System.out.println("🔼 Chat movido a posición 0 por mensaje nuevo (ID: " + nuevoMensajeId + ")");
            } else if (chatActualizado != null) {
                notifyItemChanged(0);
                System.out.println("✅ Chat actualizado en posición 0");
            }
        } else {
            // No hay mensaje nuevo, solo actualizamos el tiempo si es necesario
            System.out.println("⏭️ Chat " + chatId + " sin mensajes nuevos (ID actual: " + mensajeIdActual + ")");
        }
    }

    // NUEVO MÉTODO: Actualizar solo los tiempos sin reordenar
    public void actualizarTiempos() {
        // Solo actualizar las vistas que muestran tiempo, sin cambiar el orden
        for (int i = 0; i < chatsList.size(); i++) {
            notifyItemChanged(i);
        }
    }

    // Método para compatibilidad
    public void actualizarChat(int chatId, String nuevoMensaje, String horaMensaje) {
        // No hacer nada aquí, usar actualizarChatSiEsNecesario en su lugar
        System.out.println("⚠️ actualizarChat obsoleto llamado, usar actualizarChatSiEsNecesario");
    }

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
                return "Ahora";
            } else if (dias == 0 && horas == 0) {
                return "hace " + minutos + " min";
            } else if (dias == 0) {
                return "hace " + horas + " h";
            } else if (dias == 1) {
                return "Ayer";
            } else if (dias < 7) {
                return "hace " + dias + " d";
            } else {
                return fechaLimpia.substring(0, 10);
            }

        } catch (Exception e) {
            return "";
        }
    }

    // Agrega este método helper para ejecutar en UI thread
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

        public void bind(Chat chat) {
            int position = getAdapterPosition();

            // Determinar el ID del otro usuario
            int otroUsuarioId = (chat.getUsuario1id() == usuarioActual.getUsuarioid()) ?
                    chat.getUsuario2id() : chat.getUsuario1id();

            // MOSTRAR DATOS
            Usuario usuario = usuariosMap.get(otroUsuarioId);
            if (usuario != null) {
                mostrarDatosUsuario(usuario);
            } else {
                textUserName.setText("Cargando...");
                imgProfile.setImageResource(R.drawable.ico_logo_findtogive);
            }

            // MOSTRAR MENSAJE Y TIEMPO (calculado en tiempo real)
            String ultimoMensaje = ultimoMensajeMap.get(chat.getChatid());
            String fechaEnvio = ultimaFechaEnvioMap.get(chat.getChatid());

            if (ultimoMensaje != null) {
                textLastMessage.setText(ultimoMensaje);
                // Calcular el tiempo en tiempo real cada vez que se renderiza
                if (fechaEnvio != null) {
                    String tiempoActualizado = calcularTiempoMensaje(fechaEnvio);
                    textLastMessageTime.setText(tiempoActualizado);
                } else {
                    textLastMessageTime.setText("");
                }
            } else {
                textLastMessage.setText("Cargando mensajes...");
                textLastMessageTime.setText("");
            }

            itemView.setOnClickListener(v -> {
                System.out.println("👆 Click en chat " + chat.getChatid());
                Intent intent = new Intent(context, ChatC.class);
                intent.putExtra("chat_id", chat.getChatid());
                intent.putExtra("solicitud_id", chat.getSolicitudid());
                intent.putExtra("otro_usuario_id", otroUsuarioId);

                Usuario usuarioParaChat = usuariosMap.get(otroUsuarioId);
                String nombreChat = usuarioParaChat != null ?
                        usuarioParaChat.getNombre() + " " + usuarioParaChat.getApellido() : "Usuario";
                intent.putExtra("chat_nombre", nombreChat);

                context.startActivity(intent);
            });
        }

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