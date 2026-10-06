package com.chukchuk.haksa.domain.portal.controller;

import com.chukchuk.haksa.application.portal.ScrapeResultCallbackService;
import com.chukchuk.haksa.domain.portal.controller.docs.PortalLinkCallbackControllerDocs;
import com.chukchuk.haksa.global.common.response.MessageOnlyResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** 스크래핑 워커의 서명된 결과 콜백을 검증 서비스에 전달한다. */
@RestController
@RequestMapping("/internal/scrape-results")
@RequiredArgsConstructor
public class InternalScrapeResultController implements PortalLinkCallbackControllerDocs {

  private final ScrapeResultCallbackService scrapeResultCallbackService;

  @Override
  @PostMapping
  public ResponseEntity<SuccessResponse<MessageOnlyResponse>> handleCallback(
      @RequestBody String rawBody,
      @RequestHeader("X-Timestamp") String timestamp,
      @RequestHeader("X-Signature") String signature) {
    ServletRequestAttributes requestAttributes =
        (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    String attemptHeader = requestAttributes.getRequest().getHeader("X-Callback-Attempt");
    String workerRequestId = requestAttributes.getRequest().getHeader("X-Request-Id");
    scrapeResultCallbackService.handleCallback(
        rawBody, timestamp, signature, attemptHeader, workerRequestId);
    return ResponseEntity.ok(SuccessResponse.of(new MessageOnlyResponse("콜백 처리 완료")));
  }
}
