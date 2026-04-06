package br.sistema;

import br.sistema.repository.*;
import br.sistema.view.TelaPrincipal;

public class Main {
    public static void main(String[] args) {

        try {
            System.out.println("Iniciando sincronização com PostgreSQL...");

            // ORDEM OBRIGATÓRIA PARA O POSTGRES:
            // 1. Tabelas independentes
            new PacienteDAO();
            new VacinaDAO();
            new LancamentoOutrosDAO();
            new ConfiguracaoDAO();
            new DespesaFixaDAO();

            // 2. Tabelas que dependem das anteriores (Foreign Keys)
            new AplicacaoDAO();

            System.out.println("Banco de dados sincronizado com sucesso!");

        } catch (Exception e) {
            System.err.println("Erro crítico ao iniciar banco de dados: " + e.getMessage());
            e.printStackTrace();
        }

        // Agora sim, abre a tela com tudo pronto
        TelaPrincipal.iniciar();
    }
}