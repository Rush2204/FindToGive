package sv.edu.catolica.findtogive.Modelado;

public class HospitalUbicacion {
    private int hospitalid;
    private String nombre;
    private String link;
    private double latitud;
    private double longitud;

    public HospitalUbicacion() {}

    public HospitalUbicacion(int hospitalid, String nombre, String link, double latitud, double longitud) {
        this.hospitalid = hospitalid;
        this.nombre = nombre;
        this.link = link;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    // Getters y Setters
    public int getHospitalid() { return hospitalid; }
    public void setHospitalid(int hospitalid) { this.hospitalid = hospitalid; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public double getLatitud() { return latitud; }
    public void setLatitud(double latitud) { this.latitud = latitud; }

    public double getLongitud() { return longitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }

    @Override
    public String toString() {
        return nombre;
    }
}
