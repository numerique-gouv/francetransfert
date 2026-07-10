/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */
 
package fr.gouv.culture.francetransfert.configuration;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import fr.gouv.culture.francetransfert.core.utils.CorrelationIdResolver;
import fr.gouv.culture.francetransfert.core.utils.MdcKeys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CorrelationIdMdcFilter extends OncePerRequestFilter {

	private final CorrelationIdResolver correlationIdResolver;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String correlationId = correlationIdResolver.resolveCorrelationId(request.getHeader(MdcKeys.CORRELATION_ID_HEADER));
		String sessionId = correlationIdResolver.resolveSessionId(request.getHeader(MdcKeys.SESSION_ID_HEADER));
		String callerIp = correlationIdResolver.resolveCallerIp(request.getHeader(MdcKeys.FORWARDED_FOR_HEADER),
				request.getHeader(MdcKeys.REAL_IP_HEADER), request.getRemoteAddr());

		response.setHeader(MdcKeys.CORRELATION_ID_HEADER, correlationId);
		response.setHeader(MdcKeys.SESSION_ID_HEADER, sessionId);
		MDC.put(MdcKeys.CORRELATION_ID, correlationId);
		MDC.put(MdcKeys.SESSION_ID, sessionId);
		MDC.put(MdcKeys.CALLER_IP, callerIp);
		try {
			filterChain.doFilter(request, response);
		} finally {
			MDC.clear();
		}
	}
}
