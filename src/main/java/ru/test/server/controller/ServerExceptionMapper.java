package ru.test.server.controller;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ru.test.domain.exception.CsvParserException;
import ru.test.domain.exception.ErrorCode;
import ru.test.domain.exception.MigrationException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Provider
public class ServerExceptionMapper implements ExceptionMapper<Throwable> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable ex) {
        int status = 500;
        String title = "Internal Server Error";
        String errorCode = "UNKNOWN_ERROR";

        if (ex instanceof WebApplicationException wae) {
            status = wae.getResponse().getStatus();
            title = wae.getMessage();
            errorCode = "HTTP_" + status;
        } else if (ex instanceof MigrationException me) {
            errorCode = me.getErrorCode().name();
            if (me.getErrorCode() == ErrorCode.FUTURE_SCHEMA_VERSION) {
                status = 409;
                title = "Future Schema Version Conflict";
            } else {
                status = 400;
                title = "Migration / Schema Validation Error";
            }
        } else if (ex instanceof CsvParserException ce) {
            status = 400;
            title = "Validation Error";
            errorCode = ce.getErrorCode().name();
        } else if (ex instanceof IllegalArgumentException) {
            status = 400;
            title = "Bad Request";
            errorCode = "BAD_REQUEST";
        } else if (ex instanceof NoSuchElementException) {
            status = 404;
            title = "Not Found";
            errorCode = "NOT_FOUND";
        } else if (ex instanceof IllegalStateException) {
            status = 409;
            title = "Conflict";
            errorCode = "CONFLICT";
        } else if (ex instanceof UnsupportedOperationException) {
            status = 422;
            title = "Unprocessable Entity (Read-Only)";
            errorCode = "READ_ONLY_ENTITY";
        }

        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", "about:blank");
        problem.put("title", title);
        problem.put("status", status);
        problem.put("detail", ex.getMessage());
        problem.put("errorCode", errorCode);
        problem.put("instance", uriInfo != null ? uriInfo.getPath() : "");

        return Response.status(status)
                .header("Content-Type", "application/problem+json")
                .entity(problem)
                .build();
    }
}