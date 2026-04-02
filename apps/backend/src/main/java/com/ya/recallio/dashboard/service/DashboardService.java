package com.ya.recallio.dashboard.service;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface DashboardService {

    DashboardSummary buildSummary(UUID userId, OffsetDateTime referenceTime);
}
