package br.sistema.view.panels;

import br.sistema.model.Aplicacao;
import br.sistema.repository.AplicacaoDAO;
import br.sistema.util.Cores;
import br.sistema.view.TelaPrincipal;
import br.sistema.view.components.GlassPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PainelDashboard extends JPanel {
    private TelaPrincipal frame;
    private JTable tabela;
    private DefaultTableModel modeloTabela;
    private int hoveredRow = -1;
    private String abaAtiva = "Recentes";

    private JButton btnAbaRecentes;
    private JButton btnAbaHoje;
    private JButton btnAbaProximas;

    // --- AGORA USAMOS O BANCO REAL ---
    private AplicacaoDAO dao;
    private List<Aplicacao> todasAplicacoes;
    private List<Aplicacao> aplicacoesFiltradas; // Usado para saber em qual linha clicamos

    public PainelDashboard(TelaPrincipal frame) {
        this.frame = frame;
        this.dao = new AplicacaoDAO();
        setOpaque(false);
        setLayout(new BorderLayout(0, 20));
        setBorder(new EmptyBorder(30, 40, 30, 40));

        carregarDadosDoBanco(); // Puxa do SQLite
        montarCardsTop();       // Calcula baseado nos dados
        montarTabelaCentral();  // Desenha a tabela
        atualizarDadosTabela(); // Filtra a aba atual
    }

    private void carregarDadosDoBanco() {
        // Traz as aplicações reais que você salvou no formulário
        todasAplicacoes = dao.listarTodas();
    }

    private void montarCardsTop() {
        LocalDate hoje = LocalDate.now();
        String dataFormatada = hoje.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        long agendadasHoje = 0;
        long pendentesGeral = 0;
        double faturamentoHoje = 0.0;

        // Calcula os resumos automaticamente
        for (Aplicacao app : todasAplicacoes) {
            try {
                // Pega só os 10 primeiros caracteres para a data (dd/MM/yyyy)
                String dataStr = app.getDataHora().length() >= 10 ? app.getDataHora().substring(0, 10) : app.getDataHora();
                LocalDate dataApp = LocalDate.parse(dataStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                if (app.getStatus().equalsIgnoreCase("Agendado")) {
                    pendentesGeral++;
                    if (dataApp.equals(hoje)) {
                        agendadasHoje++;
                    }
                }
                if (app.getStatus().equalsIgnoreCase("Aplicado") && dataApp.equals(hoje)) {
                    faturamentoHoje += app.getValor();
                }
            } catch (Exception e) {
                // Ignora erros de formatação na data para não quebrar o cálculo
            }
        }

        JPanel painelCards = new JPanel(new GridLayout(1, 3, 25, 0));
        painelCards.setOpaque(false);
        painelCards.setPreferredSize(new Dimension(0, 130));

        painelCards.add(criarCardResumo("Agendadas para Hoje", String.valueOf(agendadasHoje), Cores.VERDE_AQUA));
        painelCards.add(criarCardLembrete());
        painelCards.add(criarCardResumo("Faturamento - " + dataFormatada, String.format("R$ %.2f", faturamentoHoje), Cores.CINZA_GRAFITE));

        add(painelCards, BorderLayout.NORTH);
    }

    private void montarTabelaCentral() {
        GlassPanel cardVidro = new GlassPanel();
        cardVidro.setLayout(new BorderLayout());
        cardVidro.setBorder(new EmptyBorder(25, 35, 30, 35));

        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 15, 0));

        JLabel titulo = new JLabel("Painel de Aplicações");
        titulo.setFont(new Font("Segoe UI Semilight", Font.PLAIN, 28));
        titulo.setForeground(Cores.CINZA_GRAFITE);
        header.add(titulo, BorderLayout.WEST);

        JPanel controles = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        controles.setOpaque(false);

        JTextField txtBusca = new JTextField();
        txtBusca.setPreferredSize(new Dimension(220, 45));
        txtBusca.putClientProperty("JTextField.placeholderText", "Pesquisar paciente...");
        txtBusca.setFont(new Font("Segoe UI", Font.PLAIN, 15));

        JButton btnBuscar = new JButton("🔍");
        btnBuscar.setPreferredSize(new Dimension(50, 45));
        btnBuscar.setBackground(Color.WHITE);
        btnBuscar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JButton btnNovo = new JButton("+ Agendar / Registrar");
        btnNovo.setBackground(Cores.VERDE_AQUA);
        btnNovo.setForeground(Color.WHITE);
        btnNovo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnNovo.setPreferredSize(new Dimension(190, 45));
        btnNovo.addActionListener(e -> frame.trocarTelaCentral(new PainelFormulario(frame)));

        controles.add(txtBusca);
        controles.add(btnBuscar);
        controles.add(btnNovo);
        header.add(controles, BorderLayout.EAST);
        cardVidro.add(header, BorderLayout.NORTH);

        JPanel painelAbasWrapper = new JPanel(new BorderLayout());
        painelAbasWrapper.setOpaque(false);

        JPanel painelAbas = new JPanel(new FlowLayout(FlowLayout.LEFT, 30, 0));
        painelAbas.setOpaque(false);
        painelAbas.setBorder(new EmptyBorder(0, 0, 15, 0));

        btnAbaRecentes = criarBotaoAba("Recentes", true);
        btnAbaHoje = criarBotaoAba("Hoje", false);
        btnAbaProximas = criarBotaoAba("Próximas", false);

        painelAbas.add(btnAbaRecentes);
        painelAbas.add(btnAbaHoje);
        painelAbas.add(btnAbaProximas);
        painelAbasWrapper.add(painelAbas, BorderLayout.NORTH);

        String[] colunas = {"DATA", "PACIENTE", "VACINA", "STATUS", "TOTAL"};
        modeloTabela = new DefaultTableModel(new Object[][]{}, colunas) {
            public boolean isCellEditable(int row, int column) { return false; }
        };

        tabela = new JTable(modeloTabela);
        tabela.setRowHeight(50);
        tabela.setShowVerticalLines(false);
        tabela.setShowHorizontalLines(false);
        tabela.setIntercellSpacing(new Dimension(0, 6));
        tabela.setFont(new Font("Segoe UI", Font.PLAIN, 15));

        tabela.setDefaultRenderer(Object.class, new CustomTableRenderer());

        JPopupMenu menuContexto = criarMenuOpcoesTabela();

        tabela.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = tabela.rowAtPoint(e.getPoint());
                if (hoveredRow != row) {
                    hoveredRow = row;
                    tabela.repaint();
                }
            }
        });

        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoveredRow = -1;
                tabela.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int linhaClicada = tabela.rowAtPoint(e.getPoint());
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && linhaClicada != -1) {
                    // Pega a aplicação real da lista filtrada
                    Aplicacao app = aplicacoesFiltradas.get(linhaClicada);
                    abrirModalDetalhes(app);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int linhaClicada = tabela.rowAtPoint(e.getPoint());
                    if (linhaClicada >= 0) {
                        tabela.setRowSelectionInterval(linhaClicada, linhaClicada);
                        menuContexto.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tabela);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setBackground(Color.WHITE);

        painelAbasWrapper.add(scroll, BorderLayout.CENTER);
        cardVidro.add(painelAbasWrapper, BorderLayout.CENTER);

        add(cardVidro, BorderLayout.CENTER);
    }

    private JButton criarBotaoAba(String titulo, boolean ativoInicial) {
        JButton btn = new JButton(titulo);
        btn.setFont(new Font("Segoe UI", ativoInicial ? Font.BOLD : Font.PLAIN, 18));
        btn.setForeground(ativoInicial ? Cores.VERDE_AQUA : Cores.CINZA_LABEL);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(5, 0, 5, 0));

        btn.addActionListener(e -> {
            abaAtiva = titulo;
            btnAbaRecentes.setFont(new Font("Segoe UI", Font.PLAIN, 18)); btnAbaRecentes.setForeground(Cores.CINZA_LABEL);
            btnAbaHoje.setFont(new Font("Segoe UI", Font.PLAIN, 18)); btnAbaHoje.setForeground(Cores.CINZA_LABEL);
            btnAbaProximas.setFont(new Font("Segoe UI", Font.PLAIN, 18)); btnAbaProximas.setForeground(Cores.CINZA_LABEL);

            btn.setFont(new Font("Segoe UI", Font.BOLD, 18));
            btn.setForeground(Cores.VERDE_AQUA);

            atualizarDadosTabela();
        });
        return btn;
    }

    private void atualizarDadosTabela() {
        modeloTabela.setRowCount(0);
        LocalDate hoje = LocalDate.now();
        aplicacoesFiltradas = new ArrayList<>();

        for (Aplicacao a : todasAplicacoes) {
            try {
                String dataStr = a.getDataHora().length() >= 10 ? a.getDataHora().substring(0, 10) : a.getDataHora();
                LocalDate dataApp = LocalDate.parse(dataStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                if (abaAtiva.equals("Recentes") && a.getStatus().equalsIgnoreCase("Aplicado")) {
                    aplicacoesFiltradas.add(a);
                } else if (abaAtiva.equals("Hoje") && a.getStatus().equalsIgnoreCase("Agendado") && dataApp.equals(hoje)) {
                    aplicacoesFiltradas.add(a);
                } else if (abaAtiva.equals("Próximas") && a.getStatus().equalsIgnoreCase("Agendado") && dataApp.isAfter(hoje)) {
                    aplicacoesFiltradas.add(a);
                }
            } catch (Exception e) {
                // Se a data for inválida, joga nas recentes pra não perder o dado
                if (abaAtiva.equals("Recentes")) aplicacoesFiltradas.add(a);
            }
        }

        // Ordenar as "Próximas" pela data mais perto de hoje
        if (abaAtiva.equals("Próximas")) {
            aplicacoesFiltradas.sort((a1, a2) -> {
                try {
                    LocalDate d1 = LocalDate.parse(a1.getDataHora().substring(0, 10), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    LocalDate d2 = LocalDate.parse(a2.getDataHora().substring(0, 10), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    return d1.compareTo(d2);
                } catch (Exception e) { return 0; }
            });
        }

        for (Aplicacao a : aplicacoesFiltradas) {
            String valorMonetario = String.format("R$ %.2f", a.getValor());
            modeloTabela.addRow(new Object[]{a.getDataHora(), a.getPaciente(), a.getVacina(), a.getStatus(), valorMonetario});
        }
        hoveredRow = -1;
    }

    private class CustomTableRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBorder(new EmptyBorder(0, 15, 0, 15));

            if (isSelected) {
                c.setBackground(Cores.VERDE_AQUA);
                c.setForeground(Color.WHITE);
            } else if (row == hoveredRow) {
                c.setBackground(new Color(210, 235, 235));
                c.setForeground(Cores.CINZA_GRAFITE);
            } else {
                c.setBackground(Color.WHITE);
                c.setForeground(Cores.CINZA_GRAFITE);
            }
            return c;
        }
    }

    private JPanel criarCardLembrete() {
        GlassPanel card = new GlassPanel() {
            boolean isHovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); setCursor(new Cursor(Cursor.HAND_CURSOR)); }
                    @Override
                    public void mouseExited(MouseEvent e) { isHovered = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (isHovered) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(255, 255, 255, 150));
                    g2.fillRoundRect(0, 0, getWidth() - 12, getHeight() - 12, 25, 25);
                    g2.setColor(Cores.ROSA_KAROL);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRoundRect(0, 0, getWidth() - 12, getHeight() - 12, 25, 25);
                    g2.dispose();
                }
            }
        };
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(20, 25, 20, 25));

        JLabel lblTitulo = new JLabel("Lembrete Rápido");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitulo.setForeground(Cores.CINZA_LABEL);

        JLabel lblValor = new JLabel("<html><i>+ Adicionar nota</i></html>");
        lblValor.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblValor.setForeground(Cores.ROSA_KAROL);

        JPanel pnl = new JPanel(new BorderLayout(0, 5));
        pnl.setOpaque(false);
        pnl.add(lblTitulo, BorderLayout.NORTH);
        pnl.add(lblValor, BorderLayout.CENTER);

        card.add(pnl, BorderLayout.CENTER);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String texto = JOptionPane.showInputDialog(frame, "Digite o lembrete para hoje:", "Novo Lembrete", JOptionPane.PLAIN_MESSAGE);
                if (texto != null && !texto.trim().isEmpty()) {
                    lblValor.setText("<html><div style='width: 200px; font-weight: bold; font-size: 15px;'>" + texto + "</div></html>");
                    lblValor.setForeground(Cores.CINZA_GRAFITE);
                } else if (texto != null && texto.trim().isEmpty()) {
                    lblValor.setText("<html><i>+ Adicionar nota</i></html>");
                    lblValor.setForeground(Cores.ROSA_KAROL);
                }
            }
        });
        return card;
    }

    private JPanel criarCardResumo(String titulo, String valor, Color corDestaque) {
        GlassPanel card = new GlassPanel() {
            boolean isHovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); setCursor(new Cursor(Cursor.HAND_CURSOR)); }
                    @Override
                    public void mouseExited(MouseEvent e) { isHovered = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (isHovered) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(255, 255, 255, 150));
                    g2.fillRoundRect(0, 0, getWidth() - 12, getHeight() - 12, 25, 25);
                    g2.setColor(corDestaque);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRoundRect(0, 0, getWidth() - 12, getHeight() - 12, 25, 25);
                    g2.dispose();
                }
            }
        };
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(25, 30, 25, 30));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitulo.setForeground(Cores.CINZA_LABEL);

        JLabel lblValor = new JLabel(valor);
        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblValor.setForeground(corDestaque);

        JPanel pnl = new JPanel(new GridLayout(2, 1, 0, 5));
        pnl.setOpaque(false);
        pnl.add(lblTitulo);
        pnl.add(lblValor);

        card.add(pnl, BorderLayout.CENTER);
        return card;
    }

    // --- AGORA AS AÇÕES DO BOTÃO DIREITO REALMENTE AFETAM O BANCO ---
    private JPopupMenu criarMenuOpcoesTabela() {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210), 1),
                new EmptyBorder(8, 0, 8, 0)
        ));

        JMenuItem itemAplicar = criarItemMenu("✅  Marcar como Aplicada", Cores.VERDE_AQUA, Cores.VERDE_AQUA);
        itemAplicar.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JMenuItem itemDetalhes = criarItemMenu("🔍  Ver ou Alterar Detalhes", Cores.CINZA_GRAFITE, Cores.VERDE_AQUA);
        JMenuItem itemWhatsApp = criarItemMenu("📲  Enviar via WhatsApp", Cores.CINZA_GRAFITE, Cores.VERDE_AQUA);
        JMenuItem itemExcluir  = criarItemMenu("🗑️  Excluir Registro", new Color(220, 53, 69), new Color(190, 30, 45));

        // Marcar como Aplicado (Conecta com DAO)
        itemAplicar.addActionListener(e -> {
            int linha = tabela.getSelectedRow();
            if (linha >= 0) {
                Aplicacao app = aplicacoesFiltradas.get(linha);
                dao.atualizarStatus(app.getId(), "Aplicado");
                JOptionPane.showMessageDialog(this, "Status de " + app.getPaciente() + " alterado para APLICADO!");
                frame.trocarTelaCentral(new PainelDashboard(frame)); // Recarrega tudo (tabela e cards)
            }
        });

        // Ver Detalhes (Puxa objeto real)
        itemDetalhes.addActionListener(e -> {
            int linha = tabela.getSelectedRow();
            if(linha >= 0) abrirModalDetalhes(aplicacoesFiltradas.get(linha));
        });

        // Excluir (Conecta com DAO)
        itemExcluir.addActionListener(e -> {
            int linha = tabela.getSelectedRow();
            if (linha >= 0) {
                Aplicacao app = aplicacoesFiltradas.get(linha);
                int confirm = JOptionPane.showConfirmDialog(this, "Deseja excluir permanentemente o registro de " + app.getPaciente() + "?", "Atenção", JOptionPane.YES_NO_OPTION);
                if(confirm == JOptionPane.YES_OPTION) {
                    dao.excluir(app.getId());
                    frame.trocarTelaCentral(new PainelDashboard(frame)); // Recarrega tudo
                }
            }
        });

        popup.add(itemAplicar);
        popup.addSeparator();
        popup.add(itemDetalhes);
        popup.add(itemWhatsApp);
        popup.addSeparator();
        popup.add(itemExcluir);

        return popup;
    }

    private JMenuItem criarItemMenu(String texto, Color corTextoNormal, Color corTextoHover) {
        JMenuItem item = new JMenuItem(texto);
        item.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        item.setBorder(new EmptyBorder(8, 15, 8, 25));
        item.setCursor(new Cursor(Cursor.HAND_CURSOR));

        item.setForeground(corTextoNormal);
        item.setBackground(Color.WHITE);

        item.addChangeListener(e -> {
            if (item.isArmed()) {
                item.setForeground(corTextoHover);
                item.setBackground(new Color(242, 245, 248));
            } else {
                item.setForeground(corTextoNormal);
                item.setBackground(Color.WHITE);
            }
        });
        return item;
    }

    private void abrirModalDetalhes(Aplicacao app) {
        JDialog dialog = new JDialog(frame, "Detalhes da Aplicação", true);
        dialog.setSize(550, 650);
        dialog.setLocationRelativeTo(frame);

        JPanel pnlBase = new JPanel(new BorderLayout());
        pnlBase.setBackground(Color.WHITE);
        pnlBase.setBorder(new EmptyBorder(30, 40, 30, 40));

        JLabel lblTitulo = new JLabel("Editar Registro");
        lblTitulo.setFont(new Font("Segoe UI Semilight", Font.PLAIN, 28));
        lblTitulo.setForeground(Cores.VERDE_AQUA);
        pnlBase.add(lblTitulo, BorderLayout.NORTH);

        JPanel pnlCampos = new JPanel(new GridLayout(5, 1, 0, 15));
        pnlCampos.setOpaque(false);
        pnlCampos.setBorder(new EmptyBorder(20, 0, 20, 0));

        pnlCampos.add(criarBlocoCampoModal("PACIENTE:", app.getPaciente()));
        pnlCampos.add(criarBlocoCampoModal("VACINA APLICADA:", app.getVacina()));
        pnlCampos.add(criarBlocoCampoModal("DATA / HORA:", app.getDataHora()));

        JPanel pStatus = new JPanel(new BorderLayout(0, 5));
        pStatus.setOpaque(false);
        JLabel lStatus = new JLabel("STATUS:");
        lStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lStatus.setForeground(Cores.CINZA_LABEL);
        JComboBox<String> cbStatus = new JComboBox<>(new String[]{"Agendado", "Aplicado"});
        cbStatus.setSelectedItem(app.getStatus());
        cbStatus.setPreferredSize(new Dimension(0, 45));
        cbStatus.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        cbStatus.setBackground(Color.WHITE);
        pStatus.add(lStatus, BorderLayout.NORTH);
        pStatus.add(cbStatus, BorderLayout.CENTER);
        pnlCampos.add(pStatus);

        pnlCampos.add(criarBlocoCampoModal("VALOR (R$):", String.format("%.2f", app.getValor())));

        pnlBase.add(pnlCampos, BorderLayout.CENTER);

        JPanel pnlBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        pnlBotoes.setOpaque(false);

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCancelar.setBackground(new Color(230, 230, 230));
        btnCancelar.setPreferredSize(new Dimension(120, 45));
        btnCancelar.addActionListener(e -> dialog.dispose());

        JButton btnSalvar = new JButton("Salvar Alterações");
        btnSalvar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSalvar.setForeground(Color.WHITE);
        btnSalvar.setBackground(Cores.VERDE_AQUA);
        btnSalvar.setPreferredSize(new Dimension(160, 45));
        btnSalvar.addActionListener(e -> {
            JOptionPane.showMessageDialog(dialog, "Atualização disponível na próxima versão!");
            dialog.dispose();
        });

        pnlBotoes.add(btnCancelar);
        pnlBotoes.add(btnSalvar);
        pnlBase.add(pnlBotoes, BorderLayout.SOUTH);

        dialog.add(pnlBase);
        dialog.setVisible(true);
    }

    private JPanel criarBlocoCampoModal(String label, String valor) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(Cores.CINZA_LABEL);

        JTextField t = new JTextField(valor);
        t.setPreferredSize(new Dimension(0, 45));
        t.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        p.add(l, BorderLayout.NORTH);
        p.add(t, BorderLayout.CENTER);
        return p;
    }
}