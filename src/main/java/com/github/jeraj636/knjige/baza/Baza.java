package com.github.jeraj636.knjige.baza;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.github.jeraj636.knjige.beseda.Beseda;
import com.github.jeraj636.knjige.okoljnik.NapakaFormata;
import com.github.jeraj636.knjige.okoljnik.Okoljnik;

public class Baza {
    private static String server;
    private static String pb;
    private static String uporabnik;
    private static String geslo;

    private static final Logger LOGGER = LoggerFactory.getLogger(Baza.class);

    private Baza() {
    }

    public static void init() throws IOException, SQLException {

        //Branje okoljskih spremenjivk
        try {
            Okoljnik.init();

            // Nastavlanje potrebnih iz Okoljnika
            server = Okoljnik.containsKey("SERVER") ? Okoljnik.get("SERVER") : "localhost";
            pb = Okoljnik.containsKey("PB") ? Okoljnik.get("PB") : "knjige";
            uporabnik = Okoljnik.containsKey("UPORABNIK_PB") ? Okoljnik.get("UPORABNIK_PB") : "root";
            geslo = Okoljnik.containsKey("GESLO_PB") ? Okoljnik.get("GESLO_PB") : "";

        } catch (NapakaFormata | IOException e) {
            LOGGER.error(e.getMessage());
            throw new IOException("Napaka pri branju okoljskih spremenjivk");
        }

        LOGGER.info("Inicializacija baze: {}@{} kot {}", pb, server, uporabnik);

        //Tukej je končana inicializacija pomembnih spremenjivk

        //Prevejanje dosegljivosti baze
        try (Connection conn = poveziSe();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT 1")) {
            if (rs.next() && rs.getInt(1) == 1) {
                LOGGER.info("Povezavo je možno vzpostaviti");
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            throw new SQLException("Napaka pri testiranju povezljivosti");
        }

        //Preverjanje strukture PB
        final String SHEMA_SQL_POT = "/sql/shema.sql";
        try {
            if (!preveriStrikturo()) {
                LOGGER.info("V bazi tabele niso pravilno postavljene");
                //V tem primeru moramo strukturo postaviti
                izvediSqlSkripto(SHEMA_SQL_POT);
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            throw new SQLException("SQL napaka pri postavljanju baze");
        }

    }

    private static void izvediSqlSkripto(String skripta) throws SQLException, IOException {
        try (InputStream is = Baza.class.getResourceAsStream(skripta);
                Connection povezava = poveziSe();
                Statement stmt = povezava.createStatement()) {

            if (is == null) {
                LOGGER.error("Napaka pri branju skripte");
                return;
            }

            String sql = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            stmt.execute(sql);

        } catch (IOException e) {
            throw new IOException(String.format("Napaka pri branju datoteke %s", skripta), e);
        } catch (SQLException e) {
            throw new SQLException(String.format("Napaka pri izvajanju datoteke %s", skripta), e);
        }
    }

    private static boolean preveriStrikturo() throws SQLException {
        HashSet<String> pricakovano = new HashSet<>(Set.of("besede_knjige", "besede", "knjige", "avtorji"));
        HashSet<String> dobljeno = new HashSet<>();
        try (Connection povezava = poveziSe();
                Statement stmt = povezava.createStatement();
                ResultSet rs = stmt.executeQuery("SHOW TABLES")) {
            while (rs.next())
                dobljeno.add(rs.getString(1));
            return pricakovano.equals(dobljeno);
        }

    }

    public static void urediBesede() throws IOException, SQLException {
        if (aliJeTrebaVpisovatiBesede()) {
            LOGGER.info("Zapisujem besede v bazo ...");
            naloziBesede();
        }
        LOGGER.info("Besede so zapisane v bazo");
    }

    private static boolean aliJeTrebaVpisovatiBesede() throws SQLException {
        final int PRICAKOVANO_ST_BESED = 96131;
        try (Connection povezava = poveziSe();
                Statement stmt = povezava.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM besede")) {
            return !(rs.next() && rs.getInt(1) >= PRICAKOVANO_ST_BESED);

        }
    }

    private static void naloziBesede() throws IOException, SQLException {
        final String BESEDNA_VRSTA_TAG = "BV";
        final String IZTOCNICA_TAG = "NIZT";
        final String ukaz = "INSERT IGNORE INTO besede(beseda,besedna_vrsta) VALUES(?,?)";

        try (InputStream xmlStream = Baza.class.getResourceAsStream("/odzadnji/odzadnji.xml");
                Connection povezava = poveziSe();
                PreparedStatement stmt = povezava.prepareStatement(ukaz)) {
            povezava.setAutoCommit(false);
            SAXParserFactory factory = SAXParserFactory.newInstance();
            // SonarQube zahteva:
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            SAXParser saxParser = factory.newSAXParser();
            DefaultHandler handler = new DefaultHandler() {
                private final Beseda beseda = new Beseda();
                private String trenutniElement = "";
                private String prejsnjiElement = "";
                private final StringBuilder vsebina = new StringBuilder();
                private int stevec = 0;

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) {
                    prejsnjiElement = trenutniElement;
                    trenutniElement = qName;

                    vsebina.setLength(0);
                }

                @Override
                public void characters(char[] ch, int start, int length) throws SAXException {
                    if (trenutniElement.equals(IZTOCNICA_TAG) || prejsnjiElement.equals(BESEDNA_VRSTA_TAG))
                        vsebina.append(ch, start, length);
                }

                @Override
                public void endElement(String uri, String localName, String qName) {
                    if (qName.equals(IZTOCNICA_TAG)) {
                        beseda.setVsebina(vsebina.toString().trim());
                    }
                    if (prejsnjiElement.endsWith(BESEDNA_VRSTA_TAG)) {
                        beseda.setBesednaVrsta(vsebina.toString().trim());
                        try {
                            stmt.setString(1, beseda.getVsebina());
                            stmt.setString(2, beseda.getBesednaVrsta());
                            stmt.addBatch();
                            if (++stevec % 1000 == 0)
                                stmt.executeBatch();
                        } catch (SQLException e) {
                            LOGGER.error("Napaka pri zapisu besede v PB");
                        }
                    }
                    vsebina.setLength(0);
                    prejsnjiElement = "";

                }

                @Override
                public void endDocument() {
                    try {
                        stmt.executeBatch();

                    } catch (SQLException e) {
                        LOGGER.error("Napaka pri zapisu besede v PB");
                    }
                }
            };

            saxParser.parse(xmlStream, handler);

            povezava.commit();
        } catch (SAXException | ParserConfigurationException e) {
            throw new IOException("Napaka pri parsanju XML");
        }

    }

    private static Connection poveziSe() throws SQLException {
        String url = "jdbc:mariadb://" + server + "/" + pb + "?allowMultiQueries=true";
        try {
            return DriverManager.getConnection(url, uporabnik, geslo);
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            String napaka = String.format("Napaka pri povezovanju na: %s@%s kot %s", pb, server, uporabnik);
            throw new SQLException(napaka);
        }
    }
}
