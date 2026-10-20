package com.foodpassport.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Throw one of these from anywhere and Spring turns it into an HTTP error response
 * with a JSON "problem detail" body, e.g. {"status": 404, "detail": "Stamp 7 not found"}.
 */
public class ApiException extends ResponseStatusException {

    private ApiException(HttpStatus status, String message) {
        super(status, message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, message);
    }
}
