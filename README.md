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
  - Technologies : Node.js, Express

- **francetransfert-upload-api/** : API dédiée à la gestion des téléversements de fichiers
  - Technologies : Node.js, Express

- **francetransfert-download-api/** : API dédiée à la gestion des téléchargements de fichiers
  - Technologies : Node.js, Express

- **francetransfert-upload-download-gui/** : Interface utilisateur web pour le téléversement et téléchargement de fichiers
  - Technologies : React, TypeScript

- **francetransfert-worker/** : Service de traitement asynchrone des tâches en arrière-plan
  - Technologies : Node.js

- **francetransfert-backup/** : Scripts et outils pour la gestion des sauvegardes
  - Technologies : Scripts shell, PostgreSQL

- **ft-helm/** : Charts Helm pour le déploiement sur Kubernetes
  - Technologies : Helm, Kubernetes

## Contribution

Nous utilisons le Developer Certificate of Origin (DCO) pour les contributions. En contribuant à ce projet, vous acceptez de respecter les termes du DCO.

Pour signer vos commits avec le DCO, ajoutez simplement l'option `-s` à votre commande de commit :

```bash
git commit -s -m "Votre message de commit"
```

Pour plus d'informations sur le DCO, consultez [developercertificate.org](https://developercertificate.org/).

## Licence

Ce projet est sous licence Apache 2.0 - voir le fichier [LICENSE](LICENSE.txt) pour plus de détails. 