package br.sistema.view.panels;

import br.sistema.model.Vacina;
import br.sistema.repository.VacinaDAO;
import br.sistema.util.Cores;
import br.sistema.view.TelaPrincipal;
import br.sistema.view.components.GlassPanel;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PainelEstoque extends JPanel {
    private TelaPrincipal frame;
    private JTable tabela;
    private DefaultTableModel modeloTabela;
    private VacinaDAO dao;
    private List<Vacina> listaVacinas;
    private int hoveredRow = -1;
    private JComboBox<String> cbFiltroEstoque;

    private JLabel lblTotalDoses, lblLotesAlerta, lblCapital;
    private Map<JComponent, JLabel> mapLabels = new HashMap<>();

    public PainelEstoque(TelaPrincipal frame) {
        this.frame = frame;
        this.dao = new VacinaDAO();
        setOpaque(false); setLayout(new BorderLayout(0, 20));
        setBorder(new EmptyBorder(15, 20, 15, 20));

        GlassPanel cardVidro = new GlassPanel();
        cardVidro.setLayout(new BorderLayout(0, 15)); cardVidro.setBorder(new EmptyBorder(15, 20, 15, 20));

        JPanel header = new JPanel(new BorderLayout(20, 0)); header.setOpaque(false);
        JLabel titulo = new JLabel("Estoque e Lotes");
        titulo.setFont(new Font("Segoe UI Semilight", Font.PLAIN, 32)); titulo.setForeground(Cores.CINZA_GRAFITE);
        header.add(titulo, BorderLayout.WEST);

        JPanel pnlAcoesTop = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0)); pnlAcoesTop.setOpaque(false);

        cbFiltroEstoque = new JComboBox<>(new String[]{"Estoque Ativo (> 0)", "Lotes Zerados / Histórico"});
        cbFiltroEstoque.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cbFiltroEstoque.setPreferredSize(new Dimension(220, 38));
        cbFiltroEstoque.addActionListener(e -> carregarDadosTabela());

        JButton btnNovo = new JButton(" Novo Registro");
        btnNovo.setIcon(carregarIcone("adicionar.svg", 18, Color.WHITE));
        btnNovo.setBackground(Cores.VERDE_AQUA); btnNovo.setForeground(Color.WHITE);
        btnNovo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnNovo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnNovo.addActionListener(e -> abrirModalFormulario(null, false));

        pnlAcoesTop.add(cbFiltroEstoque);
        pnlAcoesTop.add(btnNovo);
        header.add(pnlAcoesTop, BorderLayout.EAST); cardVidro.add(header, BorderLayout.NORTH);

        JPanel pnlKpis = new JPanel(new GridLayout(1, 3, 20, 0)); pnlKpis.setOpaque(false); pnlKpis.setPreferredSize(new Dimension(0, 80));
        lblTotalDoses = new JLabel("0"); lblLotesAlerta = new JLabel("0"); lblCapital = new JLabel("R$ 0,00");
        pnlKpis.add(criarKpiCard("Doses Listadas", lblTotalDoses, Cores.VERDE_AQUA));
        pnlKpis.add(criarKpiCard("Lotes em Alerta", lblLotesAlerta, new Color(230, 126, 34)));
        pnlKpis.add(criarKpiCard("Capital Imobilizado", lblCapital, Cores.ROSA_KAROL));

        JPanel pnlCentro = new JPanel(new BorderLayout(0, 20)); pnlCentro.setOpaque(false);
        pnlCentro.add(pnlKpis, BorderLayout.NORTH);

        String[] colunas = {"ID", "Vacina", "Laboratório", "Lote", "Validade", "Estoque"};
        modeloTabela = new DefaultTableModel(new Object[][]{}, colunas) { public boolean isCellEditable(int row, int column) { return false; } };
        tabela = new JTable(modeloTabela); tabela.setRowHeight(50); tabela.setShowVerticalLines(false); tabela.setShowHorizontalLines(false); tabela.setIntercellSpacing(new Dimension(0, 5)); tabela.setFont(new Font("Segoe UI", Font.PLAIN, 15));

        tabela.setDefaultRenderer(Object.class, new CustomTableRenderer());

        JPopupMenu menuContexto = criarMenuOpcoes();

        tabela.addMouseMotionListener(new MouseMotionAdapter() { public void mouseMoved(MouseEvent e) { int row = tabela.rowAtPoint(e.getPoint()); if (hoveredRow != row) { hoveredRow = row; tabela.repaint(); } } });
        tabela.addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent e) { hoveredRow = -1; tabela.repaint(); }
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int linhaClicada = tabela.rowAtPoint(e.getPoint());
                    if (linhaClicada >= 0) { tabela.setRowSelectionInterval(linhaClicada, linhaClicada); menuContexto.show(e.getComponent(), e.getX(), e.getY()); }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tabela); scroll.setBorder(BorderFactory.createEmptyBorder()); scroll.getViewport().setBackground(Color.WHITE); scroll.setOpaque(false);
        pnlCentro.add(scroll, BorderLayout.CENTER); cardVidro.add(pnlCentro, BorderLayout.CENTER); add(cardVidro, BorderLayout.CENTER);

        carregarDadosTabela();
    }

    private void carregarDadosTabela() {
        modeloTabela.setRowCount(0); listaVacinas = dao.listarTodas();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        int totalDoses = 0; int lotesAlerta = 0; double capital = 0.0;

        boolean mostrarAtivos = cbFiltroEstoque.getSelectedIndex() == 0;

        for (Vacina v : listaVacinas) {
            boolean isZerado = v.getQtdDisponivel() <= 0;
            if (mostrarAtivos && isZerado) continue;
            if (!mostrarAtivos && !isZerado) continue;

            String val = v.getValidade() != null ? v.getValidade().format(fmt) : "N/A";
            modeloTabela.addRow(new Object[]{
                    String.format("%03d", v.getId()), v.getNomeVacina(), v.getLaboratorio(), v.getLote(), val,
                    String.valueOf(v.getQtdDisponivel())
            });

            totalDoses += v.getQtdDisponivel();
            if (v.getQtdDisponivel() <= 5 && v.getQtdDisponivel() > 0) lotesAlerta++;
            capital += (v.getQtdDisponivel() * v.getValorCompra());
        }
        lblTotalDoses.setText(String.valueOf(totalDoses)); lblLotesAlerta.setText(String.valueOf(lotesAlerta)); lblCapital.setText(String.format("R$ %.2f", capital));
        hoveredRow = -1;
    }

    private JPopupMenu criarMenuOpcoes() {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(230, 235, 240), 1, true), new EmptyBorder(10, 5, 10, 5)));
        popup.setBackground(Color.WHITE);

        JMenuItem itemNovoLote = criarItemMenu("Adicionar Novo Lote...", "adicionar.svg", new Color(46, 204, 113));
        JMenuItem itemEditar = criarItemMenu("Editar este Lote", "lapis-de-blog.svg", Cores.CINZA_GRAFITE);
        JMenuItem itemDescarte = criarItemMenu("Registrar Perda/Descarte", "aviso.svg", new Color(230, 126, 34));
        JMenuItem itemExcluir = criarItemMenu("Excluir Lote (Erro digitação)", "trash.svg", new Color(220, 53, 69));

        itemNovoLote.addActionListener(e -> { int l = tabela.getSelectedRow(); if (l >= 0) abrirModalFormulario(buscarVacinaPorIdNaLista(Integer.parseInt((String) tabela.getValueAt(l, 0))), true); });
        itemEditar.addActionListener(e -> { int l = tabela.getSelectedRow(); if (l >= 0) abrirModalFormulario(buscarVacinaPorIdNaLista(Integer.parseInt((String) tabela.getValueAt(l, 0))), false); });
        itemDescarte.addActionListener(e -> { int l = tabela.getSelectedRow(); if (l >= 0) abrirModalDescarte(buscarVacinaPorIdNaLista(Integer.parseInt((String) tabela.getValueAt(l, 0)))); });
        itemExcluir.addActionListener(e -> {
            int l = tabela.getSelectedRow();
            if (l >= 0 && JOptionPane.showConfirmDialog(frame, "CUIDADO: Use isso apenas para erros de digitação!\nDeseja excluir permanentemente do banco?", "Atenção", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                dao.excluir(Integer.parseInt((String) tabela.getValueAt(l, 0))); carregarDadosTabela();
            }
        });

        popup.add(itemNovoLote); JSeparator sep1 = new JSeparator(); sep1.setForeground(new Color(240,240,240)); popup.add(sep1);
        popup.add(itemEditar); JSeparator sep2 = new JSeparator(); sep2.setForeground(new Color(240,240,240)); popup.add(sep2);
        popup.add(itemDescarte); popup.add(itemExcluir);
        return popup;
    }

    private void abrirModalDescarte(Vacina v) {
        if(v.getQtdDisponivel() <= 0) { JOptionPane.showMessageDialog(frame, "Este lote já está zerado.", "Aviso", JOptionPane.INFORMATION_MESSAGE); return; }

        JDialog dialog = new JDialog(frame, "Registrar Descarte: " + v.getNomeVacina(), true);
        dialog.setSize(450, 400); dialog.setLocationRelativeTo(frame); dialog.getContentPane().setBackground(Color.WHITE);
        JPanel pnl = new JPanel(new GridLayout(3, 1, 15, 15)); pnl.setBackground(Color.WHITE); pnl.setBorder(new EmptyBorder(20, 30, 20, 30));

        JSpinner spQtd = new JSpinner(new SpinnerNumberModel(1, 1, v.getQtdDisponivel(), 1)); spQtd.setFont(new Font("Segoe UI", Font.BOLD, 18));
        JComboBox<String> cbMotivo = new JComboBox<>(new String[]{"Quebra de Frasco", "Vencimento (Passou da Validade)", "Quebra de Cadeia Fria (Geladeira/Luz)", "Contaminação", "Outros"}); cbMotivo.setBackground(Color.WHITE);

        JLabel lblAviso = new JLabel("<html><center>Aviso: Esta ação removerá a vacina fisicamente<br>da geladeira, sem alterar o caixa financeiro.</center></html>");
        lblAviso.setForeground(new Color(230, 126, 34)); lblAviso.setHorizontalAlignment(SwingConstants.CENTER);

        pnl.add(montarBloco("QUANTIDADE PERDIDA (Máx: " + v.getQtdDisponivel() + ")", spQtd)); pnl.add(montarBloco("MOTIVO DA PERDA", cbMotivo)); pnl.add(lblAviso);

        JButton btnConfirmar = new JButton(" Confirmar Descarte"); btnConfirmar.setBackground(new Color(230, 126, 34)); btnConfirmar.setForeground(Color.WHITE); btnConfirmar.setFont(new Font("Segoe UI", Font.BOLD, 15)); btnConfirmar.setPreferredSize(new Dimension(0, 50)); btnConfirmar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnConfirmar.addActionListener(e -> {
            int perdidos = (int) spQtd.getValue();
            int opt1 = JOptionPane.showConfirmDialog(dialog, "ATENÇÃO: Você está prestes a remover " + perdidos + " dose(s) do estoque físico.\nDeseja continuar?", "1ª Confirmação de Baixa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if(opt1 == JOptionPane.YES_OPTION) {
                int opt2 = JOptionPane.showConfirmDialog(dialog, "CONFIRMAÇÃO FINAL:\nTem certeza absoluta que deseja descartar estas vacinas permanentemente?", "2ª Confirmação de Baixa", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
                if(opt2 == JOptionPane.YES_OPTION) {
                    v.setQtdDisponivel(v.getQtdDisponivel() - perdidos);
                    dao.atualizar(v);
                    JOptionPane.showMessageDialog(dialog, "Baixa de estoque registrada com sucesso.", "Estoque Atualizado", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose(); carregarDadosTabela();
                }
            }
        });

        JPanel pnlFooter = new JPanel(new BorderLayout()); pnlFooter.setBackground(Color.WHITE); pnlFooter.setBorder(new EmptyBorder(0, 30, 20, 30)); pnlFooter.add(btnConfirmar, BorderLayout.CENTER);
        dialog.add(pnl, BorderLayout.CENTER); dialog.add(pnlFooter, BorderLayout.SOUTH); dialog.setVisible(true);
    }

    private Vacina buscarVacinaPorIdNaLista(int id) { for (Vacina v : listaVacinas) { if (v.getId() == id) return v; } return null; }

    private void abrirModalFormulario(Vacina vEmEdicao, boolean isNovoLote) {
        String tituloModal = "Entrada de Novo Lote";
        if (vEmEdicao != null) tituloModal = isNovoLote ? "Novo Lote para: " + vEmEdicao.getNomeVacina() : "Atualizar Lote";

        JDialog dialog = new JDialog(frame, tituloModal, true);
        dialog.setSize(600, 720); dialog.setLocationRelativeTo(frame); dialog.getContentPane().setBackground(Color.WHITE);

        JPanel pnl = new JPanel(new GridBagLayout()); pnl.setBackground(Color.WHITE); pnl.setBorder(new EmptyBorder(20, 30, 20, 30));
        GridBagConstraints gbc = new GridBagConstraints(); gbc.fill = GridBagConstraints.HORIZONTAL; gbc.insets = new Insets(8, 8, 8, 8); gbc.weightx = 1.0;

        JTextField txtNome = criarCampoTexto("Ex: Febre amarela");
        if (vEmEdicao != null) txtNome.setText(vEmEdicao.getNomeVacina());
        txtNome.addFocusListener(new FocusAdapter() { public void focusLost(FocusEvent e) { String t = txtNome.getText().trim().toLowerCase(); if (!t.isEmpty()) txtNome.setText(t.substring(0, 1).toUpperCase() + t.substring(1)); } });

        JComboBox<String> cbTipo = new JComboBox<>(new String[]{"Inativada", "Atenuada", "Conjugada", "Subunitária", "Toxoide", "mRNA", "Vetor Viral", "Outro", "A DEFINIR"}); cbTipo.setPreferredSize(new Dimension(0, 40)); cbTipo.setBackground(Color.WHITE); cbTipo.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        if (vEmEdicao != null) cbTipo.setSelectedItem(vEmEdicao.getTipo());

        JTextField txtLab = criarCampoTexto("Nome do Laboratório"); if (vEmEdicao != null) txtLab.setText(vEmEdicao.getLaboratorio());
        JTextField txtDistribuidor = criarCampoTexto("Distribuidor/Fornecedor"); if (vEmEdicao != null) txtDistribuidor.setText(vEmEdicao.getDistribuidor());
        JTextField txtNotaFiscal = criarCampoTexto("Nº da Nota"); if (vEmEdicao != null) txtNotaFiscal.setText(vEmEdicao.getNumeroNota());

        JTextField txtCompra = criarCampoTexto("0,00"); seAplicarFormatacaoMoeda(txtCompra); if (vEmEdicao != null) txtCompra.setText(String.format("%.2f", vEmEdicao.getValorCompra()).replace(".", ","));
        JTextField txtVenda = criarCampoTexto("0,00"); seAplicarFormatacaoMoeda(txtVenda); if (vEmEdicao != null) txtVenda.setText(String.format("%.2f", vEmEdicao.getValorVenda()).replace(".", ","));
        JTextField txtObs = criarCampoTexto("Observações..."); if (vEmEdicao != null) txtObs.setText(vEmEdicao.getObservacoes());

        JTextField txtLote = criarCampoTexto("Lote Impresso"); txtLote.addFocusListener(new FocusAdapter() { public void focusLost(FocusEvent e) { txtLote.setText(txtLote.getText().trim().toUpperCase()); } });
        JFormattedTextField txtValidade = new JFormattedTextField(); aplicarMascara(txtValidade, "##/##/####"); configurarCampoFormatado(txtValidade); adicionarValidacaoDataInline(txtValidade);
        JSpinner spQuantidade = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1)); spQuantidade.setPreferredSize(new Dimension(0, 40)); spQuantidade.setFont(new Font("Segoe UI", Font.PLAIN, 15));

        if (vEmEdicao != null && !isNovoLote) {
            if (vEmEdicao.getLote() != null && !vEmEdicao.getLote().equals("AGUARDANDO LOTE")) txtLote.setText(vEmEdicao.getLote());
            if (vEmEdicao.getValidade() != null) txtValidade.setText(vEmEdicao.getValidade().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            spQuantidade.setValue(vEmEdicao.getQtdDisponivel());
        }

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; pnl.add(montarBloco("NOME DA VACINA *", txtNome), gbc);
        gbc.gridy = 1; gbc.gridwidth = 1; pnl.add(montarBloco("TIPO DA VACINA", cbTipo), gbc); gbc.gridx = 1; pnl.add(montarBloco("LABORATÓRIO", txtLab), gbc);
        gbc.gridx = 0; gbc.gridy = 2; pnl.add(montarBloco("DISTRIBUIDOR", txtDistribuidor), gbc); gbc.gridx = 1; pnl.add(montarBloco("NOTA FISCAL", txtNotaFiscal), gbc);
        gbc.gridx = 0; gbc.gridy = 3; pnl.add(montarBloco("LOTE (Identificador) *", txtLote), gbc); gbc.gridx = 1; pnl.add(montarBloco("VALIDADE DO LOTE *", txtValidade), gbc);
        gbc.gridx = 0; gbc.gridy = 4; pnl.add(montarBloco("CUSTO UNITÁRIO (R$)", txtCompra), gbc); gbc.gridx = 1; pnl.add(montarBloco("VALOR DE VENDA (R$)", txtVenda), gbc);
        gbc.gridx = 0; gbc.gridy = 5; pnl.add(montarBloco("QTD NA GELADEIRA", spQuantidade), gbc);
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2; pnl.add(montarBloco("OBSERVAÇÕES", txtObs), gbc);

        JButton btnSalvar = new JButton(" Confirmar Lote"); btnSalvar.setIcon(carregarIcone("disco.svg", 18, Color.WHITE)); btnSalvar.setBackground(Cores.VERDE_AQUA); btnSalvar.setForeground(Color.WHITE); btnSalvar.setFont(new Font("Segoe UI", Font.BOLD, 15)); btnSalvar.setPreferredSize(new Dimension(0, 50)); btnSalvar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnSalvar.addActionListener(e -> {
            try {
                if (txtNome.getText().trim().isEmpty() || txtLote.getText().trim().isEmpty() || txtValidade.getText().replaceAll("[^0-9]", "").length() != 8) { JOptionPane.showMessageDialog(dialog, "Nome, Lote e Validade são obrigatórios.", "Aviso", JOptionPane.WARNING_MESSAGE); return; }
                LocalDate val = LocalDate.parse(txtValidade.getText(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                if (val.isBefore(LocalDate.now())) { JOptionPane.showMessageDialog(dialog, "A data de validade informada já passou.\nNão é permitido cadastrar lotes vencidos.", "Data Inválida", JOptionPane.ERROR_MESSAGE); return; }

                double compra = Double.parseDouble(txtCompra.getText().replace(".", "").replace(",", ".")); double venda = Double.parseDouble(txtVenda.getText().replace(".", "").replace(",", "."));
                Vacina v = new Vacina();
                v.setNomeVacina(txtNome.getText().trim());
                v.setTipo(cbTipo.getSelectedItem().toString());
                v.setLaboratorio(txtLab.getText().trim());
                v.setDistribuidor(txtDistribuidor.getText().trim());
                v.setNumeroNota(txtNotaFiscal.getText().trim());
                v.setLote(txtLote.getText().trim());
                v.setValidade(val);
                v.setValorCompra(compra);
                v.setValorVenda(venda);
                v.setObservacoes(txtObs.getText().trim());
                int qtd = (int) spQuantidade.getValue();
                v.setQtdDisponivel(qtd);

                if (vEmEdicao == null || isNovoLote) {
                    v.setQtdTotal(qtd); dao.salvar(v);
                } else {
                    v.setId(vEmEdicao.getId()); v.setQtdTotal(vEmEdicao.getQtdTotal() == 0 ? qtd : vEmEdicao.getQtdTotal()); dao.atualizar(v);
                }
                dialog.dispose(); carregarDadosTabela();
            } catch (Exception ex) { JOptionPane.showMessageDialog(dialog, "Verifique os campos de data e valores numéricos.", "Erro", JOptionPane.ERROR_MESSAGE); }
        });

        JPanel pnlFooter = new JPanel(new BorderLayout()); pnlFooter.setBackground(Color.WHITE); pnlFooter.setBorder(new EmptyBorder(10, 30, 20, 30)); pnlFooter.add(btnSalvar, BorderLayout.CENTER);
        dialog.add(pnl, BorderLayout.CENTER); dialog.add(pnlFooter, BorderLayout.SOUTH); dialog.setVisible(true);
    }

    private void seAplicarFormatacaoMoeda(JTextField campo) {
        campo.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { String numeros = campo.getText().replaceAll("[^0-9]", ""); if (numeros.isEmpty()) { campo.setText("0,00"); return; } double valor = Double.parseDouble(numeros) / 100; campo.setText(String.format("%,.2f", valor)); } });
        campo.addFocusListener(new FocusAdapter() { public void focusGained(FocusEvent e) { if(campo.getText().equals("0,00")) campo.setText(""); } });
    }

    private void adicionarValidacaoDataInline(JTextField campo) {
        campo.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                String nums = campo.getText().replaceAll("[^0-9]", "");
                if (nums.length() == 8) {
                    try {
                        LocalDate dataDigitada = LocalDate.parse(campo.getText(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                        if (dataDigitada.isBefore(LocalDate.now())) setErroComponente(campo, true, "Data Vencida!"); else setErroComponente(campo, false, null);
                    } catch (Exception ex) { setErroComponente(campo, true, "Inválida"); }
                } else if (nums.length() > 0) { setErroComponente(campo, true, "Incompleta"); }
            }
        });
    }

    private JPanel criarKpiCard(String titulo, JLabel lblValor, Color corBorda) {
        JPanel pnl = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE); g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 25, 25);
                g2.setColor(corBorda); g2.setStroke(new BasicStroke(2f)); g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 25, 25); g2.dispose();
            }
        };
        pnl.setOpaque(false); pnl.setBorder(new EmptyBorder(15, 20, 15, 20));
        JLabel lblT = new JLabel(titulo); lblT.setFont(new Font("Segoe UI", Font.BOLD, 13)); lblT.setForeground(Cores.CINZA_LABEL);
        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 26)); lblValor.setForeground(Cores.CINZA_GRAFITE);
        pnl.add(lblT, BorderLayout.NORTH); pnl.add(lblValor, BorderLayout.CENTER); return pnl;
    }

    private String toHex(Color color) { return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()); }

    private JMenuItem criarItemMenu(String texto, String arquivoSvg, Color corAcao) {
        JMenuItem item = new JMenuItem(texto); item.setFont(new Font("Segoe UI", Font.PLAIN, 14)); item.setBorder(new EmptyBorder(10, 20, 10, 30)); item.setCursor(new Cursor(Cursor.HAND_CURSOR)); item.setForeground(Cores.CINZA_GRAFITE); item.setBackground(Color.WHITE); item.setOpaque(true);
        String corHex = toHex(corAcao);
        item.putClientProperty("FlatLaf.style", "selectionBackground: fade(" + corHex + ", 12%); selectionArc: 10; selectionForeground: " + corHex + ";");
        try {
            FlatSVGIcon icon = carregarIcone(arquivoSvg, 18, Cores.CINZA_GRAFITE); item.setIcon(icon); item.setIconTextGap(15);
            item.addChangeListener(e -> { if (item.isArmed()) icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> corAcao)); else icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> Cores.CINZA_GRAFITE)); });
        } catch (Exception ignored) {}
        return item;
    }

    private FlatSVGIcon carregarIcone(String nomeArquivo, int tamanho, Color cor) { try { return (FlatSVGIcon) new FlatSVGIcon("icons/" + nomeArquivo, tamanho, tamanho).setColorFilter(new FlatSVGIcon.ColorFilter(c -> cor)); } catch (Exception e) { return null; } }
    private JTextField criarCampoTexto(String placeholder) { JTextField f = new JTextField(); f.setPreferredSize(new Dimension(0, 40)); f.putClientProperty("JTextField.placeholderText", placeholder); f.setFont(new Font("Segoe UI", Font.PLAIN, 15)); f.setBackground(Color.WHITE); f.addMouseListener(new MouseAdapter() { public void mouseEntered(MouseEvent e) { if (!f.hasFocus()) f.setBackground(new Color(242, 248, 248)); } public void mouseExited(MouseEvent e) { if (!f.hasFocus()) f.setBackground(Color.WHITE); } }); return f; }
    private void configurarCampoFormatado(JFormattedTextField f) { f.setPreferredSize(new Dimension(0, 40)); f.setFont(new Font("Segoe UI", Font.PLAIN, 15)); f.setBackground(Color.WHITE); f.addMouseListener(new MouseAdapter() { public void mouseEntered(MouseEvent e) { if (!f.hasFocus()) f.setBackground(new Color(242, 248, 248)); } public void mouseExited(MouseEvent e) { if (!f.hasFocus()) f.setBackground(Color.WHITE); } public void mouseClicked(MouseEvent e) { if (f.getText().replaceAll("[^0-9]", "").isEmpty()) f.setCaretPosition(0); }}); f.addFocusListener(new FocusAdapter() { public void focusGained(FocusEvent e) { if (f.getText().replaceAll("[^0-9]", "").isEmpty()) SwingUtilities.invokeLater(() -> f.setCaretPosition(0)); setErroComponente(f, false, null); }}); }
    private void aplicarMascara(JFormattedTextField campo, String formato) { try { MaskFormatter mask = new MaskFormatter(formato); mask.setPlaceholderCharacter('_'); mask.install(campo); } catch (Exception e) { } }
    private JPanel montarBloco(String textoLabel, JComponent input) { JPanel p = new JPanel(new BorderLayout(0, 5)); p.setOpaque(false); JLabel l = new JLabel(textoLabel); l.setFont(new Font("Segoe UI", Font.BOLD, 12)); l.setForeground(Cores.CINZA_LABEL); mapLabels.put(input, l); input.putClientProperty("tituloOriginal", textoLabel); p.add(l, BorderLayout.NORTH); p.add(input, BorderLayout.CENTER); return p; }
    private void setErroComponente(JComponent comp, boolean comErro, String msg) { JLabel lbl = mapLabels.get(comp); String original = (String) comp.getClientProperty("tituloOriginal"); if (comErro) { comp.putClientProperty("JComponent.outline", "error"); if (lbl != null && msg != null) { lbl.setText(original + " - " + msg); lbl.setForeground(new Color(220, 53, 69)); } } else { comp.putClientProperty("JComponent.outline", null); if (lbl != null) { lbl.setText(original); lbl.setForeground(Cores.CINZA_LABEL); } } comp.repaint(); }

    private class CustomTableRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column); setBorder(new EmptyBorder(0, 15, 0, 15));
            int qtdDisponivel = 0; try { Object val = table.getValueAt(row, 5); if(val != null && !val.toString().isEmpty()) qtdDisponivel = Integer.parseInt(val.toString()); } catch (Exception ex) { qtdDisponivel = 0; }
            if (isSelected) { c.setBackground(Cores.VERDE_AQUA); c.setForeground(Color.WHITE); } else if (row == hoveredRow) { c.setBackground(new Color(210, 235, 235)); c.setForeground(Cores.CINZA_GRAFITE); } else { c.setBackground(Color.WHITE); c.setForeground(Cores.CINZA_GRAFITE); }
            if (!isSelected && qtdDisponivel <= 5 && qtdDisponivel > 0) { c.setForeground(new Color(230, 126, 34)); c.setFont(new Font("Segoe UI", Font.BOLD, 15)); } else if (!isSelected && qtdDisponivel <= 0) { c.setForeground(new Color(220, 53, 69)); c.setFont(new Font("Segoe UI", Font.BOLD, 15)); }
            return c;
        }
    }
}