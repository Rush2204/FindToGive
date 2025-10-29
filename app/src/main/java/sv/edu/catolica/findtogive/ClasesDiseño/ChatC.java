package sv.edu.catolica.findtogive.ClasesDiseño;

import android.os.Bundle;
import android.os.Handler;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.ApiService;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.MensajesAdapter;
import sv.edu.catolica.findtogive.Modelado.Mensaje;
import sv.edu.catolica.findtogive.Modelado.Usuario;
import sv.edu.catolica.findtogive.R;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.SharedPreferencesManager;

public class ChatC extends AppCompatActivity {

    private TextView textChatName;
    private ImageView btnBack;
    private RecyclerView recyclerViewChat;
    private EditText editTextMessage;
    private ImageButton btnSend;

    private int chatId;
    private int solicitudId;
    private int otroUsuarioId;
    private String chatNombre;

    private List<Mensaje> mensajesList;
    private MensajesAdapter mensajesAdapter;
    private Usuario usuarioActual;

    private Handler pollingHandler;
    private Runnable pollingRunnable;
    private static final long POLLING_INTERVAL = 3000; // 3 segundos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.desing_chat);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupRecyclerView();
        loadMensajes();
        startPolling();
    }

    private void initializeViews() {
        textChatName = findViewById(R.id.text_chat_name);
        btnBack = findViewById(R.id.btn_back);
        recyclerViewChat = findViewById(R.id.recycler_view_chat);
        editTextMessage = findViewById(R.id.edit_text_message);
        btnSend = findViewById(R.id.btn_send);

        // Obtener datos del intent
        chatId = getIntent().getIntExtra("chat_id", -1);
        solicitudId = getIntent().getIntExtra("solicitud_id", -1);
        otroUsuarioId = getIntent().getIntExtra("otro_usuario_id", -1);
        chatNombre = getIntent().getStringExtra("chat_nombre");

        usuarioActual = SharedPreferencesManager.getCurrentUser(this);

        textChatName.setText(chatNombre != null ? chatNombre : "Chat");

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> enviarMensaje());
    }

    private void setupRecyclerView() {
        mensajesList = new ArrayList<>();
        // Pasar el otroUsuarioId al adapter
        mensajesAdapter = new MensajesAdapter(mensajesList, usuarioActual.getUsuarioid(), otroUsuarioId);
        recyclerViewChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChat.setAdapter(mensajesAdapter);

        // Precargar la información del otro usuario ANTES de cargar los mensajes
        precargarOtroUsuario();

        mensajesAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                recyclerViewChat.smoothScrollToPosition(mensajesAdapter.getItemCount() - 1);
            }
        });
    }

    private void precargarOtroUsuario() {
        if (otroUsuarioId == -1) return;

        ApiService.getUsuarioById(otroUsuarioId, new ApiService.ApiCallback<Usuario>() {
            @Override
            public void onSuccess(Usuario usuario) {
                // Precargar el usuario en el adapter
                mensajesAdapter.precargarUsuario(usuario);
                // Actualizar el título del chat con el nombre real
                if (usuario != null) {
                    String nombreCompleto = usuario.getNombre() + " " + usuario.getApellido();
                    textChatName.setText(nombreCompleto);
                }
                System.out.println("✅ Usuario precargado en chat: " + usuario.getNombre());
            }

            @Override
            public void onError(String error) {
                System.out.println("❌ Error precargando usuario: " + error);
            }
        });
    }

    private void loadMensajes() {
        if (chatId == -1) {
            Toast.makeText(this, "Error: ID de chat inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService.getMensajesByChat(chatId, new ApiService.ListCallback<Mensaje>() {
            @Override
            public void onSuccess(List<Mensaje> mensajes) {
                runOnUiThread(() -> {
                    if (mensajes != null) {
                        // Verificar si hay mensajes nuevos
                        if (hayMensajesNuevos(mensajes)) {
                            mensajesAdapter.actualizarMensajes(mensajes);
                            if (!mensajes.isEmpty()) {
                                recyclerViewChat.scrollToPosition(mensajes.size() - 1);
                            }
                            System.out.println("🔄 Mensajes actualizados: " + mensajes.size());
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    System.out.println("❌ Error cargando mensajes: " + error);
                    // No mostrar Toast para no molestar al usuario
                });
            }
        });
    }

    private boolean hayMensajesNuevos(List<Mensaje> nuevosMensajes) {
        if (mensajesList.size() != nuevosMensajes.size()) {
            return true;
        }

        // Verificar si el último mensaje es diferente
        if (!mensajesList.isEmpty() && !nuevosMensajes.isEmpty()) {
            Mensaje ultimoActual = mensajesList.get(mensajesList.size() - 1);
            Mensaje ultimoNuevo = nuevosMensajes.get(nuevosMensajes.size() - 1);

            return ultimoActual.getMensajeid() != ultimoNuevo.getMensajeid();
        }

        return !mensajesList.isEmpty() || !nuevosMensajes.isEmpty();
    }

    private void startPolling() {
        pollingHandler = new Handler();
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                loadMensajes();
                pollingHandler.postDelayed(this, POLLING_INTERVAL);
            }
        };
        pollingHandler.postDelayed(pollingRunnable, POLLING_INTERVAL);
    }

    private void stopPolling() {
        if (pollingHandler != null && pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
        }
    }

    private void enviarMensaje() {
        String contenido = editTextMessage.getText().toString().trim();
        if (contenido.isEmpty()) {
            Toast.makeText(this, "Escribe un mensaje", Toast.LENGTH_SHORT).show();
            return;
        }

        if (chatId == -1) {
            Toast.makeText(this, "Error: No se puede enviar el mensaje", Toast.LENGTH_SHORT).show();
            return;
        }

        Mensaje nuevoMensaje = new Mensaje(chatId, usuarioActual.getUsuarioid(), contenido);

        ApiService.createMensaje(nuevoMensaje, new ApiService.ApiCallback<Mensaje>() {
            @Override
            public void onSuccess(Mensaje mensaje) {
                runOnUiThread(() -> {
                    editTextMessage.setText("");
                    // Agregar el mensaje localmente para mejor UX
                    mensajesAdapter.agregarMensaje(mensaje);
                    recyclerViewChat.smoothScrollToPosition(mensajesAdapter.getItemCount() - 1);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(ChatC.this, "Error enviando mensaje: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPolling();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPolling();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startPolling();
        // Recargar mensajes inmediatamente al volver
        loadMensajes();
    }
}