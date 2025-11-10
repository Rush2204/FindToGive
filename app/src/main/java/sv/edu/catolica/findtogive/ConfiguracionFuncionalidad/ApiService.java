package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sv.edu.catolica.findtogive.Modelado.Chat;
import sv.edu.catolica.findtogive.Modelado.HistorialDonacion;
import sv.edu.catolica.findtogive.Modelado.Mensaje;
import sv.edu.catolica.findtogive.Modelado.Notificacion;
import sv.edu.catolica.findtogive.Modelado.SolicitudDonacion;
import sv.edu.catolica.findtogive.Modelado.Usuario;

public class ApiService {
    private static final Gson gson = new Gson();

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public interface ListCallback<T> {
        void onSuccess(List<T> result);
        void onError(String error);
    }

    // ========== USUARIO - LOGIN MODIFICADO ==========
    public static void loginUser(String email, String password, ApiCallback<Usuario> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                // Primero obtener el usuario por email
                String url = SupabaseClient.URLs.usuario() + "?email=eq." + email + "&activo=eq.true&limit=1";

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", SupabaseClient.Headers.getAuthHeader())
                        .addHeader("apikey", SupabaseClient.Headers.getApiKeyHeader())
                        .build();

                try (Response response = SupabaseClient.getHttpClient().newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String json = response.body().string();
                        System.out.println("🔍 Response login: " + json);

                        Type listType = new TypeToken<List<Usuario>>(){}.getType();
                        List<Usuario> usuarios = gson.fromJson(json, listType);

                        if (usuarios != null && !usuarios.isEmpty()) {
                            Usuario usuario = usuarios.get(0);
                            String storedHash = usuario.getContrasena();

                            // Verificar la contraseña con BCrypt
                            if (SecurityHelper.verifyPassword(password, storedHash)) {
                                // Contraseña correcta
                                callback.onSuccess(usuario);
                            } else {
                                // Contraseña incorrecta
                                callback.onError("Credenciales inválidas");
                            }
                        } else {
                            callback.onError("Usuario no encontrado");
                        }
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                        System.out.println("❌ Error en login: " + response.code() + " - " + errorBody);
                        callback.onError("Error: " + response.code());
                    }
                }

            } catch (Exception e) {
                System.out.println("❌ Exception en loginUser: " + e.getMessage());
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    // ========== USUARIO - REGISTRO MODIFICADO ==========
    public static void registerUser(Usuario usuario, ApiCallback<Usuario> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                // Hashear la contraseña antes de enviar
                String hashedPassword = SecurityHelper.hashPassword(usuario.getContrasena());

                // Crear JSON manualmente excluyendo usuarioid
                String json = "{" +
                        "\"nombre\":\"" + usuario.getNombre() + "\"," +
                        "\"apellido\":\"" + usuario.getApellido() + "\"," +
                        "\"email\":\"" + usuario.getEmail() + "\"," +
                        "\"contrasena\":\"" + hashedPassword + "\"," + // ← Usar contraseña hasheada
                        "\"edad\":" + usuario.getEdad() + "," +
                        "\"telefono\":\"" + usuario.getTelefono() + "\"," +
                        "\"rolid\":" + usuario.getRolid() + "," +
                        "\"tiposangreid\":" + usuario.getTiposangreid() + "," +
                        "\"activo\":true" +
                        "}";

                System.out.println("📤 JSON a enviar: " + json);

                RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

                Request request = new Request.Builder()
                        .url(SupabaseClient.URLs.usuario())
                        .post(body)
                        .addHeader("Authorization", SupabaseClient.Headers.getAuthHeader())
                        .addHeader("apikey", SupabaseClient.Headers.getApiKeyHeader())
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=minimal")
                        .build();

                // Ejecutar INSERT
                try (Response response = SupabaseClient.getHttpClient().newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        System.out.println("✅ INSERT exitoso, código: " + response.code());

                        // Pequeño delay para asegurar consistencia
                        Thread.sleep(1000);

                        // Buscar el usuario recién creado por email
                        buscarUsuarioRecienCreado(usuario.getEmail(), callback);

                    } else {
                        String errorBody = response.body() != null ?
                                response.body().string() : "Sin detalles";
                        System.out.println("❌ Error en INSERT: " + response.code() + " - " + errorBody);

                        if (response.code() == 409) {
                            callback.onError("El email ya está registrado");
                        } else if (response.code() == 400) {
                            callback.onError("Error en los datos enviados: " + errorBody);
                        } else {
                            callback.onError("Error en registro: " + response.code());
                        }
                    }
                }

            } catch (Exception e) {
                System.out.println("❌ Exception en registerUser: " + e.getMessage());
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    // ========== MÉTODO AUXILIAR (sin cambios) ==========
    private static void buscarUsuarioRecienCreado(String email, ApiCallback<Usuario> callback) {
        try {
            // Pequeño delay para asegurar que el INSERT se complete
            Thread.sleep(500);

            String url = SupabaseClient.URLs.usuario() + "?email=eq." + email + "&limit=1";

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", SupabaseClient.Headers.getAuthHeader())
                    .addHeader("apikey", SupabaseClient.Headers.getApiKeyHeader())
                    .build();

            try (Response response = SupabaseClient.getHttpClient().newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    System.out.println("🔍 Response buscar usuario: " + json);

                    Type listType = new TypeToken<List<Usuario>>(){}.getType();
                    List<Usuario> usuarios = gson.fromJson(json, listType);

                    if (usuarios != null && !usuarios.isEmpty()) {
                        Usuario usuario = usuarios.get(0);
                        System.out.println("✅ Usuario encontrado - ID: " + usuario.getUsuarioid());
                        callback.onSuccess(usuario);
                    } else {
                        callback.onError("Usuario creado pero no encontrado en la base de datos");
                    }
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                    System.out.println("❌ Error al buscar usuario: " + response.code() + " - " + errorBody);
                    callback.onError("Error al obtener datos del usuario: " + response.code());
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Exception en buscarUsuarioRecienCreado: " + e.getMessage());
            callback.onError("Error: " + e.getMessage());
        }
    }

    // ========== EL RESTO DE LOS MÉTODOS PERMANECEN IGUAL ==========

    // ========== SOLICITUDES - ACTUALIZACIÓN ==========
    public static void updateSolicitud(SolicitudDonacion solicitud, ApiCallback<SolicitudDonacion> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                // Crear JSON solo con los campos que queremos actualizar
                String json = "{" +
                        "\"imagen_url\":\"" + (solicitud.getImagenUrl() != null ? solicitud.getImagenUrl() : "") + "\"" +
                        "}";

                System.out.println("📤 JSON Update Solicitud: " + json);

                RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

                String url = SupabaseClient.URLs.solicitudDonacion() + "?solicitudid=eq." + solicitud.getSolicitudid();

                Request request = new Request.Builder()
                        .url(url)
                        .patch(body)
                        .addHeader("Authorization", SupabaseClient.Headers.getAuthHeader())
                        .addHeader("apikey", SupabaseClient.Headers.getApiKeyHeader())
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=representation")
                        .build();

                try (Response response = SupabaseClient.getHttpClient().newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        System.out.println("✅ UPDATE solicitud exitoso: " + responseBody);

                        Type listType = new TypeToken<List<SolicitudDonacion>>(){}.getType();
                        List<SolicitudDonacion> solicitudesActualizadas = gson.fromJson(responseBody, listType);

                        if (solicitudesActualizadas != null && !solicitudesActualizadas.isEmpty()) {
                            callback.onSuccess(solicitudesActualizadas.get(0));
                        } else {
                            callback.onSuccess(solicitud); // Retornar la solicitud original si no hay respuesta
                        }
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                        System.out.println("❌ Error en UPDATE solicitud: " + response.code() + " - " + errorBody);
                        callback.onError("Error al actualizar solicitud: " + response.code());
                    }
                }

            } catch (Exception e) {
                System.out.println("❌ Exception en updateSolicitud: " + e.getMessage());
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    // ========== SOLICITUD POR ID ==========
    public static void getSolicitudById(int solicitudId, ApiCallback<SolicitudDonacion> callback) {
        String url = SupabaseClient.URLs.solicitudDonacion() + "?solicitudid=eq." + solicitudId + "&limit=1";
        getSingle(url, new TypeToken<List<SolicitudDonacion>>(){}.getType(), callback);
    }

    // ========== USUARIOS - MÉTODOS ADICIONALES ==========
    public static void getUsuarioById(int usuarioId, ApiCallback<Usuario> callback) {
        String url = SupabaseClient.URLs.usuario() + "?usuarioid=eq." + usuarioId + "&limit=1";
        getSingle(url, new TypeToken<List<Usuario>>(){}.getType(), callback);
    }

    public static void getUsuariosByIds(List<Integer> usuarioIds, ListCallback<Usuario> callback) {
        if (usuarioIds == null || usuarioIds.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        // Crear filtro para múltiples IDs: usuarioid=in.(1,2,3)
        String idsParam = usuarioIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        String url = SupabaseClient.URLs.usuario() + "?usuarioid=in.(" + idsParam + ")";
        getList(url, new TypeToken<List<Usuario>>(){}.getType(), callback);
    }

    public static void updateUser(Usuario usuario, ApiCallback<Usuario> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                // Usar el nuevo método toJsonForUpdate
                String json = usuario.toJsonForUpdate();
                System.out.println("📤 JSON Update Usuario: " + json);

                RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

                String url = SupabaseClient.URLs.usuario() + "?usuarioid=eq." + usuario.getUsuarioid();

                Request request = new Request.Builder()
                        .url(url)
                        .patch(body)
                        .addHeader("Authorization", SupabaseClient.Headers.getAuthHeader())
                        .addHeader("apikey", SupabaseClient.Headers.getApiKeyHeader())
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=representation")
                        .build();

                try (Response response = SupabaseClient.getHttpClient().newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        System.out.println("✅ UPDATE usuario exitoso: " + responseBody);

                        Type listType = new TypeToken<List<Usuario>>(){}.getType();
                        List<Usuario> usuariosActualizados = gson.fromJson(responseBody, listType);

                        if (usuariosActualizados != null && !usuariosActualizados.isEmpty()) {
                            callback.onSuccess(usuariosActualizados.get(0));
                        } else {
                            callback.onSuccess(usuario); // Retornar el usuario original si no hay respuesta
                        }
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                        System.out.println("❌ Error en UPDATE usuario: " + response.code() + " - " + errorBody);
                        callback.onError("Error al actualizar usuario: " + response.code());
                    }
                }

            } catch (Exception e) {
                System.out.println("❌ Exception en updateUser: " + e.getMessage());
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    // ========== SOLICITUDES ==========
    public static void getSolicitudesActivas(ListCallback<SolicitudDonacion> callback) {
        String url = SupabaseClient.URLs.solicitudDonacion() + "?estado=eq.activa&order=fecha_publicacion.desc";
        getList(url, new TypeToken<List<SolicitudDonacion>>(){}.getType(), callback);
    }

    // ========== SOLICITUDES ==========
    public static void createSolicitud(SolicitudDonacion solicitud, ApiCallback<SolicitudDonacion> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                // Obtener fecha actual en formato correcto
                java.time.format.DateTimeFormatter formatter =
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
                String fechaActual = java.time.LocalDateTime.now().format(formatter);

                // Crear JSON incluyendo la fecha
                String json = "{" +
                        "\"usuarioid\":" + solicitud.getUsuarioid() + "," +
                        "\"titulo\":\"" + solicitud.getTitulo() + "\"," +
                        "\"descripcion\":\"" + solicitud.getDescripcion() + "\"," +
                        "\"tiposangreid\":" + solicitud.getTiposangreid() + "," +
                        "\"ubicacion\":\"" + solicitud.getUbicacion() + "\"," +
                        "\"fecha_publicacion\":\"" + fechaActual + "\"," + // ← Fecha desde app
                        "\"estado\":\"activa\"" +
                        "}";

                System.out.println("📤 JSON Solicitud: " + json);

                RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

                Request request = new Request.Builder()
                        .url(SupabaseClient.URLs.solicitudDonacion())
                        .post(body)
                        .addHeader("Authorization", SupabaseClient.Headers.getAuthHeader())
                        .addHeader("apikey", SupabaseClient.Headers.getApiKeyHeader())
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=representation")
                        .build();

                // Ejecutar INSERT
                try (Response response = SupabaseClient.getHttpClient().newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        System.out.println("✅ INSERT solicitud exitoso: " + responseBody);

                        // Parsear la respuesta para obtener la solicitud creada con ID
                        Type listType = new TypeToken<List<SolicitudDonacion>>(){}.getType();
                        List<SolicitudDonacion> solicitudesCreadas = gson.fromJson(responseBody, listType);

                        if (solicitudesCreadas != null && !solicitudesCreadas.isEmpty()) {
                            SolicitudDonacion solicitudCreada = solicitudesCreadas.get(0);
                            System.out.println("✅ Solicitud creada con ID: " + solicitudCreada.getSolicitudid());
                            callback.onSuccess(solicitudCreada);
                        } else {
                            // Si no retorna datos, buscar la última solicitud del usuario
                            buscarSolicitudRecienCreada(solicitud.getUsuarioid(), callback);
                        }
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                        System.out.println("❌ Error en INSERT solicitud: " + response.code() + " - " + errorBody);
                        callback.onError("Error al crear solicitud: " + response.code());
                    }
                }

            } catch (Exception e) {
                System.out.println("❌ Exception en createSolicitud: " + e.getMessage());
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    // ========== MÉTODOS DE BÚSQUEDA Y FILTRADO ==========
    public static void buscarSolicitudes(String query, Integer tipoSangreId, ListCallback<SolicitudDonacion> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                // Construir URL base
                StringBuilder urlBuilder = new StringBuilder(SupabaseClient.URLs.solicitudDonacion());
                urlBuilder.append("?estado=eq.activa");

                // Agregar filtros
                List<String> filters = new ArrayList<>();

                if (query != null && !query.trim().isEmpty()) {
                    // Usar ilike para búsqueda case-insensitive
                    filters.add("titulo=ilike.%25" + query.trim() + "%25");
                }

                if (tipoSangreId != null && tipoSangreId > 0) {
                    filters.add("tiposangreid=eq." + tipoSangreId);
                }

                // Combinar filtros
                if (!filters.isEmpty()) {
                    urlBuilder.append("&").append(String.join("&", filters));
                }

                // Ordenar por fecha de publicación
                urlBuilder.append("&order=fecha_publicacion.desc");

                String finalUrl = urlBuilder.toString();
                System.out.println("🔍 URL de búsqueda: " + finalUrl);

                getList(finalUrl, new TypeToken<List<SolicitudDonacion>>(){}.getType(), callback);

            } catch (Exception e) {
                System.out.println("❌ Error en buscarSolicitudes: " + e.getMessage());
                callback.onError("Error en búsqueda: " + e.getMessage());
            }
        });
    }

    // Método para obtener todas las solicitudes sin filtros (reset)
    public static void getSolicitudesSinFiltros(ListCallback<SolicitudDonacion> callback) {
        getSolicitudesActivas(callback);
    }

    private static void buscarSolicitudRecienCreada(int usuarioId, ApiCallback<SolicitudDonacion> callback) {
        try {
            // Pequeño delay para asegurar consistencia
            Thread.sleep(1000);

            String url = SupabaseClient.URLs.solicitudDonacion() +
                    "?usuarioid=eq." + usuarioId +
                    "&order=fecha_publicacion.desc&limit=1";

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", SupabaseClient.Headers.getAuthHeader())
                    .addHeader("apikey", SupabaseClient.Headers.getApiKeyHeader())
                    .build();

            try (Response response = SupabaseClient.getHttpClient().newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    System.out.println("🔍 Response buscar solicitud: " + json);

                    Type listType = new TypeToken<List<SolicitudDonacion>>(){}.getType();
                    List<SolicitudDonacion> solicitudes = gson.fromJson(json, listType);

                    if (solicitudes != null && !solicitudes.isEmpty()) {
                        SolicitudDonacion solicitud = solicitudes.get(0);
                        System.out.println("✅ Solicitud encontrada - ID: " + solicitud.getSolicitudid());
                        callback.onSuccess(solicitud);
                    } else {
                        callback.onError("Solicitud creada pero no encontrada en la base de datos");
                    }
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                    System.out.println("❌ Error al buscar solicitud: " + response.code() + " - " + errorBody);
                    callback.onError("Error al obtener datos de la solicitud: " + response.code());
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Exception en buscarSolicitudRecienCreada: " + e.getMessage());
            callback.onError("Error: " + e.getMessage());
        }
    }

    // ========== CHATS ==========
    public static void createChat(Chat chat, ApiCallback<Chat> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                // Obtener fecha actual desde la app
                java.time.format.DateTimeFormatter formatter =
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
                String fechaActual = java.time.LocalDateTime.now().format(formatter);

                String json = "{" +
                        "\"usuario1id\":" + chat.getUsuario1id() + "," +
                        "\"usuario2id\":" + chat.getUsuario2id() + "," +
                        "\"solicitudid\":" + chat.getSolicitudid() + "," +
                        "\"fecha_creacion\":\"" + fechaActual + "\"" + // ← Fecha desde app
                        "}";

                RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

                Request request = new Request.Builder()
                        .url(SupabaseClient.URLs.chat())
                        .post(body)
                        .addHeader("Authorization", SupabaseClient.Headers.getAuthHeader())
                        .addHeader("apikey", SupabaseClient.Headers.getApiKeyHeader())
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=representation")
                        .build();

                try (Response response = SupabaseClient.getHttpClient().newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        Type listType = new TypeToken<List<Chat>>(){}.getType();
                        List<Chat> chatsCreados = gson.fromJson(responseBody, listType);

                        if (chatsCreados != null && !chatsCreados.isEmpty()) {
                            callback.onSuccess(chatsCreados.get(0));
                        } else {
                            callback.onError("Chat creado pero no retornado");
                        }
                    } else {
                        callback.onError("Error al crear chat: " + response.code());
                    }
                }
            } catch (Exception e) {
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    public static void getChatsByUsuario(int usuarioId, ListCallback<Chat> callback) {
        String url = SupabaseClient.URLs.chat() +
                "?or=(usuario1id.eq." + usuarioId + ",usuario2id.eq." + usuarioId + ")" +
                "&order=fecha_creacion.desc";
        getList(url, new TypeToken<List<Chat>>(){}.getType(), callback);
    }

    public static void getChatByUsuariosAndSolicitud(int usuario1id, int usuario2id, int solicitudid, ApiCallback<Chat> callback) {
        String url = SupabaseClient.URLs.chat() +
                "?and=(usuario1id.eq." + usuario1id + ",usuario2id.eq." + usuario2id + ",solicitudid.eq." + solicitudid + ")" +
                "&limit=1";
        getSingle(url, new TypeToken<List<Chat>>(){}.getType(), callback);
    }

    // ========== MENSAJES ==========
    public static void createMensaje(Mensaje mensaje, ApiCallback<Mensaje> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                // Obtener fecha actual desde la app
                java.time.format.DateTimeFormatter formatter =
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
                String fechaActual = java.time.LocalDateTime.now().format(formatter);

                String json = "{" +
                        "\"chatid\":" + mensaje.getChatid() + "," +
                        "\"emisorioid\":" + mensaje.getEmisorioid() + "," +
                        "\"contenido\":\"" + mensaje.getContenido() + "\"," +
                        "\"fecha_envio\":\"" + fechaActual + "\"," + // ← Fecha desde app
                        "\"leido\":false" +
                        "}";

                RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

                Request request = new Request.Builder()
                        .url(SupabaseClient.URLs.mensaje())
                        .post(body)
                        .addHeader("Authorization", SupabaseClient.Headers.getAuthHeader())
                        .addHeader("apikey", SupabaseClient.Headers.getApiKeyHeader())
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=representation")
                        .build();

                try (Response response = SupabaseClient.getHttpClient().newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        Type listType = new TypeToken<List<Mensaje>>(){}.getType();
                        List<Mensaje> mensajesCreados = gson.fromJson(responseBody, listType);

                        if (mensajesCreados != null && !mensajesCreados.isEmpty()) {
                            callback.onSuccess(mensajesCreados.get(0));
                        } else {
                            callback.onError("Mensaje creado pero no retornado");
                        }
                    } else {
                        callback.onError("Error al crear mensaje: " + response.code());
                    }
                }
            } catch (Exception e) {
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    public static void getMensajesByChat(int chatId, ListCallback<Mensaje> callback) {
        String url = SupabaseClient.URLs.mensaje() +
                "?chatid=eq." + chatId +
                "&order=fecha_envio.asc";
        getList(url, new TypeToken<List<Mensaje>>(){}.getType(), callback);
    }

    // ========== MÉTODOS GENÉRICOS ==========
    private static <T> void getSingle(String url, Type type, ApiCallback<T> callback) {
        CompletableFuture.runAsync(() -> {
            Request request = createGetRequest(url);
            executeRequest(request, type, callback);
        });
    }

    private static <T> void getList(String url, Type type, ListCallback<T> callback) {
        CompletableFuture.runAsync(() -> {
            Request request = createGetRequest(url);
            executeListRequest(request, type, callback);
        });
    }

    private static <T> void postWithReturn(String url, Object data, Type type, ApiCallback<T> callback) {
        CompletableFuture.runAsync(() -> {
            Request request = createPostRequest(url, data);
            executeRequest(request, type, callback);
        });
    }

    private static <T> void patch(String url, Object data, Type type, ApiCallback<T> callback) {
        CompletableFuture.runAsync(() -> {
            Request request = createPatchRequest(url, data);
            executeRequest(request, type, callback);
        });
    }

    // ========== MÉTODOS DE REQUEST ==========
    private static Request createGetRequest(String url) {
        return new Request.Builder()
                .url(url)
                .addHeader("Authorization", SupabaseClient.Headers.getAuthHeader())
                .addHeader("apikey", SupabaseClient.Headers.getApiKeyHeader())
                .build();
    }

    private static Request createPostRequest(String url, Object data) {
        String json = gson.toJson(data);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        return new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", SupabaseClient.Headers.getAuthHeader())
                .addHeader("apikey", SupabaseClient.Headers.getApiKeyHeader())
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build();
    }

    private static Request createPatchRequest(String url, Object data) {
        String json = gson.toJson(data);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        return new Request.Builder()
                .url(url)
                .patch(body)
                .addHeader("Authorization", SupabaseClient.Headers.getAuthHeader())
                .addHeader("apikey", SupabaseClient.Headers.getApiKeyHeader())
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build();
    }

    private static <T> void executeRequest(Request request, Type type, ApiCallback<T> callback) {
        try (Response response = SupabaseClient.getHttpClient().newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                System.out.println("📨 Response: " + json);
                List<T> result = gson.fromJson(json, type);
                if (result != null && !result.isEmpty()) {
                    callback.onSuccess(result.get(0));
                } else {
                    callback.onError("No se encontraron datos en la respuesta");
                }
            } else {
                String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                System.out.println("❌ Error: " + response.code() + " - " + errorBody);
                callback.onError("Error: " + response.code());
            }
        } catch (IOException e) {
            System.out.println("❌ IOException: " + e.getMessage());
            callback.onError("Error de red: " + e.getMessage());
        }
    }

    private static <T> void executeListRequest(Request request, Type type, ListCallback<T> callback) {
        try (Response response = SupabaseClient.getHttpClient().newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                List<T> result = gson.fromJson(json, type);
                callback.onSuccess(result);
            } else {
                callback.onError("Error: " + response.code());
            }
        } catch (IOException e) {
            callback.onError("Error de red: " + e.getMessage());
        }
    }

    // ========== SOLICITUDES POR USUARIO ==========
    public static void getSolicitudesByUsuarioId(int usuarioId, ListCallback<SolicitudDonacion> callback) {
        String url = SupabaseClient.URLs.solicitudDonacion() +
                "?usuarioid=eq." + usuarioId +
                "&order=fecha_publicacion.desc";
        getList(url, new TypeToken<List<SolicitudDonacion>>(){}.getType(), callback);
    }
    public static void deleteHistorialDonacion(int historialId, ApiCallback<Void> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                // DELETE: .../historial_donacion?historialid=eq.X
                String url = SupabaseClient.URLs.historialDonacion() +
                        "?historialid=eq." + historialId;

                Request request = new Request.Builder()
                        .url(url)
                        .delete() // Usamos el método DELETE
                        // Asegúrate de que las cabeceras estén correctas
                        .addHeader("Authorization", SupabaseClient.Headers.getAuthHeader())
                        .addHeader("apikey", SupabaseClient.Headers.getApiKeyHeader())
                        .addHeader("Prefer", "return=minimal")
                        .build();

                try (Response response = SupabaseClient.getHttpClient().newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        // Código 200 o 204 significa éxito en DELETE
                        callback.onSuccess(null);
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                        callback.onError("Error al eliminar: " + response.code() + " - " + errorBody);
                    }
                }

            } catch (Exception e) {
                callback.onError("Error de red: " + e.getMessage());
            }
        });
    }

    // ========== ACTUALIZAR ESTADO DE SOLICITUD ==========
    public static void updateSolicitudEstado(int solicitudId, String nuevoEstado, ApiCallback<SolicitudDonacion> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                String json = "{\"estado\":\"" + nuevoEstado + "\"}";

                RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

                String url = SupabaseClient.URLs.solicitudDonacion() + "?solicitudid=eq." + solicitudId;

                Request request = new Request.Builder()
                        .url(url)
                        .patch(body)
                        .addHeader("Authorization", SupabaseClient.Headers.getAuthHeader())
                        .addHeader("apikey", SupabaseClient.Headers.getApiKeyHeader())
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=representation")
                        .build();

                try (Response response = SupabaseClient.getHttpClient().newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        System.out.println("✅ UPDATE estado solicitud exitoso: " + responseBody);

                        Type listType = new TypeToken<List<SolicitudDonacion>>(){}.getType();
                        List<SolicitudDonacion> solicitudesActualizadas = gson.fromJson(responseBody, listType);

                        if (solicitudesActualizadas != null && !solicitudesActualizadas.isEmpty()) {
                            callback.onSuccess(solicitudesActualizadas.get(0));
                        } else {
                            callback.onError("No se pudo obtener la solicitud actualizada");
                        }
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                        System.out.println("❌ Error en UPDATE estado solicitud: " + response.code() + " - " + errorBody);
                        callback.onError("Error al actualizar estado: " + response.code());
                    }
                }

            } catch (Exception e) {
                System.out.println("❌ Exception en updateSolicitudEstado: " + e.getMessage());
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    // ========== NOTIFICACIONES ==========
    public static void createNotificacion(Notificacion notificacion, ApiCallback<Notificacion> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                // Obtener fecha actual desde la app
                java.time.format.DateTimeFormatter formatter =
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
                String fechaActual = java.time.LocalDateTime.now().format(formatter);

                String json = "{" +
                        "\"usuarioid\":" + notificacion.getUsuarioid() + "," +
                        "\"titulo\":\"" + notificacion.getTitulo() + "\"," +
                        "\"mensaje\":\"" + notificacion.getMensaje() + "\"," +
                        "\"fecha_envio\":\"" + fechaActual + "\"," +
                        "\"leida\":" + notificacion.isLeida() +
                        "}";

                System.out.println("📤 JSON Notificación: " + json);

                RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

                Request request = new Request.Builder()
                        .url(SupabaseClient.URLs.notificacion())
                        .post(body)
                        .addHeader("Authorization", SupabaseClient.Headers.getAuthHeader())
                        .addHeader("apikey", SupabaseClient.Headers.getApiKeyHeader())
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=representation")
                        .build();

                try (Response response = SupabaseClient.getHttpClient().newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        System.out.println("✅ Notificación creada: " + responseBody);

                        Type listType = new TypeToken<List<Notificacion>>(){}.getType();
                        List<Notificacion> notificacionesCreadas = gson.fromJson(responseBody, listType);

                        if (notificacionesCreadas != null && !notificacionesCreadas.isEmpty()) {
                            callback.onSuccess(notificacionesCreadas.get(0));
                        } else {
                            callback.onError("Notificación creada pero no retornada");
                        }
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                        System.out.println("❌ Error creando notificación: " + response.code() + " - " + errorBody);
                        callback.onError("Error al crear notificación: " + response.code());
                    }
                }
            } catch (Exception e) {
                System.out.println("❌ Exception en createNotificacion: " + e.getMessage());
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    public static void getNotificacionesByUsuario(int usuarioId, ListCallback<Notificacion> callback) {
        String url = SupabaseClient.URLs.notificacion() +
                "?usuarioid=eq." + usuarioId +
                "&order=fecha_envio.desc";
        getList(url, new TypeToken<List<Notificacion>>(){}.getType(), callback);
    }

    public static void updateNotificacionLeida(int notificacionId, ApiCallback<Notificacion> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                String json = "{\"leida\":true}";

                RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

                String url = SupabaseClient.URLs.notificacion() + "?notificacionid=eq." + notificacionId;

                Request request = new Request.Builder()
                        .url(url)
                        .patch(body)
                        .addHeader("Authorization", SupabaseClient.Headers.getAuthHeader())
                        .addHeader("apikey", SupabaseClient.Headers.getApiKeyHeader())
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=representation")
                        .build();

                try (Response response = SupabaseClient.getHttpClient().newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        Type listType = new TypeToken<List<Notificacion>>(){}.getType();
                        List<Notificacion> notificacionesActualizadas = gson.fromJson(responseBody, listType);

                        if (notificacionesActualizadas != null && !notificacionesActualizadas.isEmpty()) {
                            callback.onSuccess(notificacionesActualizadas.get(0));
                        } else {
                            callback.onError("No se pudo obtener la notificación actualizada");
                        }
                    } else {
                        callback.onError("Error al actualizar notificación: " + response.code());
                    }
                }
            } catch (Exception e) {
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    public static void getNotificacionesNoLeidas(int usuarioId, ListCallback<Notificacion> callback) {
        String url = SupabaseClient.URLs.notificacion() +
                "?usuarioid=eq." + usuarioId +
                "&leida=eq.false" +
                "&order=fecha_envio.desc";
        getList(url, new TypeToken<List<Notificacion>>(){}.getType(), callback);
    }

    // Método para obtener solo mensajes no leídos
    public static void getMensajesNoLeidos(int usuarioId, ListCallback<Mensaje> callback) {
        String url = SupabaseClient.URLs.mensaje() +
                "?leido=eq.false" +
                "&emisorioid=neq." + usuarioId + // Mensajes de otros usuarios
                "&order=fecha_envio.desc";
        getList(url, new TypeToken<List<Mensaje>>(){}.getType(), callback);
    }

    // ========== CHATS - MÉTODOS ADICIONALES ==========
    public static void getChatById(int chatId, ApiCallback<Chat> callback) {
        String url = SupabaseClient.URLs.chat() + "?chatid=eq." + chatId + "&limit=1";
        getSingle(url, new TypeToken<List<Chat>>(){}.getType(), callback);
    }

    // ========== ACTUALIZAR MENSAJES COMO LEÍDOS ==========
    public static void updateMensajesLeidos(int chatId, int usuarioActualId, ApiCallback<Void> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                String json = "{\"leido\":true}";

                RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

                // Actualizar mensajes que NO son del usuario actual y están sin leer
                String url = SupabaseClient.URLs.mensaje() +
                        "?chatid=eq." + chatId +
                        "&emisorioid=neq." + usuarioActualId +
                        "&leido=eq.false";

                Request request = new Request.Builder()
                        .url(url)
                        .patch(body)
                        .addHeader("Authorization", SupabaseClient.Headers.getAuthHeader())
                        .addHeader("apikey", SupabaseClient.Headers.getApiKeyHeader())
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=minimal")
                        .build();

                try (Response response = SupabaseClient.getHttpClient().newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        System.out.println("✅ Mensajes marcados como leídos para chat: " + chatId);
                        callback.onSuccess(null);
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                        System.out.println("❌ Error marcando mensajes como leídos: " + response.code() + " - " + errorBody);
                        callback.onError("Error al marcar mensajes como leídos: " + response.code());
                    }
                }

            } catch (Exception e) {
                System.out.println("❌ Exception en updateMensajesLeidos: " + e.getMessage());
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

}