package com.ya.recallio.activity.service;

import com.ya.recallio.activity.model.ActivityLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;

public interface ActivityLogService {

    ActivityLog createLog(ActivityLogCreateRequest request);

    ActivityLog getOwnedLog(UUID userId, UUID activityLogId);

    Page<ActivityLog> getOwnedHistory(ActivityHistoryQuery query);

    List<ActivityLog> getRecentLogs(UUID userId, int limit);

    void deleteOwnedLog(UUID userId, UUID activityLogId);
}
