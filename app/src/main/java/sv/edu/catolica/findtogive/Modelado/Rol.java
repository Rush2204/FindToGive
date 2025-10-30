package sv.edu.catolica.findtogive.Modelado;

public class Rol {
    private int rolid;
    private String nombre;
    private boolean activo;

    public Rol() {}

    public Rol(int rolid, String nombre, boolean activo) {
        this.rolid = rolid;
        this.nombre = nombre;
        this.activo = activo;
    }

    // Getters y Setters
    public int getRolid() { return rolid; }
    public void setRolid(int rolid) { this.rolid = rolid; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
