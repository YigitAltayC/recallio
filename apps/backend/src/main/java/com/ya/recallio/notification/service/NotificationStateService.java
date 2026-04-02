package com.ya.recallio.notification.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationStateService {

    List<NotificationCandidate> getCandidates(UUID userId, OffsetDateTime referenceTime);
}
