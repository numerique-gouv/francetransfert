/* 
 * Copyright (c) Direction Interministérielle du Numérique (2023) 
 * 
 * SPDX-License-Identifier: Apache-2.0 
 * License-Filename: LICENSE.txt 
 */

/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationContent implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2503290077135024645L;

	private String type;

	private String link;

	private String content;

	public String title;

	private String email;

}
