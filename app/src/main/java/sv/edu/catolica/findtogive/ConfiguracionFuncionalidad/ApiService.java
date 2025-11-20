package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sv.edu.catolica.findtogive.Modelado.Chat;
import sv.edu.catolica.findtogive.Modelado.HospitalUbicacion;
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

    /**
     * Autentica un usuario con email y contraseña
     * @param email Email del usuario
     * @param password Contraseña del usuario
     * @param callback Callback para manejar el resultado
     */
    public static void loginUser(String email, String password, ApiCallback<Usuario> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                String url = SupabaseClient.URLs.usuario() + "?email=eq." + email + "&activo=eq.true&limit=1";

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", SupabaseClient.Headers.getAuthHeader())
                        .addHeader("apikey", SupabaseClient.Headers.getApiKeyHeader())
                        .build();

                try (Response response = SupabaseClient.getHttpClient().newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String json = response.body().string();

                        Type listType = new TypeToken<List<Usuario>>(){}.getType();
                        List<Usuario> usuarios = gson.fromJson(json, listType);

                        if (usuarios != null && !usuarios.isEmpty()) {
                            Usuario usuario = usuarios.get(0);
                            String storedHash = usuario.getContrasena();

                            if (SecurityHelper.verifyPassword(password, storedHash)) {
                                callback.onSuccess(usuario);
                            } else {
                                callback.onError("Credenciales inválidas");
                            }
                        } else {
                            callback.onError("Usuario no encontrado");
                        }
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                        callback.onError("Error: " + response.code());
                    }
                }

            } catch (Exception e) {
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    /**
     * Registra un nuevo usuario en el sistema
     * @param usuario Objeto usuario con los datos del registro
     * @param callback Callback para manejar el resultado
     */
    public static void registerUser(Usuario usuario, ApiCallback<Usuario> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                String hashedPassword = SecurityHelper.hashPassword(usuario.getContrasena());

                String json = "{" +
                        "\"nombre\":\"" + usuario.getNombre() + "\"," +
                        "\"apellido\":\"" + usuario.getApellido() + "\"," +
                        "\"email\":\"" + usuario.getEmail() + "\"," +
                        "\"contrasena\":\"" + hashedPassword + "\"," +
                        "\"edad\":" + usuario.getEdad() + "," +
                        "\"telefono\":\"" + usuario.getTelefono() + "\"," +
                        "\"rolid\":" + usuario.getRolid() + "," +
                        "\"tiposangreid\":" + usuario.getTiposangreid() + "," +
                        "\"activo\":true" +
                        "}";

                RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

                Request request = new Request.Builder()
                        .url(SupabaseClient.URLs.usuario())
                        .post(body)
                        .addHeader("Authorization", SupabaseClient.Headers.getAuthHeader())
                        .addHeader("apikey", SupabaseClient.Headers.getApiKeyHeader())
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=minimal")
                        .build();

                try (Response response = SupabaseClient.getHttpClient().newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        Thread.sleep(1000);
                        buscarUsuarioRecienCreado(usuario.getEmail(), callback);
                    } else {
                        String errorBody = response.body() != null ?
                                response.body().string() : "Sin detalles";

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
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    /**
     * Busca un usuario recién creado por email
     * @param email Email del usuario a buscar
     * @param callback Callback para manejar el resultado
     */
    private static void buscarUsuarioRecienCreado(String email, ApiCallback<Usuario> callback) {
        try {
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

                    Type listType = new TypeToken<List<Usuario>>(){}.getType();
                    List<Usuario> usuarios = gson.fromJson(json, listType);

                    if (usuarios != null && !usuarios.isEmpty()) {
                        Usuario usuario = usuarios.get(0);
                        callback.onSuccess(usuario);
                    } else {
                        callback.onError("Usuario creado pero no encontrado en la base de datos");
                    }
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                    callback.onError("Error al obtener datos del usuario: " + response.code());
                }
            }
        } catch (Exception e) {
            callback.onError("Error: " + e.getMessage());
        }
    }

    /**
     * Actualiza una solicitud de donación existente
     * @param solicitud Solicitud con los datos actualizados
     * @param callback Callback para manejar el resultado
     */
    public static void updateSolicitud(SolicitudDonacion solicitud, ApiCallback<SolicitudDonacion> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                String json = "{" +
                        "\"imagen_url\":\"" + (solicitud.getImagenUrl() != null ? solicitud.getImagenUrl() : "") + "\"" +
                        "}";

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

                        Type listType = new TypeToken<List<SolicitudDonacion>>(){}.getType();
                        List<SolicitudDonacion> solicitudesActualizadas = gson.fromJson(responseBody, listType);

                        if (solicitudesActualizadas != null && !solicitudesActualizadas.isEmpty()) {
                            callback.onSuccess(solicitudesActualizadas.get(0));
                        } else {
                            callback.onSuccess(solicitud);
                        }
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                        callback.onError("Error al actualizar solicitud: " + response.code());
                    }
                }

            } catch (Exception e) {
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    /**
     * Obtiene una solicitud por su ID
     * @param solicitudId ID de la solicitud
     * @param callback Callback para manejar el resultado
     */
    public static void getSolicitudById(int solicitudId, ApiCallback<SolicitudDonacion> callback) {
        String url = SupabaseClient.URLs.solicitudDonacion() + "?solicitudid=eq." + solicitudId + "&limit=1";
        getSingle(url, new TypeToken<List<SolicitudDonacion>>(){}.getType(), callback);
    }

    /**
     * Obtiene un usuario por su ID
     * @param usuarioId ID del usuario
     * @param callback Callback para manejar el resultado
     */
    public static void getUsuarioById(int usuarioId, ApiCallback<Usuario> callback) {
        String url = SupabaseClient.URLs.usuario() + "?usuarioid=eq." + usuarioId + "&limit=1";
        getSingle(url, new TypeToken<List<Usuario>>(){}.getType(), callback);
    }

    /**
     * Obtiene múltiples usuarios por sus IDs
     * @param usuarioIds Lista de IDs de usuarios
     * @param callback Callback para manejar el resultado
     */
    public static void getUsuariosByIds(List<Integer> usuarioIds, ListCallback<Usuario> callback) {
        if (usuarioIds == null || usuarioIds.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        String idsParam = usuarioIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        String url = SupabaseClient.URLs.usuario() + "?usuarioid=in.(" + idsParam + ")";
        getList(url, new TypeToken<List<Usuario>>(){}.getType(), callback);
    }

    /**
     * Actualiza los datos de un usuario
     * @param usuario Usuario con los datos actualizados
     * @param callback Callback para manejar el resultado
     */
    public static void updateUser(Usuario usuario, ApiCallback<Usuario> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                String json = usuario.toJsonForUpdate();

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

                        Type listType = new TypeToken<List<Usuario>>(){}.getType();
                        List<Usuario> usuariosActualizados = gson.fromJson(responseBody, listType);

                        if (usuariosActualizados != null && !usuariosActualizados.isEmpty()) {
                            callback.onSuccess(usuariosActualizados.get(0));
                        } else {
                            callback.onSuccess(usuario);
                        }
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                        callback.onError("Error al actualizar usuario: " + response.code());
                    }
                }

            } catch (Exception e) {
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    /**
     * Obtiene todas las solicitudes activas
     * @param callback Callback para manejar el resultado
     */
    public static void getSolicitudesActivas(ListCallback<SolicitudDonacion> callback) {
        String url = SupabaseClient.URLs.solicitudDonacion() + "?estado=eq.activa&order=fecha_publicacion.desc";
        getList(url, new TypeToken<List<SolicitudDonacion>>(){}.getType(), callback);
    }

    /**
     * Crea una nueva solicitud de donación
     * @param solicitud Solicitud a crear
     * @param callback Callback para manejar el resultado
     */
    public static void createSolicitud(SolicitudDonacion solicitud, ApiCallback<SolicitudDonacion> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                java.time.format.DateTimeFormatter formatter =
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
                String fechaActual = java.time.LocalDateTime.now().format(formatter);

                String json = "{" +
                        "\"usuarioid\":" + solicitud.getUsuarioid() + "," +
                        "\"titulo\":\"" + solicitud.getTitulo() + "\"," +
                        "\"descripcion\":\"" + solicitud.getDescripcion() + "\"," +
                        "\"tiposangreid\":" + solicitud.getTiposangreid() + "," +
                        "\"hospitalid\":" + solicitud.getHospitalid() + "," +
                        "\"fecha_publicacion\":\"" + fechaActual + "\"," +
                        "\"estado\":\"activa\"" +
                        "}";

                RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

                Request request = new Request.Builder()
                        .url(SupabaseClient.URLs.solicitudDonacion())
                        .post(body)
                        .addHeader("Authorization", SupabaseClient.Headers.getAuthHeader())
                        .addHeader("apikey", SupabaseClient.Headers.getApiKeyHeader())
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=representation")
                        .build();

                try (Response response = SupabaseClient.getHttpClient().newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();

                        Type listType = new TypeToken<List<SolicitudDonacion>>(){}.getType();
                        List<SolicitudDonacion> solicitudesCreadas = gson.fromJson(responseBody, listType);

                        if (solicitudesCreadas != null && !solicitudesCreadas.isEmpty()) {
                            SolicitudDonacion solicitudCreada = solicitudesCreadas.get(0);
                            callback.onSuccess(solicitudCreada);
                        } else {
                            buscarSolicitudRecienCreada(solicitud.getUsuarioid(), callback);
                        }
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                        callback.onError("Error al crear solicitud: " + response.code());
                    }
                }

            } catch (Exception e) {
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    /**
     * Obtiene la lista de hospitales
     * @param callback Callback para manejar el resultado
     */
    public static void getHospitales(ListCallback<HospitalUbicacion> callback) {
        String url = SupabaseClient.URLs.hospitalUbicacion();
        getList(url, new TypeToken<List<HospitalUbicacion>>(){}.getType(), callback);
    }

    /**
     * Obtiene un hospital por su ID
     * @param hospitalId ID del hospital
     * @param callback Callback para manejar el resultado
     */
    public static void getHospitalById(int hospitalId, ApiCallback<HospitalUbicacion> callback) {
        String url = SupabaseClient.URLs.hospitalUbicacion() + "?hospitalid=eq." + hospitalId + "&limit=1";
        getSingle(url, new TypeToken<List<HospitalUbicacion>>(){}.getType(), callback);
    }

    /**
     * Busca solicitudes por query y tipo de sangre
     * @param query Texto de búsqueda
     * @param tipoSangreId ID del tipo de sangre
     * @param callback Callback para manejar el resultado
     */
    public static void buscarSolicitudes(String query, Integer tipoSangreId, ListCallback<SolicitudDonacion> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                StringBuilder urlBuilder = new StringBuilder(SupabaseClient.URLs.solicitudDonacion());
                urlBuilder.append("?estado=eq.activa");

                List<String> filters = new ArrayList<>();

                if (query != null && !query.trim().isEmpty()) {
                    filters.add("titulo=ilike.%25" + query.trim() + "%25");
                }

                if (tipoSangreId != null && tipoSangreId > 0) {
                    filters.add("tiposangreid=eq." + tipoSangreId);
                }

                if (!filters.isEmpty()) {
                    urlBuilder.append("&").append(String.join("&", filters));
                }

                urlBuilder.append("&order=fecha_publicacion.desc");

                String finalUrl = urlBuilder.toString();
                getList(finalUrl, new TypeToken<List<SolicitudDonacion>>(){}.getType(), callback);

            } catch (Exception e) {
                callback.onError("Error en búsqueda: " + e.getMessage());
            }
        });
    }

    /**
     * Obtiene todas las solicitudes sin filtros
     * @param callback Callback para manejar el resultado
     */
    public static void getSolicitudesSinFiltros(ListCallback<SolicitudDonacion> callback) {
        getSolicitudesActivas(callback);
    }

    /**
     * Busca una solicitud recién creada por usuario
     * @param usuarioId ID del usuario
     * @param callback Callback para manejar el resultado
     */
    private static void buscarSolicitudRecienCreada(int usuarioId, ApiCallback<SolicitudDonacion> callback) {
        try {
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

                    Type listType = new TypeToken<List<SolicitudDonacion>>(){}.getType();
                    List<SolicitudDonacion> solicitudes = gson.fromJson(json, listType);

                    if (solicitudes != null && !solicitudes.isEmpty()) {
                        SolicitudDonacion solicitud = solicitudes.get(0);
                        callback.onSuccess(solicitud);
                    } else {
                        callback.onError("Solicitud creada pero no encontrada en la base de datos");
                    }
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                    callback.onError("Error al obtener datos de la solicitud: " + response.code());
                }
            }
        } catch (Exception e) {
            callback.onError("Error: " + e.getMessage());
        }
    }

    /**
     * Crea un nuevo chat
     * @param chat Chat a crear
     * @param callback Callback para manejar el resultado
     */
    public static void createChat(Chat chat, ApiCallback<Chat> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                java.time.format.DateTimeFormatter formatter =
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
                String fechaActual = java.time.LocalDateTime.now().format(formatter);

                String json = "{" +
                        "\"usuario1id\":" + chat.getUsuario1id() + "," +
                        "\"usuario2id\":" + chat.getUsuario2id() + "," +
                        "\"solicitudid\":" + chat.getSolicitudid() + "," +
                        "\"fecha_creacion\":\"" + fechaActual + "\"" +
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

    /**
     * Obtiene los chats de un usuario
     * @param usuarioId ID del usuario
     * @param callback Callback para manejar el resultado
     */
    public static void getChatsByUsuario(int usuarioId, ListCallback<Chat> callback) {
        String url = SupabaseClient.URLs.chat() +
                "?or=(usuario1id.eq." + usuarioId + ",usuario2id.eq." + usuarioId + ")" +
                "&order=fecha_creacion.desc";
        getList(url, new TypeToken<List<Chat>>(){}.getType(), callback);
    }

    /**
     * Obtiene un chat específico por usuarios y solicitud
     * @param usuario1id ID del primer usuario
     * @param usuario2id ID del segundo usuario
     * @param solicitudid ID de la solicitud
     * @param callback Callback para manejar el resultado
     */
    public static void getChatByUsuariosAndSolicitud(int usuario1id, int usuario2id, int solicitudid, ApiCallback<Chat> callback) {
        String url = SupabaseClient.URLs.chat() +
                "?and=(usuario1id.eq." + usuario1id + ",usuario2id.eq." + usuario2id + ",solicitudid.eq." + solicitudid + ")" +
                "&limit=1";
        getSingle(url, new TypeToken<List<Chat>>(){}.getType(), callback);
    }

    /**
     * Crea un nuevo mensaje
     * @param mensaje Mensaje a crear
     * @param callback Callback para manejar el resultado
     */
    public static void createMensaje(Mensaje mensaje, ApiCallback<Mensaje> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                java.time.format.DateTimeFormatter formatter =
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
                String fechaActual = java.time.LocalDateTime.now().format(formatter);


                String contenidoEscapado = mensaje.getContenido()
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t");

                String json = "{" +
                        "\"chatid\":" + mensaje.getChatid() + "," +
                        "\"emisorioid\":" + mensaje.getEmisorioid() + "," +
                        "\"contenido\":\"" + contenidoEscapado + "\"," +
                        "\"fecha_envio\":\"" + fechaActual + "\"," +
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
                        String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                        callback.onError("Error al crear mensaje: " + response.code() + " - " + errorBody);
                    }
                }
            } catch (Exception e) {
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    /**
     * Obtiene los mensajes de un chat
     * @param chatId ID del chat
     * @param callback Callback para manejar el resultado
     */
    public static void getMensajesByChat(int chatId, ListCallback<Mensaje> callback) {
        String url = SupabaseClient.URLs.mensaje() +
                "?chatid=eq." + chatId +
                "&order=fecha_envio.asc";
        getList(url, new TypeToken<List<Mensaje>>(){}.getType(), callback);
    }

    // ========== MÉTODOS GENÉRICOS ==========

    /**
     * Ejecuta una consulta para obtener un solo elemento
     * @param url URL de la consulta
     * @param type Tipo del resultado
     * @param callback Callback para manejar el resultado
     */
    private static <T> void getSingle(String url, Type type, ApiCallback<T> callback) {
        CompletableFuture.runAsync(() -> {
            Request request = createGetRequest(url);
            executeRequest(request, type, callback);
        });
    }

    /**
     * Ejecuta una consulta para obtener una lista de elementos
     * @param url URL de la consulta
     * @param type Tipo del resultado
     * @param callback Callback para manejar el resultado
     */
    private static <T> void getList(String url, Type type, ListCallback<T> callback) {
        CompletableFuture.runAsync(() -> {
            Request request = createGetRequest(url);
            executeListRequest(request, type, callback);
        });
    }

    /**
     * Crea una petición GET
     * @param url URL de la petición
     * @return Request configurado
     */
    private static Request createGetRequest(String url) {
        return new Request.Builder()
                .url(url)
                .addHeader("Authorization", SupabaseClient.Headers.getAuthHeader())
                .addHeader("apikey", SupabaseClient.Headers.getApiKeyHeader())
                .build();
    }

    /**
     * Ejecuta una petición y procesa la respuesta para un solo elemento
     * @param request Petición a ejecutar
     * @param type Tipo del resultado
     * @param callback Callback para manejar el resultado
     */
    private static <T> void executeRequest(Request request, Type type, ApiCallback<T> callback) {
        try (Response response = SupabaseClient.getHttpClient().newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                List<T> result = gson.fromJson(json, type);
                if (result != null && !result.isEmpty()) {
                    callback.onSuccess(result.get(0));
                } else {
                    callback.onError("No se encontraron datos en la respuesta");
                }
            } else {
                String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                callback.onError("Error: " + response.code());
            }
        } catch (IOException e) {
            callback.onError("Error de red: " + e.getMessage());
        }
    }

    /**
     * Ejecuta una petición y procesa la respuesta para una lista
     * @param request Petición a ejecutar
     * @param type Tipo del resultado
     * @param callback Callback para manejar el resultado
     */
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

    /**
     * Obtiene las solicitudes de un usuario
     * @param usuarioId ID del usuario
     * @param callback Callback para manejar el resultado
     */
    public static void getSolicitudesByUsuarioId(int usuarioId, ListCallback<SolicitudDonacion> callback) {
        String url = SupabaseClient.URLs.solicitudDonacion() +
                "?usuarioid=eq." + usuarioId +
                "&order=fecha_publicacion.desc";
        getList(url, new TypeToken<List<SolicitudDonacion>>(){}.getType(), callback);
    }

    /**
     * Actualiza el estado de una solicitud
     * @param solicitudId ID de la solicitud
     * @param nuevoEstado Nuevo estado de la solicitud
     * @param callback Callback para manejar el resultado
     */
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

                        Type listType = new TypeToken<List<SolicitudDonacion>>(){}.getType();
                        List<SolicitudDonacion> solicitudesActualizadas = gson.fromJson(responseBody, listType);

                        if (solicitudesActualizadas != null && !solicitudesActualizadas.isEmpty()) {
                            callback.onSuccess(solicitudesActualizadas.get(0));
                        } else {
                            callback.onError("No se pudo obtener la solicitud actualizada");
                        }
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                        callback.onError("Error al actualizar estado: " + response.code());
                    }
                }

            } catch (Exception e) {
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    /**
     * Crea una nueva notificación
     * @param notificacion Notificación a crear
     * @param callback Callback para manejar el resultado
     */
    public static void createNotificacion(Notificacion notificacion, ApiCallback<Notificacion> callback) {
        CompletableFuture.runAsync(() -> {
            try {
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

                        Type listType = new TypeToken<List<Notificacion>>(){}.getType();
                        List<Notificacion> notificacionesCreadas = gson.fromJson(responseBody, listType);

                        if (notificacionesCreadas != null && !notificacionesCreadas.isEmpty()) {
                            callback.onSuccess(notificacionesCreadas.get(0));
                        } else {
                            callback.onError("Notificación creada pero no retornada");
                        }
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                        callback.onError("Error al crear notificación: " + response.code());
                    }
                }
            } catch (Exception e) {
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    /**
     * Obtiene las notificaciones de un usuario
     * @param usuarioId ID del usuario
     * @param callback Callback para manejar el resultado
     */
    public static void getNotificacionesByUsuario(int usuarioId, ListCallback<Notificacion> callback) {
        String url = SupabaseClient.URLs.notificacion() +
                "?usuarioid=eq." + usuarioId +
                "&order=fecha_envio.desc";
        getList(url, new TypeToken<List<Notificacion>>(){}.getType(), callback);
    }

    /**
     * Marca una notificación como leída
     * @param notificacionId ID de la notificación
     * @param callback Callback para manejar el resultado
     */
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

    /**
     * Actualiza la ubicación de un usuario
     * @param usuarioId ID del usuario
     * @param latitud Latitud de la ubicación
     * @param longitud Longitud de la ubicación
     * @param callback Callback para manejar el resultado
     */
    public static void updateUserLocation(int usuarioId, double latitud, double longitud, ApiCallback<Usuario> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                String json = "{" +
                        "\"latitud\":" + latitud + "," +
                        "\"longitud\":" + longitud +
                        "}";

                RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

                String url = SupabaseClient.URLs.usuario() + "?usuarioid=eq." + usuarioId;

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

                        Type listType = new TypeToken<List<Usuario>>(){}.getType();
                        List<Usuario> usuariosActualizados = gson.fromJson(responseBody, listType);

                        if (usuariosActualizados != null && !usuariosActualizados.isEmpty()) {
                            callback.onSuccess(usuariosActualizados.get(0));
                        } else {
                            callback.onError("No se pudo obtener el usuario actualizado");
                        }
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                        callback.onError("Error al actualizar ubicación: " + response.code());
                    }
                }

            } catch (Exception e) {
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    /**
     * Obtiene solicitudes cercanas a una ubicación
     * @param latitudUsuario Latitud del usuario
     * @param longitudUsuario Longitud del usuario
     * @param radioKm Radio de búsqueda en kilómetros
     * @param callback Callback para manejar el resultado
     */
    public static void getSolicitudesCercanas(double latitudUsuario, double longitudUsuario,
                                              double radioKm, ListCallback<SolicitudDonacion> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                String url = SupabaseClient.URLs.solicitudDonacion() + "?estado=eq.activa&order=fecha_publicacion.desc";

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", SupabaseClient.Headers.getAuthHeader())
                        .addHeader("apikey", SupabaseClient.Headers.getApiKeyHeader())
                        .build();

                try (Response response = SupabaseClient.getHttpClient().newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String json = response.body().string();
                        Type listType = new TypeToken<List<SolicitudDonacion>>(){}.getType();
                        List<SolicitudDonacion> todasSolicitudes = gson.fromJson(json, listType);

                        if (todasSolicitudes == null || todasSolicitudes.isEmpty()) {
                            callback.onSuccess(new ArrayList<>());
                            return;
                        }

                        ApiService.getHospitales(new ListCallback<HospitalUbicacion>() {
                            @Override
                            public void onSuccess(List<HospitalUbicacion> hospitales) {
                                Map<Integer, HospitalUbicacion> hospitalesMap = new HashMap<>();
                                for (HospitalUbicacion hospital : hospitales) {
                                    hospitalesMap.put(hospital.getHospitalid(), hospital);
                                }

                                List<SolicitudDonacion> solicitudesCercanas = new ArrayList<>();

                                for (SolicitudDonacion solicitud : todasSolicitudes) {
                                    HospitalUbicacion hospital = hospitalesMap.get(solicitud.getHospitalid());

                                    if (hospital != null) {
                                        double distancia = calcularDistancia(
                                                latitudUsuario, longitudUsuario,
                                                hospital.getLatitud(), hospital.getLongitud()
                                        );

                                        if (distancia <= radioKm) {
                                            solicitudesCercanas.add(solicitud);
                                        }
                                    }
                                }

                                callback.onSuccess(solicitudesCercanas);
                            }

                            @Override
                            public void onError(String error) {
                                callback.onError("Error al obtener hospitales: " + error);
                            }
                        });

                    } else {
                        callback.onError("Error: " + response.code());
                    }
                }

            } catch (Exception e) {
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    /**
     * Calcula la distancia entre dos puntos geográficos usando la fórmula de Haversine
     * @param lat1 Latitud del primer punto
     * @param lon1 Longitud del primer punto
     * @param lat2 Latitud del segundo punto
     * @param lon2 Longitud del segundo punto
     * @return Distancia en kilómetros
     */
    private static double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radio de la Tierra en km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Distancia en kilómetros
    }

    /**
     * Obtiene un chat por su ID
     * @param chatId ID del chat
     * @param callback Callback para manejar el resultado
     */
    public static void getChatById(int chatId, ApiCallback<Chat> callback) {
        String url = SupabaseClient.URLs.chat() + "?chatid=eq." + chatId + "&limit=1";
        getSingle(url, new TypeToken<List<Chat>>(){}.getType(), callback);
    }

    /**
     * Marca los mensajes de un chat como leídos de manera más eficiente
     * @param chatId ID del chat
     * @param usuarioActualId ID del usuario actual
     * @param callback Callback para manejar el resultado
     */
    public static void updateMensajesLeidos(int chatId, int usuarioActualId, ApiCallback<Integer> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                String json = "{\"leido\":true}";

                RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

                // Marcar solo mensajes del otro usuario que no estén leídos
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
                        .addHeader("Prefer", "return=representation")
                        .build();

                try (Response response = SupabaseClient.getHttpClient().newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        // Obtener cuántos mensajes se actualizaron
                        if (response.body() != null) {
                            String responseBody = response.body().string();
                            Type listType = new TypeToken<List<Mensaje>>(){}.getType();
                            List<Mensaje> mensajesActualizados = gson.fromJson(responseBody, listType);
                            int cantidadActualizada = mensajesActualizados != null ? mensajesActualizados.size() : 0;
                            callback.onSuccess(cantidadActualizada);
                        } else {
                            callback.onSuccess(0);
                        }
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                        callback.onError("Error al marcar mensajes como leídos: " + response.code());
                    }
                }

            } catch (Exception e) {
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    /**
     * Obtiene la cantidad de mensajes no leídos en un chat para un usuario
     */
    public static void getMensajesNoLeidosCount(int chatId, int usuarioId, ApiCallback<Integer> callback) {
        String url = SupabaseClient.URLs.mensaje() +
                "?chatid=eq." + chatId +
                "&emisorioid=neq." + usuarioId +
                "&leido=eq.false" +
                "&select=count";

        getSingle(url, new TypeToken<List<Map<String, Integer>>>(){}.getType(),
                new ApiCallback<Map<String, Integer>>() {
                    @Override
                    public void onSuccess(Map<String, Integer> result) {
                        if (result != null && result.containsKey("count")) {
                            callback.onSuccess(result.get("count"));
                        } else {
                            callback.onSuccess(0);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }
                });
    }
}