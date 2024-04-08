/* 
 * Copyright (c) Ministère de la Culture (2022) 
 * 
 * SPDX-License-Identifier: Apache-2.0 
 * License-Filename: LICENSE.txt 
 */

package fr.gouv.culture.francetransfert.core.enums;

//---
public enum StatutEnum {

	
	INI("000-INI", "Initialisé"), 
	ECC("010-ECC", "En cours de chargement"),
	ECH("011-ECH", "Erreur lors du chargement du pli"), 
	CHT("012-CHT", "Chargement terminé"),
	ANA("020-ANA", "Analyse du pli"),
	ETF("021-ETF", "Erreur détectée lors de l’analyse des types de fichier"),
	EAV("022-EAV", "Erreur détectée lors de l’analyse antivirale"),
	APT("023-APT", "Analyse du pli terminée"),
	EDC("030-EDC", "Envoi des courriels"),
	EEC("031-EEC", "Erreur lors de l’envoi des courriels"),	
	PAT("032-PAT", "Prêt au téléchargement"),
	ECT("040-ECT", "En cours de téléchargement"),    
	ARC("090-ARC", "Archivé");
	
	

	StatutEnum(String code, String word) {
		this.setCode(code);
		this.setWord(word);
	}

	public String getCode() {
		return code;
	}

	public String getWord() {
		return word;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public void setWord(String word) {
		this.word = word;
	}

	private String code;
	private String word;

}
