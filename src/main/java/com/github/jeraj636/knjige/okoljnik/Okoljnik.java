package com.github.jeraj636.knjige.okoljnik;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Okoljnik {
    private static final String DOTENV = ".env";
    private static final Logger LOGGER = LoggerFactory.getLogger(Okoljnik.class);

    private Okoljnik() {
        // ker je statično
    }

    private static final Map<String, String> slovar = new HashMap<>();

    public static String get(String key) {
        return slovar.get(key);
    }

    public static boolean containsKey(String key) {
        return slovar.containsKey(key);
    }

    private static boolean aliJeInicializiran = false;

    public static void init() throws IOException, NapakaFormata {
        //Ok ker nisem našel knjižnice bom to pač počel ročno
        if (aliJeInicializiran)
            return;
        aliJeInicializiran = true;
        try (Scanner sc = new Scanner(new File(DOTENV))) {
            while (sc.hasNextLine()) {
                String vrstica = sc.nextLine();
                if (!vrstica.startsWith("#")) { // ignoriramo komentarje
                    String[] vrsticaSplit = vrstica.split("="); // Vse ostale morajo biti v pravem formatu
                    if (vrsticaSplit == null || vrsticaSplit.length != 2) {
                        throw new NapakaFormata("Datoteka je v napačnem formatu");
                    }
                    slovar.put(vrsticaSplit[0], vrsticaSplit[1]);
                }
            }

        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            throw new IOException("Ni .env datoteke");
        } catch (NullPointerException e) {
            LOGGER.error(e.getMessage());
            throw new NapakaFormata("Datoteka je v napačnem formatu");
        }
    }
}
