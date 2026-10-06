package com.chukchuk.haksa.infrastructure.portal.client;

import com.chukchuk.haksa.global.config.ScrapingProperties;
import com.chukchuk.haksa.infrastructure.portal.exception.ScrapeResultPayloadAccessException;
import java.nio.charset.StandardCharsets;
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

/** 외부 저장소에서 스크래핑 결과 payload를 조회한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScrapeResultStoreClient {

  private static final String ERROR_CODE = "SCRAPE_S3_FAILURE";
  private static final int MAX_ATTEMPTS = 3;
  private static final long INITIAL_BACKOFF_MS = 200L;

  private final S3Client s3Client;
  private final ScrapingProperties scrapingProperties;

  /**
   * 허용된 S3 위치의 스크래핑 결과를 크기 검증 후 UTF-8 문자열로 조회한다.
   *
   * <p>일시적인 S3 오류와 key 미발견은 지수 backoff로 최대 세 번 시도한다.
   *
   * @param requestedLocation 설정된 bucket의 key 또는 {@code s3://bucket/key} 형식 위치
   * @return 최대 payload 크기를 넘지 않는 UTF-8 결과 본문
   * @throws ScrapeResultPayloadAccessException 위치가 허용되지 않거나 S3 조회에 실패한 경우
   */
  public String fetch(String requestedLocation) {
    S3Location location = validateLocation(requestedLocation);
    return fetchWithRetry(location);
  }

  /**
   * 요청 위치를 설정된 bucket과 prefix 안의 S3 위치로 정규화하고 검증한다.
   *
   * @param requestedLocation key 또는 {@code s3://bucket/key} 형식 위치
   * @return 설정된 결과 저장소 안에서 정규화한 bucket과 key
   * @throws ScrapeResultPayloadAccessException URL, 다른 bucket, 경로 탐색 또는 허용 prefix 밖인 경우
   */
  public S3Location validateLocation(String requestedLocation) {
    return resolveLocation(requestedLocation, scrapingProperties.getResultStore());
  }

  /**
   * S3 key의 허용 prefix 바로 다음 경로가 지정한 작업 식별자인지 확인한다.
   *
   * @param location 검증이 끝난 결과 저장소 위치
   * @param jobId 작업 식별자
   * @return 허용된 prefix 아래 첫 경로가 작업 식별자와 같으면 {@code true}
   */
  public boolean isJobScopedLocation(S3Location location, String jobId) {
    if (jobId == null || jobId.isBlank()) {
      return false;
    }
    String key = location.key();
    String prefix = scrapingProperties.getResultStore().getPrefix();
    String remainder = key;
    if (prefix != null && !prefix.isBlank()) {
      if (!key.startsWith(prefix)) {
        return false;
      }
      remainder = key.substring(prefix.length());
    }

    int slashIndex = remainder.indexOf('/');
    if (slashIndex <= 0) {
      return false;
    }
    return remainder.substring(0, slashIndex).equals(jobId);
  }

  private String fetchWithRetry(S3Location location) {
    ScrapingProperties.ResultStore store = scrapingProperties.getResultStore();
    long backoffMs = INITIAL_BACKOFF_MS;
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        HeadObjectResponse head =
            s3Client.headObject(
                HeadObjectRequest.builder().bucket(location.bucket()).key(location.key()).build());
        long contentLength = head.contentLength() == null ? -1L : head.contentLength();
        validateContentLength(store, contentLength);

        ResponseBytes<GetObjectResponse> responseBytes =
            s3Client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(location.bucket()).key(location.key()).build());
        byte[] payload = responseBytes.asByteArray();
        if (payload.length > store.getMaxPayloadBytes()) {
          throw new ScrapeResultPayloadAccessException(
              ERROR_CODE, "S3 payload exceeds max bytes: " + payload.length, false);
        }
        return new String(payload, StandardCharsets.UTF_8);
      } catch (NoSuchKeyException exception) {
        if (attempt == MAX_ATTEMPTS) {
          throw new ScrapeResultPayloadAccessException(
              ERROR_CODE, "S3 key not found: " + location.key(), true, exception);
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
              exception);
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

  private void validateContentLength(ScrapingProperties.ResultStore store, long contentLength) {
    if (contentLength < 0) {
      return;
    }
    if (contentLength > store.getMaxPayloadBytes()) {
      throw new ScrapeResultPayloadAccessException(
          ERROR_CODE, "S3 payload exceeds max bytes: " + contentLength, false);
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

  private S3Location resolveLocation(
      String requestedLocation, ScrapingProperties.ResultStore store) {
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
        throw new ScrapeResultPayloadAccessException(
            ERROR_CODE, "Invalid s3 uri: " + location, false);
      }
      bucket = withoutScheme.substring(0, slashIndex);
      key = withoutScheme.substring(slashIndex + 1);
    } else if (location.startsWith("/")) {
      key = location.substring(1);
    }

    if (bucket == null || bucket.isBlank()) {
      throw new ScrapeResultPayloadAccessException(
          ERROR_CODE, "Result store bucket is not configured", false);
    }
    if (!bucket.equals(store.getBucket())) {
      throw new ScrapeResultPayloadAccessException(
          ERROR_CODE, "Bucket not allowed: " + bucket, false);
    }

    String prefix = store.getPrefix();
    if (key.isBlank()) {
      throw new ScrapeResultPayloadAccessException(ERROR_CODE, "S3 key is blank", false);
    }
    if (key.contains("..") || key.contains("//")) {
      throw new ScrapeResultPayloadAccessException(
          ERROR_CODE, "S3 key contains invalid path traversal", false);
    }
    if (prefix != null && !prefix.isBlank() && !key.startsWith(prefix)) {
      throw new ScrapeResultPayloadAccessException(
          ERROR_CODE, "S3 key outside allowed prefix", false);
    }

    return new S3Location(bucket, key);
  }

  /**
   * 검증이 끝난 스크래핑 결과 객체의 S3 bucket과 key를 전달한다.
   *
   * @param bucket 설정에서 허용한 S3 bucket
   * @param key 허용 prefix 안의 객체 key
   */
  public record S3Location(String bucket, String key) {}
}
