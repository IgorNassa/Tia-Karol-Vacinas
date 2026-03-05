package br.sistema.util;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

public class DbConfig {

    public static void criarTabelas() {
        String pacienteSQL = "CREATE TABLE IF NOT EXISTS Paciente (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nome TEXT NOT NULL," +
                "cpf TEXT UNIQUE," +
                "data_nascimento TEXT," +
                "alergias TEXT" +
                ");";

        String estoqueSQL = "CREATE TABLE IF NOT EXISTS Estoque (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nome_vacina TEXT NOT NULL," +
                "lote TEXT," +
                "validade TEXT," +
                "laboratorio TEXT," +
                "qtd_total INTEGER," +
                "qtd_disponivel INTEGER," +
                "valor_compra REAL," +
                "valor_venda REAL" +
                ");";

        String aplicacaoSQL = "CREATE TABLE IF NOT EXISTS Aplicacao (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "paciente_id INTEGER," +
                "estoque_id INTEGER," +
                "data_hora TEXT," +
                "forma_pagamento TEXT," +
                "num_parcelas INTEGER," +
                "valor_bruto REAL," +
                "desconto REAL," +
                "valor_final REAL," +
                "usuario TEXT," +
                "FOREIGN KEY (paciente_id) REFERENCES Paciente(id)," +
                "FOREIGN KEY (estoque_id) REFERENCES Estoque(id)" +
                ");";

        try (Connection conn = Conexao.getConexao()) {
            if (conn == null) {
                System.err.println("Erro: A conexão retornou nula. Verifique se o driver JDBC foi adicionado!");
                return; // Sai do método se não houver conexão
            }

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(pacienteSQL);
                stmt.execute(estoqueSQL);
                stmt.execute(aplicacaoSQL);
                System.out.println("Banco de dados pronto para uso!");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}