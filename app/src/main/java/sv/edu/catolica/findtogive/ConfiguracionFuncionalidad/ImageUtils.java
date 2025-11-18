package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ImageUtils {

    /**
     * Convierte un Uri de contenido a un archivo físico en el directorio de cache.
     * Lee el InputStream del Uri y escribe los datos en un archivo temporal.
     * Retorna el archivo creado o null si ocurre algún error.
     */
    public static File uriToFile(Context context, Uri uri) {
        try {
            ContentResolver contentResolver = context.getContentResolver();
            InputStream inputStream = contentResolver.openInputStream(uri);

            if (inputStream == null) {
                return null;
            }

            // Obtener información del archivo
            String displayName = getFileName(context, uri);
            String fileExtension = getFileExtension(context, uri);

            if (fileExtension == null) {
                fileExtension = "jpg"; // Extensión por defecto
            }

            String fileName = "upload_" + System.currentTimeMillis() + "." + fileExtension;
            File file = new File(context.getCacheDir(), fileName);

            FileOutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytes = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }

            inputStream.close();
            outputStream.close();

            return file;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Obtiene el nombre del archivo a partir de un Uri.
     * Primero intenta obtenerlo mediante una consulta al ContentResolver,
     * si falla, extrae el nombre de la ruta del Uri.
     */
    private static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    /**
     * Obtiene la extensión del archivo a partir de un Uri.
     * Primero intenta obtenerla del tipo MIME mediante el ContentResolver,
     * si no es posible, extrae la extensión del nombre del archivo.
     * Retorna "jpg" como extensión por defecto si no puede determinarse.
     */
    private static String getFileExtension(Context context, Uri uri) {
        ContentResolver contentResolver = context.getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();

        // Primero intentar obtener la extensión del tipo MIME
        String mimeType = contentResolver.getType(uri);
        if (mimeType != null) {
            String extension = mime.getExtensionFromMimeType(mimeType);
            if (extension != null) {
                return extension;
            }
        }

        // Fallback: obtener del nombre del archivo
        String fileName = getFileName(context, uri);
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        }

        return "jpg"; // Extensión por defecto
    }
}