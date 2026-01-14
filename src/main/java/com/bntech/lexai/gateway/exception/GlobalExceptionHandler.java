package com.bntech.lexai.gateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(ResourceNotFoundException.class)
        public Mono<ResponseEntity<ErrorResponse>> handleResourceNotFound(
                        ResourceNotFoundException ex, ServerWebExchange exchange) {
                log.warn("Resource not found: {}", ex.getMessage());
                String path = exchange.getRequest().getPath().value();
                ErrorResponse response = ErrorResponse.builder()
                                .timestamp(java.time.LocalDateTime.now())
                                .status(HttpStatus.NOT_FOUND.value())
                                .error("Not Found")
                                .message(ex.getMessage())
                                .path(path)
                                .resourceType(ex.getResourceType())
                                .resourceId(ex.getResourceId())
                                .build();
                return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(response));
        }

        @ExceptionHandler(BadRequestException.class)
        public Mono<ResponseEntity<ErrorResponse>> handleBadRequest(
                        BadRequestException ex, ServerWebExchange exchange) {
                log.warn("Bad request: {}", ex.getMessage());
                String path = exchange.getRequest().getPath().value();
                ErrorResponse response = ErrorResponse.builder()
                                .timestamp(java.time.LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("Bad Request")
                                .message(ex.getMessage())
                                .path(path)
                                .field(ex.getField())
                                .build();
                return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
        }

        @ExceptionHandler(UnauthorizedException.class)
        public Mono<ResponseEntity<ErrorResponse>> handleUnauthorized(
                        UnauthorizedException ex, ServerWebExchange exchange) {
                log.warn("Unauthorized: {}", ex.getMessage());
                String path = exchange.getRequest().getPath().value();
                ErrorResponse response = ErrorResponse.of(
                                HttpStatus.UNAUTHORIZED.value(),
                                "Unauthorized",
                                ex.getMessage(),
                                path);
                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response));
        }

        @ExceptionHandler(ForbiddenException.class)
        public Mono<ResponseEntity<ErrorResponse>> handleForbidden(
                        ForbiddenException ex, ServerWebExchange exchange) {
                log.warn("Forbidden: {}", ex.getMessage());
                String path = exchange.getRequest().getPath().value();
                ErrorResponse response = ErrorResponse.of(
                                HttpStatus.FORBIDDEN.value(),
                                "Forbidden",
                                ex.getMessage(),
                                path);
                return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(response));
        }

        @ExceptionHandler(AccessDeniedException.class)
        public Mono<ResponseEntity<ErrorResponse>> handleAccessDenied(
                        AccessDeniedException ex, ServerWebExchange exchange) {
                log.warn("Access denied: {}", ex.getMessage());
                String path = exchange.getRequest().getPath().value();
                ErrorResponse response = ErrorResponse.of(
                                HttpStatus.FORBIDDEN.value(),
                                "Forbidden",
                                "Access denied",
                                path);
                return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(response));
        }

        @ExceptionHandler(ConflictException.class)
        public Mono<ResponseEntity<ErrorResponse>> handleConflict(
                        ConflictException ex, ServerWebExchange exchange) {
                log.warn("Conflict: {}", ex.getMessage());
                String path = exchange.getRequest().getPath().value();
                ErrorResponse response = ErrorResponse.builder()
                                .timestamp(java.time.LocalDateTime.now())
                                .status(HttpStatus.CONFLICT.value())
                                .error("Conflict")
                                .message(ex.getMessage())
                                .path(path)
                                .resourceType(ex.getResourceType())
                                .build();
                return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(response));
        }

        @ExceptionHandler(RateLimitException.class)
        public Mono<ResponseEntity<ErrorResponse>> handleRateLimitException(
                        RateLimitException ex, ServerWebExchange exchange) {
                log.warn("Rate limit exceeded: {} (retry after: {} seconds)", ex.getMessage(), ex.getRetryAfter());
                String path = exchange.getRequest().getPath().value();
                ErrorResponse response = ErrorResponse.builder()
                                .timestamp(java.time.LocalDateTime.now())
                                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                                .error("Rate Limit Exceeded")
                                .message(ex.getMessage())
                                .path(path)
                                .retryAfter(ex.getRetryAfter())
                                .build();
                return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response));
        }

        @ExceptionHandler(ServiceException.class)
        public Mono<ResponseEntity<ErrorResponse>> handleServiceException(
                        ServiceException ex, ServerWebExchange exchange) {
                log.error("Service error: {}", ex.getMessage(), ex);
                String path = exchange.getRequest().getPath().value();
                ErrorResponse response = ErrorResponse.of(
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "Internal Server Error",
                                ex.getMessage(),
                                path);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
        }

        @ExceptionHandler(AuthenticationException.class)
        public Mono<ResponseEntity<ErrorResponse>> handleAuthenticationException(
                        AuthenticationException ex, ServerWebExchange exchange) {
                log.warn("Authentication failed: {}", ex.getMessage());
                String path = exchange.getRequest().getPath().value();
                ErrorResponse response = ErrorResponse.of(
                                HttpStatus.UNAUTHORIZED.value(),
                                "Unauthorized",
                                "Authentication failed",
                                path);
                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response));
        }

        @ExceptionHandler(WebExchangeBindException.class)
        public Mono<ResponseEntity<ErrorResponse>> handleValidationException(
                        WebExchangeBindException ex, ServerWebExchange exchange) {
                log.warn("Validation error: {}", ex.getMessage());
                String path = exchange.getRequest().getPath().value();
                String message = ex.getBindingResult().getFieldErrors().stream()
                                .findFirst()
                                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                                .orElse("Validation failed");
                String field = ex.getBindingResult().getFieldErrors().stream()
                                .findFirst()
                                .map(error -> error.getField())
                                .orElse(null);
                ErrorResponse response = ErrorResponse.builder()
                                .timestamp(java.time.LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("Bad Request")
                                .message(message)
                                .path(path)
                                .field(field)
                                .build();
                return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public Mono<ResponseEntity<ErrorResponse>> handleIllegalArgument(
                        IllegalArgumentException ex, ServerWebExchange exchange) {
                log.warn("Illegal argument: {}", ex.getMessage());
                String path = exchange.getRequest().getPath().value();
                ErrorResponse response = ErrorResponse.of(
                                HttpStatus.BAD_REQUEST.value(),
                                "Bad Request",
                                ex.getMessage(),
                                path);
                return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
        }

        @ExceptionHandler(org.springframework.web.reactive.resource.NoResourceFoundException.class)
        public Mono<ResponseEntity<ErrorResponse>> handleNoResourceFound(
                        org.springframework.web.reactive.resource.NoResourceFoundException ex,
                        ServerWebExchange exchange) {
                log.warn("Resource not found: {}", ex.getMessage());
                String path = exchange.getRequest().getPath().value();
                ErrorResponse response = ErrorResponse.of(
                                HttpStatus.NOT_FOUND.value(),
                                "Not Found",
                                "The requested resource was not found",
                                path);
                return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(response));
        }

        @ExceptionHandler(Exception.class)
        public Mono<ResponseEntity<ErrorResponse>> handleGenericException(
                        Exception ex, ServerWebExchange exchange) {
                log.error("Unexpected error: {}", ex.getMessage(), ex);
                String path = exchange.getRequest().getPath().value();
                ErrorResponse response = ErrorResponse.of(
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "Internal Server Error",
                                "An unexpected error occurred",
                                path);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
        }
}
