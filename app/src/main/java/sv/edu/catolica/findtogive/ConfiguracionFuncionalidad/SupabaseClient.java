package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class SupabaseClient {
    private static OkHttpClient httpClient;

    public static OkHttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        }
        return httpClient;
    }

    // URLs base para las APIs
    public static class URLs {
        public static final String BASE_URL = Config.SUPABASE_URL + "/rest/v1/";
        public static final String STORAGE_URL = Config.SUPABASE_URL + "/storage/v1/object/";

        public static String usuario() { return BASE_URL + "usuario"; }
        public static String solicitudDonacion() { return BASE_URL + "solicitud_donacion"; }
        public static String rol() { return BASE_URL + "rol"; }
        public static String tipoSangre() { return BASE_URL + "tipo_sangre"; }
        public static String chat() { return BASE_URL + "chat"; }
        public static String mensaje() { return BASE_URL + "mensaje"; }
        public static String historialDonacion() { return BASE_URL + "historial_donacion"; }
        public static String notificacion() { return BASE_URL + "notificacion"; }
    }

    // Headers para las requests
    public static class Headers {
        public static String getAuthHeader() {
            return "Bearer " + Config.SUPABASE_ANON_KEY;
        }

        public static String getApiKeyHeader() {
            return Config.SUPABASE_ANON_KEY;
        }

        public static String getContentType() {
            return "application/json";
        }
    }

    public static boolean isConfigured() {
        return !Config.SUPABASE_URL.equals("https://hwgodqjlhevibonrvhlr.supabase.co") &&
                !Config.SUPABASE_ANON_KEY.equals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imh3Z29kcWpsaGV2aWJvbnJ2aGxyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA1MDA5MjEsImV4cCI6MjA3NjA3NjkyMX0.EQHr8wRsLTuPDnlxArtZ4CxFEeEIF6KdmNgwtGptWR4");
    }
}
