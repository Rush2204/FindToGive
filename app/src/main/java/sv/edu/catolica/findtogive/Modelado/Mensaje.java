package sv.edu.catolica.findtogive.Modelado;

public class Mensaje {
    private int mensajeid;
    private int chatid;
    private int emisorioid;
    private String contenido;
    private String fecha_envio;
    private boolean leido;

    public Mensaje() {}

    public Mensaje(int chatid, int emisorioid, String contenido) {
        this.chatid = chatid;
        this.emisorioid = emisorioid;
        this.contenido = contenido;
        this.leido = false;
    }

    // Getters y Setters
    public int getMensajeid() { return mensajeid; }
    public void setMensajeid(int mensajeid) { this.mensajeid = mensajeid; }

    public int getChatid() { return chatid; }
    public void setChatid(int chatid) { this.chatid = chatid; }

    public int getEmisorioid() { return emisorioid; }
    public void setEmisorioid(int emisorioid) { this.emisorioid = emisorioid; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }

    public String getFechaEnvio() { return fecha_envio; }
    public void setFechaEnvio(String fechaEnvio) { this.fecha_envio = fechaEnvio; }

    public boolean isLeido() { return leido; }
    public void setLeido(boolean leido) { this.leido = leido; }
}