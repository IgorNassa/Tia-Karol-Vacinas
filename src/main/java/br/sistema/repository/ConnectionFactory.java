package br.sistema.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {
    // Define o nome do arquivo do banco de dados. Ele será criado na raiz do seu projeto.
    private static final String URL = "jdbc:sqlite:vacincontrol.db";

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao conectar com o banco de dados SQLite: " + e.getMessage());
        }
    }
}