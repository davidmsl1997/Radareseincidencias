package com.example.david.radareseincidencias.sampledata;

import com.google.android.gms.maps.model.LatLng;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Incidencia {
    //Los atributos tienen que ser públicos para que se puedan guardar en Firebase
    public String tipo; //accidente, radar, obras, ...
    public String usuario;
    public String calle;
    public String fecha;
    public double latitud;
    public double longitud;
    public Integer velocidad; //Solo para los radares
    public Integer cuentaEliminado;

    public Incidencia(String tipo, String usuario, String calle, double latitud, double longitud){
        this.tipo = tipo;
        this.usuario = usuario;
        this.calle = calle;

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        this.fecha = formatter.format(date);
        this.latitud = latitud;
        this.longitud = longitud;
        this.cuentaEliminado = 0;

        this.velocidad = null;
    }

    public Incidencia(String tipo, String usuario, String calle, double latitud, double longitud, int velocidad){
        this.tipo = tipo;
        this.usuario = usuario;
        this.calle = calle;

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        this.fecha = formatter.format(date);
        this.latitud = latitud;
        this.longitud = longitud;
        this.cuentaEliminado = 0;

        this.velocidad = velocidad;
    }
}
