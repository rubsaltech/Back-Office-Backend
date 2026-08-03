package com.backoffice.pos.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Base for domain errors that map to a specific HTTP status. */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
