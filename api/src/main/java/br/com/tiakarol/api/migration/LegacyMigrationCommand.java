package br.com.tiakarol.api.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LegacyMigrationCommand {
    private LegacyMigrationCommand() {
    }

    public static void main(String[] args) throws Exception {
        String mode = environment("LEGACY_MIGRATION_MODE", "dry-run");
        if (!"dry-run".equalsIgnoreCase(mode)) {
            throw new IllegalArgumentException("Somente LEGACY_MIGRATION_MODE=dry-run está liberado nesta fase.");
        }
        String url = required("LEGACY_DATABASE_URL");
        String username = required("LEGACY_DATABASE_USERNAME");
        String password = required("LEGACY_DATABASE_PASSWORD");
        Path output = Path.of(environment("LEGACY_MIGRATION_REPORT", "legacy-migration-report.json"))
                .toAbsolutePath().normalize();

        LegacySnapshot snapshot = new JdbcLegacySnapshotReader().read(url, username, password);
        LegacyMigrationReport report = new LegacyMigrationAnalyzer().analyze(snapshot);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules().enable(SerializationFeature.INDENT_OUTPUT);
        if (output.getParent() != null) Files.createDirectories(output.getParent());
        mapper.writeValue(output.toFile(), report);

        System.out.printf("Dry-run concluído: %d erro(s), %d aviso(s). Relatório: %s%n",
                report.errorCount(), report.warningCount(), output);
        if (!report.readyForImport()) System.exit(2);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Variável obrigatória ausente: " + name);
        }
        return value;
    }

    private static String environment(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
