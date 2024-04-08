/* 
 * Copyright (c) Ministère de la Culture (2022) 
 * 
 * SPDX-License-Identifier: Apache-2.0 
 * License-Filename: LICENSE.txt 
 */

/* 
 * Copyright (c) Ministère de la Culture (2022) 
 * 
 * SPDX-License-Identifier: Apache-2.0 
 * License-Filename: LICENSE.txt 
 */

package fr.gouv.culture.francetransfert.core.enums;

//---
public enum ValidationErrorEnum {

    FT01("typePli", "ERR_FT01_001", "Le type de pli est obligatoire"),
    FT02("typePli", "ERR_FT01_002",
            "La valeur fournie pour le champ typePli doit appartenir à la liste de valeur « Liste des types de pli »"),
    FT04("courrielExpediteur", "ERR_FTXX_001",
            "Le domaine de messagerie du courriel expéditeur / utilisateur doit être celui d’un agent de l’Etat"),
    FT05("courrielExpediteur", "ERR_FTXX_002", "Le courriel expéditeur / utilisateur est obligatoire"),
    FT06("courrielExpediteur", "ERR_FTXX_003",
            "Le courriel expéditeur / utilisateur doit respecter le format d’un courriel"),
    FT07("destinataires", "ERR_FT01_007",
            "Une liste de destinataires est obligatoire si le type de Pli est « courriel »"),
    FT08("destinataires.courrielDestinataire", "ERR_FT01_008",
            "Si une liste de destinataires est fournie alors au moins un destinataire doit être renseigné"),
    FT09("destinataires.courrielDestinataire", "ERR_FT01_009",
            "Le courriel destinataire doit respecter le format d’un courriel"),
    FT010("preferences.dateValidite", "ERR_FT01_010",
            "La date de fin de validité du pli doit être comprise entre J+1 et J+90 jours"),
    FT011("preferences.dateValidite", "ERR_FT01_011", "La date de fin de validité doit respecter le format d’une date"),
    FT012("preferences.motDePasse", "ERR_FT01_012",
            "Le mot de passe doit respecter les critères de robustesse et caractères autorisés : 12 caractères minimum - 64 caractères maximum - Au moins 3 lettres minuscules (a-z non accentué) - Au moins 3 lettres majuscules (A-Z non accentué) - Au moins 3 chiffres - Au moins 3 caractères spéciaux (!@#$%^&*()_-:+) - aucun caractère spécial non supporté\r\n"),
    FT014("preferences.langueCourriel", "ERR_FT01_014",
            "La valeur fournie pour le champ langueCourriel doit appartenir à la liste de valeur « Liste des langues de courriel »"),
    FT016("preferences.protectionArchive", "ERR_FT01_016",
            "Le champ protectionArchive doit respecter le format d’un booléen"),
    FT018("fichiers", "ERR_FT01_018", "Au moins un fichier doit être fourni"),
    FT019("fichiers.nomFichier", "ERR_FTXX_006", "Le nom de fichier est obligatoire"),
    FT020("fichiers.tailleFichier", "ERR_FTXX_007", "La taille de fichier est obligatoire"),
    FT021("fichiers.tailleFichier", "ERR_FT01_021",
            "La taille de chaque fichier ne peut pas dépasser 2 147 483 648 octets (2 Go)"),
    FT022("fichiers.tailleFichier", "ERR_FT01_022",
            "La taille totale du pli ne peut pas dépasser 21 474 836 480 (20 Go)"),
    FT023("fichiers.idFichier", "ERR_FTXX_005", "L’identifiant de fichier est obligatoire"),
    FT024("fichiers.cheminRelatif", "ERR_FT01_024", "Le chemin relatif d’accès au fichier est obligatoire"),
    FT025("fichiers.nomFichier", "ERR_FT01_025", "Le type d’un des fichiers du pli n’est pas autorisé"),
    FT026("destinataires", "ERR_FT01_026",
            "Le nombre de destinataires est limité à 100"),
    FT027("message", "ERR_FT01_027",
            "La taille maximum du message est de 25 000 caractères"),
    FT028("fichiers.nomFichier", "ERR_FT01_028",
            "Le nom du fichier doit être unique"),
    FT030("fichiers.idFichier", "ERR_FT01_030",
            "L’id du fichier doit être unique"),
    FT204("courrielExpediteur", "ERR_FT02_004", "Le courriel expéditeur doit respecter le format d’un courriel"),
    FT205("idPli", "ERR_FTXX_004",
            "L’identifiant du pli doit être connu de France transfert."),
    FT2020("courrielExpediteur", "ERR_FTXX_008",
            "Le courriel expéditeur n’est pas cohérent avec l’identifiant de pli indiqués dans la requête."),
    FT206("idFichier", "ERR_FT02_006", "L’identifiant de fichier est obligatoire"),
    FT207("nomFichier", "ERR_FT02_007", "Le nom de fichier est obligatoire"),
    FT208("numMorceauFichier", "ERR_FT02_008", "Le numéro de morceau du fichier est obligatoire"),
    FT209("numMorceauFichier", "ERR_FT02_009",
            "Le numéro de morceau du fichier doit être inférieur ou égal au nombre total de morceaux du fichier"),
    FT2010("numMorceauFichier", "ERR_FT02_010", "Le numéro de morceau du fichier doit être un entier supérieur à 0"),
    FT2011("totalMorceauxFichier", "ERR_FT02_011", "Le nombre total de morceaux du fichier est obligatoire"),
    FT2012("totalMorceauxFichier", "ERR_FT02_012",
            "Le nombre total de morceaux du fichier doit être un entier supérieur à 0"),
    FT2013("tailleMorceauFichier", "ERR_FT02_013", "La taille du morceau du fichier est obligatoire"),
    FT2014("tailleMorceauFichier", "ERR_FT02_014", "La taille du morceau du fichier doit être un entier supérieur à 0"),
    FT2016("fichier", "ERR_FT02_016", "Le contenu du fichier est obligatoire"),
    FT2017("", "ERR_FT02_017",
            "Pour autoriser le chargement des fichiers du pli, le statut du pli doit être 000-INI ou 010-ECC"),
    FT2018("tailleFichier", "ERR_FT02_018", "La taille du fichier doit être un entier supérieur à 0"),
    FT406("", "ERR_FT04_006",
            "Il n’est pas possible d’obtenir les métadonnées du pli si le statut du pli n’est pas 032-PAT, 040-ECT ou 090-ARC"),
    FT2019("", "ERR_FT02_019",
            "L’identifiant de fichier fourni doit correspondre à un identifiant de fichier déclaré à l’initialisation du pli"),
    FT601("idPli", "ERR_FT06_001",
            "Pour autoriser la fourniture de l’URL de téléchargement du pli, le statut du pli doit être 032-PAT ou 040-ECT"),
    FT602("courrielUtilisateur", "ERR_FT06_002",
            "Le courriel fourni doit correspondre à un des destinataires du pli"),
    FT603("courrielUtilisateur", "ERR_FT06_003",
            "Le nombre de téléchargements par destinataire est limité à 5 téléchargements."),
    FT604("motDePasse", "ERR_FT06_004",
            "Le mot de passe d’accès au pli est obligatoire pour les partenaires/clients de l’API paramétrés comme tel. "),
    FT605("motDePasse", "ERR_FT06_005",
            "Le mot de passe fourni doit être celui associé au pli"),
    FT606("motDePasse", "ERR_FT06_006",
            "Au-delà de 5 erreurs de mot de passe par jour, il n’est pas possible de télécharger le pli"),

    FT701("courrielUtilisateur", "ERR_FT07_001",
            "Le courriel utilisateur fourni doit correspondre à un des destinataires du pli"),
    FT801("canal", "ERR_FT08_001",
            "Le canal est obligatoire"),
    FT802("canal", "ERR_FT08_002",
            "La valeur fournie pour le champ canal doit appartenir à la liste de valeur « Liste des canaux d’information destinataire »");

    ValidationErrorEnum(String codeChamp, String numErreur, String libelleErreur) {
        this.setCodeChamp(codeChamp);
        this.setNumErreur(numErreur);
        this.setLibelleErreur(libelleErreur);
    }

    public String getCodeChamp() {
        return codeChamp;
    }

    public String getNumErreur() {
        return numErreur;
    }

    public String getLibelleErreur() {
        return libelleErreur;
    }

    public void setCodeChamp(String codeChamp) {
        this.codeChamp = codeChamp;
    }

    public void setNumErreur(String numErreur) {
        this.numErreur = numErreur;
    }

    public void setLibelleErreur(String libelleErreur) {
        this.libelleErreur = libelleErreur;
    }

    private String codeChamp;
    private String numErreur;
    private String libelleErreur;

}
