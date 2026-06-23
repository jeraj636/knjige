DROP TABLE IF EXISTS besede_knjige;
DROP TABLE IF EXISTS besede;
DROP TABLE IF EXISTS knjige;
DROP TABLE IF EXISTS avtorji;

CREATE TABLE avtorji (
    avtor_id INT AUTO_INCREMENT PRIMARY KEY,
    ime VARCHAR(30) NOT NULL,
    priimek VARCHAR(30) NOT NULL
);

CREATE TABLE knjige (
    knjiga_id INT AUTO_INCREMENT PRIMARY KEY,
    naslov VARCHAR(40) NOT NULL,
    avtor_id INT NOT NULL,
    leto_nastanka INT,
    CONSTRAINT fk_avtor_knjige
        FOREIGN KEY (avtor_id) REFERENCES avtorji(avtor_id)
);

CREATE TABLE  besede (
    beseda_id INT AUTO_INCREMENT PRIMARY KEY,
    beseda VARCHAR(30) NOT NULL,
    besedna_vrsta VARCHAR(30) NOT NULL,
    CONSTRAINT u_besedna_vrsta UNIQUE(beseda, besedna_vrsta)
);

CREATE TABLE besede_knjige (
    knjiga_id INT NOT NULL,
    beseda_id INT NOT NULL,
    pojavitve INT NOT NULL,
    PRIMARY KEY(knjiga_id,beseda_id),
    CONSTRAINT fk_knjiga_besede_knjige
        FOREIGN KEY (knjiga_id) REFERENCES knjige(knjiga_id),
    CONSTRAINT fk_beseda_besede_knjige
        FOREIGN KEY (beseda_id) REFERENCES besede(beseda_id)
);
