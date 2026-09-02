package br.sistema.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {
    public static Connection getConnection() {
        String url = required("LEGACY_DATABASE_URL", "legacy.database.url");
        String user = required("LEGACY_DATABASE_USERNAME", "legacy.database.username");
        String password = required("LEGACY_DATABASE_PASSWORD", "legacy.database.password");
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new IllegalStateException("Não foi possível conectar ao banco legado. Verifique a configuração.", e);
        }
    }

    private static String required(String environmentVariable, String systemProperty) {
        String value = System.getenv(environmentVariable);
        if (value == null || value.isBlank()) value = System.getProperty(systemProperty);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Configuração obrigatória ausente: " + environmentVariable
                    + " (ou -D" + systemProperty + ").");
        }
        return value;
    }
}
