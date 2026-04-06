package br.sistema.repository;

import br.sistema.model.Aplicacao;
import br.sistema.model.Paciente;
import br.sistema.model.Vacina;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AplicacaoDAO {

    public AplicacaoDAO() {
        criarTabelaSeNaoExistir();
    }

    private void criarTabelaSeNaoExistir() {
        String sql = "CREATE TABLE IF NOT EXISTS aplicacoes_v2 ("
                + "id SERIAL PRIMARY KEY,"
                + "paciente_id INTEGER NOT NULL,"
                + "vacina_id INTEGER NOT NULL,"
                + "data_hora TIMESTAMP NOT NULL,"
                + "status VARCHAR(50) NOT NULL,"
                + "forma_pagamento VARCHAR(50),"
                + "valor NUMERIC(10,2),"
                + "reacoes TEXT,"
                + "observacoes_adicionais TEXT,"
                + "FOREIGN KEY(paciente_id) REFERENCES pacientes(id),"
                + "FOREIGN KEY(vacina_id) REFERENCES vacinas(id))";

        try (Connection conn = ConnectionFactory.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Erro ao criar tabela aplicacoes_v2: " + e.getMessage());
        }
    }

    public boolean salvar(Aplicacao app) {
        String sql = "INSERT INTO aplicacoes_v2 (paciente_id, vacina_id, data_hora, status, forma_pagamento, valor, reacoes, observacoes_adicionais) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, app.getPaciente().getId());
            stmt.setInt(2, app.getVacina().getId());
            stmt.setTimestamp(3, java.sql.Timestamp.valueOf(app.getDataHora()));
            stmt.setString(4, app.getStatus());
            stmt.setString(5, app.getFormaPagamento());
            stmt.setDouble(6, app.getValor());
            stmt.setString(7, app.getReacoes());
            stmt.setString(8, app.getObservacoesAdicionais());

            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao salvar: " + e.getMessage());
            return false;
        }
    }

    public boolean atualizar(Aplicacao app) {
        String sql = "UPDATE aplicacoes_v2 SET paciente_id=?, vacina_id=?, data_hora=?, status=?, forma_pagamento=?, valor=?, reacoes=?, observacoes_adicionais=? WHERE id=?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, app.getPaciente().getId());
            stmt.setInt(2, app.getVacina().getId());
            stmt.setTimestamp(3, Timestamp.valueOf(app.getDataHora()));
            stmt.setString(4, app.getStatus());
            stmt.setString(5, app.getFormaPagamento());
            stmt.setDouble(6, app.getValor());
            stmt.setString(7, app.getReacoes());
            stmt.setString(8, app.getObservacoesAdicionais());
            stmt.setInt(9, app.getId());

            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar aplicação: " + e.getMessage());
            return false;
        }
    }

    // =========================================================
    // NOVO MÉTODO: ATUALIZA APENAS O STATUS E O PAGAMENTO
    // =========================================================
    public boolean atualizarStatusEPagamento(int id, String status, String formaPagamento) {
        String sql = "UPDATE aplicacoes_v2 SET status = ?, forma_pagamento = ? WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setString(2, formaPagamento);
            stmt.setInt(3, id);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Erro ao atualizar status e pagamento: " + e.getMessage());
            return false;
        }
    }

    public List<Aplicacao> listarTodas() {
        List<Aplicacao> lista = new ArrayList<>();
        String sql = "SELECT a.*, p.nome as nome_paciente, v.nome_vacina, v.lote " +
                "FROM aplicacoes_v2 a " +
                "JOIN pacientes p ON a.paciente_id = p.id " +
                "JOIN vacinas v ON a.vacina_id = v.id " +
                "ORDER BY a.data_hora DESC";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Aplicacao app = new Aplicacao();
                app.setId(rs.getInt("id"));

                Paciente p = new Paciente();
                p.setId(rs.getInt("paciente_id"));
                p.setNome(rs.getString("nome_paciente"));
                app.setPaciente(p);

                Vacina v = new Vacina();
                v.setId(rs.getInt("vacina_id"));
                v.setNomeVacina(rs.getString("nome_vacina"));
                v.setLote(rs.getString("lote"));
                app.setVacina(v);

                if (rs.getTimestamp("data_hora") != null) {
                    app.setDataHora(rs.getTimestamp("data_hora").toLocalDateTime());
                }

                app.setStatus(rs.getString("status"));
                app.setFormaPagamento(rs.getString("forma_pagamento"));
                app.setValor(rs.getDouble("valor"));

                app.setReacoes(rs.getString("reacoes"));
                app.setObservacoesAdicionais(rs.getString("observacoes_adicionais"));

                lista.add(app);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar: " + e.getMessage());
        }
        return lista;
    }

    public boolean excluir(int id) {
        String sql = "DELETE FROM aplicacoes_v2 WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao excluir aplicação: " + e.getMessage());
            return false;
        }
    }
}