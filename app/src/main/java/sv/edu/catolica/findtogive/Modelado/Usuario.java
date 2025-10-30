package sv.edu.catolica.findtogive.Modelado;

public class Usuario {
    private int usuarioid;
    private String nombre;
    private String apellido;
    private String email;
    private String contrasena;
    private int edad;
    private String telefono;
    private String ubicacion;
    private double latitud;
    private double longitud;
    private String foto_url; // Este es el nombre correcto del campo
    private boolean activo;
    private String fechaRegistro;
    private int rolid;
    private int tiposangreid;

    public Usuario() {}

    // Constructor para registro (sin usuarioid)
    public Usuario(String nombre, String apellido, String email, String contrasena,
                   int edad, String telefono, int rolid, int tiposangreid) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.contrasena = contrasena;
        this.edad = edad;
        this.telefono = telefono;
        this.rolid = rolid;
        this.tiposangreid = tiposangreid;
        this.activo = true;
    }

    // Getters y Setters...
    public int getUsuarioid() { return usuarioid; }
    public void setUsuarioid(int usuarioid) { this.usuarioid = usuarioid; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }

    public int getEdad() { return edad; }
    public void setEdad(int edad) { this.edad = edad; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public double getLatitud() { return latitud; }
    public void setLatitud(double latitud) { this.latitud = latitud; }

    public double getLongitud() { return longitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }

    // CORREGIDO: Usar foto_url en lugar de fotoUrl
    public String getFotoUrl() { return foto_url; }
    public void setFotoUrl(String foto_url) { this.foto_url = foto_url; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public String getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(String fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public int getRolid() { return rolid; }
    public void setRolid(int rolid) { this.rolid = rolid; }

    public int getTiposangreid() { return tiposangreid; }
    public void setTiposangreid(int tiposangreid) { this.tiposangreid = tiposangreid; }

    // Método útil para obtener nombre completo
    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    // Método para crear JSON sin usuarioid
    public String toJsonForRegistration() {
        return "{" +
                "\"nombre\":\"" + nombre + "\"," +
                "\"apellido\":\"" + apellido + "\"," +
                "\"email\":\"" + email + "\"," +
                "\"contrasena\":\"" + contrasena + "\"," +
                "\"edad\":" + edad + "," +
                "\"telefono\":\"" + telefono + "\"," +
                "\"rolid\":" + rolid + "," +
                "\"tiposangreid\":" + tiposangreid + "," +
                "\"activo\":true" +
                "}";
    }

    // Método para crear JSON para actualización (CORREGIDO)
    public String toJsonForUpdate() {
        StringBuilder json = new StringBuilder();
        json.append("{");

        // Solo incluir campos que se pueden actualizar
        if (nombre != null) json.append("\"nombre\":\"").append(nombre).append("\",");
        if (apellido != null) json.append("\"apellido\":\"").append(apellido).append("\",");
        if (email != null) json.append("\"email\":\"").append(email).append("\",");
        if (telefono != null) json.append("\"telefono\":\"").append(telefono).append("\",");
        if (ubicacion != null) json.append("\"ubicacion\":\"").append(ubicacion).append("\",");
        json.append("\"edad\":").append(edad).append(",");
        json.append("\"rolid\":").append(rolid).append(",");
        json.append("\"tiposangreid\":").append(tiposangreid);

        // CORREGIDO: Usar foto_url en lugar de fotoUrl
        if (foto_url != null && !foto_url.isEmpty()) {
            json.append(",\"foto_url\":\"").append(foto_url).append("\"");
        }

        json.append("}");

        return json.toString();
    }

    // Método para actualizar solo los campos editables
    public void updateFrom(Usuario updatedUser) {
        this.nombre = updatedUser.getNombre();
        this.apellido = updatedUser.getApellido();
        this.email =updatedUser.getEmail();
        this.telefono = updatedUser.getTelefono();
        this.ubicacion = updatedUser.getUbicacion();
        this.edad = updatedUser.getEdad();
        this.rolid = updatedUser.getRolid();
        this.tiposangreid = updatedUser.getTiposangreid();
        if (updatedUser.getFotoUrl() != null) {
            this.foto_url = updatedUser.getFotoUrl();
        }
    }
}
