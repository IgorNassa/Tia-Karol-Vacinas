package br.sistema.repository;

import br.sistema.model.Endereco;
import br.sistema.model.Paciente;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PacienteDAO {

    public PacienteDAO() {
        criarTabelaSeNaoExistir();
    }

    private void criarTabelaSeNaoExistir() {
        String sql = "CREATE TABLE IF NOT EXISTS pacientes ("
                + "id SERIAL PRIMARY KEY,"
                + "nome VARCHAR(255) NOT NULL,"
                + "cpf VARCHAR(20),"
                + "data_nascimento VARCHAR(20),"
                + "sexo VARCHAR(20),"
                + "cartao_sus VARCHAR(50),"
                + "telefone VARCHAR(20), "
                + "telefone_2 VARCHAR(20), "
                + "alergias TEXT, "
                + "medico_encaminhador VARCHAR(255),"
                + "nome_responsavel VARCHAR(255), "
                + "cpf_responsavel VARCHAR(20),"
                + "foto BYTEA," // BLOB vira BYTEA
                + "cep VARCHAR(20), "
                + "rua VARCHAR(255), "
                + "numero VARCHAR(20), "
                + "complemento VARCHAR(255),"
                + "bairro VARCHAR(100), "
                + "cidade VARCHAR(100), "
                + "uf VARCHAR(2), "
                + "codigo_ibge VARCHAR(20))";

        try (java.sql.Connection conn = ConnectionFactory.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (java.sql.SQLException e) {
            System.out.println("Erro ao criar tabela pacientes no Postgres: " + e.getMessage());
        }
    }

    public void salvar(Paciente p) {
        String sql = "INSERT INTO pacientes (nome, cpf, data_nascimento, sexo, cartao_sus, telefone, telefone_2, alergias, medico_encaminhador, "
                + "nome_responsavel, cpf_responsavel, foto, cep, rua, numero, complemento, bairro, cidade, uf, codigo_ibge) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"; // 20 valores agora
        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, p.getNome());
            pstmt.setString(2, p.getCpf());
            pstmt.setString(3, p.getDataNascimento() != null ? p.getDataNascimento().toString() : null);
            pstmt.setString(4, p.getSexo());
            pstmt.setString(5, p.getCartaoSus());
            pstmt.setString(6, p.getTelefone());
            pstmt.setString(7, p.getTelefone2());
            pstmt.setString(8, p.getAlergias());
            pstmt.setString(9, p.getMedicoEncaminhador()); // <- ADICIONADO AQUI
            pstmt.setString(10, p.getNomeResponsavel());
            pstmt.setString(11, p.getCpfResponsavel());
            pstmt.setBytes(12, p.getFoto());

            Endereco end = p.getEndereco();
            pstmt.setString(13, end != null ? end.getCep() : null);
            pstmt.setString(14, end != null ? end.getRua() : null);
            pstmt.setString(15, end != null ? end.getNumero() : null);
            pstmt.setString(16, end != null ? end.getComplemento() : null);
            pstmt.setString(17, end != null ? end.getBairro() : null);
            pstmt.setString(18, end != null ? end.getCidade() : null);
            pstmt.setString(19, end != null ? end.getUf() : null);
            pstmt.setString(20, end != null ? end.getCodigoIbge() : null);

            pstmt.executeUpdate();
        } catch (SQLException e) { System.out.println("Erro ao salvar: " + e.getMessage()); }
    }

    public void atualizar(Paciente p) {
        String sql = "UPDATE pacientes SET nome = ?, cpf = ?, data_nascimento = ?, sexo = ?, cartao_sus = ?, telefone = ?, telefone_2 = ?, alergias = ?, medico_encaminhador = ?, "
                + "nome_responsavel = ?, cpf_responsavel = ?, foto = ?, "
                + "cep = ?, rua = ?, numero = ?, complemento = ?, bairro = ?, cidade = ?, uf = ?, codigo_ibge = ? WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, p.getNome());
            pstmt.setString(2, p.getCpf());
            pstmt.setString(3, p.getDataNascimento() != null ? p.getDataNascimento().toString() : null);
            pstmt.setString(4, p.getSexo());
            pstmt.setString(5, p.getCartaoSus());
            pstmt.setString(6, p.getTelefone());
            pstmt.setString(7, p.getTelefone2());
            pstmt.setString(8, p.getAlergias());
            pstmt.setString(9, p.getMedicoEncaminhador()); // <- ADICIONADO AQUI
            pstmt.setString(10, p.getNomeResponsavel());
            pstmt.setString(11, p.getCpfResponsavel());
            pstmt.setBytes(12, p.getFoto());

            Endereco end = p.getEndereco();
            pstmt.setString(13, end != null ? end.getCep() : null);
            pstmt.setString(14, end != null ? end.getRua() : null);
            pstmt.setString(15, end != null ? end.getNumero() : null);
            pstmt.setString(16, end != null ? end.getComplemento() : null);
            pstmt.setString(17, end != null ? end.getBairro() : null);
            pstmt.setString(18, end != null ? end.getCidade() : null);
            pstmt.setString(19, end != null ? end.getUf() : null);
            pstmt.setString(20, end != null ? end.getCodigoIbge() : null);
            pstmt.setInt(21, p.getId()); // ID agora é o 21

            pstmt.executeUpdate();
        } catch (SQLException e) { System.out.println("Erro ao atualizar: " + e.getMessage()); }
    }

    public List<Paciente> listarTodos() {
        List<Paciente> lista = new ArrayList<>();
        String sql = "SELECT * FROM pacientes ORDER BY nome ASC";
        try (Connection conn = ConnectionFactory.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Paciente p = new Paciente();
                p.setId(rs.getInt("id"));
                p.setNome(rs.getString("nome"));
                p.setCpf(rs.getString("cpf"));

                String dataStr = rs.getString("data_nascimento");
                if (dataStr != null && !dataStr.isEmpty()) p.setDataNascimento(LocalDate.parse(dataStr));

                p.setSexo(rs.getString("sexo"));
                p.setCartaoSus(rs.getString("cartao_sus"));
                p.setTelefone(rs.getString("telefone"));
                p.setTelefone2(rs.getString("telefone_2"));
                p.setAlergias(rs.getString("alergias"));
                p.setMedicoEncaminhador(rs.getString("medico_encaminhador")); // <- ADICIONADO AQUI
                p.setNomeResponsavel(rs.getString("nome_responsavel"));
                p.setCpfResponsavel(rs.getString("cpf_responsavel"));
                p.setFoto(rs.getBytes("foto"));

                Endereco end = new Endereco(
                        rs.getString("cep"), rs.getString("rua"), rs.getString("numero"),
                        rs.getString("complemento"), rs.getString("bairro"), rs.getString("cidade"),
                        rs.getString("uf"), rs.getString("codigo_ibge")
                );
                p.setEndereco(end);

                lista.add(p);
            }
        } catch (SQLException e) { }
        return lista;
    }

    public List<String> listarNomes() {
        List<String> nomes = new ArrayList<>(); nomes.add("Selecione o paciente...");
        for(Paciente p : listarTodos()) { nomes.add(p.getNome()); } return nomes;
    }

    public String buscarTelefonePorNome(String nome) {
        String sql = "SELECT telefone FROM pacientes WHERE nome = ?";
        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nome); ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString("telefone");
        } catch (SQLException e) { } return "";
    }

    public void excluir(int id) {
        String sql = "DELETE FROM pacientes WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id); pstmt.executeUpdate();
        } catch (SQLException e) { }
    }
}