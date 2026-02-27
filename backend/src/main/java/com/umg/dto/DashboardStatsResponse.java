package com.umg.dto;

import lombok.*;

/**
 * Response DTO for aggregated dashboard statistics.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {

    private long totalUsers;
    private long totalTools;
    private long approvedTools;
    private long pendingTools;
    private long rejectedTools;
    private long totalExecutionsToday;
    private long successfulExecutionsToday;
    private long failedExecutionsToday;
    private long totalApiKeys;
}
