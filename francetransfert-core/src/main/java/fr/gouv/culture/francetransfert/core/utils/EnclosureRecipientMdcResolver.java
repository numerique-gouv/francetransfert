/*
 * Copyright (c) Direction Interministerielle du Numerique
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE.txt
 */

package fr.gouv.culture.francetransfert.core.utils;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EnclosureRecipientMdcResolver {

	private static final Set<String> ENCLOSURE_PARAM_NAMES = Set.of("enclosureid", "idpli", "enclosure");
	private static final Set<String> RECIPIENT_PARAM_NAMES = Set.of("courrielexpediteur", "sendermail", "senderid",
			"courrielutilisateur", "recipientmail", "recipientid", "recipient");

	private static final String[] ENCLOSURE_GETTERS = { "getEnclosureId", "getEnclosure", "getIdEnclosure", "getPlis" };
	private static final String[] RECIPIENT_GETTERS = { "getRecipientMail", "getCourrielUtilisateur", "getRecipient",
			"getSenderMail", "getRecipientId", "getSenderEmail", "getMailAdress" };

	private final Base64CryptoService base64CryptoService;
	private final StringUploadUtils stringUploadUtils;

	@SuppressWarnings("resource")
	public MdcScope enrich(Parameter[] parameters, Object[] args, String connectedUser) {
		String enclosureId = resolveValue(parameters, args, ENCLOSURE_PARAM_NAMES, ENCLOSURE_GETTERS);
		String recipient = normalizeRecipient(resolveValue(parameters, args, RECIPIENT_PARAM_NAMES, RECIPIENT_GETTERS));

		return new MdcScope().put(MdcKeys.ENCLOSURE_ID, enclosureId).put(MdcKeys.RECIPIENT, recipient)
				.put(MdcKeys.CONNECTED_USER, connectedUser);
	}

	private String normalizeRecipient(String recipientValue) {
		if (!StringUtils.hasText(recipientValue)) {
			return null;
		}
		if (stringUploadUtils.isValidEmail(recipientValue)) {
			return recipientValue;
		}
		try {
			String decoded = base64CryptoService.base64Decoder(recipientValue);
			if (StringUtils.hasText(decoded) && stringUploadUtils.isValidEmail(decoded)) {
				return decoded;
			}
			return recipientValue;
		} catch (IllegalArgumentException | UnsupportedEncodingException _) {
			return recipientValue;
		}
	}

	private String resolveValue(Parameter[] parameters, Object[] args, Set<String> paramNames, String[] getters) {
		for (int i = 0; i < parameters.length && i < args.length; i++) {
			Object value = args[i];
			if (value == null) {
				continue;
			}
			String resolved = resolveFromParameter(parameters[i], value, paramNames, getters);
			if (resolved != null) {
				return resolved;
			}
		}
		return null;
	}

	private String resolveFromParameter(Parameter parameter, Object value, Set<String> paramNames, String[] getters) {
		if (isWebParam(parameter)) {
			if (value instanceof String stringValue && StringUtils.hasText(stringValue)
					&& paramNames.contains(resolveParamName(parameter))) {
				return stringValue;
			}
			return null;
		}
		if (parameter.isAnnotationPresent(RequestBody.class)) {
			return extractFromGetters(value, getters);
		}
		return null;
	}

	private boolean isWebParam(Parameter parameter) {
		return parameter.isAnnotationPresent(RequestParam.class) || parameter.isAnnotationPresent(PathVariable.class);
	}

	private String resolveParamName(Parameter parameter) {
		RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
		if (requestParam != null) {
			return annotationName(requestParam.value(), requestParam.name(), parameter);
		}
		PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
		if (pathVariable != null) {
			return annotationName(pathVariable.value(), pathVariable.name(), parameter);
		}
		return parameter.getName().toLowerCase();
	}

	private String annotationName(String value, String name, Parameter parameter) {
		if (StringUtils.hasText(value)) {
			return value.toLowerCase();
		}
		if (StringUtils.hasText(name)) {
			return name.toLowerCase();
		}
		return parameter.getName().toLowerCase();
	}

	private String extractFromGetters(Object target, String[] getters) {
		for (String getterName : getters) {
			try {
				Method getter = target.getClass().getMethod(getterName);
				Object result = getter.invoke(target);
				if (result instanceof String value && StringUtils.hasText(value)) {
					return value;
				}
			} catch (ReflectiveOperationException | RuntimeException _) {
				// getter absent or not readable on this DTO: try the next one
			}
		}
		return null;
	}
}
