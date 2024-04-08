/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.application.error;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.amazonaws.SdkClientException;

import fr.gouv.culture.francetransfert.core.error.ApiError;
import fr.gouv.culture.francetransfert.core.error.ApiErrorFranceTransfert;
import fr.gouv.culture.francetransfert.core.error.ApiValidationErrorReturn;
import fr.gouv.culture.francetransfert.core.exception.ApiValidationException;
import fr.gouv.culture.francetransfert.core.utils.RedisUtils;
import fr.gouv.culture.francetransfert.domain.exceptions.BusinessDomainException;
import fr.gouv.culture.francetransfert.domain.exceptions.DomainNotFoundException;
import fr.gouv.culture.francetransfert.domain.exceptions.DownloadException;
import fr.gouv.culture.francetransfert.domain.exceptions.ExpirationEnclosureException;
import fr.gouv.culture.francetransfert.domain.exceptions.InvalidHashException;

/**
 * The type StarterKit exception handler.
 * 
 * @author Open Group
 * @since 1.0.0
 */
@ControllerAdvice
public class FranceTransfertDownloadExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger LOG = LoggerFactory.getLogger(FranceTransfertDownloadExceptionHandler.class);

	@Override
	protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		LOG.error("Handle error type handleNoHandlerFoundException : " + ex.getMessage(), ex);
		return new ResponseEntity<>(new ApiError(status.value(), "NOT FOUND", "NOT_FOUND"), status);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getFieldErrors().forEach((error) -> {
			String fieldName = ((FieldError) error).getField();
			String errorMessage = error.getDefaultMessage();
			errors.put(fieldName, errorMessage);
			LOG.error("Invalid Field: {} -- Reason: {}", fieldName, errorMessage);
		});
		ApiErrorFranceTransfert apiError = new ApiErrorFranceTransfert(HttpStatus.BAD_REQUEST,
				"MethodArgumentNotValidException", errors);
		LOG.error("MethodArgumentNotValidException : " + ex.getMessage(), ex);
		return new ResponseEntity<Object>(apiError, new HttpHeaders(), apiError.getStatus());
	}

	@ExceptionHandler(DomainNotFoundException.class)
	public ResponseEntity<Object> handleDomainNotFoundException(Exception ex) {
		LOG.error("Handle error type DomainNotFoundException : " + ex.getMessage(), ex);
		String errorId = RedisUtils.generateGUID();
		LOG.error("Type: {} -- id: {} -- message: {}", ErrorEnum.TECHNICAL_ERROR.getValue(), errorId, ex.getMessage(),
				ex);
		return new ResponseEntity<>(
				new ApiError(HttpStatus.NOT_FOUND.value(), ErrorEnum.TECHNICAL_ERROR.getValue(), errorId),
				HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler({ MaxTryException.class })
	public ResponseEntity<Object> handleMaxTryException(MaxTryException ex) {
		LOG.error("Handle error type MaxTryException : " + ex.getMessage(), ex);
		LOG.error("Type: {} -- id: {} -- message: {}", ErrorEnum.TECHNICAL_ERROR.getValue(), ex.getId(),
				ex.getMessage(), ex);
		return new ResponseEntity<>(new ApiError(HttpStatus.UNAUTHORIZED.value(), ex.getMessage(), ex.getId()),
				HttpStatus.UNAUTHORIZED);
	}

	@ExceptionHandler({ AccessDeniedException.class })
	public ResponseEntity<Object> handleUnauthorizedException(Exception ex) {
		LOG.error("Handle error type AccessDeniedException : " + ex.getMessage(), ex);
		String errorId = RedisUtils.generateGUID();
		LOG.error("Type: {} -- id: {} -- message: {}", ErrorEnum.TECHNICAL_ERROR.getValue(), errorId, ex.getMessage(),
				ex);
		return new ResponseEntity<>(new ApiError(HttpStatus.UNAUTHORIZED.value(), ex.getMessage(), errorId),
				HttpStatus.UNAUTHORIZED);
	}

	@ExceptionHandler(BusinessDomainException.class)
	public ResponseEntity<Object> handleBusinessDomainException(Exception ex) {
		LOG.error("Handle error type BusinessDomainException : " + ex.getMessage(), ex);
		return generateError(ex, ErrorEnum.TECHNICAL_ERROR.getValue());
	}

	@ExceptionHandler(SdkClientException.class)
	public ResponseEntity<Object> handleSdkClientException(Exception ex) {
		LOG.error("Handle error type SdkClientException : " + ex.getMessage(), ex);
		return generateError(ex, ErrorEnum.TECHNICAL_ERROR.getValue());
	}

	@ExceptionHandler(UnauthorizedAccessException.class)
	public ResponseEntity<Object> handleUnauthorizedAccessException(UnauthorizedAccessException ex) {
		LOG.error("Handle error type UnauthorizedAccessException : " + ex.getMessage(), ex);
		String errorId = UUID.randomUUID().toString();
		LOG.error("Type: {} -- id: {} -- message: {}", ErrorEnum.TECHNICAL_ERROR.getValue(), errorId, ex.getMessage(),
				ex);
		return new ResponseEntity<>(new ApiError(HttpStatus.UNAUTHORIZED.value(), ex.getType(), ex.getMessage()),
				HttpStatus.UNAUTHORIZED);
	}

	@ExceptionHandler(InvalidHashException.class)
	public ResponseEntity<Object> handleInvalidHashException(Exception ex) {
		LOG.error("Handle error type InvalidHashException : " + ex.getMessage(), ex);
		LOG.error("Type: {} -- id: {} -- message: {}", ErrorEnum.TECHNICAL_ERROR.getValue(), null, ex.getMessage(), ex);
		return new ResponseEntity<>(
				new ApiError(HttpStatus.NOT_FOUND.value(), ErrorEnum.HASH_INVALID.getValue(), ex.getMessage()),
				HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(ExpirationEnclosureException.class)
	public ResponseEntity<Object> handleExpirationEnclosureException(Exception ex) {
		LOG.error("Handle error type ExpirationEnclosureException : " + ex.getMessage(), ex);
		LOG.error("Type: {} -- id: {} -- message: {}", ErrorEnum.TECHNICAL_ERROR.getValue(), null, ex.getMessage(), ex);
		return new ResponseEntity<>(
				new ApiError(HttpStatus.NOT_FOUND.value(), ErrorEnum.TECHNICAL_ERROR.getValue(), ex.getMessage()),
				HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(DownloadException.class)
	public ResponseEntity<Object> handleDownloadException(DownloadException ex) {
		LOG.error("Handle error type DownloadException : " + ex.getMessage(), ex);
		LOG.error("Type: {} -- id: {} -- message: {}", ex.getId(), ex.getMessage(), ex);
		return new ResponseEntity<>(new ApiError(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), ex.getId()),
				HttpStatus.BAD_REQUEST);
	}

	private ResponseEntity<Object> generateError(Exception ex, String errorType) {
		String errorId = UUID.randomUUID().toString();
		LOG.error("generateError :Type: {} -- id: {} -- message: {}", errorType, errorId, ex.getMessage(), ex);
		return new ResponseEntity<>(new ApiError(HttpStatus.BAD_REQUEST.value(), errorType, errorId),
				HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(PasswordException.class)
	public ResponseEntity<Object> handleConfirmationCodeExcption(PasswordException ex) {
		LOG.error("Handle error type PasswordException : " + HttpStatus.UNAUTHORIZED.toString(), ex);
		LOG.error("Type: {} -- id: {} -- message: {}", ex.getType(), ex.getId(), HttpStatus.UNAUTHORIZED.toString(),
				ex);
		return new ResponseEntity<>(
				new WrongCodeError(HttpStatus.UNAUTHORIZED.value(), ex.getCount(), HttpStatus.UNAUTHORIZED.toString()),
				HttpStatus.UNAUTHORIZED);
	}

	@ExceptionHandler({ Exception.class, RuntimeException.class })
	public ResponseEntity<Object> handleException(Exception ex) {
		LOG.error("Handle error type Exception : " + ex.getMessage(), ex);
		return new ResponseEntity<>(
				new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.toString(), ""),
				HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(ApiValidationException.class)
	public ResponseEntity<ApiValidationErrorReturn> ApiValidationException(ApiValidationException ex) {
		ApiValidationErrorReturn ret = new ApiValidationErrorReturn();
		ret.setErreurs(ex.getErreurs());
		return new ResponseEntity<ApiValidationErrorReturn>(ret, HttpStatus.UNPROCESSABLE_ENTITY);
	}

	@ExceptionHandler(UnauthorizedApiAccessException.class)
	protected ResponseEntity<Void> handleUnauthorizedAccessException(UnauthorizedApiAccessException ex) {
		LOG.error("Handle error UnauthorizedApiAccessException : " + ex.getMessage(), ex);
		LOG.error("UnauthorizedAccessException : " + ex.getMessage(), ex);
		return new ResponseEntity<Void>(null, new HttpHeaders(), HttpStatus.FORBIDDEN);
	}

}
