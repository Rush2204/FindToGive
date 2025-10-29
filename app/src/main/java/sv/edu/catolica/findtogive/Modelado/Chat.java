package sv.edu.catolica.findtogive.Modelado;

public class Chat {
    private int chatid;
    private int usuario1id;
    private int usuario2id;
    private int solicitudid;
    private String fecha_creacion;

    public Chat() {}

    public Chat(int usuario1id, int usuario2id, int solicitudid) {
        this.usuario1id = usuario1id;
        this.usuario2id = usuario2id;
        this.solicitudid = solicitudid;
    }

    // Getters y Setters
    public int getChatid() { return chatid; }
    public void setChatid(int chatid) { this.chatid = chatid; }

    public int getUsuario1id() { return usuario1id; }
    public void setUsuario1id(int usuario1id) { this.usuario1id = usuario1id; }

    public int getUsuario2id() { return usuario2id; }
    public void setUsuario2id(int usuario2id) { this.usuario2id = usuario2id; }

    public int getSolicitudid() { return solicitudid; }
    public void setSolicitudid(int solicitudid) { this.solicitudid = solicitudid; }

    public String getFechaCreacion() { return fecha_creacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fecha_creacion = fechaCreacion; }
}
