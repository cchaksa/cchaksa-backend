package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.portal.dto.PortalLinkDto;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJob;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOperationType;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobRepository;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.service.UserService;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.BaseException;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import com.chukchuk.haksa.infrastructure.portal.dto.raw.RawPortalData;
import com.chukchuk.haksa.infrastructure.portal.mapper.PortalDataMapper;
import com.chukchuk.haksa.infrastructure.portal.model.PortalData;
import com.chukchuk.haksa.infrastructure.security.HmacSignatureVerifier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapeResultCallbackService {

    private final ScrapeJobRepository scrapeJobRepository;
    private final PortalSyncService portalSyncService;
    private final UserService userService;
    private final StudentService studentService;
    private final AcademicCache academicCache;
    private final HmacSignatureVerifier hmacSignatureVerifier;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Transactional
    public void handleCallback(String rawBody, String timestamp, String signature) {
        hmacSignatureVerifier.verify(timestamp, rawBody, signature);
        PortalLinkDto.ScrapeResultCallbackRequest request = parseRequest(rawBody);
        ScrapeJob job = scrapeJobRepository.findForUpdateByJobId(request.job_id())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SCRAPE_JOB_NOT_FOUND));

        if (job.isCompleted()) {
            log.info("[BIZ] scrape.job.callback.duplicate jobId={} status={}", job.getJobId(), job.getStatus());
            return;
        }

        Instant finishedAt = request.finished_at() == null ? Instant.now() : request.finished_at();
        String normalizedStatus = normalize(request.status());

        if ("succeeded".equals(normalizedStatus)) {
            handleSucceeded(job, request, finishedAt);
            return;
        }

        if ("failed".equals(normalizedStatus)) {
            job.markFailed(request.error_code(), request.error_message(), request.retryable(), finishedAt);
            log.info("[BIZ] scrape.job.failed jobId={} errorCode={} retryable={}",
                    job.getJobId(), request.error_code(), request.retryable());
            return;
        }

        throw new CommonException(ErrorCode.INVALID_ARGUMENT);
    }

    private PortalLinkDto.ScrapeResultCallbackRequest parseRequest(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, PortalLinkDto.ScrapeResultCallbackRequest.class);
        } catch (JsonProcessingException e) {
            throw new CommonException(ErrorCode.INVALID_ARGUMENT, e);
        }
    }

    private void handleSucceeded(ScrapeJob job, PortalLinkDto.ScrapeResultCallbackRequest request, Instant finishedAt) {
        try {
            if (request.result_payload() == null || request.result_payload().isNull()) {
                throw new CommonException(ErrorCode.INVALID_ARGUMENT);
            }
            RawPortalData rawPortalData = objectMapper.treeToValue(request.result_payload(), RawPortalData.class);
            PortalData portalData = PortalDataMapper.toPortalData(rawPortalData);

            if (job.getOperationType() == ScrapeJobOperationType.LINK) {
                applyLink(job, portalData);
            } else {
                applyRefresh(job, portalData);
            }

            job.markSucceeded(writeJson(request.result_payload()), finishedAt);
            log.info("[BIZ] scrape.job.succeeded jobId={} operationType={}", job.getJobId(), job.getOperationType());
        } catch (BaseException | IllegalArgumentException e) {
            job.markFailed("BUSINESS_RULE_VIOLATION", e.getMessage(), false, finishedAt);
            log.warn("[BIZ] scrape.job.business_fail jobId={} operationType={} message={}",
                    job.getJobId(), job.getOperationType(), e.getMessage());
        } catch (JsonProcessingException e) {
            throw new CommonException(ErrorCode.INVALID_ARGUMENT, e);
        }
    }

    private void applyLink(ScrapeJob job, PortalData portalData) {
        UUID userId = job.getUserId();
        User mergedUser = userService.tryMergeWithExistingUser(userId, portalData.student().studentCode());
        if (Boolean.TRUE.equals(mergedUser.getPortalConnected())) {
            log.info("[BIZ] scrape.job.link.skip jobId={} mergedUserId={} reason=already_connected",
                    job.getJobId(), mergedUser.getId());
            return;
        }

        portalSyncService.syncWithPortal(mergedUser.getId(), portalData);
    }

    private void applyRefresh(ScrapeJob job, PortalData portalData) {
        portalSyncService.refreshFromPortal(job.getUserId(), portalData);
        UUID studentId = studentService.getRequiredStudentIdByUserId(job.getUserId());
        academicCache.deleteAllByStudentId(studentId);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new CommonException(ErrorCode.INVALID_ARGUMENT, e);
        }
    }

    private String normalize(String status) {
        return status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
    }
}
