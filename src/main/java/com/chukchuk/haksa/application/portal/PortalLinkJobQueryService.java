package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.portal.dto.PortalLinkDto;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJob;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortalLinkJobQueryService {

    private final ScrapeJobRepository scrapeJobRepository;

    @Transactional(readOnly = true)
    public PortalLinkDto.JobStatusResponse getJobStatus(UUID userId, String jobId) {
        ScrapeJob job = scrapeJobRepository.findByJobIdAndUserId(jobId, userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SCRAPE_JOB_NOT_FOUND));

        return new PortalLinkDto.JobStatusResponse(
                job.getJobId(),
                job.getPortalType(),
                job.getStatus().name().toLowerCase(Locale.ROOT),
                job.getErrorCode(),
                job.getErrorMessage(),
                job.getRetryable(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                job.getFinishedAt()
        );
    }
}
