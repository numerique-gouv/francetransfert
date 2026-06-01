package fr.gouv.culture.francetransfert.configuration;

import java.io.IOException;
import java.util.regex.Pattern;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CorrelationIdMdcFilter extends OncePerRequestFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(CorrelationIdMdcFilter.class);

	private static final String CORRELATION_ID_HEADER = "x-correlation-id";
	private static final String SESSION_ID_HEADER = "x-session-id";
	private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
	private static final String REAL_IP_HEADER = "X-Real-IP";

	private static final String CORRELATION_ID_MDC_KEY = "correlationId";
	private static final String SESSION_ID_MDC_KEY = "sessionId";
	private static final String CALLER_IP_MDC_KEY = "callerIp";
	private static final Pattern CALLER_IP_PATTERN = Pattern.compile("^[A-Fa-f0-9:._%-]{1,128}$");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String correlationId = resolveCorrelationId(request);
		String sessionId = resolveSessionId(request);
		String callerIp = resolveCallerIp(request);

		MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
		MDC.put(SESSION_ID_MDC_KEY, sessionId);
		MDC.put(CALLER_IP_MDC_KEY, callerIp);
		response.setHeader(CORRELATION_ID_HEADER, correlationId);
		response.setHeader(SESSION_ID_HEADER, sessionId);
		try {
			filterChain.doFilter(request, response);
		} finally {
			MDC.remove(CORRELATION_ID_MDC_KEY);
			MDC.remove(SESSION_ID_MDC_KEY);
			MDC.remove(CALLER_IP_MDC_KEY);
		}
	}

	private String resolveCorrelationId(HttpServletRequest request) {
		String correlationId = request.getHeader(CORRELATION_ID_HEADER);
		return StringUtils.hasText(correlationId) ? correlationId.trim() : UUID.randomUUID().toString();
	}

	private String resolveSessionId(HttpServletRequest request) {
		String sessionId = request.getHeader(SESSION_ID_HEADER);
		return StringUtils.hasText(sessionId) ? sessionId.trim() : UUID.randomUUID().toString();
	}

	private String resolveCallerIp(HttpServletRequest request) {
		String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
		if (StringUtils.hasText(forwardedFor)) {
			LOGGER.debug("forwardedFor: {}", forwardedFor);
			return sanitizeCallerIp(forwardedFor.split(",")[0].trim(), request.getRemoteAddr());
		}

		String realIp = request.getHeader(REAL_IP_HEADER);
		if (StringUtils.hasText(realIp)) {
			LOGGER.debug("realIp: {}", realIp);
			return sanitizeCallerIp(realIp.trim(), request.getRemoteAddr());
		}
		LOGGER.debug("remoteAddr: {}", request.getRemoteAddr());
		return sanitizeCallerIp(request.getRemoteAddr(), "-");
	}

	private String sanitizeCallerIp(String candidate, String fallback) {
		if (StringUtils.hasText(candidate) && CALLER_IP_PATTERN.matcher(candidate).matches()) {
			return candidate;
		}
		if (StringUtils.hasText(fallback) && CALLER_IP_PATTERN.matcher(fallback).matches()) {
			return fallback;
		}
		return "-";
	}
}
