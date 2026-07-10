/*
 * Copyright (c) Direction Interministerielle du Numerique
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE.txt
 */

package fr.gouv.culture.francetransfert.application.configuration;

import java.lang.reflect.Parameter;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import fr.gouv.culture.francetransfert.core.utils.EnclosureRecipientMdcResolver;
import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class EnclosureRecipientMdcAspect {

	private final EnclosureRecipientMdcResolver enclosureRecipientMdcResolver;

	@Around("within(fr.gouv.culture.francetransfert.application.resources..*) && execution(public * *(..))")
	public Object enrichMdc(ProceedingJoinPoint joinPoint) throws Throwable {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Parameter[] parameters = signature.getMethod().getParameters();
		Object[] args = joinPoint.getArgs();
		enclosureRecipientMdcResolver.enrich(parameters, args, resolveConnectedUser());
		return joinPoint.proceed();
	}

	private String resolveConnectedUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication instanceof JwtAuthenticationToken jwtAuthentication && jwtAuthentication.isAuthenticated()) {
			return jwtAuthentication.getName();
		}
		return null;
	}
}
