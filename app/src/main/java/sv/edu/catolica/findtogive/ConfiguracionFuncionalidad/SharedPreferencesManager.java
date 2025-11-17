package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import sv.edu.catolica.findtogive.Modelado.Usuario;

public class SharedPreferencesManager {
    private static final String PREF_NAME = "FindToGivePrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_DATA = "userData";

    public static void saveUser(Context context, Usuario usuario) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Gson gson = new Gson();
        String userJson = gson.toJson(usuario);

        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_DATA, userJson);
        editor.apply();
    }

    // NUEVO: Método alias para mantener compatibilidad
    public static void saveCurrentUser(Context context, Usuario usuario) {
        saveUser(context, usuario);
    }

    public static Usuario getCurrentUser(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String userJson = prefs.getString(KEY_USER_DATA, null);

        if (userJson != null) {
            Gson gson = new Gson();
            return gson.fromJson(userJson, Usuario.class);
        }
        return null;
    }

    public static boolean isLoggedIn(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public static void logout(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }
}