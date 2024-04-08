/* 
 * Copyright (c) Ministère de la Culture (2023) 
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
 
/* 
 * Copyright (c) Ministère de la Culture (2022) 
 * 
 * SPDX-License-Identifier: Apache-2.0 
 * License-Filename: LICENSE.txt 
 */

package fr.gouv.culture.francetransfert.core.enums;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public enum TypeCanalEnum {

    NOTIFICATION("NOT"), COURRIEL("COU");

	
	private String key;


	TypeCanalEnum(String key) {
		this.key = key;
	}
	
	public String getKey() {
		return key;
	}
	public static List<String> keys() {
		return Stream.of(TypeCanalEnum.values()).map(e -> e.key).collect(Collectors.toList());
	}


}
