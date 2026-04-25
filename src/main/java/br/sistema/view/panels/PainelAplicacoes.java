package br.sistema.view.panels;

import br.sistema.model.Aplicacao;
import br.sistema.model.Paciente;
import br.sistema.repository.AplicacaoDAO;
import br.sistema.repository.PacienteDAO;
import br.sistema.util.Cores;
import br.sistema.view.TelaPrincipal;
import br.sistema.view.components.GlassPanel;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PainelAplicacoes extends JPanel {
    private TelaPrincipal frame;
    private JTable tabela;
    private DefaultTableModel modeloTabela;
    private AplicacaoDAO dao;
    private List<Aplicacao> listaFiltrada;
    private int hoveredRow = -1;
    private String abaAtual = "HOJE";

    private JButton btnNavHoje, btnNavRecentes, btnNavProximas, btnNavObs;
    private JButton btnMarcarAplicada;
    private JTextField txtBusca; // CAMPO DE BUSCA

    public PainelAplicacoes(TelaPrincipal frame) {
        this.frame = frame;
        this.dao = new AplicacaoDAO();
        setOpaque(false);
        setLayout(new BorderLayout(0, 20));
        setBorder(new EmptyBorder(15, 20, 15, 20)); // Margem reduzida para caber em telas menores

        GlassPanel cardVidro = new GlassPanel();
        cardVidro.setLayout(new BorderLayout(0, 15));
        cardVidro.setBorder(new EmptyBorder(15, 20, 15, 20)); // Margem reduzida do card

        // HEADER
        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setOpaque(false);
        JLabel titulo = new JLabel("Controle de Aplicações");
        titulo.setFont(new Font("Segoe UI Semilight", Font.PLAIN, 32));
        titulo.setForeground(Cores.CINZA_GRAFITE);
        header.add(titulo, BorderLayout.WEST);

        JButton btnNovo = new JButton(" Nova Aplicação");
        btnNovo.setIcon(carregarIcone("adicionar.svg", 18, Color.WHITE));
        btnNovo.setBackground(Cores.VERDE_AQUA);
        btnNovo.setForeground(Color.WHITE);
        btnNovo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnNovo.setPreferredSize(new Dimension(150, 40)); // Reduzido para melhor responsividade
        btnNovo.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnNovo.addActionListener(e -> frame.trocarTelaCentral(new PainelFormulario(frame)));
        header.add(btnNovo, BorderLayout.EAST);
        cardVidro.add(header, BorderLayout.NORTH);

        // =========================================================
        // NAVBAR E BARRA DE BUSCA
        // =========================================================
        JPanel pnlCentro = new JPanel(new BorderLayout(0, 20));
        pnlCentro.setOpaque(false);

        JPanel pnlFiltros = new JPanel(new BorderLayout());
        pnlFiltros.setOpaque(false);
        pnlFiltros.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 210, 210)));

        JPanel pnlNavbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 40, 0));
        pnlNavbar.setOpaque(false);

        btnNavHoje = criarBotaoNav("Hoje", true);
        btnNavRecentes = criarBotaoNav("Recentes", false);
        btnNavProximas = criarBotaoNav("Próximas", false);
        btnNavObs = criarBotaoNav("Observações", false);

        btnNavHoje.addActionListener(e -> trocarAba("HOJE", btnNavHoje));
        btnNavRecentes.addActionListener(e -> trocarAba("RECENTES", btnNavRecentes));
        btnNavProximas.addActionListener(e -> trocarAba("PROXIMAS", btnNavProximas));
        btnNavObs.addActionListener(e -> trocarAba("OBSERVACOES", btnNavObs));

        pnlNavbar.add(btnNavHoje);
        pnlNavbar.add(btnNavRecentes);
        pnlNavbar.add(btnNavProximas);
        pnlNavbar.add(btnNavObs);

        // O novo Campo de Busca
        JPanel pnlBusca = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 5));
        pnlBusca.setOpaque(false);
        txtBusca = new JTextField();
        txtBusca.setPreferredSize(new Dimension(200, 40)); // Reduzido para não empurrar a navbar

        // Texto sem o emoji
        txtBusca.putClientProperty("JTextField.placeholderText", "Buscar paciente ou vacina...");

        // MÁGICA DO FLATLAF: Coloca o ícone SVG dentro do Input, no lado esquerdo (leading)
        txtBusca.putClientProperty("JTextField.leadingIcon", carregarIcone("procurar.svg", 18, Cores.CINZA_LABEL));

        txtBusca.setFont(new Font("Segoe UI", Font.PLAIN, 15));

        txtBusca.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                carregarDadosTabela();
            }
        });

        pnlBusca.add(txtBusca);

        pnlFiltros.add(pnlNavbar, BorderLayout.WEST);
        pnlFiltros.add(pnlBusca, BorderLayout.EAST);

        pnlCentro.add(pnlFiltros, BorderLayout.NORTH);

        // =========================================================
        // TABELA
        // =========================================================
        modeloTabela = new DefaultTableModel(new Object[][]{}, new String[]{"ID", "Paciente", "Vacina", "Data/Hora", "Status"}) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tabela = new JTable(modeloTabela);
        tabela.setRowHeight(52);
        tabela.setShowGrid(false);
        tabela.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        tabela.setDefaultRenderer(Object.class, new CustomTableRenderer());

        tabela.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int l = tabela.rowAtPoint(e.getPoint());
                    if (l >= 0) {
                        tabela.setRowSelectionInterval(l, l);
                        Aplicacao app = getAplicacaoSelecionada();
                        criarMenuOpcoesPadrao(app).show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tabela);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);

        pnlCentro.add(scroll, BorderLayout.CENTER);

        // =========================================================
        // BOTÃO MARCAR COMO APLICADA
        // =========================================================
        JPanel pnlAcoesTabela = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlAcoesTabela.setOpaque(false);

        btnMarcarAplicada = new JButton("Marcar como Aplicada");
        btnMarcarAplicada.setBackground(Cores.VERDE_AQUA);
        btnMarcarAplicada.setForeground(Color.WHITE);
        btnMarcarAplicada.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnMarcarAplicada.setPreferredSize(new Dimension(180, 40)); // Reduzido para melhorar layout
        btnMarcarAplicada.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnMarcarAplicada.addActionListener(e -> marcarComoAplicada());

        pnlAcoesTabela.add(btnMarcarAplicada);
        pnlCentro.add(pnlAcoesTabela, BorderLayout.SOUTH);

        cardVidro.add(pnlCentro, BorderLayout.CENTER);
        add(cardVidro, BorderLayout.CENTER);

        carregarDadosTabela();
    }

    private void marcarComoAplicada() {
        Aplicacao app = getAplicacaoSelecionada();

        if (app == null) {
            JOptionPane.showMessageDialog(frame, "Por favor, selecione uma aplicação na tabela primeiro.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if ("Aplicado".equals(app.getStatus()) || "Concluído".equals(app.getStatus())) {
            JOptionPane.showMessageDialog(frame, "Esta aplicação já foi marcada como aplicada!", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String[] opcoesPagamento = {"PIX", "Cartão de Crédito", "Cartão de Débito", "Dinheiro"};

        String formaPagamento = (String) JOptionPane.showInputDialog(
                frame,
                "Aplicação realizada!\nQual será a forma de pagamento?",
                "Finalizar Aplicação",
                JOptionPane.QUESTION_MESSAGE,
                null,
                opcoesPagamento,
                opcoesPagamento[0]
        );

        if (formaPagamento != null) {
            boolean sucesso = dao.atualizarStatusEPagamento(app.getId(), "Aplicado", formaPagamento);

            if (sucesso) {
                carregarDadosTabela();
                JOptionPane.showMessageDialog(frame, "Aplicação finalizada com sucesso!\nPagamento via: " + formaPagamento, "Concluído", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(frame, "Erro ao salvar no banco de dados.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void carregarDadosTabela() {
        modeloTabela.setRowCount(0);
        List<Aplicacao> todas = dao.listarTodas();
        listaFiltrada = new ArrayList<>();

        LocalDate hoje = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        String termoBusca = txtBusca != null ? txtBusca.getText().toLowerCase().trim() : "";

        for (Aplicacao app : todas) {
            LocalDate dataApp = app.getDataHora().toLocalDate();
            String statusDB = app.getStatus() != null ? app.getStatus().trim() : "";

            // A MÁGICA DA CORREÇÃO AQUI:
            // Só considera como "Concluído" se a etiqueta existir E o status for realmente "Aplicado"
            boolean isConcluido = app.getObservacoesAdicionais() != null
                    && app.getObservacoesAdicionais().startsWith("[CONCLUÍDO]")
                    && statusDB.equalsIgnoreCase("Aplicado");

            // Verifica a busca
            boolean atendeBusca = termoBusca.isEmpty() ||
                    app.getPaciente().getNome().toLowerCase().contains(termoBusca) ||
                    app.getVacina().getNomeVacina().toLowerCase().contains(termoBusca);

            if (!atendeBusca) continue;

            // Filtros das abas
            if (abaAtual.equals("HOJE")) {
                if (dataApp.equals(hoje)) listaFiltrada.add(app);
            }
            else if (abaAtual.equals("RECENTES")) {
                if (statusDB.equalsIgnoreCase("Aplicado") || statusDB.equalsIgnoreCase("Concluído")) {
                    listaFiltrada.add(app);
                }
            }
            else if (abaAtual.equals("PROXIMAS")) {
                if (statusDB.equalsIgnoreCase("Agendado") && dataApp.isAfter(hoje)) {
                    listaFiltrada.add(app);
                }
            }
            else if (abaAtual.equals("OBSERVACOES")) {
                if (statusDB.equalsIgnoreCase("Aplicado") && !isConcluido) {
                    listaFiltrada.add(app);
                }
            }
        }

        // Ordenação
        if (abaAtual.equals("HOJE")) {
            listaFiltrada.sort((a1, a2) -> {
                boolean ag1 = a1.getStatus().equalsIgnoreCase("Agendado");
                boolean ag2 = a2.getStatus().equalsIgnoreCase("Agendado");
                if (ag1 && !ag2) return -1;
                if (!ag1 && ag2) return 1;
                return a1.getDataHora().compareTo(a2.getDataHora());
            });
        } else if (abaAtual.equals("RECENTES") || abaAtual.equals("OBSERVACOES")) {
            listaFiltrada.sort((a1, a2) -> a2.getDataHora().compareTo(a1.getDataHora()));
        } else if (abaAtual.equals("PROXIMAS")) {
            listaFiltrada.sort((a1, a2) -> a1.getDataHora().compareTo(a2.getDataHora()));
        }

        // Desenhar a Tabela
        for (Aplicacao p : listaFiltrada) {
            String stDisplay = p.getStatus();

            // REPETE A VERIFICAÇÃO AQUI PARA A TELA NÃO SE CONFUNDIR
            boolean isConcluido = p.getObservacoesAdicionais() != null
                    && p.getObservacoesAdicionais().startsWith("[CONCLUÍDO]")
                    && stDisplay.equalsIgnoreCase("Aplicado");

            if (isConcluido) {
                stDisplay = "Concluído";
            } else if (abaAtual.equals("OBSERVACOES")) {
                stDisplay = "Em Acompanhamento";
            }

            modeloTabela.addRow(new Object[]{
                    String.format("%03d", p.getId()),
                    p.getPaciente().getNome(),
                    p.getVacina().getNomeVacina(),
                    p.getDataHora().format(fmt),
                    stDisplay
            });
        }
    }

    private JPopupMenu criarMenuOpcoesPadrao(Aplicacao app) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(new LineBorder(new Color(230, 235, 240)));

        JMenuItem itemWhats = criarItemMenu("Enviar Mensagem no WhatsApp", "aplicativos.svg", new Color(37, 211, 102));
        JMenuItem itemRecibo = criarItemMenu("Emitir Comprovante", "imprimir.svg", Cores.VERDE_AQUA);
        JMenuItem itemEditar = criarItemMenu("Alterar Aplicação", "lapis-de-blog.svg", Cores.CINZA_GRAFITE);
        JMenuItem itemExcluir = criarItemMenu("Excluir Registro", "trash.svg", new Color(220, 53, 69));

        boolean jaTemObs = app.getObservacoesAdicionais() != null && app.getObservacoesAdicionais().startsWith("[CONCLUÍDO]");
        String textoObs = jaTemObs ? "Ver / Alterar Observações" : "Registrar Observações";
        JMenuItem itemObs = criarItemMenu(textoObs, "lapis-de-blog.svg", Cores.ROSA_KAROL);

        itemWhats.addActionListener(e -> abrirWhatsAppPaciente());
        itemRecibo.addActionListener(e -> gerarEImprimirComprovante(app));
        itemObs.addActionListener(e -> abrirModalObservacaoPremium(app));

        itemEditar.addActionListener(e -> {
            if (app != null) frame.trocarTelaCentral(new PainelFormulario(frame, app));
        });

        itemExcluir.addActionListener(e -> {
            if (app != null) {
                int opt = JOptionPane.showConfirmDialog(frame, "Atenção: Tem certeza que deseja excluir esta aplicação?", "Confirmar Exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (opt == JOptionPane.YES_OPTION) {
                    dao.excluir(app.getId());
                    carregarDadosTabela();
                }
            }
        });

        popup.add(itemWhats); popup.add(new JSeparator());

        if (app.getStatus().equals("Aplicado") || app.getStatus().equals("Concluído")) {
            popup.add(itemObs); popup.add(new JSeparator());
        }

        popup.add(itemRecibo); popup.add(new JSeparator());
        popup.add(itemEditar); popup.add(new JSeparator());
        popup.add(itemExcluir);
        return popup;
    }

    // =========================================================
    // WHATSAPP FIX (Forçando a busca completa do paciente)
    // =========================================================
    private void abrirWhatsAppPaciente() {
        Aplicacao app = getAplicacaoSelecionada();
        if (app != null && app.getPaciente() != null) {

            // Vai no banco buscar a ficha completa do paciente para garantir que o telefone venha junto
            String telefoneSeguro = null;
            PacienteDAO pDao = new PacienteDAO();
            for (Paciente p : pDao.listarTodos()) {
                if (p.getId() == app.getPaciente().getId()) {
                    telefoneSeguro = p.getTelefone();
                    break;
                }
            }

            // Fallback: se não achar na lista, usa o que já estava na memória
            if (telefoneSeguro == null) {
                telefoneSeguro = app.getPaciente().getTelefone();
            }

            if (telefoneSeguro != null && !telefoneSeguro.trim().isEmpty() && !telefoneSeguro.equals("(  )      -    ")) {
                String fone = telefoneSeguro.replaceAll("[^0-9]", "");

                if (fone.length() >= 10) {
                    try {
                        String url = "https://api.whatsapp.com/send?phone=55" + fone;
                        if (Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().browse(new java.net.URI(url));
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(frame, "Erro ao abrir o navegador. Verifique se há um navegador padrão definido no Windows.", "Erro", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(frame, "O telefone cadastrado (" + telefoneSeguro + ") é inválido. Atualize o cadastro do paciente.", "Aviso", JOptionPane.WARNING_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Nenhum telefone cadastrado para este paciente.", "Aviso", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void abrirModalObservacaoPremium(Aplicacao app) {
        JDialog diag = new JDialog(frame, "Prontuário de Observação", true);
        diag.setSize(550, 480);
        diag.setLocationRelativeTo(frame);

        JPanel p = new JPanel(new BorderLayout(0, 20));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(25, 35, 25, 35));

        JLabel t = new JLabel("Observações da Aplicação");
        t.setFont(new Font("Segoe UI", Font.BOLD, 22));
        t.setForeground(new Color(30, 102, 105));
        p.add(t, BorderLayout.NORTH);

        JPanel pnlAgrupador = new JPanel(new BorderLayout());
        pnlAgrupador.setOpaque(false);

        JPanel centro = new JPanel(new GridLayout(2, 1, 0, 15));
        centro.setOpaque(false);

        JCheckBox chkSemReacoes = new JCheckBox("Sem reações relatadas");
        chkSemReacoes.setOpaque(false);
        chkSemReacoes.setCursor(new Cursor(Cursor.HAND_CURSOR));

        String textoReacaoAtual = app.getReacoes() != null ? app.getReacoes() : "";
        boolean isSemReacao = textoReacaoAtual.equals("Nenhuma reação relatada.");

        JTextArea t1 = new JTextArea(isSemReacao ? "" : textoReacaoAtual);
        t1.setRows(4);
        t1.setLineWrap(true);
        t1.setWrapStyleWord(true);
        t1.setEnabled(!isSemReacao);
        chkSemReacoes.setSelected(isSemReacao);

        chkSemReacoes.addActionListener(e -> {
            t1.setEnabled(!chkSemReacoes.isSelected());
            if (chkSemReacoes.isSelected()) t1.setText("");
        });

        centro.add(montarBlocoObs("REAÇÕES ADVERSAS / SINTOMAS", t1, Cores.ROSA_KAROL, chkSemReacoes));

        JCheckBox chkSemObs = new JCheckBox("Sem observações");
        chkSemObs.setOpaque(false);
        chkSemObs.setCursor(new Cursor(Cursor.HAND_CURSOR));

        String obsLimpa = app.getObservacoesAdicionais() != null ? app.getObservacoesAdicionais().replace("[CONCLUÍDO]", "").trim() : "";
        boolean isSemObs = obsLimpa.equals("Nenhuma observação geral.");

        JTextArea t2 = new JTextArea(isSemObs ? "" : obsLimpa);
        t2.setRows(4);
        t2.setLineWrap(true);
        t2.setWrapStyleWord(true);
        t2.setEnabled(!isSemObs);
        chkSemObs.setSelected(isSemObs);

        chkSemObs.addActionListener(e -> {
            t2.setEnabled(!chkSemObs.isSelected());
            if (chkSemObs.isSelected()) t2.setText("");
        });

        centro.add(montarBlocoObs("OBSERVAÇÕES GERAIS (ENFERMAGEM)", t2, Cores.CINZA_LABEL, chkSemObs));

        pnlAgrupador.add(centro, BorderLayout.NORTH);
        p.add(pnlAgrupador, BorderLayout.CENTER);

        JButton btn = new JButton("Salvar Prontuário");
        btn.setBackground(Cores.VERDE_AQUA);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btn.setPreferredSize(new Dimension(0, 50));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addActionListener(e -> {
            if (!chkSemReacoes.isSelected() && t1.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(diag, "Por favor, descreva as reações do paciente ou marque a caixa 'Sem reações relatadas'.", "Campo Obrigatório", JOptionPane.WARNING_MESSAGE);
                t1.requestFocus();
                return;
            }
            if (!chkSemObs.isSelected() && t2.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(diag, "Por favor, preencha as observações gerais ou marque a caixa 'Sem observações'.", "Campo Obrigatório", JOptionPane.WARNING_MESSAGE);
                t2.requestFocus();
                return;
            }

            String textoFinalReacao = chkSemReacoes.isSelected() ? "Nenhuma reação relatada." : t1.getText().trim();
            String textoFinalObs = chkSemObs.isSelected() ? "Nenhuma observação geral." : t2.getText().trim();

            app.setReacoes(textoFinalReacao);
            app.setObservacoesAdicionais("[CONCLUÍDO] " + textoFinalObs);

            dao.atualizar(app);
            diag.dispose();
            carregarDadosTabela();
            JOptionPane.showMessageDialog(frame, "Observações registradas com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        });

        p.add(btn, BorderLayout.SOUTH);
        diag.add(p);
        diag.setVisible(true);
    }

    private JPanel montarBlocoObs(String tit, JTextArea area, Color cor, JCheckBox checkbox) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);

        JPanel pnlTopo = new JPanel(new BorderLayout());
        pnlTopo.setOpaque(false);
        JLabel l = new JLabel(tit);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(cor);

        pnlTopo.add(l, BorderLayout.WEST);
        pnlTopo.add(checkbox, BorderLayout.EAST);

        area.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(230,230,230)), new EmptyBorder(10,10,10,10)));
        p.add(pnlTopo, BorderLayout.NORTH);
        p.add(new JScrollPane(area), BorderLayout.CENTER);
        return p;
    }

    private Aplicacao getAplicacaoSelecionada() {
        int l = tabela.getSelectedRow();
        if (l >= 0) {
            int id = Integer.parseInt(tabela.getValueAt(l, 0).toString());
            for (Aplicacao a : listaFiltrada) {
                if (a.getId() == id) return a;
            }
        }
        return null;
    }

    private class CustomTableRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            super.getTableCellRendererComponent(t, v, s, f, r, c);
            setBorder(new EmptyBorder(0, 15, 0, 15));
            if (s) {
                setBackground(Cores.VERDE_AQUA);
                setForeground(Color.WHITE);
            } else {
                setBackground(Color.WHITE);
                if (c == 4) {
                    String val = v.toString();
                    if (val.equals("Agendado")) setForeground(new Color(230, 126, 34));
                    else if (val.equals("Aplicado")) setForeground(Cores.VERDE_AQUA);
                    else if (val.equals("Concluído")) setForeground(Cores.ROSA_KAROL);
                    else if (val.equals("Em Acompanhamento")) setForeground(new Color(230, 126, 34));
                } else {
                    setForeground(Cores.CINZA_GRAFITE);
                }
            }
            return this;
        }
    }

    private JButton criarBotaoNav(String t, boolean a) {
        JButton b = new JButton(t);
        b.setFont(new Font("Segoe UI", a ? Font.BOLD : Font.PLAIN, 16));
        b.setForeground(a ? Cores.VERDE_AQUA : Cores.CINZA_LABEL);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void trocarAba(String a, JButton b) {
        abaAtual = a;
        JButton[] btns = {btnNavHoje, btnNavRecentes, btnNavProximas, btnNavObs};
        for (JButton x : btns) {
            x.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            x.setForeground(Cores.CINZA_LABEL);
        }
        b.setFont(new Font("Segoe UI", Font.BOLD, 16));
        b.setForeground(Cores.VERDE_AQUA);
        carregarDadosTabela();
    }

    private JMenuItem criarItemMenu(String t, String i, Color c) {
        JMenuItem it = new JMenuItem(t);
        it.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        it.setBorder(new EmptyBorder(10, 20, 10, 30));
        it.setBackground(Color.WHITE);
        it.setOpaque(true);
        it.putClientProperty("FlatLaf.style", "selectionBackground: fade(" + String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue()) + ", 12%); selectionForeground: " + String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue()));
        try {
            FlatSVGIcon icon = carregarIcone(i, 18, Cores.CINZA_GRAFITE);
            it.setIcon(icon);
            it.setIconTextGap(15);
            it.addChangeListener(ev -> {
                if (it.isArmed()) icon.setColorFilter(new FlatSVGIcon.ColorFilter(x -> c));
                else icon.setColorFilter(new FlatSVGIcon.ColorFilter(x -> Cores.CINZA_GRAFITE));
            });
        } catch (Exception ignored) {}
        return it;
    }

    private FlatSVGIcon carregarIcone(String n, int t, Color c) {
        try {
            return (FlatSVGIcon) new FlatSVGIcon("icons/" + n, t, t).setColorFilter(new FlatSVGIcon.ColorFilter(x -> c));
        } catch(Exception e) {
            return null;
        }
    }

    private void gerarEImprimirComprovante(Aplicacao app) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: sans-serif; text-align: center; padding: 20px;'>");
        html.append("<h2 style='color: #1E6669;'>Comprovante de Vacinação</h2>");
        html.append("<p><b>Paciente:</b> ").append(app.getPaciente().getNome()).append("</p>");
        html.append("<p><b>Vacina:</b> ").append(app.getVacina().getNomeVacina()).append("</p>");
        html.append("<p><b>Lote:</b> ").append(app.getVacina().getLote()).append("</p>");
        html.append("<p><b>Data:</b> ").append(app.getDataHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("</p>");

        String reacoes = app.getReacoes();
        if (reacoes != null && !reacoes.trim().isEmpty() && !reacoes.equals("Nenhuma reação relatada.")) {
            html.append("<p style='margin-top:20px;'><b>Reações Relatadas:</b> <br>").append(reacoes).append("</p>");
        }

        String obs = app.getObservacoesAdicionais();
        if (obs != null && !obs.trim().isEmpty() && !obs.equals("[CONCLUÍDO] Nenhuma observação geral.")) {
            String obsLimpa = obs.replace("[CONCLUÍDO]", "").trim();
            html.append("<p><b>Observações Gerais:</b> <br>").append(obsLimpa).append("</p>");
        }

        html.append("<hr style='border:1px dashed #ccc; margin-top:30px; margin-bottom: 20px;'/>");

        String nomeClinica = "Clínica de Vacinação";

        html.append("<h3 style='color: #444; margin:0;'>").append(nomeClinica).append("</h3>");
        html.append("<p style='color: #888; font-size: 12px; margin-top:5px;'>Documento auxiliar de registro vacinal</p>");
        html.append("</body></html>");

        try {
            java.io.File arquivoFicha = java.io.File.createTempFile("Comprovante", ".html");
            java.nio.file.Files.writeString(arquivoFicha.toPath(), html.toString(), java.nio.charset.StandardCharsets.UTF_8);
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(arquivoFicha);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}