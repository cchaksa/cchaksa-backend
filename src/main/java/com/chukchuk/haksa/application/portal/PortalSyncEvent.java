package com.chukchuk.haksa.application.portal;

import java.util.UUID;

public record PortalSyncEvent(Long jobId, UUID userId) {
}
