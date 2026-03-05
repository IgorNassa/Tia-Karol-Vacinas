package br.sistema.repository;

import br.sistema.model.Aplicacao;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AplicacaoDAO {

    public AplicacaoDAO() {
        criarTabelaSeNaoExistir();
    }

    private void criarTabelaSeNaoExistir() {
        String sql = "CREATE TABLE IF NOT EXISTS aplicacoes ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "paciente TEXT NOT NULL,"
                + "data_hora TEXT NOT NULL,"
                + "vacina TEXT NOT NULL,"
                + "status TEXT NOT NULL,"
                + "forma_pagamento TEXT,"
                + "valor REAL)";

        try (Connection conn = ConnectionFactory.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println("Erro ao criar tabela: " + e.getMessage());
        }
    }

    // CREATE (Salvar do Formulário)
    public void salvar(Aplicacao app) {
        String sql = "INSERT INTO aplicacoes (paciente, data_hora, vacina, status, forma_pagamento, valor) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, app.getPaciente());
            pstmt.setString(2, app.getDataHora());
            pstmt.setString(3, app.getVacina());
            pstmt.setString(4, app.getStatus());
            pstmt.setString(5, app.getFormaPagamento());
            pstmt.setDouble(6, app.getValor());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erro ao salvar aplicação: " + e.getMessage());
        }
    }

    // READ (Carregar a Tabela do Dashboard)
    public List<Aplicacao> listarTodas() {
        List<Aplicacao> lista = new ArrayList<>();
        String sql = "SELECT * FROM aplicacoes ORDER BY id DESC"; // Traz as mais recentes primeiro

        try (Connection conn = ConnectionFactory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Aplicacao app = new Aplicacao();
                app.setId(rs.getInt("id"));
                app.setPaciente(rs.getString("paciente"));
                app.setDataHora(rs.getString("data_hora"));
                app.setVacina(rs.getString("vacina"));
                app.setStatus(rs.getString("status"));
                app.setFormaPagamento(rs.getString("forma_pagamento"));
                app.setValor(rs.getDouble("valor"));
                lista.add(app);
            }
        } catch (SQLException e) {
            System.out.println("Erro ao listar aplicações: " + e.getMessage());
        }
        return lista;
    }

    // UPDATE RÁPIDO (Botão Direito: Marcar como Aplicada)
    public void atualizarStatus(int id, String novoStatus) {
        String sql = "UPDATE aplicacoes SET status = ? WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, novoStatus);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erro ao atualizar status: " + e.getMessage());
        }
    }

    // DELETE (Botão Direito: Excluir)
    public void excluir(int id) {
        String sql = "DELETE FROM aplicacoes WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erro ao excluir aplicação: " + e.getMessage());
        }
    }
}