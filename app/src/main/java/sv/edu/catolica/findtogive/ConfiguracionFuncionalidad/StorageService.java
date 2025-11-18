package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class StorageService {

    public interface UploadCallback {
        void onSuccess(String imageUrl);
        void onError(String error);
    }

    /**
     * Sube una imagen para una solicitud de donación específica al bucket de solicitudes.
     * Convierte el Uri a archivo, lo sube a Supabase Storage y retorna la URL pública.
     */
    public static void uploadSolicitudImage(Context context, Uri imageUri, int solicitudId, UploadCallback callback) {
        uploadImage(context, imageUri, "solicitud_" + solicitudId, Config.STORAGE_REQUESTS_BUCKET, callback);
    }

    /**
     * Sube una imagen de perfil de usuario al bucket de perfiles.
     * Convierte el Uri a archivo, lo sube a Supabase Storage y retorna la URL pública.
     */
    public static void uploadProfileImage(Context context, Uri imageUri, int usuarioId, UploadCallback callback) {
        uploadImage(context, imageUri, "profile_" + usuarioId, Config.STORAGE_PROFILES_BUCKET, callback);
    }

    /**
     * Método genérico para subir imágenes a Supabase Storage.
     * Convierte el Uri a archivo temporal, crea una petición multipart,
     * sube el archivo al bucket especificado y retorna la URL pública.
     * Maneja errores HTTP comunes y limpia el archivo temporal después de la subida.
     */
    private static void uploadImage(Context context, Uri imageUri, String baseFileName, String bucket, UploadCallback callback) {
        CompletableFuture.runAsync(() -> {
            try {
                // Convertir URI a File
                File imageFile = ImageUtils.uriToFile(context, imageUri);
                if (imageFile == null || !imageFile.exists()) {
                    callback.onError("Error: No se pudo acceder a la imagen");
                    return;
                }

                // Crear el nombre del archivo único
                String fileName = baseFileName + "_" + System.currentTimeMillis() + ".jpg";

                // Crear el cuerpo de la petición multipart
                RequestBody fileBody = RequestBody.create(imageFile, MediaType.parse("image/jpeg"));
                MultipartBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", fileName, fileBody)
                        .build();

                // URL para Supabase Storage
                String storageUrl = Config.SUPABASE_URL + "/storage/v1/object/" + bucket + "/" + fileName;

                // Crear la petición
                Request request = new Request.Builder()
                        .url(storageUrl)
                        .post(requestBody)
                        .addHeader("Authorization", "Bearer " + Config.SUPABASE_ANON_KEY)
                        .addHeader("apikey", Config.SUPABASE_ANON_KEY)
                        .build();

                // Ejecutar la petición
                try (Response response = SupabaseClient.getHttpClient().newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        // Construir la URL pública de la imagen
                        String publicUrl = Config.SUPABASE_URL + "/storage/v1/object/public/" +
                                bucket + "/" + fileName;
                        callback.onSuccess(publicUrl);
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Sin detalles";

                        // Errores específicos
                        if (response.code() == 400) {
                            callback.onError("Error 400: Petición mal formada");
                        } else if (response.code() == 401) {
                            callback.onError("Error 401: No autorizado - Verifica tus API keys");
                        } else if (response.code() == 403) {
                            callback.onError("Error 403: Prohibido - Verifica los permisos del bucket");
                        } else if (response.code() == 413) {
                            callback.onError("Error 413: Archivo demasiado grande");
                        } else {
                            callback.onError("Error " + response.code() + ": " + errorBody);
                        }
                    }
                }

                // Limpiar archivo temporal
                if (imageFile.exists()) {
                    imageFile.delete();
                }

            } catch (Exception e) {
                callback.onError("Error: " + e.getMessage());
            }
        });
    }
}