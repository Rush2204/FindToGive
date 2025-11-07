package sv.edu.catolica.findtogive.ClasesDiseño;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.ApiService;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.ChatsAdapter;
import sv.edu.catolica.findtogive.Modelado.Chat;
import sv.edu.catolica.findtogive.Modelado.Mensaje;
import sv.edu.catolica.findtogive.Modelado.Notificacion;
import sv.edu.catolica.findtogive.Modelado.Usuario;
import sv.edu.catolica.findtogive.R;
import sv.edu.catolica.findtogive.ConfiguracionFuncionalidad.SharedPreferencesManager;

public class Mensajeria extends AppCompatActivity {

    private RecyclerView recyclerViewChats;
    private LinearLayout layoutEmptyState;
    private ChatsAdapter chatsAdapter;
    private List<Chat> chatsList;
    private Usuario usuarioActual;

    private Handler autoRefreshHandler;
    private Handler messagesPollingHandler;
    private Runnable autoRefreshRunnable;
    private Runnable messagesPollingRunnable;
    private static final long AUTO_REFRESH_INTERVAL = 100;
    private static final long MESSAGES_POLLING_INTERVAL = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.desing_mensajeria);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        usuarioActual = SharedPreferencesManager.getCurrentUser(this);
        initializeViews();
        setupRecyclerView();
        setupBottomNavigation();

        // Cargar datos inmediatamente
        loadChats();

        // Iniciar actualizaciones automáticas
        startTimeRefresh();
        startMessagesPolling();
    }

    private void initializeViews() {
        recyclerViewChats = findViewById(R.id.recycler_view_chats_list);
        layoutEmptyState = findViewById(R.id.layout_empty_state_chats);
    }

    private void setupRecyclerView() {
        chatsList = new ArrayList<>();
        chatsAdapter = new ChatsAdapter(chatsList, this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewChats.setLayoutManager(layoutManager);
        recyclerViewChats.setAdapter(chatsAdapter);
        recyclerViewChats.setHasFixedSize(false);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation_bar);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_inicio) {
                Intent intent = new Intent(this, FeedDonacion.class);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_crear) {
                if (usuarioActual != null && (usuarioActual.getRolid() == 2 || usuarioActual.getRolid() == 3)) {
                    Intent intent = new Intent(this, SolicitudDonacionC.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Solo receptores pueden crear solicitudes", Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (itemId == R.id.nav_notificaciones) {
                startActivity(new Intent(this, Notificaciones.class));
                finish();
                Toast.makeText(this, "Avisos y notificaciones", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_historial) {
                Intent intent = new Intent(this, HistorialDonaciones.class);
                startActivity(intent);
                Toast.makeText(this, "Historial de donaciones", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_perfil) {
                Intent intent = new Intent(this, PerfilUsuario.class);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_mensajeria) {
                return true;
            }
            return false;
        });

        if (usuarioActual != null && usuarioActual.getRolid() == 1) {
            bottomNavigation.getMenu().findItem(R.id.nav_crear).setVisible(false);
        }

        bottomNavigation.setSelectedItemId(R.id.nav_mensajeria);
    }

    private void loadChats() {
        if (usuarioActual == null) {
            System.out.println("❌ Usuario actual es null");
            return;
        }

        System.out.println("🔄 Cargando chats para usuario: " + usuarioActual.getUsuarioid());

        ApiService.getChatsByUsuario(usuarioActual.getUsuarioid(), new ApiService.ListCallback<Chat>() {
            @Override
            public void onSuccess(List<Chat> chats) {
                runOnUiThread(() -> {
                    if (chats != null && !chats.isEmpty()) {
                        System.out.println("✅ " + chats.size() + " chats cargados exitosamente");
                        chatsAdapter.actualizarChats(chats);
                        showChatsList();
                    } else {
                        System.out.println("ℹ️ No hay chats disponibles");
                        showEmptyState();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    System.out.println("❌ Error cargando chats: " + error);
                    showEmptyState();
                });
            }
        });
    }

    // NUEVO MÉTODO: Actualizar solo los mensajes de los chats existentes
    private void refreshMessagesForAllChats() {
        if (chatsList == null || chatsList.isEmpty()) {
            return;
        }

        System.out.println("🔄 Verificando mensajes nuevos para " + chatsList.size() + " chats");

        for (Chat chat : chatsList) {
            refreshMessagesForChat(chat.getChatid());
        }
    }

    // MÉTODO MODIFICADO: Solo actualiza si hay mensajes nuevos
    private void refreshMessagesForChat(int chatId) {
        ApiService.getMensajesByChat(chatId, new ApiService.ListCallback<Mensaje>() {
            @Override
            public void onSuccess(List<Mensaje> mensajes) {
                runOnUiThread(() -> {
                    if (mensajes != null && !mensajes.isEmpty()) {
                        // Encontrar el mensaje con ID más alto (más reciente)
                        Mensaje ultimoMensaje = mensajes.get(0);
                        for (Mensaje mensaje : mensajes) {
                            if (mensaje.getMensajeid() > ultimoMensaje.getMensajeid()) {
                                ultimoMensaje = mensaje;
                            }
                        }

                        String contenido = ultimoMensaje.getContenido();
                        String fechaEnvio = ultimoMensaje.getFechaEnvio();
                        int ultimoMensajeId = ultimoMensaje.getMensajeid();

                        // Solo actualizar si hay un mensaje nuevo
                        chatsAdapter.actualizarChatSiEsNecesario(chatId, contenido, fechaEnvio, ultimoMensajeId);
                    }
                });
            }

            @Override
            public void onError(String error) {
                System.out.println("❌ Error actualizando mensajes para chat " + chatId + ": " + error);
            }
        });
    }

    // NUEVO MÉTODO: Solo actualizar tiempos sin reordenar
    private void startTimeRefresh() {
        autoRefreshHandler = new Handler();
        autoRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                // Solo actualizar los tiempos, sin reordenar
                if (chatsAdapter != null) {
                    chatsAdapter.actualizarTiempos();
                }
                autoRefreshHandler.postDelayed(this, AUTO_REFRESH_INTERVAL);
            }
        };
        autoRefreshHandler.post(autoRefreshRunnable);
    }

    // NUEVO MÉTODO: Polling para mensajes nuevos
    private void startMessagesPolling() {
        messagesPollingHandler = new Handler();
        messagesPollingRunnable = new Runnable() {
            @Override
            public void run() {
                System.out.println("📨 POLLING: Buscando mensajes nuevos");
                refreshMessagesForAllChats();
                messagesPollingHandler.postDelayed(this, MESSAGES_POLLING_INTERVAL);
            }
        };
        messagesPollingHandler.postDelayed(messagesPollingRunnable, MESSAGES_POLLING_INTERVAL);
    }

    private void stopAllHandlers() {
        if (autoRefreshHandler != null && autoRefreshRunnable != null) {
            autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        }
        if (messagesPollingHandler != null && messagesPollingRunnable != null) {
            messagesPollingHandler.removeCallbacks(messagesPollingRunnable);
        }
        System.out.println("⏹️ Todos los handlers detenidos");
    }

    private void showChatsList() {
        recyclerViewChats.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        recyclerViewChats.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        System.out.println("🔄 Mensajeria onResume");

        // Recargar chats inmediatamente
        loadChats();

        // Reactivar actualizaciones automáticas
        startTimeRefresh();
        startMessagesPolling();

        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation_bar);
        bottomNavigation.setSelectedItemId(R.id.nav_mensajeria);
    }

    @Override
    protected void onPause() {
        super.onPause();
        System.out.println("⏸️ Mensajeria onPause");
        stopAllHandlers();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.out.println("🗑️ Mensajeria onDestroy");
        stopAllHandlers();
    }

    @Override
    protected void onStop() {
        super.onStop();
        System.out.println("🛑 Mensajeria onStop");
        stopAllHandlers();
    }
}