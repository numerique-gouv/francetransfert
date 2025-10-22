/*
  * Copyright (c) Direction Interministérielle du Numérique 
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
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.model.AmazonS3Exception;

import fr.gouv.culture.francetransfert.core.error.ApiError;
import fr.gouv.culture.francetransfert.core.error.ApiErrorFranceTransfert;
import fr.gouv.culture.francetransfert.core.error.ApiValidationErrorReturn;
import fr.gouv.culture.francetransfert.core.exception.ApiValidationException;
import fr.gouv.culture.francetransfert.core.utils.RedisUtils;
import fr.gouv.culture.francetransfert.domain.exceptions.BusinessDomainException;
import fr.gouv.culture.francetransfert.domain.exceptions.ConfirmationCodeException;
import fr.gouv.culture.francetransfert.domain.exceptions.DomainNotFoundException;
import fr.gouv.culture.francetransfert.domain.exceptions.ExtensionNotFoundException;
import fr.gouv.culture.francetransfert.domain.exceptions.FlowChunkNotExistException;
import fr.gouv.culture.francetransfert.domain.exceptions.InvalidCaptchaException;
import fr.gouv.culture.francetransfert.domain.exceptions.MaxTryException;
import fr.gouv.culture.francetransfert.domain.exceptions.UnauthorizedMailAddressException;
import fr.gouv.culture.francetransfert.domain.exceptions.UploadException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import redis.clients.jedis.exceptions.JedisDataException;

/**
 * The type StarterKit exception handler.
 * 
 * @author Open Group
 * @since 1.0.0
 */
@ControllerAdvice
public class FranceTransertUploadExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger LOG = LoggerFactory.getLogger(FranceTransertUploadExceptionHandler.class);

	@Override
	protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		LOG.error("Handle error handleNoHandlerFoundException : " + ex.getMessage(), ex);
		return new ResponseEntity<>(new ApiError(status.value(), "NOT FOUND", "NOT_FOUND"), status);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		LOG.error("Handle error handleMethodArgumentNotValid : " + ex.getMessage(), ex);
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

	@ExceptionHandler(UnauthorizedAccessException.class)
	protected ResponseEntity<Object> handleUnauthorizedAccessException(UnauthorizedAccessException ex) {
		LOG.error("Handle error handleUnauthorizedAccessException : " + ex.getMessage(), ex);
		Map<String, String> errors = new HashMap<>();
		errors.put("token", "Invalid Token");
		ApiErrorFranceTransfert apiError = new ApiErrorFranceTransfert(HttpStatus.UNAUTHORIZED,
				"UnauthorizedAccessException", errors);
		LOG.error("UnauthorizedAccessException : " + ex.getMessage(), ex);
		return new ResponseEntity<Object>(apiError, new HttpHeaders(), apiError.getStatus());
	}

	@ExceptionHandler(UnauthorizedApiAccessException.class)
	protected ResponseEntity<Void> handleUnauthorizedAccessException(UnauthorizedApiAccessException ex) {
		LOG.error("Handle error UnauthorizedApiAccessException : " + ex.getMessage(), ex);
		LOG.error("UnauthorizedAccessException : " + ex.getMessage(), ex);
		return new ResponseEntity<Void>(null, new HttpHeaders(), HttpStatus.FORBIDDEN);
	}

	@ExceptionHandler(DomainNotFoundException.class)
	public ResponseEntity<Object> handleDomainNotFoundException(Exception ex) {
		LOG.error("Handle error DomainNotFoundException : " + ex.getMessage(), ex);
		String errorId = RedisUtils.generateGUID();
		LOG.error("Type: {} -- id: {} -- message: {}", ErrorEnum.TECHNICAL_ERROR.getValue(), errorId, ex.getMessage(),
				ex);
		return new ResponseEntity<>(
				new ApiError(HttpStatus.UNAUTHORIZED.value(), ErrorEnum.TECHNICAL_ERROR.getValue(), errorId),
				HttpStatus.UNAUTHORIZED);
	}

	@ExceptionHandler(BusinessDomainException.class)
	public ResponseEntity<Object> handleBusinessDomainException(Exception ex) {
		LOG.error("Handle error BusinessDomainException : " + ex.getMessage(), ex);
		return generateError(ex, ErrorEnum.TECHNICAL_ERROR.getValue());
	}

	@ExceptionHandler(ExtensionNotFoundException.class)
	public ResponseEntity<Object> handleExtensionNotFoundException(Exception ex) {
		LOG.error("Handle error ExtensionNotFoundException : " + ex.getMessage(), ex);
		String errorId = RedisUtils.generateGUID();
		return new ResponseEntity<>(
				new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(), ErrorEnum.TECHNICAL_ERROR.getValue(), errorId),
				HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(FlowChunkNotExistException.class)
	public ResponseEntity<Object> handleFlowChunkNotExistException(Exception ex) {
		LOG.error("Handle error FlowChunkNotExistException : " + ex.getMessage(), ex);
		String errorId = RedisUtils.generateGUID();
		LOG.error("Type: {} -- id: {} -- message: {}", ErrorEnum.TECHNICAL_ERROR.getValue(), errorId, ex.getMessage());
		return new ResponseEntity<>(
				new ApiError(HttpStatus.EXPECTATION_FAILED.value(), ErrorEnum.TECHNICAL_ERROR.getValue(), errorId),
				HttpStatus.EXPECTATION_FAILED);
	}

	@ExceptionHandler(UnauthorizedMailAddressException.class)
	public ResponseEntity<Object> handleUnauthorizedMailAddressException(Exception ex) {
		LOG.error("Handle error type UnauthorizedMailAddressException : " + ex.getMessage(), ex);
		return generateError(ex, ErrorEnum.TECHNICAL_ERROR.getValue());
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<Object> handleValidationEmailException(ConstraintViolationException ex, WebRequest request) {
		LOG.error("Handle error ConstraintViolationException : " + ex.getMessage(), ex);
		Map<String, String> errors = new HashMap<>();
		for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
			String field = null;
			for (Path.Node node : violation.getPropertyPath()) {
				field = node.getName();
			}
			errors.put(field, violation.getMessage());
		}
		ApiErrorFranceTransfert apiError = new ApiErrorFranceTransfert(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage(),
				errors);
		LOG.error("ConstraintViolationException : " + ex.getMessage(), ex);
		return new ResponseEntity<Object>(apiError, new HttpHeaders(), apiError.getStatus());
	}

	@ExceptionHandler(DateUpdateException.class)
	public ResponseEntity<Object> handleValidationEmailException(DateUpdateException ex, WebRequest request) {
		LOG.error("Exception handler DateUpdateException" + ex.getMessage(), ex);
		ApiErrorFranceTransfert apiError = new ApiErrorFranceTransfert(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage(),
				null);
		return new ResponseEntity<Object>(apiError, new HttpHeaders(), apiError.getStatus());
	}

	@ExceptionHandler({ AccessDeniedException.class })
	public ResponseEntity<Object> handleUnauthorizedException(Exception ex) {
		LOG.error("Handle error AccessDeniedException : " + ex.getMessage(), ex);
		String errorId = RedisUtils.generateGUID();
		LOG.error("Type: {} -- id: {} -- message: {}", ErrorEnum.TECHNICAL_ERROR.getValue(), errorId, ex.getMessage(),
				ex);
		return new ResponseEntity<>(
				new ApiError(HttpStatus.UNAUTHORIZED.value(), ErrorEnum.TECHNICAL_ERROR.getValue(), errorId),
				HttpStatus.UNAUTHORIZED);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<Object> handleMissingParams(MissingServletRequestParameterException ex,
			HttpServletRequest request) {
		LOG.error("Handle error type MissingServletRequestParameterException : {}", ex.getMessage(), ex);
		return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
	}

	@ExceptionHandler(JedisDataException.class)
	public ResponseEntity<Object> handleRedisException(Exception ex) {
		LOG.error("Handle error type JedisDataException : " + ex.getMessage(), ex);
		return generateError(ex, ErrorEnum.TECHNICAL_ERROR.getValue());
	}

	@ExceptionHandler(AmazonS3Exception.class)
	public ResponseEntity<Object> handleAmazonS3Exception(Exception ex) {
		LOG.error("Handle error type AmazonS3Exception : " + ex.getMessage(), ex);
		return generateError(ex, ErrorEnum.TECHNICAL_ERROR.getValue());
	}

	@ExceptionHandler(SdkClientException.class)
	public ResponseEntity<Object> handleSdkClientException(Exception ex) {
		LOG.error("Handle error type SdkClientException : " + ex.getMessage(), ex);
		return generateError(ex, ErrorEnum.TECHNICAL_ERROR.getValue());
	}

	@ExceptionHandler(AmazonServiceException.class)
	public ResponseEntity<Object> handleAmazonServiceException(Exception ex) {
		LOG.error("Handle error type AmazonServiceException : " + ex.getMessage(), ex);
		return generateError(ex, ErrorEnum.TECHNICAL_ERROR.getValue());
	}

	@ExceptionHandler(UploadException.class)
	public ResponseEntity<Object> handleUploadExcption(UploadException ex) {
		LOG.error("Handle error type UploadExcption : " + ex.getMessage(), ex);
		LOG.error("Type: {} -- id: {} -- message: {}", ex.getType(), ex.getId(), ex.getMessage(), ex);
		return new ResponseEntity<>(new ApiError(HttpStatus.BAD_REQUEST.value(), ex.getType(), ex.getId()),
				HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(ConfirmationCodeException.class)
	public ResponseEntity<Object> handleConfirmationCodeExcption(ConfirmationCodeException ex) {
		LOG.error("Handle error type ConfirmationCodeException : " + ex.getMessage(), ex);
		LOG.error("Type: {} -- message: {}", ex.getType(), ex.getMessage());
		return new ResponseEntity<>(new WrongCodeError(HttpStatus.UNAUTHORIZED.value(), ex.getCount(), ex.getMessage()),
				HttpStatus.UNAUTHORIZED);
	}

	@ExceptionHandler(MaxTryException.class)
	public ResponseEntity<Object> handleMaxTryException(MaxTryException ex) {
		LOG.error("Handle error type MaxTryException : " + ex.getMessage(), ex);
		LOG.error("message: {}", ex.getMessage(), ex);
		return new ResponseEntity<>(new ApiError(HttpStatus.UNAUTHORIZED.value(), "", ex.getMessage()),
				HttpStatus.UNAUTHORIZED);
	}

	@ExceptionHandler(InvalidCaptchaException.class)
	public ResponseEntity<Object> InvalidCaptchaException(InvalidCaptchaException ex) {
		LOG.error("Handle error type InvalidCaptchaException : " + ex.getMessage(), ex);
		LOG.error("message: {}", ex.getMessage(), ex);
		return new ResponseEntity<>(new ApiError(HttpStatus.BAD_REQUEST.value(), "", ex.getMessage()),
				HttpStatus.BAD_REQUEST);
	}

	private ResponseEntity<Object> generateError(Exception ex, String errorType) {
		String errorId = UUID.randomUUID().toString();
		LOG.error("generateError Type: {} -- id: {} -- message: {}", errorType, errorId, ex.getMessage(), ex);
		return new ResponseEntity<>(new ApiError(HttpStatus.BAD_REQUEST.value(), errorType, errorId),
				HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(ApiValidationException.class)
	public ResponseEntity<ApiValidationErrorReturn> ApiValidationException(ApiValidationException ex) {
		ApiValidationErrorReturn ret = new ApiValidationErrorReturn();
		ret.setErreurs(ex.getErreurs());
		return new ResponseEntity<ApiValidationErrorReturn>(ret, HttpStatus.UNPROCESSABLE_ENTITY);
	}

}
