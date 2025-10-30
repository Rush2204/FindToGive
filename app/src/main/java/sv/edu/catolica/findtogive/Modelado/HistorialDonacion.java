package sv.edu.catolica.findtogive.Modelado;

public class HistorialDonacion {
    private int historialid;
    private int donanteid;
    private int receptorid;
    private String fechaDonacion;
    private String lugar;
    private String tipoDonacion;
    private String observaciones;

    public HistorialDonacion() {}

    public HistorialDonacion(int donanteid, int receptorid, String fechaDonacion,
                             String lugar, String tipoDonacion) {
        this.donanteid = donanteid;
        this.receptorid = receptorid;
        this.fechaDonacion = fechaDonacion;
        this.lugar = lugar;
        this.tipoDonacion = tipoDonacion;
    }

    // Getters y Setters
    public int getHistorialid() { return historialid; }
    public void setHistorialid(int historialid) { this.historialid = historialid; }

    public int getDonanteid() { return donanteid; }
    public void setDonanteid(int donanteid) { this.donanteid = donanteid; }

    public int getReceptorid() { return receptorid; }
    public void setReceptorid(int receptorid) { this.receptorid = receptorid; }

    public String getFechaDonacion() { return fechaDonacion; }
    public void setFechaDonacion(String fechaDonacion) { this.fechaDonacion = fechaDonacion; }

    public String getLugar() { return lugar; }
    public void setLugar(String lugar) { this.lugar = lugar; }

    public String getTipoDonacion() { return tipoDonacion; }
    public void setTipoDonacion(String tipoDonacion) { this.tipoDonacion = tipoDonacion; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
