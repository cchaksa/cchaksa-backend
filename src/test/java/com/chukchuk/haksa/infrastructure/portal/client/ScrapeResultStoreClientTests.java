package com.chukchuk.haksa.infrastructure.portal.client;

import com.chukchuk.haksa.global.config.ScrapingProperties;
import com.chukchuk.haksa.infrastructure.portal.exception.ScrapeResultPayloadAccessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScrapeResultStoreClientTests {

    @Test
    @DisplayName("허용 prefix 다음 첫 path segment가 jobId일 때만 유효한 key로 본다")
    void isJobScopedLocation_matchesFirstSegmentAfterPrefix() {
        ScrapingProperties properties = properties();
        ScrapeResultStoreClient client = new ScrapeResultStoreClient(Mockito.mock(S3Client.class), properties);

        assertThat(client.isJobScopedLocation(
                new ScrapeResultStoreClient.S3Location("bucket", "develop-shadow/job-1/result.json"),
                "job-1"
        )).isTrue();
        assertThat(client.isJobScopedLocation(
                new ScrapeResultStoreClient.S3Location("bucket", "develop-shadow/not-job-1/result.json"),
                "job-1"
        )).isFalse();
        assertThat(client.isJobScopedLocation(
                new ScrapeResultStoreClient.S3Location("bucket", "develop-shadow/other/job-1/result.json"),
                "job-1"
        )).isFalse();
    }

    @Test
    @DisplayName("URL 형태 key는 거부한다")
    void validateLocation_rejectsUrl() {
        ScrapeResultStoreClient client = new ScrapeResultStoreClient(Mockito.mock(S3Client.class), properties());

        assertThatThrownBy(() -> client.validateLocation("https://example.com/result.json"))
                .isInstanceOf(ScrapeResultPayloadAccessException.class);
    }

    private ScrapingProperties properties() {
        ScrapingProperties properties = new ScrapingProperties();
        properties.getResultStore().setBucket("bucket");
        properties.getResultStore().setPrefix("develop-shadow/");
        return properties;
    }
}
