package sv.edu.catolica.findtogive.Modelado;

import com.google.gson.annotations.SerializedName;

public class SolicitudDonacion {
    private int solicitudid;
    private int usuarioid;
    private String titulo;
    private String descripcion;
    private int tiposangreid;
    private String ubicacion;

    @SerializedName("imagen_url")
    private String imagen_url;

    @SerializedName("fecha_publicacion")  // <- AÑADE ESTA LÍNEA
    private String fecha_publicacion;

    private String estado;

    public SolicitudDonacion() {}

    // Constructor para crear nueva solicitud
    public SolicitudDonacion(int usuarioid, String titulo, String descripcion,
                             int tiposangreid, String ubicacion) {
        this.usuarioid = usuarioid;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.tiposangreid = tiposangreid;
        this.ubicacion = ubicacion;
        this.estado = "activa";
    }

    // Getters y Setters (MANTÉN LOS NOMBRES ACTUALES)
    public int getSolicitudid() { return solicitudid; }
    public void setSolicitudid(int solicitudid) { this.solicitudid = solicitudid; }

    public int getUsuarioid() { return usuarioid; }
    public void setUsuarioid(int usuarioid) { this.usuarioid = usuarioid; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public int getTiposangreid() { return tiposangreid; }
    public void setTiposangreid(int tiposangreid) { this.tiposangreid = tiposangreid; }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public String getImagenUrl() { return imagen_url; }
    public void setImagenUrl(String imagenUrl) { this.imagen_url = imagenUrl; }

    public String getFechaPublicacion() { return fecha_publicacion; }
    public void setFechaPublicacion(String fechaPublicacion) { this.fecha_publicacion = fechaPublicacion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
