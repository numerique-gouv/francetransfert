/* 
 * Copyright (c) Direction Interministérielle du Numérique 
 * 
 * SPDX-License-Identifier: Apache-2.0 
 * License-Filename: LICENSE.txt 
 */

package fr.gouv.culture.francetransfert.core.enums;

import java.util.Arrays;

//---
public enum StatutEnum {

	INI("000-INI", "Initialisé", 0),
	ECC("010-ECC", "En cours de chargement", 1),
	ECH("011-ECH", "Erreur lors du chargement du pli", 2),
	CHT("012-CHT", "Chargement terminé", 3),
	ANA("020-ANA", "Analyse du pli", 4),
	ETF("021-ETF", "Erreur détectée lors de l’analyse des types de fichier", 5),
	EAV("022-EAV", "Erreur détectée lors de l’analyse antivirale", 6),
	APT("023-APT", "Analyse du pli terminée", 7),
	EDC("030-EDC", "Envoi des courriels", 8),
	EEC("031-EEC", "Erreur lors de l’envoi des courriels", 9),
	PAT("032-PAT", "Prêt au téléchargement", 10),
	ECT("040-ECT", "En cours de téléchargement", 11),
	ARC("090-ARC", "Archivé", 12);

	StatutEnum(String code, String word, int order) {
		this.setCode(code);
		this.setWord(word);
		this.setOrder(order);
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

	public void setOrder(int order) {
		this.order = order;
	}

	public int getOrder() {
		return order;
	}

	private String code;
	private String word;
	private int order;

	public boolean isAfter(StatutEnum status) {
		return this.getOrder() > status.getOrder();
	}

	public static StatutEnum getFromCode(String code) {
		return Arrays.stream(StatutEnum.values()).filter(statut -> statut.getCode().equals(code)).findFirst()
				.orElse(null);
	}

}
