package br.sistema.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexao {
    private static final String URL = "jdbc:sqlite:vacincontrol.db";

    public static Connection getConexao() {
        try {
            // Esta linha força o carregamento do driver que você adicionou nas Libraries
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection(URL);
        } catch (ClassNotFoundException e) {
            System.err.println("Driver do SQLite não encontrado nas bibliotecas!");
            return null;
        } catch (SQLException e) {
            System.err.println("Erro ao conectar ao SQLite: " + e.getMessage());
            return null;
        }
    }
}