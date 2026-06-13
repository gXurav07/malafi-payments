package com.malafi.payments.malafi_payments.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.malafi.payments.malafi_payments.payment.dto.PaymentResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.Supplier;

@Service
public class IdempotencyService {

    private static final int MAX_KEY_LENGTH = 128;

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public IdempotencyService(IdempotencyRecordRepository idempotencyRecordRepository) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
    }

    public IdempotencyResult<PaymentResponse> executePaymentResponse(
            String idempotencyKey,
            String requestFingerprint,
            HttpStatus successStatus,
            Supplier<PaymentResponse> action) {
        if (idempotencyKey == null) {
            return new IdempotencyResult<>(action.get(), successStatus);
        }

        validateKey(idempotencyKey);

        String requestHash = sha256(requestFingerprint);
        IdempotencyRecord record = tryCreateProcessingRecord(idempotencyKey, requestHash);
        if (record == null) {
            return resolveExistingRecord(idempotencyKey, requestHash);
        }

        PaymentResponse response;
        try {
            response = action.get();
            record.complete(successStatus.value(), serialize(response));
            idempotencyRecordRepository.saveAndFlush(record);
        } catch (RuntimeException exception) {
            idempotencyRecordRepository.delete(record);
            throw exception;
        }

        return new IdempotencyResult<>(response, successStatus);
    }

    private IdempotencyRecord tryCreateProcessingRecord(String idempotencyKey, String requestHash) {
        try {
            return idempotencyRecordRepository.saveAndFlush(new IdempotencyRecord(idempotencyKey, requestHash));
        } catch (DataIntegrityViolationException exception) {
            return null;
        }
    }

    private IdempotencyResult<PaymentResponse> resolveExistingRecord(String idempotencyKey, String requestHash) {
        IdempotencyRecord record = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Idempotency-Key is already being claimed"
                ));

        if (!record.getRequestHash().equals(requestHash)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Idempotency-Key was already used with a different request"
            );
        }

        if (record.getStatus() == IdempotencyStatus.PROCESSING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Idempotent request is already being processed"
            );
        }

        return new IdempotencyResult<>(
                deserializePaymentResponse(record.getResponseBody()),
                HttpStatus.valueOf(record.getResponseStatus())
        );
    }

    private void validateKey(String idempotencyKey) {
        if (idempotencyKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key cannot be blank");
        }

        if (idempotencyKey.length() > MAX_KEY_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key cannot exceed 128 characters");
        }
    }

    public String fingerprint(String operation, Object request) {
        return operation + ":" + serialize(request);
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize idempotency payload", exception);
        }
    }

    private PaymentResponse deserializePaymentResponse(String responseBody) {
        try {
            return objectMapper.readValue(responseBody, PaymentResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize idempotency response", exception);
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
