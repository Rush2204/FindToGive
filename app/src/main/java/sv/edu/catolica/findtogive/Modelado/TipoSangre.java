package sv.edu.catolica.findtogive.Modelado;

public class TipoSangre {
    private int tiposangreid;
    private String nombre;
    private boolean activo;

    public TipoSangre() {}

    public TipoSangre(int tiposangreid, String nombre, boolean activo) {
        this.tiposangreid = tiposangreid;
        this.nombre = nombre;
        this.activo = activo;
    }

    // Getters y Setters
    public int getTiposangreid() { return tiposangreid; }
    public void setTiposangreid(int tiposangreid) { this.tiposangreid = tiposangreid; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
