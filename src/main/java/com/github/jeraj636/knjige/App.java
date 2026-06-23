package com.github.jeraj636.knjige;

import java.io.IOException;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jeraj636.knjige.baza.Baza;

public class App {
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        try {
            Baza.init();
            //Inicializacija baze
            Baza.urediBesede();
        } catch (IOException | SQLException e) {
            LOGGER.error(e.getMessage());
        }
    }
}
