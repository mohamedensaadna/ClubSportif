# Gestion de club sportif

Application de bureau Java Swing pour gérer les membres, les entraîneurs, les séances, les clubs, les salles et les paiements. Les données sont stockées dans une base Oracle à l'aide d'un modèle objet-relationnel.

## Prérequis

- JDK 21
- Maven
- Une base Oracle compatible, accessible depuis l'application

## Base de données

Le script de création du schéma et des données de démonstration se trouve dans [`Oracle SQL/club_objet_relationnel.sql`](Oracle%20SQL/club_objet_relationnel.sql).

Exécutez-le avec un outil Oracle, connecté au schéma qui hébergera les tables et les types. Le script crée les objets et insère des données de démonstration ; il est prévu pour un schéma neuf et ne doit pas être relancé tel quel sur un schéma déjà initialisé.

## Configuration de la connexion

Configurez ces variables d'environnement avant de lancer l'application :

| Variable | Valeur par défaut ou description |
| --- | --- |
| `CLUB_DB_URL` | `jdbc:oracle:thin:@//localhost:1521/FREEPDB1` |
| `CLUB_DB_USER` | `saadna` |
| `CLUB_DB_PWD` | Mot de passe du compte Oracle ; obligatoire |

Exemple sous Linux/macOS :

```sh
export CLUB_DB_URL='jdbc:oracle:thin:@//localhost:1521/FREEPDB1'
export CLUB_DB_USER='votre_utilisateur'
export CLUB_DB_PWD='votre_mot_de_passe'
```

Ne publiez jamais un vrai mot de passe dans le dépôt.

## Compilation et lancement

À la racine du projet :

```sh
mvn clean package
java -jar target/ClubSportif-1.0.jar
```

Le projet utilise Maven pour télécharger les dépendances, notamment le pilote JDBC Oracle et FlatLaf.

## Structure du projet

- `src/main/java/club/` : code source Java
- `src/main/resources/` : ressources intégrées à l'application, dont le logo
- `Oracle SQL/` : script Oracle de création et de démonstration
- `pom.xml` : configuration Maven
