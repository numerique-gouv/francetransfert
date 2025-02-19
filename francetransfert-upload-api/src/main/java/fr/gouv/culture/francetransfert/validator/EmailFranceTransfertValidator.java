/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.validator;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import fr.gouv.culture.francetransfert.application.resources.model.FranceTransfertDataRepresentation;
import fr.gouv.culture.francetransfert.core.utils.StringUploadUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EmailFranceTransfertValidator
		implements ConstraintValidator<EmailsFranceTransfert, FranceTransfertDataRepresentation> {

	@Autowired
	StringUploadUtils stringUploadUtils;

	/**
	 *
	 * @param metadata
	 * @param constraintValidatorContext
	 * @return
	 */
	public boolean isValid(FranceTransfertDataRepresentation metadata,
			ConstraintValidatorContext constraintValidatorContext) {

		boolean isValid = false;

		// Check public link
		if (metadata.getPublicLink()) {
			if (stringUploadUtils.isValidEmail(metadata.getSenderEmail())) {
				isValid = stringUploadUtils.isValidEmailIgni(metadata.getSenderEmail());
			}
			return isValid;
		}

		// Empty check recipients
		if (CollectionUtils.isEmpty(metadata.getRecipientEmails()) || StringUtils.isBlank(metadata.getSenderEmail())) {
			return isValid;
		}

		// Check sender/recipient validity
		if (stringUploadUtils.isValidEmail(metadata.getSenderEmail())) {
			isValid = stringUploadUtils.isValidEmailIgni(metadata.getSenderEmail());
			if (isValid) {
				return isValid;
			} else if (CollectionUtils.isNotEmpty(metadata.getRecipientEmails())) {
				// Sender Public Mail
				// All the Receivers Email must be Gouv Mail else request rejected.
				boolean canUpload = false;
				canUpload = metadata.getRecipientEmails().stream().noneMatch(x -> {
					return !stringUploadUtils.isValidEmailIgni(x);
				});
				isValid = canUpload;
			}
		}

		return isValid;
	}
}
