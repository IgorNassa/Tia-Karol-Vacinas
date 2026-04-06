package br.sistema.view.panels;

import br.sistema.model.Aplicacao;
import br.sistema.model.Vacina;
import br.sistema.repository.AplicacaoDAO;
import br.sistema.repository.PacienteDAO;
import br.sistema.repository.VacinaDAO;
import br.sistema.util.Cores;
import br.sistema.util.GerenciadorCaixa;
import br.sistema.view.TelaPrincipal;
import br.sistema.view.components.GlassPanel;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PainelDashboard extends JPanel {
    private TelaPrincipal frame;
    private CardLayout cardLayout;
    private JPanel pnlCards;
    private JButton btnNavVisao, btnNavFinanceiro;

    public PainelDashboard(TelaPrincipal frame) {
        this.frame = frame;
        setOpaque(false);
        setLayout(new BorderLayout(0, 20));
        setBorder(new EmptyBorder(30, 40, 30, 40));

        GlassPanel cardVidro = new GlassPanel();
        cardVidro.setLayout(new BorderLayout());
        cardVidro.setBorder(new EmptyBorder(25, 35, 30, 35));

        // --- HEADER ---
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));

        JLabel titulo = new JLabel("Visão Geral da Clínica");
        titulo.setFont(new Font("Segoe UI Semilight", Font.PLAIN, 32));
        titulo.setForeground(Cores.CINZA_GRAFITE);
        header.add(titulo, BorderLayout.WEST);

        // --- NAVBAR (Abas do Dashboard) ---
        JPanel pnlNavbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        pnlNavbar.setOpaque(false);
        pnlNavbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)));

        btnNavVisao = criarBotaoNav("Visão Geral", true);
        btnNavFinanceiro = criarBotaoNav("Relatório Financeiro", false);

        btnNavVisao.addActionListener(e -> trocarAba("VISAO", btnNavVisao));
        btnNavFinanceiro.addActionListener(e -> trocarAba("FINANCEIRO", btnNavFinanceiro));

        pnlNavbar.add(btnNavVisao);
        pnlNavbar.add(btnNavFinanceiro);
        header.add(pnlNavbar, BorderLayout.SOUTH);

        cardVidro.add(header, BorderLayout.NORTH);

        // --- CARDS PRINCIPAIS (CardLayout) ---
        cardLayout = new CardLayout();
        pnlCards = new JPanel(cardLayout);
        pnlCards.setOpaque(false);

        pnlCards.add(criarAbaVisaoGeral(), "VISAO");
        pnlCards.add(criarAbaFinanceiro(), "FINANCEIRO");

        cardVidro.add(pnlCards, BorderLayout.CENTER);
        add(cardVidro, BorderLayout.CENTER);
    }

    private JPanel criarAbaVisaoGeral() {
        JPanel pnl = new JPanel(new BorderLayout(20, 20));
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(15, 0, 0, 0));

        // 1. CARDS DE RESUMO RÁPIDO (DADOS REAIS)
        JPanel pnlResumo = new JPanel(new GridLayout(1, 3, 20, 0));
        pnlResumo.setOpaque(false);
        pnlResumo.setPreferredSize(new Dimension(0, 120));

        int totalPacientes = new PacienteDAO().listarTodos().size();

        List<Aplicacao> todasAplicacoes = new AplicacaoDAO().listarTodas();
        long totalAplicadas = todasAplicacoes.stream()
                .filter(a -> "Aplicado".equalsIgnoreCase(a.getStatus()))
                .count();

        int totalEstoque = 0;
        // AJUSTE: Usando qtdDisponivel conforme o seu modelo
        for (Vacina v : new VacinaDAO().listarTodas()) {
            totalEstoque += v.getQtdDisponivel();
        }

        pnlResumo.add(criarKpiCard("Pacientes Registrados", String.valueOf(totalPacientes), "member-list.svg", Cores.VERDE_AQUA));
        pnlResumo.add(criarKpiCard("Doses Aplicadas", String.valueOf(totalAplicadas), "adicionar.svg", Cores.ROSA_KAROL));
        pnlResumo.add(criarKpiCard("Vacinas no Estoque", String.valueOf(totalEstoque), "procurar.svg", new Color(243, 156, 18)));
        pnl.add(pnlResumo, BorderLayout.NORTH);

        // 2. CORPO (Gráficos e Listas)
        JPanel pnlCorpo = new JPanel(new GridLayout(1, 2, 20, 0));
        pnlCorpo.setOpaque(false);

        // DADOS REAIS: Vacinas Mais Aplicadas
        JPanel pnlTopVacinas = criarPainelSessao("Vacinas Mais Aplicadas");

        Map<String, Integer> contagemVacinas = new HashMap<>();
        for (Aplicacao app : todasAplicacoes) {
            if ("Aplicado".equalsIgnoreCase(app.getStatus())) {
                String nome = app.getVacina().getNomeVacina();
                contagemVacinas.put(nome, contagemVacinas.getOrDefault(nome, 0) + 1);
            }
        }

        List<Map.Entry<String, Integer>> topVacinas = contagemVacinas.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(4)
                .collect(Collectors.toList());

        Color[] coresTop = {new Color(52, 152, 219), new Color(155, 89, 182), new Color(230, 126, 34), new Color(46, 204, 113)};

        if (topVacinas.isEmpty()) {
            pnlTopVacinas.add(new JLabel("Aguardando primeiras aplicações..."));
        } else {
            int maxAplicacoes = topVacinas.get(0).getValue();
            for (int i = 0; i < topVacinas.size(); i++) {
                Map.Entry<String, Integer> entry = topVacinas.get(i);
                int porcentagem = (int) (((double) entry.getValue() / maxAplicacoes) * 100);
                pnlTopVacinas.add(criarBarraProgresso(entry.getKey() + " (" + entry.getValue() + ")", porcentagem, coresTop[i % coresTop.length]));
                if(i < topVacinas.size() -1) pnlTopVacinas.add(Box.createVerticalStrut(15));
            }
        }
        pnlCorpo.add(pnlTopVacinas);

        // DADOS REAIS: Próximos Agendamentos
        JPanel pnlAgendamentos = criarPainelSessao("📅 Próximos Agendamentos");

        LocalDate hoje = LocalDate.now();
        List<Aplicacao> agendamentos = todasAplicacoes.stream()
                .filter(a -> "Agendado".equalsIgnoreCase(a.getStatus()))
                .filter(a -> a.getDataHora() != null && !a.getDataHora().toLocalDate().isBefore(hoje))
                .sorted((a1, a2) -> a1.getDataHora().compareTo(a2.getDataHora()))
                .limit(3)
                .collect(Collectors.toList());

        if (agendamentos.isEmpty()) {
            pnlAgendamentos.add(new JLabel("Nenhum agendamento para os próximos dias."));
        } else {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM HH:mm");
            for (Aplicacao app : agendamentos) {
                pnlAgendamentos.add(criarItemNotificacao(
                        app.getPaciente().getNome(),
                        app.getDataHora().format(dtf),
                        app.getVacina().getNomeVacina()
                ));
            }
        }
        pnlCorpo.add(pnlAgendamentos);

        pnl.add(pnlCorpo, BorderLayout.CENTER);
        return pnl;
    }

    private JPanel criarAbaFinanceiro() {
        JPanel pnl = new JPanel(new BorderLayout(20, 20));
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(15, 0, 0, 0));

        JPanel pnlCaixa = new JPanel(new GridLayout(1, 3, 20, 0));
        pnlCaixa.setOpaque(false);
        pnlCaixa.setPreferredSize(new Dimension(0, 120));

        // DADOS REAIS: Puxando o saldo do GerenciadorCaixa
        double saldoCaixa = GerenciadorCaixa.getSaldoAtualCaixa();

        // Puxando entradas totais e saídas totais apenas para visualização
        double totalEntradas = calcularTotalEntradas();
        double totalSaidas = calcularTotalSaidas();

        pnlCaixa.add(criarKpiCard("Entradas Totais", String.format("R$ %,.2f", totalEntradas), "adicionar.svg", new Color(46, 204, 113)));
        pnlCaixa.add(criarKpiCard("Saídas Totais", String.format("R$ %,.2f", totalSaidas), "trash.svg", new Color(231, 76, 60)));
        pnlCaixa.add(criarKpiCard("Saldo em Caixa", String.format("R$ %,.2f", saldoCaixa), "doar.svg", new Color(52, 152, 219)));
        pnl.add(pnlCaixa, BorderLayout.NORTH);

        JPanel pnlCorpo = new JPanel(new BorderLayout());
        pnlCorpo.setOpaque(false);

        JPanel pnlGrafico = criarPainelSessao("Comparativo Geral (Entradas vs Saídas)");
        pnlGrafico.setLayout(new BorderLayout());

        // Passa os valores reais para o gráfico desenhar
        GraficoPizzaFinanceiro graficoPizza = new GraficoPizzaFinanceiro(totalEntradas, totalSaidas);
        pnlGrafico.add(graficoPizza, BorderLayout.CENTER);

        JPanel pnlLegendas = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
        pnlLegendas.setOpaque(false);

        // Calcula a porcentagem para a legenda
        double somaGeral = totalEntradas + totalSaidas;
        int pctEntradas = somaGeral > 0 ? (int) Math.round((totalEntradas / somaGeral) * 100) : 0;
        int pctSaidas = somaGeral > 0 ? (int) Math.round((totalSaidas / somaGeral) * 100) : 0;

        pnlLegendas.add(criarLegenda("Entradas (" + pctEntradas + "%)", new Color(46, 204, 113)));
        pnlLegendas.add(criarLegenda("Saídas (" + pctSaidas + "%)", new Color(231, 76, 60)));

        pnlGrafico.add(pnlLegendas, BorderLayout.SOUTH);

        pnlCorpo.add(pnlGrafico, BorderLayout.CENTER);
        pnl.add(pnlCorpo, BorderLayout.CENTER);

        return pnl;
    }

    // Métodos Auxiliares de Consulta Direta ao Banco para Totais
    private double calcularTotalEntradas() {
        double entradas = 0;
        for (Aplicacao a : new AplicacaoDAO().listarTodas()) { entradas += a.getValor(); }
        try (Connection c = br.sistema.repository.ConnectionFactory.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT SUM(valor) FROM lancamentos_outros WHERE tipo = 'Entrada avulsa' OR tipo = 'Receita'")) {
            try (ResultSet rs = p.executeQuery()) { if (rs.next()) entradas += rs.getDouble(1); }
        } catch (Exception e) {}
        return entradas;
    }

    private double calcularTotalSaidas() {
        double saidas = 0;
        try (Connection c = br.sistema.repository.ConnectionFactory.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT SUM(valor) FROM lancamentos_outros WHERE tipo = 'Saída avulsa' OR tipo = 'Despesa'")) {
            try (ResultSet rs = p.executeQuery()) { if (rs.next()) saidas += rs.getDouble(1); }
        } catch (Exception e) {}
        return saidas;
    }

    private JPanel criarKpiCard(String titulo, String valor, String icone, Color corAcento) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(230, 230, 230), 1, true),
                new EmptyBorder(20, 20, 20, 20)
        ));

        JPanel textos = new JPanel(new GridLayout(2, 1));
        textos.setOpaque(false);

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitulo.setForeground(Cores.CINZA_LABEL);

        JLabel lblValor = new JLabel(valor);
        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblValor.setForeground(new Color(50, 50, 50));

        textos.add(lblTitulo); textos.add(lblValor);
        card.add(textos, BorderLayout.CENTER);

        JLabel lblIcone = new JLabel();
        Icon icn = carregarIconeBlindado(icone, 40, corAcento);
        if (icn != null) lblIcone.setIcon(icn);
        card.add(lblIcone, BorderLayout.EAST);

        JPanel linhaBase = new JPanel();
        linhaBase.setPreferredSize(new Dimension(0, 4));
        linhaBase.setBackground(corAcento);
        card.add(linhaBase, BorderLayout.SOUTH);

        return card;
    }

    private JPanel criarPainelSessao(String titulo) {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
        pnl.setBackground(Color.WHITE);
        pnl.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(230, 230, 230), 1, true),
                new EmptyBorder(20, 20, 20, 20)
        ));

        JLabel lbl = new JLabel(titulo);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lbl.setForeground(new Color(50, 50, 50));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        pnl.add(lbl); pnl.add(Box.createVerticalStrut(20));
        return pnl;
    }

    private JPanel criarBarraProgresso(String tituloBarra, int porcentagemParaBarra, Color cor) {
        JPanel pnl = new JPanel(new BorderLayout(0, 5));
        pnl.setOpaque(false); pnl.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JLabel lbl = new JLabel(tituloBarra);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(Cores.CINZA_GRAFITE);

        JProgressBar pb = new JProgressBar(0, 100);
        pb.setValue(porcentagemParaBarra); pb.setStringPainted(false); pb.setForeground(cor);
        pb.setBackground(new Color(240, 240, 240)); pb.setBorderPainted(false);
        pb.setPreferredSize(new Dimension(0, 10));

        pnl.add(lbl, BorderLayout.NORTH); pnl.add(pb, BorderLayout.CENTER);
        return pnl;
    }

    private JPanel criarItemNotificacao(String nomePaciente, String detalheData, String nomeVacina) {
        JPanel pnl = new JPanel(new BorderLayout(15, 0));
        pnl.setBackground(new Color(250, 252, 255));
        pnl.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 230, 240), 1, true),
                new EmptyBorder(10, 15, 10, 15)
        ));
        pnl.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        JLabel lblIcone = new JLabel("💉");
        lblIcone.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        pnl.add(lblIcone, BorderLayout.WEST);

        JPanel pnlTextos = new JPanel(new GridLayout(2, 1)); pnlTextos.setOpaque(false);

        JLabel lblNome = new JLabel(nomePaciente + " - " + detalheData);
        lblNome.setFont(new Font("Segoe UI", Font.BOLD, 14)); lblNome.setForeground(new Color(50, 50, 50));

        JLabel lblVacina = new JLabel("Vacina: " + nomeVacina);
        lblVacina.setFont(new Font("Segoe UI", Font.ITALIC, 12)); lblVacina.setForeground(Cores.VERDE_AQUA);

        pnlTextos.add(lblNome); pnlTextos.add(lblVacina);
        pnl.add(pnlTextos, BorderLayout.CENTER);

        return pnl;
    }

    private JPanel criarLegenda(String texto, Color cor) {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)); pnl.setOpaque(false);
        JPanel bolinha = new JPanel(); bolinha.setPreferredSize(new Dimension(12, 12)); bolinha.setBackground(cor);
        JLabel lbl = new JLabel(texto); lbl.setFont(new Font("Segoe UI", Font.BOLD, 12)); lbl.setForeground(new Color(100, 100, 100));
        pnl.add(bolinha); pnl.add(lbl);
        return pnl;
    }

    // Gráfico de Pizza Dinâmico para Financeiro
    private class GraficoPizzaFinanceiro extends JPanel {
        private double entradas;
        private double saidas;

        public GraficoPizzaFinanceiro(double entradas, double saidas) {
            this.entradas = entradas;
            this.saidas = saidas;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth(); int height = getHeight();
            int size = Math.min(width, height) - 40;
            if (size <= 0) return;

            int x = (width - size) / 2; int y = (height - size) / 2;

            double total = entradas + saidas;
            if (total == 0) {
                // Se não tem dados, desenha um arco cinza claro
                g2.setColor(new Color(230, 230, 230));
                g2.fill(new Arc2D.Double(x, y, size, size, 0, 360, Arc2D.PIE));
            } else {
                double anguloEntradas = (entradas / total) * 360;
                double anguloSaidas = (saidas / total) * 360;

                // Desenha Entradas (Verde)
                g2.setColor(new Color(46, 204, 113));
                g2.fill(new Arc2D.Double(x, y, size, size, 0, anguloEntradas, Arc2D.PIE));

                // Desenha Saídas (Vermelho)
                g2.setColor(new Color(231, 76, 60));
                g2.fill(new Arc2D.Double(x, y, size, size, anguloEntradas, anguloSaidas, Arc2D.PIE));

                // Linhas Divisórias Brancas
                g2.setColor(Color.WHITE); g2.setStroke(new BasicStroke(3f));
                g2.draw(new Arc2D.Double(x, y, size, size, 0, anguloEntradas, Arc2D.PIE));
                g2.draw(new Arc2D.Double(x, y, size, size, anguloEntradas, anguloSaidas, Arc2D.PIE));
            }

            // Furo no meio para fazer o "Donut Chart"
            int furoSize = size / 2;
            g2.setColor(Color.WHITE);
            g2.fillOval(x + (size - furoSize) / 2, y + (size - furoSize) / 2, furoSize, furoSize);

            g2.setColor(Cores.CINZA_GRAFITE); g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
            FontMetrics fm = g2.getFontMetrics(); String textoCentro = "Fluxo";
            g2.drawString(textoCentro, (width - fm.stringWidth(textoCentro)) / 2, height / 2);
        }
    }

    private JButton criarBotaoNav(String texto, boolean ativo) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Segoe UI", ativo ? Font.BOLD : Font.PLAIN, 18));
        btn.setForeground(ativo ? Cores.VERDE_AQUA : Cores.CINZA_LABEL);
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); btn.setBorder(new EmptyBorder(10, 15, 10, 15));
        return btn;
    }

    private void trocarAba(String aba, JButton btnAtivo) {
        cardLayout.show(pnlCards, aba);
        btnNavVisao.setFont(new Font("Segoe UI", Font.PLAIN, 18)); btnNavVisao.setForeground(Cores.CINZA_LABEL);
        btnNavFinanceiro.setFont(new Font("Segoe UI", Font.PLAIN, 18)); btnNavFinanceiro.setForeground(Cores.CINZA_LABEL);
        btnAtivo.setFont(new Font("Segoe UI", Font.BOLD, 18)); btnAtivo.setForeground(Cores.VERDE_AQUA);
    }

    private FlatSVGIcon carregarIconeBlindado(String nomeArquivo, int tamanho, Color cor) {
        try {
            java.net.URL imgURL = getClass().getResource("/icons/" + nomeArquivo);
            if (imgURL != null) {
                return (FlatSVGIcon) new FlatSVGIcon(imgURL).derive(tamanho, tamanho).setColorFilter(new FlatSVGIcon.ColorFilter(c -> cor));
            }
        } catch (Exception e) {}
        return null;
    }
}