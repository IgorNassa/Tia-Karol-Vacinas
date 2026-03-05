package br.sistema;

import br.sistema.util.DbConfig;
import br.sistema.view.TelaPrincipal;

public class Main {
    public static void main(String[] args) {
        // 1. Garante que o banco e as tabelas existam
        DbConfig.criarTabelas();
        TelaPrincipal.iniciar();
    }
}