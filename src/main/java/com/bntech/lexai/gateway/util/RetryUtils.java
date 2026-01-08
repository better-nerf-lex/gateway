package com.bntech.lexai.gateway.util;

import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

public final class RetryUtils {

    private RetryUtils() {
    }

    public static boolean isRetryableException(Throwable throwable) {
        return throwable instanceof ResourceAccessException
                || throwable instanceof TimeoutException
                || throwable instanceof IOException
                || (throwable.getCause() instanceof ReadTimeoutException)
                || (throwable.getCause() instanceof WriteTimeoutException)
                || (throwable.getCause() instanceof SocketTimeoutException)
                || (throwable.getCause() instanceof ConnectException)
                || (throwable.getMessage() != null && (throwable.getMessage().contains("timeout") ||
                throwable.getMessage().contains("connection") ||
                throwable.getMessage().contains("503") ||
                throwable.getMessage().contains("502") ||
                throwable.getMessage().contains("rate_limit")));
    }
}
