package sv.edu.catolica.findtogive.Modelado;

public class Notificacion {
    private int notificacionid;
    private int usuarioid;
    private String titulo;
    private String mensaje;
    private String fechaEnvio;
    private boolean leida;

    public Notificacion() {}

    public Notificacion(int usuarioid, String titulo, String mensaje) {
        this.usuarioid = usuarioid;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.leida = false;
    }

    // Getters y Setters
    public int getNotificacionid() { return notificacionid; }
    public void setNotificacionid(int notificacionid) { this.notificacionid = notificacionid; }

    public int getUsuarioid() { return usuarioid; }
    public void setUsuarioid(int usuarioid) { this.usuarioid = usuarioid; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getFechaEnvio() { return fechaEnvio; }
    public void setFechaEnvio(String fechaEnvio) { this.fechaEnvio = fechaEnvio; }

    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }
}
