# SCRUM-25 — Inscription

Formulaire JavaFX natif et feuille CSS locale. Le formulaire de connexion
de main est conservé ; cette branche est indépendante de SCRUM-26.
Le conteneur commun est agrandi pour laisser de la place au formulaire.
La zone d'inscription défile si nécessaire, notamment lors de l'affichage d'erreurs.

## Installation et lancement

JDK 21, Maven 3.9+ et Docker Compose, depuis la racine :

```sh
docker compose up --build -d
mvn clean verify
mvn javafx:run
```

Ouvrir l'onglet « Create account ». L'appel REGISTER existant utilise
127.0.0.1:5050 et conserve le protocole TCP et l'accueil après succès.
« Déjà un compte ? Se connecter » revient à l'onglet de connexion.
Les erreurs du serveur restent affichées sous les onglets.

## Règles de saisie

Les règles reprennent AuthService : téléphone nettoyé avec + facultatif puis
8 à 15 chiffres, pseudo nettoyé et converti en minuscules (3 à 32 lettres ASCII,
chiffres ou tirets bas), nom affiché nettoyé de 1 à 80 caractères et mot de passe
d'au moins 8 caractères. Tous les champs sont obligatoires. Le mot de passe
n'est ni nettoyé ni normalisé. La confirmation existante est conservée.
Le serveur reste responsable de l'unicité du numéro et du pseudo.

## Scénarios de revue

- Champs vides, chaque champ manquant, téléphone incorrect, bornes 8/15 chiffres.
- Pseudo incorrect, bornes 3/32 caractères et normalisation des majuscules.
- Nom affiché vide/trop long, mot de passe court, confirmation différente.
- Correction des erreurs puis soumission, sans appel réseau pour une saisie invalide.
- Serveur indisponible ou compte déjà utilisé : erreur visible, nouvel essai possible.
- Création réussie : accueil avec le nom et le pseudo renvoyés, retour à la connexion.
- Navigation par le lien et au clavier ; défilement et fenêtre minimale.

## Vérification réalisée

- `mvn clean verify` et `git diff --check` : succès.
- Test graphique JavaFX sur macOS (JDK 27 disponible, compilation Java 21) :
  chaque champ obligatoire, numéros invalides et bornes 8/15, pseudos invalides
  et bornes 3/32, nom de 80/81 caractères, mot de passe de 7/8 caractères,
  confirmation différente, absence de requête pour chaque saisie invalide : succès.
- Serveur TCP simulé : erreur d'unicité affichée, bouton désactivé pendant la
  requête, protocole REGISTER et ordre des champs vérifiés, nettoyage/normalisation,
  accueil après succès, retour à la connexion, soumission par Entrée et panne réseau : succès.
- Rendu inspecté aux tailles initiale et minimale, ainsi qu'avec les erreurs visibles.
- Le démon Docker est indisponible : inscription persistée dans PostgreSQL,
  doublon réel et connexion avec le compte créé restent à vérifier.
- Base commune avec SCRUM-26 : main e3ec433, récupéré via le connecteur GitHub
  avec empreintes Git vérifiées après l'échec d'authentification du terminal.
