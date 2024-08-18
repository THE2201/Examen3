package com.example.examentresabel;

import java.io.Serializable;

public class EntrevistaModel implements Serializable {
    private String idOrden;
    private String descripcion;
    private String periodista;
    private String fecha;
    private String imagenUrl;
    private String audioUrl;

    public EntrevistaModel(String idOrden, String descripcion, String periodista, String fecha, String imagenUrl, String audioUrl) {
        this.idOrden = idOrden;
        this.descripcion = descripcion;
        this.periodista = periodista;
        this.fecha = fecha;
        this.imagenUrl = imagenUrl;
        this.audioUrl = audioUrl;
    }
    public EntrevistaModel() {}

    public EntrevistaModel(String descripcion, String periodista, String fecha, String imagenUrl, String audioUrl) {
    }

    public String getIdOrden() {
        return idOrden;
    }
    public String getDescripcion() {
        return descripcion;
    }
    public String getPeriodista() {
        return periodista;
    }
    public String getFecha() {
        return fecha;
    }
    public String getImagenUrl() {
        return imagenUrl;
    }
    public String getAudioUrl() {
        return audioUrl;
    }

}
