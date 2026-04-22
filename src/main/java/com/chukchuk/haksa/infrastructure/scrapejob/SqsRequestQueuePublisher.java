package com.chukchuk.haksa.infrastructure.scrapejob;

import com.chukchuk.haksa.global.config.ScrapingProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Qualifier("requestScrapeJobPublisher")
public class SqsRequestQueuePublisher extends AbstractSqsPublisher {

    public SqsRequestQueuePublisher(ScrapingProperties scrapingProperties) {
        super(scrapingProperties);
    }

    @Override
    protected String resolveQueueUrl() {
        return scrapingProperties.getRequestQueue().getQueueUrl();
    }
}
