# FranceTransfert

France transfert est un service créé par l'Etat pour aider ses usagers (citoyens, professionnels, entreprises, associations…), partenaires ou prestataires à envoyer (aux agents de l'Etat) ou recevoir (des agents de l'Etat), des fichiers et dossiers volumineux qui ne peuvent pas transiter par les messageries électroniques. Il a été conçu et est opéré par le ministère de la Culture, et mis à la disposition de tous les ministères par la direction interministérielle du numérique (DINUM).

## Fonctionnalités

Les principales fonctionnalités d'ores et déjà développées permettent :

* Echange de plis pouvant atteindre 20 Go, composés de fichiers avec une taille de 2 Go maximum;
* Vérification lors du premier envoi avec le code de confirmation (valide 20 min), l'usager est connecté par défaut avec une session active de 30 minutes : adresse mèl expéditeur pré-renseigné, pas de saisie de nouveau code de confirmation pour l'envoi d'un pli;
* Si connecté, l'usager a accès à son historique des plis envoyés actifs ainsi que ceux reçus;
* Administration du pli :
  * Pouvoir supprimer le pli,
  * Ajouter/supprimer un destinataire,
  * Avoir le nombre de téléchargements par destinataire ainsi que l'horodatage des téléchargements;
* La validité du pli peut être paramétrée d'un jour à 90 jours;
* Le mot de passe d'accès au pli peut être personnalisé par l'expéditeur;
* Mise en place de la pastille SNAP (Rizomo).

## Documentation et support utilisateurs

Une Foire Aux Questions est disponible ici : https://francetransfert.numerique.gouv.fr/faq

Il est également possible de contacter le support Utilisateurs dont le niveau 1 est assuré par la DINUM : https://francetransfert.numerique.gouv.fr/contact

Enfin pour les agents de l'Etat, il y a un salon "France transfert" dans TCHAP qui vous permettra de suivre les évolutions ou d'échanger avec les autres membres. Vous êtes libres de vous y abonner.

## Description des répertoires

- **francetransfert-core/** : Composants centraux et logique métier principale de FranceTransfert
  - Technologies : Java

- **francetransfert-upload-api/** : API dédiée à la gestion des téléversements de fichiers
  - Technologies : Java

- **francetransfert-download-api/** : API dédiée à la gestion des téléchargements de fichiers
  - Technologies : Java

- **francetransfert-upload-download-gui/** : Interface utilisateur web pour le téléversement et téléchargement de fichiers
  - Technologies : Angular

- **francetransfert-worker/** : Service de traitement asynchrone des tâches en arrière-plan
  - Technologies : Java

- **francetransfert-backup/** : Scripts et outils pour la gestion des sauvegardes
  - Technologies : Docker

- **ft-helm/** : Charts Helm pour le déploiement sur Kubernetes
  - Technologies : Helm, Kubernetes

## Démarrage local (Docker Compose)

Un `docker-compose.yml` à la racine fournit une pile complète pour tester en local : Redis, MinIO (S3 compatible), MailCatcher, Keycloak, ClamAV, les 3 APIs Java et la GUI. Aucun JDK n'est requis sur l'hôte (build Maven dans Docker).

```bash
cp .env.model .env.local
```

Deux modes au choix :

### Mode « fixe » (images fixes à rebuilder si modifications)

Les images Java et GUI sont construites via leur Dockerfile, puis exécutées. Aucun "watch", chaque modification de code demande un `docker compose build` du service concerné. À utiliser pour valider une intégration ou un déploiement.

```bash
docker compose --env-file .env.local up
```

### Mode « dev » (hot reload)

Nous proposons une surcharge dans `docker-compose.dev.yml` qui remplace les conteneurs Java et GUI par des conteneurs Maven / Node avec source montée et reload automatique.

- **GUI** : `ng serve` avec proxy vers les APIs. Reload navigateur à chaque sauvegarde `.ts/.html/.scss`
- **Java** : `mvn spring-boot:run` avec Spring DevTools + watcher `entr` qui lance `mvn compile -o` sur modification `.java/.properties/.xml`

```bash
docker compose --env-file .env.local \
  -f docker-compose.yml -f docker-compose.dev.yml \
  up gui upload-api download-api worker
```

À noter :
- Le module partagé `francetransfert-core` est installé via `mvn install` au démarrage de chaque service Java en mode dev. Si vous modifiez du code dans `core` il faudra manuellement lancer :
  ```bash
  docker compose --env-file .env.local \
    -f docker-compose.yml -f docker-compose.dev.yml \
    exec upload-api sh -c 'cd /build/francetransfert-core && mvn install -DskipTests -q'
  ```
- Les caches sont dans des volumes Docker nommés (`gui-node-modules`, `maven-cache`) — survivent à un `down`. Pour repartir de zéro : `docker compose --env-file .env.local down -v`.
- Le watcher Java fait un `mvn compile -o` (offline) ; si tu ajoutes une dépendance, redémarre le service pour qu'il récupère les jars : `docker compose --env-file .env.local -f ... restart upload-api`.

### Services exposés (les deux modes)

| Service           | URL                                                     |
| ----------------- | ------------------------------------------------------- |
| GUI               | http://localhost:4200/                                  |
| upload-api        | http://localhost:8080/swagger-ui/index.html             |
| download-api      | http://localhost:8081/swagger-ui/index.html             |
| Keycloak admin    | http://localhost:7110/admin (admin / admin)             |
| MinIO console     | http://localhost:9101/ (identifiants dans `.env.local`) |
| MailCatcher       | http://localhost:1080/                                  |

La GUI consomme les APIs via `/api-private/*` (proxy nginx en mode fixe, proxy Angular en mode dev) — pas de configuration CORS nécessaire. Les ports 8080/8081 restent exposés pour debug direct (Swagger, curl).

### Identifiants Keycloak pour tester la connexion agent

- Utilisateur : **`ft`** / **`ft`** (email `ft@numerique.gouv.fr`)
- Realm : `francetransfert`, client public `francetransfert`

### Arrêt + nettoyage

```bash
docker compose --env-file .env.local down -v        # -v pour effacer les volumes (Redis, MinIO, caches dev)
```

### Restrictions sur les emails à utiliser

L'envoi par lien ou par email est réservé aux agents de l'État. La règle est purement par **domaine email** : le domaine doit être présent dans le SET Redis `enclosure-mails:mails`. `docker/redis-init.sh` y pré-charge quelques `*.gouv.fr` afin de procéder à des tests. Pour ajouter un domaine au vol :

```bash
docker compose --env-file .env.local exec redis \
  redis-cli -a ftpass --no-auth-warning SADD enclosure-mails:mails mondomaine.gouv.fr
```

### Antivirus

ClamAV est utilisé comme antivirus, mais celui-ci n'est actif sur le serveur que pour des plis non-chiffrés.

## Contribution

Nous utilisons le Developer Certificate of Origin (DCO) pour les contributions. En contribuant à ce projet, vous acceptez de respecter les termes du DCO.

Pour signer vos commits avec le DCO, ajoutez simplement l'option `-s` à votre commande de commit :

```bash
git commit -s -m "Votre message de commit"
```

Pour plus d'informations sur le DCO, consultez [developercertificate.org](https://developercertificate.org/).

## Licence

Ce projet est sous licence Apache 2.0 - voir le fichier [LICENSE](LICENSE.txt) pour plus de détails.