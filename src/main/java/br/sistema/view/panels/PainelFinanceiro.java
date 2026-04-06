package br.sistema.view.panels;

import br.sistema.model.Aplicacao;
import br.sistema.model.DespesaFixa;
import br.sistema.model.LancamentoOutros;
import br.sistema.model.Vacina;
import br.sistema.repository.AplicacaoDAO;
import br.sistema.repository.ConnectionFactory;
import br.sistema.repository.DespesaFixaDAO;
import br.sistema.repository.LancamentoOutrosDAO;
import br.sistema.repository.VacinaDAO;
import br.sistema.util.Cores;
import br.sistema.view.TelaPrincipal;
import br.sistema.view.components.GlassPanel;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PainelFinanceiro extends JPanel {
    private TelaPrincipal frame;
    private String abaAtiva = "Aplicações";

    private DespesaFixaDAO despesaDAO = new DespesaFixaDAO();
    private LancamentoOutrosDAO outrosDAO = new LancamentoOutrosDAO();
    private AplicacaoDAO aplicacaoDAO = new AplicacaoDAO();
    private VacinaDAO vacinaDAO = new VacinaDAO();

    private JPanel pnlConteudoDinamico;
    private JLabel lblEntradas, lblSaidas, lblLucro;

    public PainelFinanceiro(TelaPrincipal frame) {
        this.frame = frame;
        setOpaque(false);
        setLayout(new BorderLayout(0, 20));
        setBorder(new EmptyBorder(30, 40, 30, 40));

        montarInterface();
        carregarAbaAtiva();
        atualizarCards();
    }

    private void atualizarCards() {
        double entradas = 0, saidas = 0;

        try { for (Aplicacao a : aplicacaoDAO.listarTodas()) entradas += a.getValor(); } catch (Exception e) {}

        try (Connection c = ConnectionFactory.getConnection(); PreparedStatement p = c.prepareStatement("SELECT SUM(valor) FROM lancamentos_outros WHERE tipo = ?")) {
            p.setString(1, "Entrada avulsa");
            try (ResultSet rs = p.executeQuery()) { if (rs.next()) entradas += rs.getDouble(1); }
        } catch (Exception e) {}

        try {
            for (Vacina v : vacinaDAO.listarTodas()) {
                // Considera a QtdTotal comprada como custo, se existir. Senão usa a atual.
                int qtdParaCalculo = v.getQtdTotal() > 0 ? v.getQtdTotal() : v.getQtdDisponivel();
                saidas += (qtdParaCalculo * v.getValorCompra());
            }
        } catch (Exception e) {}

        try (Connection c = ConnectionFactory.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT SUM(valor_pago) FROM historico_despesas")) {
            if (rs.next()) saidas += rs.getDouble(1);
        } catch (Exception e) {}

        try (Connection c = ConnectionFactory.getConnection(); PreparedStatement p = c.prepareStatement("SELECT SUM(valor) FROM lancamentos_outros WHERE tipo = ?")) {
            p.setString(1, "Saída avulsa");
            try (ResultSet rs = p.executeQuery()) { if (rs.next()) saidas += rs.getDouble(1); }
        } catch (Exception e) {}

        lblEntradas.setText(String.format("R$ %,.2f", entradas));
        lblSaidas.setText(String.format("R$ %,.2f", saidas));
        lblLucro.setText(String.format("R$ %,.2f", entradas - saidas));
    }

    private void montarInterface() {
        GlassPanel cardVidro = new GlassPanel();
        cardVidro.setLayout(new BorderLayout());
        cardVidro.setBorder(new EmptyBorder(25, 35, 30, 35));

        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false); header.setBorder(new EmptyBorder(0, 0, 20, 0));
        JLabel titulo = new JLabel("Lançamentos Financeiros"); titulo.setFont(new Font("Segoe UI Semilight", Font.PLAIN, 32)); titulo.setForeground(Cores.CINZA_GRAFITE);
        header.add(titulo, BorderLayout.WEST);

        JButton btnRelatorio = new JButton(" Imprimir Relatório");
        btnRelatorio.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnRelatorio.setBackground(Cores.CINZA_GRAFITE);
        btnRelatorio.setForeground(Color.WHITE);
        btnRelatorio.setCursor(new Cursor(Cursor.HAND_CURSOR));
        try { btnRelatorio.setIcon(new FlatSVGIcon("icons/imprimir.svg", 18, 18).setColorFilter(new FlatSVGIcon.ColorFilter(c -> Color.WHITE))); } catch(Exception e){}
        btnRelatorio.addActionListener(e -> abrirModalRelatorio());
        header.add(btnRelatorio, BorderLayout.EAST);

        JPanel pnlCards = new JPanel(new GridLayout(1, 3, 20, 0)); pnlCards.setOpaque(false);
        lblEntradas = new JLabel("R$ 0,00"); lblSaidas = new JLabel("R$ 0,00"); lblLucro = new JLabel("R$ 0,00");
        pnlCards.add(criarMiniCard("Receitas Totais", lblEntradas, new Color(46, 204, 113)));
        pnlCards.add(criarMiniCard("Despesas Totais", lblSaidas, new Color(231, 76, 60)));
        pnlCards.add(criarMiniCard("Lucro Líquido", lblLucro, Cores.VERDE_AQUA));

        JPanel topo = new JPanel(new BorderLayout(0, 15)); topo.setOpaque(false); topo.add(header, BorderLayout.NORTH); topo.add(pnlCards, BorderLayout.CENTER);

        JPanel painelAbas = new JPanel(new FlowLayout(FlowLayout.LEFT, 30, 0)); painelAbas.setOpaque(false); painelAbas.setBorder(new EmptyBorder(20, 0, 10, 0));
        painelAbas.add(criarBotaoAba("Aplicações", true)); painelAbas.add(criarBotaoAba("Vacinas", false));
        painelAbas.add(criarBotaoAba("Fixos", false)); painelAbas.add(criarBotaoAba("Outros", false));

        pnlConteudoDinamico = new JPanel(new BorderLayout()); pnlConteudoDinamico.setOpaque(false);
        JPanel pnlSuper = new JPanel(new BorderLayout()); pnlSuper.setOpaque(false); pnlSuper.add(topo, BorderLayout.NORTH); pnlSuper.add(painelAbas, BorderLayout.CENTER);

        cardVidro.add(pnlSuper, BorderLayout.NORTH); cardVidro.add(pnlConteudoDinamico, BorderLayout.CENTER); add(cardVidro, BorderLayout.CENTER);
    }

    private void carregarAbaAtiva() {
        pnlConteudoDinamico.removeAll();
        switch (abaAtiva) {
            case "Aplicações" -> renderizarAbaAplicacoes();
            case "Vacinas" -> renderizarAbaVacinas();
            case "Fixos" -> renderizarAbaFixos();
            case "Outros" -> renderizarAbaOutros();
        }
        pnlConteudoDinamico.revalidate(); pnlConteudoDinamico.repaint();
    }

    // =========================================================
    // MÓDULO DE RELATÓRIO
    // =========================================================

    class RegistroLinha implements Comparable<RegistroLinha> {
        LocalDate dataReal; String dataExibicao; String descricao; String categoria; double valor; boolean isEntrada;
        public RegistroLinha(LocalDate dR, String dE, String desc, String cat, double v, boolean in) {
            this.dataReal = dR; this.dataExibicao = dE; this.descricao = desc; this.categoria = cat; this.valor = v; this.isEntrada = in;
        }
        @Override public int compareTo(RegistroLinha o) { return this.dataReal.compareTo(o.dataReal); }
    }

    private void abrirModalRelatorio() {
        JDialog diag = new JDialog(frame, "Gerar Relatório Financeiro", true);
        diag.setSize(450, 320); diag.setLocationRelativeTo(frame); diag.getContentPane().setBackground(Color.WHITE);
        JPanel p = new JPanel(new GridLayout(2, 1, 15, 15)); p.setBackground(Color.WHITE); p.setBorder(new EmptyBorder(30, 40, 20, 40));

        SpinnerDateModel modelIni = new SpinnerDateModel(new java.util.Date(), null, null, java.util.Calendar.DAY_OF_MONTH);
        JSpinner spnIni = new JSpinner(modelIni); spnIni.setEditor(new JSpinner.DateEditor(spnIni, "dd/MM/yyyy"));
        spnIni.setFont(new Font("Segoe UI", Font.PLAIN, 16)); spnIni.setPreferredSize(new Dimension(0, 45));

        SpinnerDateModel modelFim = new SpinnerDateModel(new java.util.Date(), null, null, java.util.Calendar.DAY_OF_MONTH);
        JSpinner spnFim = new JSpinner(modelFim); spnFim.setEditor(new JSpinner.DateEditor(spnFim, "dd/MM/yyyy"));
        spnFim.setFont(new Font("Segoe UI", Font.PLAIN, 16)); spnFim.setPreferredSize(new Dimension(0, 45));

        p.add(montarBloco("Data Inicial:", spnIni)); p.add(montarBloco("Data Final (Máx 60 dias):", spnFim));

        JButton btnGerar = criarBotaoAcao("Visualizar Relatório", Cores.VERDE_AQUA, Color.WHITE); setBotaoAtivo(btnGerar, true); btnGerar.setPreferredSize(new Dimension(0, 50));
        btnGerar.addActionListener(e -> {
            LocalDate dtIni = ((java.util.Date) spnIni.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate dtFim = ((java.util.Date) spnFim.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (dtFim.isBefore(dtIni)) { JOptionPane.showMessageDialog(diag, "A Data Final não pode ser menor que a Data Inicial!", "Erro de Data", JOptionPane.ERROR_MESSAGE); return; }
            if (ChronoUnit.DAYS.between(dtIni, dtFim) > 60) { JOptionPane.showMessageDialog(diag, "O período selecionado ultrapassa 60 dias. Por favor, diminua o intervalo.", "Atenção", JOptionPane.WARNING_MESSAGE); return; }
            diag.dispose(); gerarEImprimirRelatorio(dtIni, dtFim);
        });

        JPanel pBot = new JPanel(new BorderLayout()); pBot.setBorder(new EmptyBorder(0, 40, 30, 40)); pBot.setBackground(Color.WHITE); pBot.add(btnGerar, BorderLayout.CENTER);
        diag.add(p, BorderLayout.CENTER); diag.add(pBot, BorderLayout.SOUTH); diag.setVisible(true);
    }

    // AGORA ESTÁ PÚBLICO! A Data da vacina puxa o Cadastro real daquele lote.
    public void gerarEImprimirRelatorio(LocalDate ini, LocalDate fim) {
        String dbIni = ini.toString(); String dbFim = fim.toString();
        DateTimeFormatter brFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        double totAplica = 0, totAvulsosEntrada = 0, totAvulsosSaida = 0, totFixos = 0, totVacinas = 0;
        List<RegistroLinha> listaMista = new ArrayList<>();

        // 1. Aplicações
        try {
            for (Aplicacao a : aplicacaoDAO.listarTodas()) {
                if (a.getFormaPagamento().equalsIgnoreCase("Pendente")) {
                    continue;
                }
                if (a.getDataHora() != null) {
                    LocalDate d = a.getDataHora().toLocalDate();
                    if (!d.isBefore(ini) && !d.isAfter(fim)) {
                        double v = a.getValor(); totAplica += v;
                        String desc = "Aplicação: " + (a.getPaciente() != null ? a.getPaciente().getNome() : "Desconhecido");
                        listaMista.add(new RegistroLinha(d, d.format(brFmt), desc, "Aplicações", v, true));
                    }
                }
            }
        } catch (Exception e) {}

        // 2. Vacinas (Atualizado para a Data de Cadastro!)
        try {
            for (Vacina v : vacinaDAO.listarTodas()) {
                if (v.getDataCadastro() != null) {
                    LocalDate d = v.getDataCadastro().toLocalDate();
                    if (!d.isBefore(ini) && !d.isAfter(fim)) {
                        int qtdComprada = v.getQtdTotal() > 0 ? v.getQtdTotal() : v.getQtdDisponivel();
                        double total = qtdComprada * v.getValorCompra();
                        if (total > 0) {
                            totVacinas += total;
                            String loteInfo = (v.getLote() != null && !v.getLote().isEmpty()) ? " (Lote: " + v.getLote() + ")" : "";
                            listaMista.add(new RegistroLinha(d, d.format(brFmt), "Compra de Vacina: " + v.getNomeVacina() + loteInfo, "Estoque", total, false));
                        }
                    }
                }
            }
        } catch (Exception e) {}

        // 3. Outros Lançamentos e Fixos
        try (Connection conn = ConnectionFactory.getConnection()) {
            try (PreparedStatement pst = conn.prepareStatement("SELECT nome, data_lancamento, valor FROM lancamentos_outros WHERE tipo = 'Entrada avulsa' AND data_lancamento BETWEEN ? AND ?")) {
                pst.setString(1, dbIni); pst.setString(2, dbFim);
                ResultSet rs = pst.executeQuery();
                while(rs.next()) {
                    double v = rs.getDouble("valor"); totAvulsosEntrada += v;
                    LocalDate d = LocalDate.parse(rs.getString("data_lancamento"));
                    listaMista.add(new RegistroLinha(d, d.format(brFmt), rs.getString("nome"), "Avulso", v, true));
                }
            }
            try (PreparedStatement pst = conn.prepareStatement("SELECT nome, data_lancamento, valor FROM lancamentos_outros WHERE tipo = 'Saída avulsa' AND data_lancamento BETWEEN ? AND ?")) {
                pst.setString(1, dbIni); pst.setString(2, dbFim);
                ResultSet rs = pst.executeQuery();
                while(rs.next()) {
                    double v = rs.getDouble("valor"); totAvulsosSaida += v;
                    LocalDate d = LocalDate.parse(rs.getString("data_lancamento"));
                    listaMista.add(new RegistroLinha(d, d.format(brFmt), rs.getString("nome"), "Avulso", v, false));
                }
            }
            try (PreparedStatement pst = conn.prepareStatement("SELECT d.nome, h.data_pagamento, h.valor_pago FROM historico_despesas h JOIN despesas_fixas d ON h.despesa_id = d.id WHERE h.data_pagamento BETWEEN ? AND ?")) {
                pst.setString(1, dbIni); pst.setString(2, dbFim);
                ResultSet rs = pst.executeQuery();
                while(rs.next()) {
                    double v = rs.getDouble("valor_pago"); totFixos += v;
                    LocalDate d = LocalDate.parse(rs.getString("data_pagamento"));
                    listaMista.add(new RegistroLinha(d, d.format(brFmt), "Custo Mensal: " + rs.getString("nome"), "Fixos", v, false));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }

        Collections.sort(listaMista);

        StringBuilder html = new StringBuilder();
        html.append("<html><body style=\"font-family: sans-serif; color: #333; padding: 20px;\">");

        html.append("<h1 align=\"center\" style=\"color: #1E6669; margin-bottom: 5px;\">RELATÓRIO DE CAIXA DIÁRIO/PERÍODO</h1>");
        html.append("<p align=\"center\" style=\"color: #666; margin-top: 0;\">Período Selecionado: <b>").append(ini.format(brFmt)).append("</b> até <b>").append(fim.format(brFmt)).append("</b></p><br>");

        html.append("<table width=\"100%\" cellpadding=\"10\" cellspacing=\"0\" border=\"1\" bordercolor=\"#e0e0e0\">");
        html.append("<tr bgcolor=\"#1E6669\">");
        html.append("<th align=\"center\"><font color=\"white\">Data</font></th>");
        html.append("<th align=\"left\"><font color=\"white\">Descrição do Movimento</font></th>");
        html.append("<th align=\"center\"><font color=\"white\">Categoria</font></th>");
        html.append("<th align=\"right\"><font color=\"white\">Valor Total</font></th>");
        html.append("</tr>");

        boolean zebra = false;
        for (RegistroLinha r : listaMista) {
            String bg = zebra ? "#f9fbfb" : "#ffffff"; zebra = !zebra;
            String valStr = String.format("R$ %.2f", r.valor);
            String corTexto = r.isEntrada ? "#27ae60" : "#c0392b";
            String prefixo = r.isEntrada ? "+ " : "- ";

            html.append("<tr bgcolor=\"").append(bg).append("\">");
            html.append("<td align=\"center\">").append(r.dataExibicao).append("</td>");
            html.append("<td align=\"left\">").append(r.descricao).append("</td>");
            html.append("<td align=\"center\">").append(r.categoria).append("</td>");
            html.append("<td align=\"right\"><b><font color=\"").append(corTexto).append("\">").append(prefixo).append(valStr).append("</font></b></td>");
            html.append("</tr>");
        }

        if (listaMista.isEmpty()) html.append("<tr><td colspan=\"4\" align=\"center\">Nenhum lançamento registrado neste período.</td></tr>");
        html.append("</table><br><br>");

        html.append("<table width=\"100%\" cellpadding=\"8\" cellspacing=\"0\" border=\"1\" bordercolor=\"#e0e0e0\">");
        html.append("<tr bgcolor=\"#f4f6f7\"><td colspan=\"2\"><b><font size=\"4\" color=\"#2c3e50\"> Detalhamento de Resultados</font></b></td></tr>");
        html.append("<tr><td>Receitas de Vacinação (Aplicações)</td><td align=\"right\"><b><font color=\"#27ae60\">R$ ").append(String.format("%.2f", totAplica)).append("</font></b></td></tr>");
        html.append("<tr><td>Entradas Diversas (Avulsos)</td><td align=\"right\"><b><font color=\"#27ae60\">R$ ").append(String.format("%.2f", totAvulsosEntrada)).append("</font></b></td></tr>");
        html.append("<tr><td>Saídas Diversas (Avulsos)</td><td align=\"right\"><b><font color=\"#c0392b\">- R$ ").append(String.format("%.2f", totAvulsosSaida)).append("</font></b></td></tr>");
        html.append("<tr><td>Pagamento de Despesas Fixas</td><td align=\"right\"><b><font color=\"#c0392b\">- R$ ").append(String.format("%.2f", totFixos)).append("</font></b></td></tr>");
        html.append("<tr><td>Custo Imobilizado (Compra de Vacinas no Período)</td><td align=\"right\"><b><font color=\"#c0392b\">- R$ ").append(String.format("%.2f", totVacinas)).append("</font></b></td></tr>");
        html.append("</table><br><br>");

        double superEntradas = totAplica + totAvulsosEntrada;
        double superSaidas = totFixos + totAvulsosSaida + totVacinas;
        double superLucro = superEntradas - superSaidas;
        String corLucro = superLucro >= 0 ? "#2980b9" : "#c0392b";

        html.append("<table width=\"100%\" cellpadding=\"15\" cellspacing=\"0\" border=\"0\"><tr>");
        html.append("<td width=\"33%\" bgcolor=\"#e8f8f5\" align=\"center\" style=\"border: 1px solid #a3e4d7;\"><font color=\"#27ae60\">ENTRADAS TOTAIS<br><b><font size=\"5\">R$ ").append(String.format("%.2f", superEntradas)).append("</font></b></font></td><td width=\"2%\"></td>");
        html.append("<td width=\"33%\" bgcolor=\"#fdedec\" align=\"center\" style=\"border: 1px solid #f5b7b1;\"><font color=\"#c0392b\">SAÍDAS TOTAIS<br><b><font size=\"5\">- R$ ").append(String.format("%.2f", superSaidas)).append("</font></b></font></td><td width=\"2%\"></td>");
        html.append("<td width=\"30%\" bgcolor=\"#eaf2f8\" align=\"center\" style=\"border: 1px solid #a9cce3;\"><font color=\"").append(corLucro).append("\">SALDO DO PERÍODO<br><b><font size=\"5\">R$ ").append(String.format("%.2f", superLucro)).append("</font></b></font></td>");
        html.append("</tr></table></body></html>");

        JDialog previewDialog = new JDialog(frame, "Visualização do Relatório (PDF)", true);
        previewDialog.setSize(800, 750); previewDialog.setLocationRelativeTo(frame);

        JEditorPane editorPane = new JEditorPane(); editorPane.setContentType("text/html"); editorPane.setEditable(false);
        editorPane.setText(html.toString()); editorPane.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(editorPane); scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        previewDialog.add(scrollPane, BorderLayout.CENTER);

        JButton btnImprimir = criarBotaoAcao("Imprimir Relatório ou Salvar PDF", Cores.VERDE_AQUA, Color.WHITE); setBotaoAtivo(btnImprimir, true);
        btnImprimir.setPreferredSize(new Dimension(300, 50));
        btnImprimir.addActionListener(ev -> {
            try { editorPane.print(null, null, true, null, null, true); }
            catch (Exception ex) { JOptionPane.showMessageDialog(previewDialog, "Erro ao acionar a impressora.", "Erro", JOptionPane.ERROR_MESSAGE); }
        });

        JPanel pnlImprimir = new JPanel(); pnlImprimir.setBackground(Color.WHITE); pnlImprimir.setBorder(new EmptyBorder(15, 0, 15, 0));
        pnlImprimir.add(btnImprimir); previewDialog.add(pnlImprimir, BorderLayout.SOUTH);
        previewDialog.setVisible(true);
    }

    // =========================================================
    // ABA APLICAÇÕES
    // =========================================================
    private void renderizarAbaAplicacoes() {
        String[] cols = {"ID", "PACIENTE", "DATA APLICAÇÃO", "VALOR COBRADO"};
        DefaultTableModel model = new DefaultTableModel(null, cols) { public boolean isCellEditable(int r, int c) { return false; } };
        JTable tab = new JTable(model); formatarTabela(tab);

        List<Aplicacao> listaApps = aplicacaoDAO.listarTodas();
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (Aplicacao app : listaApps) {
            String data = app.getDataHora() != null ? app.getDataHora().format(f) : "-";
            String nomePac = app.getPaciente() != null && app.getPaciente().getNome() != null ? app.getPaciente().getNome() : "Desconhecido";
            model.addRow(new Object[]{app.getId(), nomePac, data, String.format("R$ %.2f", app.getValor())});
        }

        JPanel tool = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0)); tool.setOpaque(false);
        JButton btnEdit = criarBotaoAcao("Alterar Valor", Cores.CINZA_GRAFITE, Color.WHITE);
        JButton btnDel = criarBotaoLixeira();

        tab.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) { boolean s = tab.getSelectedRow() >= 0; setBotaoAtivo(btnEdit, s); setBotaoAtivo(btnDel, s); }
        });

        btnEdit.addActionListener(e -> {
            int r = tab.getSelectedRow(); if (r < 0) return;
            double valorAtual = 0;
            try { valorAtual = Double.parseDouble(model.getValueAt(r, 3).toString().replace("R$ ", "").replace(",", ".")); } catch (Exception ex) {}

            abrirModalAlterarValor("Alterar Receita da Aplicação", "Novo valor cobrado pelo serviço (R$):", valorAtual, novoValor -> {
                try(Connection c = ConnectionFactory.getConnection(); PreparedStatement p = c.prepareStatement("UPDATE aplicacoes_v2 SET valor=? WHERE id=?")) {
                    p.setDouble(1, novoValor); p.setInt(2, (int)model.getValueAt(r, 0)); p.executeUpdate();
                } catch(Exception ex) {}
                carregarAbaAtiva(); atualizarCards();
            });
        });

        btnDel.addActionListener(e -> {
            int r = tab.getSelectedRow(); if (r < 0) return;
            confirmarExclusaoComDuplaChecagem(
                    "Tem certeza que deseja excluir esta Aplicação Financeira?\n\nCUIDADO: Isso também apagará todo o registro dessa vacina do Prontuário Médico do Paciente!",
                    "CONFIRMAÇÃO FINAL: EXCLUSÃO DE PRONTUÁRIO\n\nEsta ação não pode ser desfeita. A aplicação será permanentemente deletada do sistema.",
                    () -> {
                        try(Connection c = ConnectionFactory.getConnection(); PreparedStatement p = c.prepareStatement("DELETE FROM aplicacoes_v2 WHERE id=?")) {
                            p.setInt(1, (int)model.getValueAt(r, 0)); p.executeUpdate();
                        } catch(Exception ex) {}
                    }
            );
        });

        tool.add(btnEdit); tool.add(Box.createHorizontalStrut(5)); tool.add(btnDel);
        JPanel p = new JPanel(new BorderLayout(0,15)); p.setOpaque(false); p.add(tool, BorderLayout.NORTH); p.add(new JScrollPane(tab), BorderLayout.CENTER);
        pnlConteudoDinamico.add(p, BorderLayout.CENTER);
    }

    // =========================================================
    // ABA VACINAS
    // =========================================================
    private void renderizarAbaVacinas() {
        String[] cols = {"ID", "VACINA", "LOTE", "FORNECEDOR", "QTD", "CUSTO UNIT.", "CUSTO TOTAL"};
        DefaultTableModel model = new DefaultTableModel(null, cols) { public boolean isCellEditable(int r, int c) { return false; } };
        JTable tab = new JTable(model); formatarTabela(tab);

        List<Vacina> listaVacinas = vacinaDAO.listarTodas();
        for (Vacina v : listaVacinas) {
            String fornecedor = v.getLaboratorio() != null && !v.getLaboratorio().isEmpty() ? v.getLaboratorio() : "Não informado";
            model.addRow(new Object[]{ v.getId(), v.getNomeVacina(), v.getLote(), fornecedor, v.getQtdDisponivel(), String.format("R$ %.2f", v.getValorCompra()), String.format("R$ %.2f", v.getValorCompra() * v.getQtdDisponivel()) });
        }

        JPanel tool = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0)); tool.setOpaque(false);
        JButton btnEdit = criarBotaoAcao("Alterar Custo Unitário", Cores.CINZA_GRAFITE, Color.WHITE); btnEdit.setPreferredSize(new Dimension(200, 45));
        JButton btnDel = criarBotaoLixeira();

        tab.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) { boolean s = tab.getSelectedRow() >= 0; setBotaoAtivo(btnEdit, s); setBotaoAtivo(btnDel, s); }
        });

        btnEdit.addActionListener(e -> {
            int r = tab.getSelectedRow(); if (r < 0) return;
            double valorAtual = 0;
            try { valorAtual = Double.parseDouble(model.getValueAt(r, 5).toString().replace("R$ ", "").replace(",", ".")); } catch (Exception ex) {}

            abrirModalAlterarValor("Alterar Custo de Estoque", "Novo valor unitário pago na compra (R$):", valorAtual, novoValor -> {
                int id = (int)model.getValueAt(r, 0);
                try(Connection c = ConnectionFactory.getConnection(); PreparedStatement p = c.prepareStatement("UPDATE vacinas SET valor_compra=? WHERE id=?")) {
                    p.setDouble(1, novoValor); p.setInt(2, id); p.executeUpdate();
                } catch(Exception ex) {
                    try(Connection c = ConnectionFactory.getConnection(); PreparedStatement p2 = c.prepareStatement("UPDATE vacinas SET valorCompra=? WHERE id=?")) {
                        p2.setDouble(1, novoValor); p2.setInt(2, id); p2.executeUpdate();
                    } catch(Exception ex2) {}
                }
                carregarAbaAtiva(); atualizarCards();
            });
        });

        btnDel.addActionListener(e -> {
            int r = tab.getSelectedRow(); if (r < 0) return;
            confirmarExclusaoComDuplaChecagem(
                    "Tem certeza que deseja excluir este custo de Vacina?\n\nCUIDADO: Isso também APAGARÁ todo o lote dessa vacina fisicamente do seu Estoque!",
                    "CONFIRMAÇÃO FINAL: EXCLUSÃO DE ESTOQUE\n\nEsta ação não pode ser desfeita. O lote será permanentemente deletado do inventário da clínica.",
                    () -> {
                        try(Connection c = ConnectionFactory.getConnection(); PreparedStatement p = c.prepareStatement("DELETE FROM vacinas WHERE id=?")) {
                            p.setInt(1, (int)model.getValueAt(r, 0)); p.executeUpdate();
                        } catch(Exception ex) {}
                    }
            );
        });

        tool.add(btnEdit); tool.add(Box.createHorizontalStrut(5)); tool.add(btnDel);
        JPanel p = new JPanel(new BorderLayout(0,15)); p.setOpaque(false); p.add(tool, BorderLayout.NORTH); p.add(new JScrollPane(tab), BorderLayout.CENTER);
        pnlConteudoDinamico.add(p, BorderLayout.CENTER);
    }

    // =========================================================
    // ABA OUTROS
    // =========================================================
    private void renderizarAbaOutros() {
        String[] cols = {"ID", "DESCRIÇÃO", "TIPO", "DATA", "VALOR"};
        DefaultTableModel model = new DefaultTableModel(null, cols) { public boolean isCellEditable(int r, int c) { return false; } };
        JTable tab = new JTable(model); formatarTabela(tab);

        tab.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setBorder(new EmptyBorder(0, 15, 0, 15));
                if (!sel) {
                    comp.setBackground(Color.WHITE);
                    if (t.getValueAt(r, 2).toString().contains("Entrada")) comp.setForeground(new Color(39, 174, 96));
                    else comp.setForeground(new Color(231, 76, 60));
                } else { comp.setBackground(Cores.VERDE_AQUA); comp.setForeground(Color.WHITE); }
                return comp;
            }
        });

        List<LancamentoOutros> lista = outrosDAO.listarTodos();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (LancamentoOutros l : lista) {
            model.addRow(new Object[]{l.getId(), l.getNome(), l.getTipo(), l.getDataLancamento().format(fmt), String.format("R$ %.2f", l.getValor())});
        }

        JPanel tool = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0)); tool.setOpaque(false);
        JButton btnNovo = criarBotaoAcao("Novo Avulso", Cores.VERDE_AQUA, Color.WHITE); setBotaoAtivo(btnNovo, true);
        JButton btnEdit = criarBotaoAcao("Alterar Dados", Cores.CINZA_GRAFITE, Color.WHITE);
        JButton btnDel = criarBotaoLixeira();

        tab.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) { boolean s = tab.getSelectedRow() >= 0; setBotaoAtivo(btnEdit, s); setBotaoAtivo(btnDel, s); }
        });

        btnNovo.addActionListener(e -> modalOutros(null));
        btnEdit.addActionListener(e -> { int r = tab.getSelectedRow(); if (r >= 0) modalOutros(lista.get(r)); });
        btnDel.addActionListener(e -> {
            int r = tab.getSelectedRow();
            if (r >= 0 && JOptionPane.showConfirmDialog(frame, "Deseja realmente excluir este lançamento avulso?", "Confirmação", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                outrosDAO.excluir(lista.get(r).getId()); carregarAbaAtiva(); atualizarCards();
            }
        });

        tool.add(btnNovo); tool.add(btnEdit); tool.add(Box.createHorizontalStrut(5)); tool.add(btnDel);
        JPanel p = new JPanel(new BorderLayout(0,15)); p.setOpaque(false); p.add(tool, BorderLayout.NORTH); p.add(new JScrollPane(tab), BorderLayout.CENTER);
        pnlConteudoDinamico.add(p, BorderLayout.CENTER);
    }

    private void modalOutros(LancamentoOutros edicao) {
        JDialog diag = new JDialog(frame, edicao == null ? "Novo Lançamento Avulso" : "Alterar Lançamento", true);
        diag.setSize(450, 400); diag.setLocationRelativeTo(this); diag.getContentPane().setBackground(Color.WHITE);
        JPanel pnl = new JPanel(new GridLayout(3, 1, 15, 15)); pnl.setBackground(Color.WHITE); pnl.setBorder(new EmptyBorder(30, 40, 30, 40));

        JTextField txtNome = new JTextField(); txtNome.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        JComboBox<String> cbTipo = new JComboBox<>(new String[]{"Entrada avulsa", "Saída avulsa"}); cbTipo.setBackground(Color.WHITE);
        JTextField txtValor = new JTextField(); txtValor.setFont(new Font("Segoe UI", Font.PLAIN, 15));

        if(edicao != null) { txtNome.setText(edicao.getNome()); cbTipo.setSelectedItem(edicao.getTipo()); txtValor.setText(String.format(new java.util.Locale("pt", "BR"), "%.2f", edicao.getValor())); }

        pnl.add(montarBloco("DESCRIÇÃO", txtNome)); pnl.add(montarBloco("TIPO DE MOVIMENTAÇÃO", cbTipo)); pnl.add(montarBloco("VALOR (R$)", txtValor));

        JButton btnSalvar = criarBotaoAcao("Salvar", Cores.VERDE_AQUA, Color.WHITE); setBotaoAtivo(btnSalvar, true); btnSalvar.setPreferredSize(new Dimension(0, 50));
        btnSalvar.addActionListener(e -> {
            try {
                double val = Double.parseDouble(txtValor.getText().replace(".", "").replace(",", "."));
                if(edicao == null) outrosDAO.salvar(new LancamentoOutros(txtNome.getText(), cbTipo.getSelectedItem().toString(), val, LocalDate.now()));
                else { edicao.setNome(txtNome.getText()); edicao.setTipo(cbTipo.getSelectedItem().toString()); edicao.setValor(val); outrosDAO.atualizar(edicao); }
                carregarAbaAtiva(); atualizarCards(); diag.dispose();
            } catch (Exception ex) { JOptionPane.showMessageDialog(diag, "Valor inválido!"); }
        });

        JPanel pBase = new JPanel(new BorderLayout()); pBase.add(pnl, BorderLayout.CENTER);
        JPanel pBot = new JPanel(new BorderLayout()); pBot.setBorder(new EmptyBorder(0, 40, 30, 40)); pBot.setBackground(Color.WHITE); pBot.add(btnSalvar, BorderLayout.CENTER);
        pBase.add(pBot, BorderLayout.SOUTH); diag.add(pBase); diag.setVisible(true);
    }

    // =========================================================
    // ABA FIXOS
    // =========================================================
    private JTable tabFixos;
    private List<DespesaFixa> listaFixos;

    private void renderizarAbaFixos() {
        JPanel pnlFixos = new JPanel(new BorderLayout(0, 15)); pnlFixos.setOpaque(false);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0)); toolbar.setOpaque(false);
        JButton btnNovo = criarBotaoAcao("Novo Cadastro", Cores.VERDE_AQUA, Color.WHITE); setBotaoAtivo(btnNovo, true);
        JButton btnPagar = criarBotaoAcao("Pagar Lançamento", new Color(46, 204, 113), Color.WHITE);
        JButton btnEditar = criarBotaoAcao("Alterar Dados", Cores.CINZA_GRAFITE, Color.WHITE);
        JButton btnHistorico = criarBotaoAcao("Ver Histórico", new Color(52, 152, 219), Color.WHITE);
        JButton btnDelFixo = criarBotaoLixeira();

        String[] cols = {"ID", "DESCRIÇÃO", "TIPO", "PRÓX. VENCIMENTO", "ÚLTIMO PAGO"};
        DefaultTableModel model = new DefaultTableModel(null, cols) { public boolean isCellEditable(int r, int c) { return false; } };
        tabFixos = new JTable(model); formatarTabela(tabFixos);

        tabFixos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean s = tabFixos.getSelectedRow() >= 0;
                setBotaoAtivo(btnPagar, s); setBotaoAtivo(btnEditar, s); setBotaoAtivo(btnHistorico, s); setBotaoAtivo(btnDelFixo, s);
            }
        });

        btnNovo.addActionListener(e -> abrirModalFixo(null));
        btnPagar.addActionListener(e -> {
            DespesaFixa d = getDespesaFixoSelecionada(); if (d == null) return;
            if (d.isValorVariavel()) {
                abrirModalAlterarValor("Pagar Lançamento Variável", "Valor pago para " + d.getNome() + " neste mês:", 0.0, pago -> {
                    despesaDAO.registrarPagamento(d.getId(), pago);
                    carregarAbaAtiva(); atualizarCards();
                    JOptionPane.showMessageDialog(frame, "Pagamento registrado!");
                });
            } else {
                despesaDAO.registrarPagamento(d.getId(), d.getValorPadrao());
                carregarAbaAtiva(); atualizarCards();
                JOptionPane.showMessageDialog(frame, "Pagamento registrado com sucesso!");
            }
        });

        btnEditar.addActionListener(e -> { DespesaFixa d = getDespesaFixoSelecionada(); if(d != null) abrirModalFixo(d); });

        btnHistorico.addActionListener(e -> abrirModalHistorico(getDespesaFixoSelecionada()));
        btnDelFixo.addActionListener(e -> {
            DespesaFixa d = getDespesaFixoSelecionada(); if (d == null) return;
            confirmarExclusaoComDuplaChecagem(
                    "Deseja excluir a despesa '" + d.getNome() + "' e todo o seu histórico financeiro?",
                    "CONFIRMAÇÃO FINAL: A exclusão desta despesa apagará todos os pagamentos relacionados a ela dos relatórios.",
                    () -> { despesaDAO.excluir(d.getId()); }
            );
        });

        toolbar.add(btnNovo); toolbar.add(new JLabel("   |   "));
        toolbar.add(btnPagar); toolbar.add(btnEditar); toolbar.add(btnHistorico);
        toolbar.add(Box.createHorizontalStrut(5)); toolbar.add(btnDelFixo);

        tabFixos.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setBorder(new EmptyBorder(0, 15, 0, 15));
                if (!sel) {
                    try {
                        LocalDate venc = LocalDate.parse((String) t.getValueAt(r, 3), f);
                        long dias = ChronoUnit.DAYS.between(LocalDate.now(), venc);
                        if (dias >= 0 && dias <= 5) { comp.setBackground(new Color(255, 243, 205)); comp.setForeground(new Color(133, 100, 4)); }
                        else if (dias < 0) { comp.setBackground(new Color(253, 237, 237)); comp.setForeground(new Color(185, 28, 28)); }
                        else { comp.setBackground(Color.WHITE); comp.setForeground(Cores.CINZA_GRAFITE); }
                    } catch (Exception e) { comp.setBackground(Color.WHITE); comp.setForeground(Cores.CINZA_GRAFITE); }
                } else { comp.setBackground(Cores.VERDE_AQUA); comp.setForeground(Color.WHITE); }
                return comp;
            }
        });

        listaFixos = despesaDAO.listarTodas();
        LocalDate hoje = LocalDate.now(); DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (DespesaFixa d : listaFixos) {
            int diaVenc = Math.min(d.getDiaVencimento(), hoje.lengthOfMonth());
            LocalDate proxVenc = hoje.withDayOfMonth(diaVenc);

            boolean pagoEsteMes = d.getDataUltimoPagamento() != null && d.getDataUltimoPagamento().getMonthValue() == hoje.getMonthValue() && d.getDataUltimoPagamento().getYear() == hoje.getYear();
            if (pagoEsteMes || (hoje.getDayOfMonth() > diaVenc && d.getDataUltimoPagamento() != null)) {
                proxVenc = hoje.plusMonths(1).withDayOfMonth(Math.min(d.getDiaVencimento(), hoje.plusMonths(1).lengthOfMonth()));
            }

            String tipo = d.isValorVariavel() ? "Variável" : "Fixo";
            String ultimoPago = d.getUltimoValorPago() > 0 ? String.format("R$ %.2f", d.getUltimoValorPago()) : "Pendente";
            model.addRow(new Object[]{ d.getId(), d.getNome(), tipo, proxVenc.format(fmt), ultimoPago });
        }

        pnlFixos.add(toolbar, BorderLayout.NORTH); pnlFixos.add(new JScrollPane(tabFixos), BorderLayout.CENTER);
        pnlConteudoDinamico.add(pnlFixos, BorderLayout.CENTER);
    }

    private void abrirModalFixo(DespesaFixa edicao) {
        if(edicao != null && edicao.getId() == 0) return; // Segurança
        JDialog diag = new JDialog(frame, edicao == null ? "Novo Custo Fixo" : "Alterar Dados", true);
        diag.setSize(500, 450); diag.setLocationRelativeTo(this); diag.getContentPane().setBackground(Color.WHITE);
        JPanel pnl = new JPanel(new GridLayout(4, 1, 15, 15)); pnl.setBackground(Color.WHITE); pnl.setBorder(new EmptyBorder(30, 40, 30, 40));

        JTextField txtNome = new JTextField(); txtNome.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        JCheckBox chk = new JCheckBox("  Este valor varia mensalmente"); chk.setBackground(Color.WHITE);
        JTextField txtValor = new JTextField(); txtValor.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        Integer[] dias = new Integer[31]; for(int i=0; i<31; i++) dias[i] = i+1;
        JComboBox<Integer> cbDia = new JComboBox<>(dias); cbDia.setBackground(Color.WHITE);

        chk.addActionListener(e -> { txtValor.setEnabled(!chk.isSelected()); txtValor.setBackground(chk.isSelected() ? new Color(240, 240, 240) : Color.WHITE); if(chk.isSelected()) txtValor.setText("0,00"); });
        if (edicao != null) { txtNome.setText(edicao.getNome()); chk.setSelected(edicao.isValorVariavel()); txtValor.setText(String.format(new java.util.Locale("pt", "BR"), "%.2f", edicao.getValorPadrao())); cbDia.setSelectedItem(edicao.getDiaVencimento()); if(chk.isSelected()) { txtValor.setEnabled(false); txtValor.setBackground(new Color(240, 240, 240)); } }

        pnl.add(montarBloco("DESCRIÇÃO DO CUSTO", txtNome)); pnl.add(chk); pnl.add(montarBloco("VALOR PADRÃO (R$)", txtValor)); pnl.add(montarBloco("DIA DO VENCIMENTO", cbDia));
        JButton btnSalvar = criarBotaoAcao("Salvar Lançamento", Cores.VERDE_AQUA, Color.WHITE); setBotaoAtivo(btnSalvar, true); btnSalvar.setPreferredSize(new Dimension(0, 50));
        btnSalvar.addActionListener(e -> {
            double val = 0; if (!chk.isSelected()) try { val = Double.parseDouble(txtValor.getText().replace(".", "").replace(",", ".")); } catch (Exception ex) { return; }
            if (edicao == null) despesaDAO.salvar(new DespesaFixa(txtNome.getText(), val, chk.isSelected(), (Integer) cbDia.getSelectedItem()));
            else { edicao.setNome(txtNome.getText()); edicao.setValorVariavel(chk.isSelected()); edicao.setValorPadrao(val); edicao.setDiaVencimento((Integer) cbDia.getSelectedItem()); despesaDAO.atualizar(edicao); }
            carregarAbaAtiva(); atualizarCards(); diag.dispose();
        });

        JPanel pBase = new JPanel(new BorderLayout()); pBase.add(pnl, BorderLayout.CENTER);
        JPanel pBot = new JPanel(new BorderLayout()); pBot.setBorder(new EmptyBorder(0, 40, 30, 40)); pBot.setBackground(Color.WHITE); pBot.add(btnSalvar, BorderLayout.CENTER);
        pBase.add(pBot, BorderLayout.SOUTH); diag.add(pBase); diag.setVisible(true);
    }

    private void abrirModalHistorico(DespesaFixa d) {
        if (d == null) return;
        JDialog diag = new JDialog(frame, "Histórico - " + d.getNome(), true);
        diag.setSize(500, 500); diag.setLocationRelativeTo(this); diag.getContentPane().setBackground(Color.WHITE);

        String[] cols = {"ID_HIST", "Data do Pagamento", "Valor Pago", ""};
        DefaultTableModel model = new DefaultTableModel(null, cols) { public boolean isCellEditable(int r, int c) { return false; } };
        JTable tabHist = new JTable(model); formatarTabela(tabHist);

        tabHist.getColumnModel().getColumn(0).setMinWidth(0); tabHist.getColumnModel().getColumn(0).setMaxWidth(0);
        tabHist.getColumnModel().getColumn(3).setMaxWidth(60);

        tabHist.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, r, c); l.setText(""); l.setHorizontalAlignment(SwingConstants.CENTER);
                try { l.setIcon(new FlatSVGIcon("icons/trash.svg", 20, 20).setColorFilter(new FlatSVGIcon.ColorFilter(cor -> new Color(231, 76, 60)))); } catch(Exception ignored){}
                return l;
            }
        });

        Runnable load = () -> { model.setRowCount(0); for (Object[] row : despesaDAO.listarHistorico(d.getId())) model.addRow(new Object[]{row[0], row[1], row[2], ""}); }; load.run();

        tabHist.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                int r = tabHist.rowAtPoint(e.getPoint()); int c = tabHist.columnAtPoint(e.getPoint());
                if (r >= 0 && c == 3) {
                    if (JOptionPane.showConfirmDialog(diag, "Deseja excluir este pagamento do histórico?", "Excluir", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                        despesaDAO.excluirHistorico((int) model.getValueAt(r, 0), d.getId()); load.run(); carregarAbaAtiva(); atualizarCards();
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tabHist); scroll.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); scroll.getViewport().setBackground(Color.WHITE);
        diag.add(scroll); diag.setVisible(true);
    }

    private DespesaFixa getDespesaFixoSelecionada() { int r = tabFixos.getSelectedRow(); return (r >= 0) ? listaFixos.get(r) : null; }

    // =========================================================
    // CONTROLE DE ESTADO E MODAIS ESTÉTICOS
    // =========================================================

    private void abrirModalAlterarValor(String titulo, String mensagem, double valorAtual, java.util.function.Consumer<Double> acaoConfirmar) {
        JDialog diag = new JDialog(frame, titulo, true);
        diag.setSize(450, 280); diag.setLocationRelativeTo(frame); diag.getContentPane().setBackground(Color.WHITE);
        JPanel p = new JPanel(new BorderLayout(0, 15)); p.setBackground(Color.WHITE); p.setBorder(new EmptyBorder(30, 40, 20, 40));

        JTextField t = new JTextField(String.format(new java.util.Locale("pt", "BR"), "%.2f", valorAtual));
        t.setFont(new Font("Segoe UI", Font.PLAIN, 16)); t.setPreferredSize(new Dimension(0, 45));
        t.setBackground(Color.WHITE);

        p.add(montarBloco(mensagem, t), BorderLayout.CENTER);

        JButton b = criarBotaoAcao("Confirmar Alteração", Cores.VERDE_AQUA, Color.WHITE); setBotaoAtivo(b, true); b.setPreferredSize(new Dimension(0, 50));
        b.addActionListener(e -> {
            try {
                double v = Double.parseDouble(t.getText().replace(".", "").replace(",", "."));
                acaoConfirmar.accept(v); diag.dispose();
            } catch (Exception ex) { JOptionPane.showMessageDialog(diag, "Valor numérico inválido!", "Erro", JOptionPane.ERROR_MESSAGE); }
        });

        JPanel pBot = new JPanel(new BorderLayout()); pBot.setBorder(new EmptyBorder(0, 40, 30, 40)); pBot.setBackground(Color.WHITE); pBot.add(b, BorderLayout.CENTER);
        diag.add(p, BorderLayout.CENTER); diag.add(pBot, BorderLayout.SOUTH); diag.setVisible(true);
    }

    private void confirmarExclusaoComDuplaChecagem(String msgAviso1, String msgAviso2, Runnable acaoDeletar) {
        int opt1 = JOptionPane.showConfirmDialog(frame, msgAviso1, "Atenção: Exclusão Permanente", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt1 == JOptionPane.YES_OPTION) {
            int opt2 = JOptionPane.showConfirmDialog(frame, msgAviso2, "Confirmação Final de Exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
            if (opt2 == JOptionPane.YES_OPTION) {
                acaoDeletar.run(); carregarAbaAtiva(); atualizarCards();
                JOptionPane.showMessageDialog(frame, "Registro excluído com sucesso do Banco de Dados.");
            }
        }
    }

    private void setBotaoAtivo(JButton b, boolean ativo) {
        b.setEnabled(ativo);
        if (ativo) {
            b.setBackground((Color) b.getClientProperty("bgAtiva"));
            b.setForeground((Color) b.getClientProperty("fgAtiva"));
            if (b.getIcon() instanceof FlatSVGIcon) ((FlatSVGIcon) b.getIcon()).setColorFilter(new FlatSVGIcon.ColorFilter(c -> Color.WHITE));
        } else {
            b.setBackground(new Color(225, 225, 225));
            b.setForeground(new Color(160, 160, 160));
            if (b.getIcon() instanceof FlatSVGIcon) ((FlatSVGIcon) b.getIcon()).setColorFilter(new FlatSVGIcon.ColorFilter(c -> new Color(160, 160, 160)));
        }
    }

    private JButton criarBotaoAcao(String t, Color bgAtiva, Color fgAtiva) {
        JButton b = new JButton(t); b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setPreferredSize(new Dimension(160, 45)); b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.putClientProperty("bgAtiva", bgAtiva); b.putClientProperty("fgAtiva", fgAtiva);
        setBotaoAtivo(b, false); return b;
    }

    private JButton criarBotaoLixeira() {
        JButton btn = new JButton(); btn.setPreferredSize(new Dimension(45, 45)); btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); btn.setToolTipText("Excluir Permanentemente");
        btn.putClientProperty("bgAtiva", new Color(231, 76, 60)); btn.putClientProperty("fgAtiva", Color.WHITE);
        try { btn.setIcon(new FlatSVGIcon("icons/trash.svg", 20, 20)); } catch(Exception ignored){}
        setBotaoAtivo(btn, false); return btn;
    }

    private void formatarTabela(JTable t) {
        t.setRowHeight(45); t.setShowVerticalLines(false); t.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12)); t.getTableHeader().setBackground(new Color(245, 245, 245));
    }

    private JPanel criarMiniCard(String titulo, JLabel lblValor, Color cor) {
        JPanel p = new JPanel(new BorderLayout(5, 5)); p.setBackground(Color.WHITE); p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(230,230,230)), new EmptyBorder(15, 20, 15, 20)));
        JLabel t = new JLabel(titulo); t.setFont(new Font("Segoe UI", Font.BOLD, 12)); t.setForeground(Cores.CINZA_LABEL);
        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 22)); lblValor.setForeground(cor);
        p.add(t, BorderLayout.NORTH); p.add(lblValor, BorderLayout.CENTER);
        JPanel base = new JPanel(); base.setPreferredSize(new Dimension(0, 4)); base.setBackground(cor); p.add(base, BorderLayout.SOUTH);
        return p;
    }

    private JPanel montarBloco(String t, JComponent c) { JPanel p = new JPanel(new BorderLayout(0, 5)); p.setOpaque(false); p.setBackground(Color.WHITE); JLabel l = new JLabel(t); l.setFont(new Font("Segoe UI", Font.BOLD, 12)); l.setForeground(Cores.CINZA_LABEL); p.add(l, BorderLayout.NORTH); p.add(c, BorderLayout.CENTER); return p; }

    private JButton criarBotaoAba(String t, boolean a) {
        JButton b = new JButton(t); b.setFont(new Font("Segoe UI", a ? Font.BOLD : Font.PLAIN, 18)); b.setForeground(a ? Cores.VERDE_AQUA : Cores.CINZA_LABEL); b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false); b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> { abaAtiva = t; for (Component c : ((JPanel) b.getParent()).getComponents()) { if (c instanceof JButton) { c.setFont(new Font("Segoe UI", Font.PLAIN, 18)); c.setForeground(Cores.CINZA_LABEL); } } b.setFont(new Font("Segoe UI", Font.BOLD, 18)); b.setForeground(Cores.VERDE_AQUA); carregarAbaAtiva(); });
        return b;
    }
}