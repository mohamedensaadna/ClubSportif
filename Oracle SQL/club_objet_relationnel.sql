-- =====================================================================
-- Projet Bases de Donnees Avancees : modele objet-relationnel Oracle
-- Domaine : club sportif (club, salle, personne/membre/entraineur, seance)
--
-- Passage du modele relationnel genere au modele objet-relationnel :
--   rue, ville, code_postal          -> type adresse_t (composition, partage)
--   telephone (1 seul)               -> VARRAY telephones_va
--   specialite (1 seule)             -> VARRAY specialites_va
--   personne -> membre / entraineur  -> heritage de types (UNDER personne_t)
--   paiements d'un membre (nouveau)  -> Nested Table paiements_nt
--   participe (association N:N)      -> Nested Table de REF membre_t dans seance_t
--   cles etrangeres                  -> REF (club, salle, entraineur)
--
-- PL/SQL : uniquement 3 TYPE BODY avec 5 fonctions d'une seule instruction RETURN.
-- Aucun trigger, aucune procedure, aucun bloc anonyme.
-- Les tables sont au pluriel pour ne pas entrer en conflit avec les tables
-- relationnelles existantes (club, salle, seance, ...).
-- NB : pas de ligne vide a l'interieur d'une instruction (SQL*Plus).
-- =====================================================================

SET LINESIZE 200 PAGESIZE 100 LONG 5000
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

-- ---------------------------------------------------------------------
-- 0. Nettoyage (decommenter tout le bloc pour re-executer le script)
-- ---------------------------------------------------------------------
-- DROP TABLE seances PURGE;
-- DROP TABLE salles PURGE;
-- DROP TABLE membres PURGE;
-- DROP TABLE entraineurs PURGE;
-- DROP TABLE clubs PURGE;
-- DROP TYPE seance_t FORCE;
-- DROP TYPE membres_ref_nt FORCE;
-- DROP TYPE salle_t FORCE;
-- DROP TYPE club_t FORCE;
-- DROP TYPE entraineur_t FORCE;
-- DROP TYPE membre_t FORCE;
-- DROP TYPE personne_t FORCE;
-- DROP TYPE paiements_nt FORCE;
-- DROP TYPE paiement_t FORCE;
-- DROP TYPE specialites_va FORCE;
-- DROP TYPE telephones_va FORCE;
-- DROP TYPE adresse_t FORCE;

-- ---------------------------------------------------------------------
-- 1. Types simples et collections
-- ---------------------------------------------------------------------
CREATE OR REPLACE TYPE adresse_t AS OBJECT (
  rue         VARCHAR2(50),
  ville       VARCHAR2(50),
  code_postal VARCHAR2(10)
);
/

CREATE OR REPLACE TYPE telephones_va AS VARRAY(3) OF VARCHAR2(20);
/

CREATE OR REPLACE TYPE specialites_va AS VARRAY(5) OF VARCHAR2(30);
/

CREATE OR REPLACE TYPE paiement_t AS OBJECT (
  date_paiement DATE,
  montant       NUMBER(8,2)
);
/

CREATE OR REPLACE TYPE paiements_nt AS TABLE OF paiement_t;
/

-- ---------------------------------------------------------------------
-- 2. Types personne / membre / entraineur (heritage)
-- ---------------------------------------------------------------------
CREATE OR REPLACE TYPE personne_t AS OBJECT (
  id_personne    INTEGER,
  nom            VARCHAR2(50),
  prenom         VARCHAR2(50),
  telephones     telephones_va,
  date_naissance DATE,
  adresse        adresse_t,
  MEMBER FUNCTION nom_complet RETURN VARCHAR2,
  MEMBER FUNCTION age RETURN NUMBER
) NOT FINAL;
/

CREATE OR REPLACE TYPE BODY personne_t AS
  MEMBER FUNCTION nom_complet RETURN VARCHAR2 IS
  BEGIN
    RETURN prenom || ' ' || nom;
  END;
  MEMBER FUNCTION age RETURN NUMBER IS
  BEGIN
    RETURN TRUNC(MONTHS_BETWEEN(SYSDATE, date_naissance) / 12);
  END;
END;
/

CREATE OR REPLACE TYPE membre_t UNDER personne_t (
  num_licence      VARCHAR2(20),
  date_inscription DATE,
  paiements        paiements_nt,
  MEMBER FUNCTION anciennete_mois RETURN NUMBER
);
/

CREATE OR REPLACE TYPE BODY membre_t AS
  MEMBER FUNCTION anciennete_mois RETURN NUMBER IS
  BEGIN
    RETURN TRUNC(MONTHS_BETWEEN(SYSDATE, date_inscription));
  END;
END;
/

CREATE OR REPLACE TYPE entraineur_t UNDER personne_t (
  specialites specialites_va
);
/

-- ---------------------------------------------------------------------
-- 3. Types club / salle / seance (references)
-- ---------------------------------------------------------------------
CREATE OR REPLACE TYPE club_t AS OBJECT (
  id_club  INTEGER,
  nom_club VARCHAR2(50),
  adresse  adresse_t
);
/

CREATE OR REPLACE TYPE salle_t AS OBJECT (
  code_salle VARCHAR2(20),
  capacite   INTEGER,
  type_salle VARCHAR2(20),
  club       REF club_t
);
/

CREATE OR REPLACE TYPE membres_ref_nt AS TABLE OF REF membre_t;
/

CREATE OR REPLACE TYPE seance_t AS OBJECT (
  id_seance     INTEGER,
  date_seance   DATE,
  duree_minutes INTEGER,
  salle         REF salle_t,
  entraineur    REF entraineur_t,
  participants  membres_ref_nt,
  MEMBER FUNCTION nb_participants RETURN NUMBER,
  MEMBER FUNCTION date_fin RETURN DATE
);
/

CREATE OR REPLACE TYPE BODY seance_t AS
  MEMBER FUNCTION nb_participants RETURN NUMBER IS
  BEGIN
    RETURN CASE WHEN participants IS NULL THEN 0 ELSE participants.COUNT END;
  END;
  MEMBER FUNCTION date_fin RETURN DATE IS
  BEGIN
    RETURN date_seance + duree_minutes / 1440;
  END;
END;
/

-- ---------------------------------------------------------------------
-- 4. Tables typees (cles primaires, unicite, NOT NULL, CHECK, SCOPE)
-- ---------------------------------------------------------------------
CREATE TABLE clubs OF club_t (
  CONSTRAINT clubs_pk PRIMARY KEY (id_club),
  nom_club NOT NULL,
  CONSTRAINT clubs_nom_uq UNIQUE (nom_club)
);

CREATE TABLE salles OF salle_t (
  CONSTRAINT salles_pk PRIMARY KEY (code_salle),
  capacite NOT NULL,
  type_salle NOT NULL,
  club NOT NULL,
  CONSTRAINT salles_capacite_ck CHECK (capacite > 0),
  SCOPE FOR (club) IS clubs
);

CREATE TABLE membres OF membre_t (
  CONSTRAINT membres_pk PRIMARY KEY (id_personne),
  nom NOT NULL,
  prenom NOT NULL,
  num_licence NOT NULL,
  date_inscription NOT NULL,
  CONSTRAINT membres_licence_uq UNIQUE (num_licence)
) NESTED TABLE paiements STORE AS membres_paiements_nt;

CREATE TABLE entraineurs OF entraineur_t (
  CONSTRAINT entraineurs_pk PRIMARY KEY (id_personne),
  nom NOT NULL,
  prenom NOT NULL,
  specialites NOT NULL
);

CREATE TABLE seances OF seance_t (
  CONSTRAINT seances_pk PRIMARY KEY (id_seance),
  date_seance NOT NULL,
  duree_minutes NOT NULL,
  salle NOT NULL,
  entraineur NOT NULL,
  CONSTRAINT seances_duree_ck CHECK (duree_minutes > 0),
  SCOPE FOR (salle) IS salles,
  SCOPE FOR (entraineur) IS entraineurs
) NESTED TABLE participants STORE AS seances_participants_nt;

-- ---------------------------------------------------------------------
-- 5. Donnees de test (ordre : clubs, salles, personnes, seances)
-- ---------------------------------------------------------------------
INSERT INTO clubs VALUES (
  club_t(1, 'Club Sportif Central', adresse_t('10 Avenue Principale', 'Tunis', '1000'))
);

INSERT INTO salles
  SELECT salle_t('S01', 30, 'Fitness', REF(c)) FROM clubs c WHERE c.id_club = 1;

INSERT INTO salles
  SELECT salle_t('S02', 15, 'Yoga', REF(c)) FROM clubs c WHERE c.id_club = 1;

INSERT INTO membres VALUES (
  membre_t(1, 'Ben Ali', 'Sami', telephones_va('20111222', '71333444'),
           DATE '1998-05-12', adresse_t('5 Rue de la Liberte', 'Tunis', '1002'),
           'LIC001', DATE '2026-09-01',
           paiements_nt(paiement_t(DATE '2026-09-01', 50),
                        paiement_t(DATE '2026-10-01', 50)))
);

INSERT INTO membres VALUES (
  membre_t(3, 'Gharbi', 'Amine', telephones_va('22888999'),
           DATE '2001-11-03', adresse_t('12 Rue des Jasmins', 'Ariana', '2080'),
           'LIC002', DATE '2026-09-15',
           paiements_nt(paiement_t(DATE '2026-09-15', 50)))
);

INSERT INTO entraineurs VALUES (
  entraineur_t(2, 'Trabelsi', 'Leila', telephones_va('22555666'),
               DATE '1990-03-20', adresse_t('3 Rue du Stade', 'Tunis', '1001'),
               specialites_va('Musculation', 'Yoga'))
);

INSERT INTO seances
  SELECT seance_t(1, DATE '2026-10-20', 60, REF(s), REF(e),
                  CAST(MULTISET(SELECT REF(m) FROM membres m WHERE m.id_personne = 1)
                       AS membres_ref_nt))
  FROM salles s, entraineurs e
  WHERE s.code_salle = 'S01' AND e.id_personne = 2;

INSERT INTO seances
  SELECT seance_t(2, DATE '2026-10-22', 45, REF(s), REF(e),
                  CAST(MULTISET(SELECT REF(m) FROM membres m WHERE m.id_personne IN (1, 3))
                       AS membres_ref_nt))
  FROM salles s, entraineurs e
  WHERE s.code_salle = 'S02' AND e.id_personne = 2;

COMMIT;

-- ---------------------------------------------------------------------
-- 6. Requetes de demonstration
-- ---------------------------------------------------------------------

-- 6.1 Types et tables crees
SELECT type_name, typecode FROM user_types ORDER BY type_name;
SELECT table_name, table_type FROM user_object_tables ORDER BY table_name;

-- 6.2 Methodes des TAD (appel avec alias de table)
SELECT m.nom_complet() AS membre, m.age() AS age, m.anciennete_mois() AS anciennete_mois
FROM membres m;

SELECT e.nom_complet() AS entraineur, e.age() AS age FROM entraineurs e;

SELECT s.id_seance, s.date_seance, s.date_fin() AS fin, s.nb_participants() AS nb
FROM seances s;

-- 6.3 Type compose partage (adresse_t) : notation pointee
SELECT m.nom, m.adresse.ville AS ville FROM membres m;
SELECT c.nom_club, c.adresse.rue AS rue FROM clubs c;

-- 6.4 VARRAY : TABLE() pour deplier la collection
SELECT m.nom, t.COLUMN_VALUE AS telephone FROM membres m, TABLE(m.telephones) t;
SELECT e.nom, t.COLUMN_VALUE AS specialite FROM entraineurs e, TABLE(e.specialites) t;

-- 6.5 Nested Table : paiements de chaque membre
SELECT m.id_personne, m.prenom || ' ' || m.nom AS membre, SUM(p.montant) AS total_paye
FROM membres m, TABLE(m.paiements) p
GROUP BY m.id_personne, m.prenom, m.nom;

-- 6.6 REF : navigation entre objets (OID)
SELECT s.id_seance, s.salle.code_salle AS salle, s.salle.club.nom_club AS club,
       s.entraineur.nom AS entraineur
FROM seances s;

SELECT s.id_seance, e.nom_complet() AS entraineur
FROM seances s, entraineurs e
WHERE s.entraineur = REF(e);

-- 6.7 REF, VALUE et DEREF
SELECT REF(c) AS ref_club FROM clubs c;
SELECT VALUE(c) AS objet_club FROM clubs c;
SELECT s.id_seance, DEREF(s.salle) AS objet_salle FROM seances s;

-- 6.8 Participants d'une seance (Nested Table de REF + jointure sur REF)
SELECT s.id_seance, m.nom_complet() AS participant
FROM seances s, TABLE(s.participants) t, membres m
WHERE t.COLUMN_VALUE = REF(m)
ORDER BY s.id_seance;

-- 6.9 Ajouter un participant a la seance 1
INSERT INTO TABLE(SELECT s.participants FROM seances s WHERE s.id_seance = 1)
SELECT REF(m) FROM membres m WHERE m.id_personne = 3;

SELECT s.id_seance, s.nb_participants() AS nb FROM seances s;

-- 6.10 Seances dont le nombre de participants depasse la capacite de la salle
SELECT s.id_seance FROM seances s WHERE s.nb_participants() > s.salle.capacite;

COMMIT;

-- 6.11 Tests de contraintes (chacun doit echouer) :
-- INSERT INTO salles SELECT salle_t('S03', 0, 'Boxe', REF(c)) FROM clubs c WHERE c.id_club = 1;
--   -> ORA-02290 (CHECK capacite > 0)
-- INSERT INTO membres VALUES (membre_t(9, 'X', 'Y', NULL, NULL, NULL, 'LIC001', SYSDATE, NULL));
--   -> ORA-00001 (num_licence deja utilise)
