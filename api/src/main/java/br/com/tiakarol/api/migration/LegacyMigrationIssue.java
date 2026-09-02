package br.com.tiakarol.api.migration;

record LegacyMigrationIssue(Severity severity, String code, String entityType, String legacyId, String message) {
    enum Severity {
        WARNING,
        ERROR
    }
}
