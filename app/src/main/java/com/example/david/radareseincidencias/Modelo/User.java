package com.example.david.radareseincidencias.sampledata;

public class User {
    public String name, surname1, surname2, userName, email, profilePhoto; //Están públicos porque si no, no deja guardar en la base de datos.
    public int reputation, policia, accidente, lluvia, calleCortada, nieve, animales, asfalto, trafico, obras, radar;

    public User (String name, String surname1, String surname2, String userName, String email) {
        this.name = name;
        this.surname1 = surname1;
        this.surname2 = surname2;
        this.userName = userName;
        this.email = email;
        this.reputation = 0; //Cuando se crea un usuario, no tiene puntos
        this.profilePhoto = " "; //Cuando se registra, se pone la imagen de por defecto

        this.policia = 0;
        this.accidente = 0;
        this.lluvia = 0;
        this.calleCortada = 0;
        this.nieve = 0;
        this.asfalto = 0;
        this.obras = 0;
        this.trafico = 0;
        this.animales = 0;
        this.radar = 0;
    }
}
