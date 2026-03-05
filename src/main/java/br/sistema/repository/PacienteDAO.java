package br.sistema.repository;

import br.sistema.model.Paciente;
import br.sistema.util.Conexao;
import java.sql.*;

public class PacienteDAO {
    public void salvar(Paciente paciente) {
        String sql = "INSERT INTO Paciente (nome, cpf, data_nascimento, alergias) VALUES (?, ?, ?, ?)";

        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, paciente.getNome());
            stmt.setString(2, paciente.getCpf());
            stmt.setString(3, paciente.getDataNascimento().toString());
            stmt.setString(4, paciente.getAlergias());

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}