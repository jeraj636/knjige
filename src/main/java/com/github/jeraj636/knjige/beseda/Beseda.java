package com.github.jeraj636.knjige.beseda;

public class Beseda extends Object {
    private String vsebina;
    private String besednaVrsta;

    public Beseda() {
        vsebina = "";
        besednaVrsta = "";
    }

    public void setBesednaVrsta(String besednaVrsta) {
        this.besednaVrsta = besednaVrsta;
    }

    public void setVsebina(String vsebina) {
        this.vsebina = vsebina;
    }

    public String getVsebina() {
        return vsebina;
    }

    public String getBesednaVrsta() {
        return besednaVrsta;
    }

    @Override
    public String toString() {
        return String.format("%s %s", vsebina, besednaVrsta);
    }

    public boolean isEmpty() {
        return vsebina.isEmpty() && besednaVrsta.isEmpty();
    }

}
