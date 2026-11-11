# ChaFlow

Client JavaFX 21, serveur TCP Java et données persistantes avec Docker Compose.

## Démarrage complet (macOS, Linux et Windows)

Prérequis : Docker Desktop démarré (ou Docker Engine avec le plugin Compose)
et un navigateur. Aucun JDK, Maven ou serveur X local n'est nécessaire.

Depuis la racine du dépôt :

```bash
docker compose up --build
```

Compose construit et lance automatiquement le serveur et le frontend JavaFX.
Le frontend attend que le serveur TCP soit disponible. Ouvrir ensuite :

<http://localhost:6080/vnc.html?autoconnect=true&resize=scale>

La fenêtre JavaFX s'affiche dans le navigateur via noVNC. Elle doit présenter
« Welcome to ChaFlow! » et « Connected · ChaFlow server is ready! ».
La souris et le clavier pilotent le bureau du conteneur : on peut déplacer,
redimensionner ou maximiser la fenêtre. Le rendu logiciel fonctionne aussi
sur Mac Apple Silicon, sans accès au GPU de l'hôte. Le frontend utilise
`linux/amd64` (émulé par Docker Desktop sur Apple Silicon), car JavaFX 21.0.6
ne publie pas les artefacts Linux ARM64 dans Maven Central. Le premier build
peut donc prendre quelques minutes. L'interface actuelle est
un écran d'accueil avec statut de connexion, sans saisie de messages.

Le port d'affichage `6080` est publié uniquement sur `127.0.0.1`.
Le serveur reste accessible sur le port TCP `5050`. Le frontend utilise le
nom Compose `chaflow-server`, pas le localhost du conteneur.

Pour démarrer en arrière-plan et vérifier l'état :

```bash
docker compose up --build -d
docker compose ps
docker compose logs chaflow-frontend chaflow-server
```

Pour relancer uniquement l'interface après avoir fermé sa fenêtre :

```bash
docker compose restart chaflow-frontend
```

Pour arrêter les services en conservant les données :

```bash
docker compose down
```

Le volume nommé `chaflow-data` conserve `/app/data/server.log`.
`docker compose down -v` supprime aussi ce volume et ses données.

## Développement JavaFX local (facultatif)

Avec JDK 21 et Maven 3.9+, `mvn clean javafx:run` lance le client local,
qui utilise par défaut `127.0.0.1:5050`. Ce lancement n'est pas nécessaire
pour le parcours Docker Compose.

## Configuration du frontend

- `docker/frontend/Dockerfile` compile le client et embarque les bibliothèques
  JavaFX Linux correspondant à l'architecture de construction.
- `docker/frontend/start.sh` lance Xvfb, Openbox, x11vnc, noVNC et JavaFX ;
  l'arrêt d'un de ces processus entraîne le redémarrage du conteneur.
- `docker-compose.yml` configure les deux services, leur ordre de démarrage,
  leurs contrôles de santé et le volume persistant.
