package com.malafi.payments.malafi_payments.idempotency;

import org.springframework.http.HttpStatus;

public record IdempotencyResult<T>(
        T response,
        HttpStatus status
) {
}
