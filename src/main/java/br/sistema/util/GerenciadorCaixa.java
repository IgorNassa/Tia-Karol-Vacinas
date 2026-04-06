package br.sistema.util;

import br.sistema.model.Aplicacao;
import br.sistema.model.LancamentoOutros;
import br.sistema.repository.AplicacaoDAO;
import br.sistema.repository.ConnectionFactory;
import br.sistema.repository.LancamentoOutrosDAO;
import br.sistema.view.TelaPrincipal;
import br.sistema.view.panels.PainelFinanceiro;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

public class GerenciadorCaixa {

    /**
     * LÓGICA BLINDADA E CONTÍNUA:
     * O valor físico da gaveta é a soma de TODAS as entradas (Vendas + Avulsas)
     * menos TODAS as saídas físicas (Sangrias).
     * Fechar ou abrir o sistema não altera o dinheiro de papel.
     */
    public static double getSaldoAtualCaixa() {
        double entradas = 0;
        double saidas = 0;

        try {
            // 1. Soma TODAS as aplicações (Dinheiro que entrou fisicamente)
            for (Aplicacao a : new AplicacaoDAO().listarTodas()) {
                entradas += a.getValor();
            }

            // 2. Soma Entradas Avulsas (Ex: Troco inicial) e Saídas Avulsas (Sangrias)
            try (Connection c = ConnectionFactory.getConnection()) {
                try (PreparedStatement p = c.prepareStatement("SELECT SUM(valor) FROM lancamentos_outros WHERE tipo = 'Entrada avulsa'")) {
                    try (ResultSet rs = p.executeQuery()) { if (rs.next()) entradas += rs.getDouble(1); }
                }
                try (PreparedStatement p = c.prepareStatement("SELECT SUM(valor) FROM lancamentos_outros WHERE tipo = 'Saída avulsa'")) {
                    try (ResultSet rs = p.executeQuery()) { if (rs.next()) saidas += rs.getDouble(1); }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }

        return entradas - saidas; // Exatamente o que tem que ter na gaveta!
    }

    // =======================================================
    // ABERTURA DE CAIXA (Apenas Informativo agora)
    // =======================================================
    public static void iniciarAbertura(TelaPrincipal frame) {
        JDialog dialog = new JDialog(frame, "Conferência de Caixa", true);
        dialog.setSize(450, 320); dialog.setLocationRelativeTo(frame); dialog.getContentPane().setBackground(Color.WHITE);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JPanel pnl = new JPanel(new BorderLayout(0, 15)); pnl.setBackground(Color.WHITE); pnl.setBorder(new EmptyBorder(35, 40, 20, 40));

        JLabel lblTitulo = new JLabel("Saldo Físico na Gaveta");
        lblTitulo.setFont(new Font("Segoe UI Semilight", Font.PLAIN, 24)); lblTitulo.setForeground(Cores.CINZA_GRAFITE); lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lblValor = new JLabel(String.format("R$ %,.2f", getSaldoAtualCaixa()));
        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 42)); lblValor.setForeground(new Color(41, 128, 185)); lblValor.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lblSub = new JLabel("Confirme se este valor está na gaveta para iniciar.");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 14)); lblSub.setForeground(Cores.CINZA_LABEL); lblSub.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel pnlCentro = new JPanel(new GridLayout(2, 1, 0, 5)); pnlCentro.setBackground(Color.WHITE); pnlCentro.add(lblValor); pnlCentro.add(lblSub);
        pnl.add(lblTitulo, BorderLayout.NORTH); pnl.add(pnlCentro, BorderLayout.CENTER);

        JButton btnConfirmar = criarBotaoElegante(" Tudo Certo, Iniciar Turno", Cores.VERDE_AQUA, "disco.svg");
        btnConfirmar.setPreferredSize(new Dimension(0, 55));

        // Agora ele apenas fecha a janela, não salva "fantasmas" na memória!
        btnConfirmar.addActionListener(e -> dialog.dispose());

        JPanel pnlBotoes = new JPanel(new BorderLayout()); pnlBotoes.setBackground(Color.WHITE); pnlBotoes.setBorder(new EmptyBorder(0, 40, 35, 40));
        pnlBotoes.add(btnConfirmar, BorderLayout.CENTER);

        dialog.add(pnl, BorderLayout.CENTER); dialog.add(pnlBotoes, BorderLayout.SOUTH); dialog.setVisible(true);
    }

    // =======================================================
    // FECHAMENTO DE CAIXA
    // =======================================================
    public static void configurarFechamento(TelaPrincipal frame) {
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exibirModalFechamento(frame);
            }
        });
    }

    private static void exibirModalFechamento(TelaPrincipal frame) {
        JDialog dialog = new JDialog(frame, "Quebra / Fechamento de Caixa", true);
        dialog.setSize(550, 480); dialog.setLocationRelativeTo(frame); dialog.getContentPane().setBackground(Color.WHITE);

        JPanel pnl = new JPanel(new BorderLayout(0, 20)); pnl.setBackground(Color.WHITE); pnl.setBorder(new EmptyBorder(30, 40, 10, 40));

        JLabel lblTitulo = new JLabel("Fechamento de Caixa Diário");
        lblTitulo.setFont(new Font("Segoe UI Semilight", Font.PLAIN, 26)); lblTitulo.setForeground(Cores.CINZA_GRAFITE); lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lblValor = new JLabel(String.format("R$ %,.2f", getSaldoAtualCaixa()));
        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 48)); lblValor.setForeground(Cores.VERDE_AQUA); lblValor.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lblSub = new JLabel("Saldo total que deve constar fisicamente na gaveta agora.");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 15)); lblSub.setForeground(Cores.CINZA_LABEL); lblSub.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel pnlCentro = new JPanel(new GridLayout(2, 1, 0, 0)); pnlCentro.setBackground(Color.WHITE); pnlCentro.add(lblValor); pnlCentro.add(lblSub);
        pnl.add(lblTitulo, BorderLayout.NORTH); pnl.add(pnlCentro, BorderLayout.CENTER);

        JButton btnSangria = criarBotaoElegante(" Fazer Sangria", new Color(230, 126, 34), null);
        JButton btnRelatorio = criarBotaoElegante(" Ver Relatório", Cores.CINZA_GRAFITE, "imprimir.svg");
        JButton btnConfirmar = criarBotaoElegante(" Confirmar Fechamento e Sair", Cores.VERDE_AQUA, "disco.svg");
        JButton btnCancelar = criarBotaoElegante("Cancelar (Voltar ao Sistema)", new Color(220, 53, 69), null);

        btnSangria.addActionListener(e -> abrirModalSangria(dialog, lblValor));

        btnRelatorio.addActionListener(e -> {
            PainelFinanceiro pf = new PainelFinanceiro(frame);
            pf.gerarEImprimirRelatorio(LocalDate.now(), LocalDate.now());
        });

        // Agora ele simplesmente desliga o sistema. O Banco de Dados já tem a verdade absoluta.
        btnConfirmar.addActionListener(e -> System.exit(0));

        btnCancelar.addActionListener(e -> dialog.dispose());

        JPanel pnlBotoesTop = new JPanel(new GridLayout(1, 2, 10, 0)); pnlBotoesTop.setBackground(Color.WHITE); pnlBotoesTop.add(btnSangria); pnlBotoesTop.add(btnRelatorio);
        JPanel pnlBotoesMain = new JPanel(new GridLayout(2, 1, 0, 10)); pnlBotoesMain.setBackground(Color.WHITE); pnlBotoesMain.add(btnConfirmar); pnlBotoesMain.add(btnCancelar);
        JPanel pnlFooter = new JPanel(new BorderLayout(0, 15)); pnlFooter.setBackground(Color.WHITE); pnlFooter.setBorder(new EmptyBorder(10, 40, 30, 40)); pnlFooter.add(pnlBotoesTop, BorderLayout.NORTH); pnlFooter.add(pnlBotoesMain, BorderLayout.CENTER);

        dialog.add(pnl, BorderLayout.CENTER); dialog.add(pnlFooter, BorderLayout.SOUTH); dialog.setVisible(true);
    }

    // =======================================================
    // MODAL DE SANGRIA
    // =======================================================
    private static void abrirModalSangria(JDialog parent, JLabel lblValorAtualizado) {
        double saldoAtual = getSaldoAtualCaixa();
        if (saldoAtual <= 0) { JOptionPane.showMessageDialog(parent, "Não há saldo em caixa para realizar sangria.", "Aviso", JOptionPane.WARNING_MESSAGE); return; }

        JDialog diag = new JDialog(parent, "Realizar Sangria", true);
        diag.setSize(420, 320); diag.setLocationRelativeTo(parent); diag.getContentPane().setBackground(Color.WHITE);

        JPanel pnl = new JPanel(new GridLayout(2, 1, 10, 15)); pnl.setBackground(Color.WHITE); pnl.setBorder(new EmptyBorder(25, 40, 20, 40));

        JLabel lblInfo = new JLabel("<html><center>Saldo Disponível: <b>R$ " + String.format("%,.2f", saldoAtual) + "</b><br><br>Valor a retirar (R$):</center></html>");
        lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 16)); lblInfo.setForeground(Cores.CINZA_GRAFITE); lblInfo.setHorizontalAlignment(SwingConstants.CENTER);

        JTextField txtValor = new JTextField("0,00");
        txtValor.setFont(new Font("Segoe UI", Font.BOLD, 32)); txtValor.setForeground(new Color(230, 126, 34)); txtValor.setHorizontalAlignment(JTextField.CENTER);

        txtValor.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                String numeros = txtValor.getText().replaceAll("[^0-9]", "");
                if (numeros.isEmpty()) { txtValor.setText("0,00"); return; }
                double valor = Double.parseDouble(numeros) / 100;
                txtValor.setText(String.format("%,.2f", valor));
            }
        });

        pnl.add(lblInfo); pnl.add(txtValor);

        JButton btnSalvar = criarBotaoElegante("Confirmar Retirada", new Color(230, 126, 34), null);
        btnSalvar.addActionListener(e -> {
            try {
                double valorRetirar = Double.parseDouble(txtValor.getText().replace(".", "").replace(",", "."));
                if (valorRetirar <= 0) { JOptionPane.showMessageDialog(diag, "O valor deve ser maior que zero.", "Erro", JOptionPane.ERROR_MESSAGE); return; }
                if (valorRetirar > saldoAtual) { JOptionPane.showMessageDialog(diag, "Saldo insuficiente! Você não pode retirar mais do que tem no caixa.", "Erro", JOptionPane.ERROR_MESSAGE); return; }

                LancamentoOutrosDAO dao = new LancamentoOutrosDAO();
                dao.salvar(new LancamentoOutros("Sangria de Caixa / Retirada", "Saída avulsa", valorRetirar, LocalDate.now()));

                lblValorAtualizado.setText(String.format("R$ %,.2f", getSaldoAtualCaixa()));
                JOptionPane.showMessageDialog(diag, "Sangria registrada com sucesso!\nO valor foi descontado do caixa.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                diag.dispose();

            } catch (Exception ex) { JOptionPane.showMessageDialog(diag, "Valor inválido.", "Erro", JOptionPane.ERROR_MESSAGE); }
        });

        JPanel pnlBot = new JPanel(new BorderLayout()); pnlBot.setBackground(Color.WHITE); pnlBot.setBorder(new EmptyBorder(0, 40, 30, 40)); pnlBot.add(btnSalvar, BorderLayout.CENTER);

        diag.add(pnl, BorderLayout.CENTER); diag.add(pnlBot, BorderLayout.SOUTH); diag.setVisible(true);
    }

    private static JButton criarBotaoElegante(String texto, Color bg, String icone) {
        JButton btn = new JButton(texto); btn.setBackground(bg); btn.setForeground(Color.WHITE); btn.setFont(new Font("Segoe UI", Font.BOLD, 15)); btn.setPreferredSize(new Dimension(0, 48)); btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); btn.setFocusPainted(false);
        if (icone != null) { try { btn.setIcon(new FlatSVGIcon("icons/" + icone, 18, 18).setColorFilter(new FlatSVGIcon.ColorFilter(c -> Color.WHITE))); } catch(Exception ignored){} }
        return btn;
    }
}