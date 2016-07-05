/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.exception;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.exception.SpServerRtException;
import org.eclipse.hawkbit.repository.exception.MultiPartFileUploadException;
import org.eclipse.hawkbit.rest.json.model.ExceptionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MultipartException;

import com.google.common.collect.Iterables;

/**
 * General controller advice for exception handling.
 */
@ControllerAdvice
public class ResponseExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ResponseExceptionHandler.class);
    private static final Map<SpServerError, HttpStatus> ERROR_TO_HTTP_STATUS = new EnumMap<>(SpServerError.class);
    private static final HttpStatus DEFAULT_RESPONSE_STATUS = HttpStatus.INTERNAL_SERVER_ERROR;

    static {
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_REPO_ENTITY_NOT_EXISTS, HttpStatus.NOT_FOUND);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_REPO_ENTITY_ALRREADY_EXISTS, HttpStatus.CONFLICT);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_REPO_ENTITY_READ_ONLY, HttpStatus.FORBIDDEN);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_REST_SORT_PARAM_INVALID_DIRECTION, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_REST_SORT_PARAM_INVALID_FIELD, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_REST_SORT_PARAM_SYNTAX, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_REST_RSQL_PARAM_INVALID_FIELD, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_REST_RSQL_SEARCH_PARAM_SYNTAX, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_INSUFFICIENT_PERMISSION, HttpStatus.FORBIDDEN);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_ARTIFACT_UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_ARTIFACT_UPLOAD_FAILED_SHA1_MATCH, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_ARTIFACT_UPLOAD_FAILED_MD5_MATCH, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_ARTIFACT_DELETE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_ARTIFACT_LOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_ACTION_STATUS_TO_MANY_ENTRIES, HttpStatus.FORBIDDEN);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_ATTRIBUTES_TO_MANY_ENTRIES, HttpStatus.FORBIDDEN);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_ACTION_NOT_CANCELABLE, HttpStatus.METHOD_NOT_ALLOWED);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_ACTION_NOT_FORCE_QUITABLE, HttpStatus.METHOD_NOT_ALLOWED);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_DS_CREATION_FAILED_MISSING_MODULE, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_DS_MODULE_UNSUPPORTED, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_DS_TYPE_UNDEFINED, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_REPO_TENANT_NOT_EXISTS, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_ENTITY_LOCKED, HttpStatus.LOCKED);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_ROLLOUT_ILLEGAL_STATE, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_CONFIGURATION_VALUE_INVALID, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_CONFIGURATION_KEY_INVALID, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(SpServerError.SP_REPO_INVALID_TARGET_ADDRESS, HttpStatus.BAD_REQUEST);
    }

    private static HttpStatus getStatusOrDefault(final SpServerError error) {
        return ERROR_TO_HTTP_STATUS.getOrDefault(error, DEFAULT_RESPONSE_STATUS);
    }

    /**
     * method for handling exception of type SpServerRtException. Called by the
     * Spring-Framework for exception handling.
     *
     * @param request
     *            the Http request
     * @param ex
     *            the exception which occurred
     *
     * @return the entity to be responded containing the exception information
     *         as entity.
     */
    @ExceptionHandler(SpServerRtException.class)
    public ResponseEntity<ExceptionInfo> handleSpServerRtExceptions(final HttpServletRequest request,
            final Exception ex) {
        logRequest(request, ex);
        final ExceptionInfo response = createExceptionInfo(ex);
        final HttpStatus responseStatus;
        if (ex instanceof SpServerRtException) {
            responseStatus = getStatusOrDefault(((SpServerRtException) ex).getError());
        } else {
            responseStatus = DEFAULT_RESPONSE_STATUS;
        }
        return new ResponseEntity<>(response, responseStatus);
    }

    /**
     * Method for handling exception of type HttpMessageNotReadableException
     * which is thrown in case the request body is not well formed and cannot be
     * deserialized. Called by the Spring-Framework for exception handling.
     *
     * @param request
     *            the Http request
     * @param ex
     *            the exception which occurred
     * @return the entity to be responded containing the exception information
     *         as entity.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ExceptionInfo> handleHttpMessageNotReadableException(final HttpServletRequest request,
            final Exception ex) {
        logRequest(request, ex);
        final ExceptionInfo response = createExceptionInfo(new MessageNotReadableException());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Method for handling exception of type {@link MultipartException} which is
     * thrown in case the request body is not well formed and cannot be
     * deserialized. Called by the Spring-Framework for exception handling.
     *
     * @param request
     *            the Http request
     * @param ex
     *            the exception which occurred
     * @return the entity to be responded containing the exception information
     *         as entity.
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ExceptionInfo> handleMultipartException(final HttpServletRequest request,
            final Exception ex) {

        logRequest(request, ex);

        final List<Throwable> throwables = ExceptionUtils.getThrowableList(ex);
        final Throwable responseCause = Iterables.getLast(throwables);
        final ExceptionInfo response = createExceptionInfo(new MultiPartFileUploadException(responseCause));
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    private void logRequest(final HttpServletRequest request, final Exception ex) {
        LOG.debug("Handling exception {} of request {}", ex.getClass().getName(), request.getRequestURL());
    }

    private ExceptionInfo createExceptionInfo(final Exception ex) {
        final ExceptionInfo response = new ExceptionInfo();
        response.setMessage(ex.getMessage());
        response.setExceptionClass(ex.getClass().getName());
        if (ex instanceof SpServerRtException) {
            response.setErrorCode(((SpServerRtException) ex).getError().getKey());
        }

        return response;
    }

}