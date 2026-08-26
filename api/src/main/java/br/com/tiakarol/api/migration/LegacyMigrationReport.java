package br.com.tiakarol.api.migration;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

record LegacyMigrationReport(
        UUID runId,
        OffsetDateTime generatedAt,
        String sourceSystem,
        String mode,
        boolean readyForImport,
        long errorCount,
        long warningCount,
        Map<String, Long> sourceCounts,
        List<LegacyMigrationIssue> issues
) {
}
