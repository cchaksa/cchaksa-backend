package com.chukchuk.haksa.infrastructure.portal.client;

import com.chukchuk.haksa.global.config.ScrapingProperties;
import com.chukchuk.haksa.infrastructure.portal.exception.ScrapeResultPayloadAccessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScrapeResultResultStoreClient {

    private static final String ERROR_CODE = "SCRAPE_S3_FAILURE";
    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 200L;

    private final S3Client s3Client;
    private final ScrapingProperties scrapingProperties;

    public String fetch(String requestedLocation) {
        ScrapingProperties.Callback.ResultStore store = scrapingProperties.getCallback().getResultStore();
        S3Location location = resolveLocation(requestedLocation, store);
        return fetchWithRetry(store, location);
    }

    private String fetchWithRetry(ScrapingProperties.Callback.ResultStore store, S3Location location) {
        long backoffMs = INITIAL_BACKOFF_MS;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                HeadObjectResponse head = s3Client.headObject(HeadObjectRequest.builder()
                        .bucket(location.bucket())
                        .key(location.key())
                        .build());
                long contentLength = head.contentLength() == null ? -1L : head.contentLength();
                validateContentLength(store, contentLength);

                ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                        .bucket(location.bucket())
                        .key(location.key())
                        .build());
                byte[] payload = responseBytes.asByteArray();
                if (payload.length > store.getMaxPayloadBytes()) {
                    throw new ScrapeResultPayloadAccessException(
                            ERROR_CODE,
                            "S3 payload exceeds max bytes: " + payload.length,
                            false
                    );
                }
                return new String(payload, StandardCharsets.UTF_8);
            } catch (NoSuchKeyException exception) {
                if (attempt == MAX_ATTEMPTS) {
                    throw new ScrapeResultPayloadAccessException(
                            ERROR_CODE,
                            "S3 key not found: " + location.key(),
                            true,
                            exception
                    );
                }
                sleep(backoffMs);
                backoffMs *= 2;
            } catch (SdkClientException | S3Exception exception) {
                boolean retryable = isRetryable(exception);
                if (!retryable || attempt == MAX_ATTEMPTS) {
                    throw new ScrapeResultPayloadAccessException(
                            ERROR_CODE,
                            "Failed to fetch result from S3: " + exception.getMessage(),
                            retryable,
                            exception
                    );
                }
                sleep(backoffMs);
                backoffMs *= 2;
            }
        }
        try {
            throw new ScrapeResultPayloadAccessException(ERROR_CODE, "S3 fetch attempts exhausted", true);
        } catch (ScrapeResultPayloadAccessException exception) {
            throw exception;
        }
    }

    private void validateContentLength(ScrapingProperties.Callback.ResultStore store, long contentLength) {
        if (contentLength < 0) {
            return;
        }
        if (contentLength > store.getMaxPayloadBytes()) {
            throw new ScrapeResultPayloadAccessException(
                    ERROR_CODE,
                    "S3 payload exceeds max bytes: " + contentLength,
                    false
            );
        }
    }

    private boolean isRetryable(Exception exception) {
        if (exception instanceof S3Exception s3Exception) {
            int status = s3Exception.statusCode();
            if (status == 403) {
                return false;
            }
            return status >= 500 || status == 404;
        }
        return true;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private S3Location resolveLocation(String requestedLocation, ScrapingProperties.Callback.ResultStore store) {
        if (requestedLocation == null || requestedLocation.isBlank()) {
            throw new ScrapeResultPayloadAccessException(ERROR_CODE, "result_s3_key is missing", true);
        }
        String location = requestedLocation.trim();
        String bucket = store.getBucket();
        String key = location;

        if (location.startsWith("http://") || location.startsWith("https://")) {
            throw new ScrapeResultPayloadAccessException(ERROR_CODE, "S3 key must not be a URL", false);
        }
        if (location.startsWith("s3://")) {
            String withoutScheme = location.substring("s3://".length());
            int slashIndex = withoutScheme.indexOf('/');
            if (slashIndex <= 0 || slashIndex == withoutScheme.length() - 1) {
                throw new ScrapeResultPayloadAccessException(ERROR_CODE, "Invalid s3 uri: " + location, false);
            }
            bucket = withoutScheme.substring(0, slashIndex);
            key = withoutScheme.substring(slashIndex + 1);
        } else if (location.startsWith("/")) {
            key = location.substring(1);
        }

        if (bucket == null || bucket.isBlank()) {
            throw new ScrapeResultPayloadAccessException(ERROR_CODE, "Result store bucket is not configured", false);
        }
        if (!bucket.equals(store.getBucket())) {
            throw new ScrapeResultPayloadAccessException(ERROR_CODE, "Bucket not allowed: " + bucket, false);
        }

        String prefix = store.getPrefix();
        if (key.isBlank()) {
            throw new ScrapeResultPayloadAccessException(ERROR_CODE, "S3 key is blank", false);
        }
        if (key.contains("..") || key.contains("//")) {
            throw new ScrapeResultPayloadAccessException(ERROR_CODE, "S3 key contains invalid path traversal", false);
        }
        if (prefix != null && !prefix.isBlank() && !key.startsWith(prefix)) {
            throw new ScrapeResultPayloadAccessException(ERROR_CODE, "S3 key outside allowed prefix", false);
        }

        return new S3Location(bucket, key);
    }

    private record S3Location(String bucket, String key) {}
}
