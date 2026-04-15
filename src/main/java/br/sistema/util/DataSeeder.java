package br.sistema.util;

import br.sistema.repository.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Random;

public class DataSeeder {

    public static void main(String[] args) {
        System.out.println("🚀 Iniciando o Super DataSeeder da Tia Karol...");
        try (Connection conn = ConnectionFactory.getConnection()) {

            // 1. GERANDO VACINAS REALISTAS
            System.out.println("💉 Populando Estoque de Vacinas...");
            String[] nomesVacinas = {"BCG", "Hepatite B", "Pentavalente", "Rotavírus", "Pneumocócica 10", "Meningocócica C", "Febre Amarela", "Tríplice Viral", "Varicela", "Hepatite A", "HPV", "Gripe Quadrivalente"};
            String sqlVacina = "INSERT INTO vacinas (nome_vacina, lote, quantidade_estoque, valor_venda) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pst = conn.prepareStatement(sqlVacina)) {
                Random rand = new Random();
                for (String nome : nomesVacinas) {
                    pst.setString(1, nome);
                    pst.setString(2, "LOTE-" + (rand.nextInt(9000) + 1000));
                    pst.setInt(3, rand.nextInt(150) + 20); // Estoque entre 20 e 170
                    pst.setDouble(4, (rand.nextInt(350) + 90) + 0.90); // Valor entre R$ 90,90 e 440,90
                    pst.executeUpdate();
                }
            }

            // 2. GERANDO PACIENTES E FAMÍLIAS
            System.out.println("👨‍👩‍👧‍👦 Gerando 100 Pacientes e Famílias...");
            String[] nomesHomens = {"João", "José", "Carlos", "Marcos", "Paulo", "Lucas", "Mateus", "Pedro", "Enzo", "Miguel", "Arthur", "Gael"};
            String[] nomesMulheres = {"Maria", "Ana", "Julia", "Fernanda", "Amanda", "Beatriz", "Laura", "Alice", "Helena", "Valentina"};
            String[] sobrenomes = {"Silva", "Santos", "Oliveira", "Souza", "Rodrigues", "Ferreira", "Alves", "Pereira", "Lima", "Gomes"};

            String sqlPaciente = "INSERT INTO pacientes (nome, cpf, data_nascimento, sexo, telefone, nome_responsavel, cpf_responsavel, nome_responsavel_2, cpf_responsavel_2) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            Random rand = new Random();
            try (PreparedStatement pst = conn.prepareStatement(sqlPaciente)) {
                for (int i = 0; i < 100; i++) {
                    boolean isHomem = rand.nextBoolean();
                    String nome = (isHomem ? nomesHomens[rand.nextInt(nomesHomens.length)] : nomesMulheres[rand.nextInt(nomesMulheres.length)]) + " " + sobrenomes[rand.nextInt(sobrenomes.length)];
                    String cpf = String.format("%03d.%03d.%03d-%02d", rand.nextInt(999), rand.nextInt(999), rand.nextInt(999), rand.nextInt(99));
                    String telefone = String.format("(%02d) 9%04d-%04d", rand.nextInt(80)+11, rand.nextInt(9999), rand.nextInt(9999));

                    boolean isMenor = rand.nextInt(100) < 40; // 40% de chance de ser criança
                    LocalDate dataNasc = isMenor ? LocalDate.now().minusYears(rand.nextInt(15)).minusDays(rand.nextInt(365)) : LocalDate.now().minusYears(18 + rand.nextInt(50)).minusDays(rand.nextInt(365));

                    pst.setString(1, nome);
                    pst.setString(2, cpf);
                    pst.setString(3, dataNasc.toString());
                    pst.setString(4, isHomem ? "Masculino" : "Feminino");
                    pst.setString(5, telefone);

                    // Lógica de Família (Dependentes)
                    if (isMenor) {
                        String pai = nomesHomens[rand.nextInt(nomesHomens.length)] + " " + sobrenomes[rand.nextInt(sobrenomes.length)];
                        String mae = nomesMulheres[rand.nextInt(nomesMulheres.length)] + " " + sobrenomes[rand.nextInt(sobrenomes.length)];

                        // Vamos usar um CPF fixo para criar "Irmãos" aleatoriamente no banco
                        String cpfRespFam1 = String.format("%03d.000.000-00", rand.nextInt(20)); // CPFs parecidos para agrupar
                        String cpfRespFam2 = String.format("%03d.111.111-11", rand.nextInt(20));

                        pst.setString(6, mae);
                        pst.setString(7, cpfRespFam1);
                        pst.setString(8, pai);
                        pst.setString(9, cpfRespFam2);
                    } else {
                        pst.setString(6, ""); pst.setString(7, ""); pst.setString(8, ""); pst.setString(9, "");
                    }
                    pst.executeUpdate();
                }
            }

            // 3. GERANDO HISTÓRICO DE APLICAÇÕES E AGENDAMENTOS
            System.out.println("📅 Gerando Histórico de 300 Aplicações e Agendamentos...");
            String sqlApp = "INSERT INTO aplicacoes_v2 (paciente_id, vacina_id, data_hora, status, forma_pagamento, valor) VALUES (?, ?, ?, ?, ?, ?)";

            try (Statement st = conn.createStatement();
                 ResultSet rsPacientes = st.executeQuery("SELECT id FROM pacientes");
                 PreparedStatement pstApp = conn.prepareStatement(sqlApp)) {

                // Pega max ID de vacinas pra não dar erro
                ResultSet rsVacinas = conn.createStatement().executeQuery("SELECT MAX(id) FROM vacinas");
                rsVacinas.next();
                int maxVacinaId = rsVacinas.getInt(1);

                while (rsPacientes.next()) {
                    int pId = rsPacientes.getInt("id");
                    int qtdVacs = rand.nextInt(5) + 1; // 1 a 5 vacinas por pessoa

                    for (int v = 0; v < qtdVacs; v++) {
                        int vId = rand.nextInt(maxVacinaId) + 1;

                        boolean isFuturo = rand.nextInt(100) < 30; // 30% de chance de ser agendado pro futuro
                        LocalDateTime dataHora;
                        String status, pagamento;

                        if (isFuturo) {
                            dataHora = LocalDateTime.now().plusDays(rand.nextInt(60)).withHour(8 + rand.nextInt(9)).withMinute(0);
                            status = "Agendado";
                            pagamento = rand.nextBoolean() ? "Pendente" : "PIX"; // Algumas já pagas adiantado
                        } else {
                            dataHora = LocalDateTime.now().minusDays(rand.nextInt(180)).withHour(8 + rand.nextInt(9)).withMinute(0);
                            status = "Aplicado";
                            String[] formas = {"PIX", "Cartão de Crédito", "Cartão de Débito", "Dinheiro"};
                            pagamento = formas[rand.nextInt(formas.length)];
                        }

                        pstApp.setInt(1, pId);
                        pstApp.setInt(2, vId);
                        pstApp.setTimestamp(3, java.sql.Timestamp.valueOf(dataHora));
                        pstApp.setString(4, status);
                        pstApp.setString(5, pagamento);
                        pstApp.setDouble(6, (rand.nextInt(350) + 90) + 0.90);
                        pstApp.executeUpdate();
                    }
                }
            }

            System.out.println("✅ PRONTO! Banco de Dados populado com sucesso.");
            System.out.println("👉 Abra o sistema e confira o Dashboard financeiro e a Tabela de Próximas Vacinas!");

        } catch (Exception e) {
            System.out.println("❌ Erro ao popular banco: " + e.getMessage());
            e.printStackTrace();
        }
    }
}