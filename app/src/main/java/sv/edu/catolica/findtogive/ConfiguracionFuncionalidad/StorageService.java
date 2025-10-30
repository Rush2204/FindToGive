package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

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

    // Método existente para subir imágenes de solicitudes
    public static void uploadSolicitudImage(Context context, Uri imageUri, int solicitudId, UploadCallback callback) {
        uploadImage(context, imageUri, "solicitud_" + solicitudId, Config.STORAGE_REQUESTS_BUCKET, callback);
    }

    // NUEVO MÉTODO: Subir imagen de perfil
    public static void uploadProfileImage(Context context, Uri imageUri, int usuarioId, UploadCallback callback) {
        uploadImage(context, imageUri, "profile_" + usuarioId, Config.STORAGE_PROFILES_BUCKET, callback);
    }

    // Método genérico para subir imágenes
    private static void uploadImage(Context context, Uri imageUri, String baseFileName, String bucket, UploadCallback callback) {
        CompletableFuture.runAsync(() -> {
            try {
                // Convertir URI a File
                File imageFile = ImageUtils.uriToFile(context, imageUri);
                if (imageFile == null || !imageFile.exists()) {
                    Log.e("StorageService", "❌ Archivo de imagen no existe o es nulo");
                    callback.onError("Error: No se pudo acceder a la imagen");
                    return;
                }

                Log.d("StorageService", "📁 Archivo a subir: " + imageFile.getAbsolutePath() +
                        " Tamaño: " + imageFile.length() + " bytes");

                // Crear el nombre del archivo único
                String fileName = baseFileName + "_" + System.currentTimeMillis() + ".jpg";
                Log.d("StorageService", "📝 Nombre de archivo: " + fileName);

                // Crear el cuerpo de la petición multipart
                RequestBody fileBody = RequestBody.create(imageFile, MediaType.parse("image/jpeg"));
                MultipartBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", fileName, fileBody)
                        .build();

                // URL para Supabase Storage
                String storageUrl = Config.SUPABASE_URL + "/storage/v1/object/" + bucket + "/" + fileName;

                Log.d("StorageService", "🌐 URL de subida: " + storageUrl);

                // Crear la petición
                Request request = new Request.Builder()
                        .url(storageUrl)
                        .post(requestBody)
                        .addHeader("Authorization", "Bearer " + Config.SUPABASE_ANON_KEY)
                        .addHeader("apikey", Config.SUPABASE_ANON_KEY)
                        .build();

                Log.d("StorageService", "🚀 Iniciando upload...");

                // Ejecutar la petición
                try (Response response = SupabaseClient.getHttpClient().newCall(request).execute()) {
                    Log.d("StorageService", "📨 Response code: " + response.code());

                    if (response.isSuccessful()) {
                        // Construir la URL pública de la imagen
                        String publicUrl = Config.SUPABASE_URL + "/storage/v1/object/public/" +
                                bucket + "/" + fileName;

                        Log.d("StorageService", "✅ Imagen subida exitosamente: " + publicUrl);
                        callback.onSuccess(publicUrl);
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                        Log.e("StorageService", "❌ Error al subir imagen: " + response.code() + " - " + errorBody);

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
                    boolean deleted = imageFile.delete();
                    Log.d("StorageService", "🧹 Archivo temporal eliminado: " + deleted);
                }

            } catch (Exception e) {
                Log.e("StorageService", "💥 Exception en uploadImage: " + e.getMessage());
                e.printStackTrace();
                callback.onError("Error: " + e.getMessage());
            }
        });
    }
}
