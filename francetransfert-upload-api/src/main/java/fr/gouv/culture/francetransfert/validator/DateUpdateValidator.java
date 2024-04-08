/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.validator;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import fr.gouv.culture.francetransfert.core.enums.EnclosureKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RedisKeysEnum;
import fr.gouv.culture.francetransfert.core.services.RedisManager;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DateUpdateValidator implements ConstraintValidator<DateUpdateConstraint, Object> {

	@Autowired
	private RedisManager redisManager;

	private String enclosureId;
	private String newDate;

	private static final String DATE_FIELD = "newDate";

	private static final String ENCLOSURE_FIELD = "enclosureId";

	@Value("${upload.expired.limit}")
	private int maxUpdateDate;

	@Override
	public void initialize(DateUpdateConstraint constraintAnnotation) {
		this.enclosureId = constraintAnnotation.enclosureId();
		this.newDate = constraintAnnotation.newDate();
	}

	@Override
	public boolean isValid(Object o, ConstraintValidatorContext constraintValidatorContext) {
		Field enclosureIdField = null;
		try {
			enclosureIdField = o.getClass().getDeclaredField(ENCLOSURE_FIELD);
			enclosureIdField.setAccessible(true);
			if (enclosureIdField.get(o) == null || ((String) enclosureIdField.get(o)).isEmpty()) {
				constraintValidatorContext.disableDefaultConstraintViolation();
				constraintValidatorContext.buildConstraintViolationWithTemplate("Enclosure must not be null")
						.addPropertyNode(ENCLOSURE_FIELD).addConstraintViolation();
				return false;
			}
		} catch (NoSuchFieldException | IllegalAccessException e) {
			return false;
		}
		Field newDateField = null;
		try {
			newDateField = o.getClass().getDeclaredField(DATE_FIELD);
			newDateField.setAccessible(true);
		} catch (NoSuchFieldException e) {
			return false;
		}
		try {
			if (((LocalDate) newDateField.get(o)).isBefore(LocalDate.now())) {
				constraintValidatorContext.disableDefaultConstraintViolation();
				constraintValidatorContext
						.buildConstraintViolationWithTemplate(
								"Date invalide, veuillez sélectionner une date supérieure à aujourd'hui")
						.addPropertyNode(DATE_FIELD).addConstraintViolation();
				return false;
			}
			Map<String, String> enclosureMap = redisManager
					.hmgetAllString(RedisKeysEnum.FT_ENCLOSURE.getKey((String) enclosureIdField.get(o)));
			if (enclosureMap.isEmpty()) {
				constraintValidatorContext.disableDefaultConstraintViolation();
				constraintValidatorContext.buildConstraintViolationWithTemplate("Invalid enclosureId")
						.addPropertyNode(ENCLOSURE_FIELD).addConstraintViolation();
				return false;
			}
			LocalDateTime result = LocalDateTime.parse(enclosureMap.get(EnclosureKeysEnum.TIMESTAMP.getKey()));
			if (result.plusDays(maxUpdateDate).toLocalDate().isBefore(((LocalDate) newDateField.get(o)))) {
				constraintValidatorContext.disableDefaultConstraintViolation();
				constraintValidatorContext
						.buildConstraintViolationWithTemplate(
								"Date invalide, veuillez sélectionner une date inférieure à " + maxUpdateDate
										+ " jours depuis la création du pli")
						.addPropertyNode(DATE_FIELD).addConstraintViolation();
				return false;
			}
		} catch (IllegalAccessException e) {
			return false;
		}

		return true;
	}
}
