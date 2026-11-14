# SCRUM-26 — Connexion

## Choix et installation

Le client utilise JavaFX Controls 21.0.6, sans FXML ni moteur HTML.
La [documentation officielle Tailwind CLI](https://tailwindcss.com/docs/installation/tailwind-cli)
décrit la génération de CSS depuis des modèles HTML. La
[documentation JavaFX WebView 21](https://openjfx.io/javadoc/21/javafx.web/javafx/scene/web/WebView.html)
décrit un composant affichant des pages web, à manipuler sur le thread JavaFX.
Ces outils ont été étudiés ; le choix retenu est de conserver les contrôles JavaFX
et de charger une feuille CSS JavaFX locale, compatible avec le projet existant.
Il n'y a donc aucune migration HTML, installation npm ou dépendance javafx-web.

Avec JDK 21, Maven 3.9+ et Docker Compose, depuis la racine :

```sh
docker compose up --build -d
mvn clean verify
mvn javafx:run
```

Le serveur est utilisé sans changement sur 127.0.0.1:5050.
Les paramètres JVM chaflow.host et chaflow.port restent disponibles dans SocketClient.

## Parcours à vérifier

- Champs vides : erreur locale, focus sur le premier champ manquant, aucun appel TCP.
- Téléphone seul / mot de passe seul : message adapté ; correction possible.
- Entrée depuis le téléphone déplace le focus vers le mot de passe ; Entrée soumet.
- Mauvais identifiants et serveur indisponible : message serveur/réseau conservé.
- Compte de démonstration +212600000001 / ChatFlow123! : écran d'accueil avec
  pseudo et nom renvoyés par le serveur ; retour à la connexion.
- Lien « Créer un compte » : ouverture de l'onglet d'inscription existant.
- Fenêtre minimale et navigation clavier : libellés, focus et erreurs lisibles.

L'appel LOGIN et le traitement asynchrone existants sont conservés.
Cette branche part de main e3ec433 et ne contient pas le redesign de l'inscription.

## Vérification initiale (serveur simulé)

- `mvn clean verify` et `git diff --check` : succès.
- Test JavaFX exécuté sur macOS (JDK 27 disponible, compilation Java 21), avec
  serveur TCP simulé : champs obligatoires sans requête réseau, bouton désactivé
  pendant l'attente, erreur serveur affichée, requête LOGIN exacte (téléphone
  nettoyé, mot de passe intact), accueil après succès et retour, lien inscription,
  soumission par Entrée et erreur réseau après arrêt du serveur : succès.
- Rendu inspecté à la taille initiale et à la taille minimale.
- Le démon Docker est indisponible : le parcours avec le serveur réel et
  PostgreSQL reste à vérifier avec les comptes de démonstration.
- La synchronisation SSH/HTTPS du terminal a échoué. Les objets manquants de main
  ont été récupérés par le connecteur GitHub et leurs empreintes Git vérifiées
  avant une avance rapide locale. Aucun changement d'URL d'origin.


## Validation avec PostgreSQL — 12 novembre 2026

- Docker Compose du dépôt démarré avec PostgreSQL 15 et le serveur réel sur 5050 ;
  migrations Flyway V1/V2 réussies et volumes conservés.
- `mvn clean verify`, `mvn javafx:run` et `git diff --check` : succès.
- Test d'intégration JavaFX (contrôles réels, aucune simulation TCP) : champs
  obligatoires, erreur après mot de passe incorrect, connexion avec le compte
  fictif créé par SCRUM-25, accueil, retour à la connexion et lien inscription : succès.
- Compte local vérifié : numéro +999506842180, pseudo qa_scrum25_506842180,
  identifiant 5. PostgreSQL confirme EN_LIGNE et une date de dernière connexion.
- Correction du nom tronqué sur l'accueil : le titre et les détails peuvent
  revenir à la ligne. Rendu après correction inspecté par capture JavaFX.
- Tests exécutés sous macOS/JDK 27, cible de compilation Java 21. Le lancement
  Maven a été vérifié ; le test d'intégration utilise un lanceur JavaFX séparé.
- Accès SSH rétabli par chargement de la clé dans l'agent utilisateur.
