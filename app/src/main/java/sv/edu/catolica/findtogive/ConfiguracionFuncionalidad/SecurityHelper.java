package sv.edu.catolica.findtogive.ConfiguracionFuncionalidad;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class SecurityHelper {

    /**
     * Hashea una contraseña usando BCrypt
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }
        return BCrypt.withDefaults().hashToString(12, password.toCharArray());
    }

    /**
     * Verifica si una contraseña coincide con el hash almacenado
     */
    public static boolean verifyPassword(String password, String hashedPassword) {
        if (password == null || hashedPassword == null ||
                password.isEmpty() || hashedPassword.isEmpty()) {
            return false;
        }

        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), hashedPassword);
        return result.verified;
    }

    /**
     * Valida la fortaleza de la contraseña
     */
    public static boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        // Al menos una mayúscula, una minúscula, un número
        String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$";
        return password.matches(passwordPattern);
    }
}
