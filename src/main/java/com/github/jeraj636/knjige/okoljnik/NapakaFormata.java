package com.github.jeraj636.knjige.okoljnik;

public class NapakaFormata extends Exception {
    private final String msg;

    public NapakaFormata(String msg) {
        this.msg = msg;
    }

    @Override
    public String getMessage() {
        return msg;
    }
}