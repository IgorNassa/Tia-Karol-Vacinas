package br.sistema.view.panels;

import br.sistema.model.Aplicacao;
import br.sistema.model.Paciente;
import br.sistema.model.Vacina;
import br.sistema.repository.AplicacaoDAO;
import br.sistema.repository.ConnectionFactory;
import br.sistema.repository.PacienteDAO;
import br.sistema.repository.VacinaDAO;
import br.sistema.util.Cores;
import br.sistema.view.TelaPrincipal;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PainelFormulario extends JPanel {
    private TelaPrincipal frame;
    private JComboBox<String> cbPacientePrincipal, cbFamiliar, cbVacina, cbStatus, cbPagamento, cbLocalAplicacao;
    private JCheckBox chkFamilia, chkRecorrencia;
    private JFormattedTextField txtData, txtHora;
    private JTextField txtDesconto, txtValorTotal, txtIntervaloDias;
    private JRadioButton rbPorcentagem, rbReais;
    private JComboBox<String> cbQtdDoses;
    private JButton btnAdicionarItem, btnCancelarItem;
    private int indiceItemEditado = -1;
    private JTable tabelaCarrinho;
    private DefaultTableModel modeloCarrinho;
    private List<Aplicacao> itensCarrinho = new ArrayList<>();
    private List<Paciente> listaPacientesCache;
    private List<Paciente> listaFamiliaresAtuais = new ArrayList<>();
    private VacinaDAO vacinaDAO = new VacinaDAO();
    private double valorBrutoCarrinho = 0.0;
    private Map<JComponent, JLabel> mapLabels = new HashMap<>();

    public PainelFormulario(TelaPrincipal frame) { this(frame, null); }

    public PainelFormulario(TelaPrincipal frame, Aplicacao appEdicao) {
        this.frame = frame;
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel pnlTopo = new JPanel(new BorderLayout(15, 5));
        pnlTopo.setOpaque(false);
        pnlTopo.setBorder(new EmptyBorder(15, 30, 10, 30));

        JLabel btnVoltar = new JLabel(" Voltar");
        btnVoltar.setIcon(carregarIcone("seta-para-a-esquerda.svg", 14, Cores.VERDE_AQUA));
        btnVoltar.setForeground(Cores.VERDE_AQUA);
        btnVoltar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnVoltar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnVoltar.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { frame.trocarTelaCentral(new PainelAplicacoes(frame)); }
        });

        JLabel lblTitulo = new JLabel(" Novo Atendimento");
        lblTitulo.setIcon(carregarIcone("report.svg", 24, Cores.CINZA_GRAFITE));
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitulo.setForeground(Cores.CINZA_GRAFITE);

        pnlTopo.add(btnVoltar, BorderLayout.NORTH);
        pnlTopo.add(lblTitulo, BorderLayout.CENTER);
        add(pnlTopo, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setOpaque(false);
        splitPane.setBorder(null);
        splitPane.setDividerSize(0);
        splitPane.setResizeWeight(0.38);

        JPanel pnlEsquerdoContainer = new JPanel(new BorderLayout());
        pnlEsquerdoContainer.setOpaque(false);
        pnlEsquerdoContainer.setBorder(new EmptyBorder(5, 30, 20, 10));

        JPanel pnlCardForm = new JPanel();
        pnlCardForm.setLayout(new BoxLayout(pnlCardForm, BoxLayout.Y_AXIS));
        pnlCardForm.putClientProperty(FlatClientProperties.STYLE, "arc: 16; background: #ffffff; border: 1,1,1,1,#E1E5E9,16;");
        pnlCardForm.setBorder(new EmptyBorder(20, 25, 20, 25));

        cbPacientePrincipal = new JComboBox<>();
        cbPacientePrincipal.addItem("Selecione o paciente principal...");
        listaPacientesCache = new PacienteDAO().listarTodos();
        for (Paciente p : listaPacientesCache) {
            cbPacientePrincipal.addItem(formatarNomeComIdade(p));
        }
        configurarCombo(cbPacientePrincipal);
        pnlCardForm.add(montarBloco("PACIENTE PRINCIPAL *", "member-list.svg", cbPacientePrincipal));
        pnlCardForm.add(Box.createVerticalStrut(12));

        JPanel pnlFamilia = new JPanel(new BorderLayout(0, 5));
        pnlFamilia.setOpaque(false);
        chkFamilia = new JCheckBox("Atendimento Múltiplo (Família)");
        chkFamilia.setFont(new Font("Segoe UI", Font.BOLD, 12));
        chkFamilia.setForeground(Cores.VERDE_AQUA);
        chkFamilia.setCursor(new Cursor(Cursor.HAND_CURSOR));
        chkFamilia.setVisible(false);

        cbFamiliar = new JComboBox<>();
        cbFamiliar.addItem("Selecione o membro da família...");
        configurarCombo(cbFamiliar);
        cbFamiliar.setVisible(false);

        pnlFamilia.add(chkFamilia, BorderLayout.NORTH);
        pnlFamilia.add(cbFamiliar, BorderLayout.CENTER);
        pnlCardForm.add(pnlFamilia);
        pnlCardForm.add(Box.createVerticalStrut(12));

        cbPacientePrincipal.addActionListener(e -> processarFamiliares());
        chkFamilia.addActionListener(e -> cbFamiliar.setVisible(chkFamilia.isSelected()));

        cbVacina = new JComboBox<>();
        cbVacina.addItem("Selecione a vacina / lote...");
        for (String item : vacinaDAO.listarLotesParaCombo()) {
            if (item != null && !item.toLowerCase().contains("selecione")) cbVacina.addItem(item);
        }
        configurarCombo(cbVacina);
        pnlCardForm.add(montarBloco("VACINA *", "vacinas.svg", cbVacina));
        pnlCardForm.add(Box.createVerticalStrut(12));

        JPanel pnlDataHora = new JPanel(new GridLayout(1, 2, 10, 0));
        pnlDataHora.setOpaque(false);
        try {
            MaskFormatter maskData = new MaskFormatter("##/##/####"); maskData.setPlaceholderCharacter('_');
            txtData = new JFormattedTextField(maskData); txtData.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            configurarCampo(txtData);

            MaskFormatter maskHora = new MaskFormatter("##:##"); maskHora.setPlaceholderCharacter('_');
            txtHora = new JFormattedTextField(maskHora); txtHora.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
            configurarCampo(txtHora);
        } catch (Exception e) {}

        pnlDataHora.add(montarBloco("DATA *", "calendar-clock.svg", txtData));
        pnlDataHora.add(montarBloco("HORA", null, txtHora));
        pnlCardForm.add(pnlDataHora);
        pnlCardForm.add(Box.createVerticalStrut(12));

        JPanel pnlStatusLocal = new JPanel(new GridLayout(1, 2, 10, 0));
        pnlStatusLocal.setOpaque(false);

        cbStatus = new JComboBox<>(new String[]{"Aplicado", "Agendado"});
        configurarCombo(cbStatus);
        pnlStatusLocal.add(montarBloco("STATUS *", "check-circle.svg", cbStatus));

        cbLocalAplicacao = new JComboBox<>(new String[]{"Não informado", "Deltoide Dir. (Braço)", "Deltoide Esq. (Braço)", "Vasto Lateral Dir. (Perna)", "Vasto Lateral Esq. (Perna)", "Glúteo Direito", "Glúteo Esquerdo", "Via Oral", "Subcutânea", "Outro"});
        configurarCombo(cbLocalAplicacao);
        pnlStatusLocal.add(montarBloco("LOCAL APLICADO", null, cbLocalAplicacao));

        pnlCardForm.add(pnlStatusLocal);
        pnlCardForm.add(Box.createVerticalStrut(12));

        JPanel pnlRec = new JPanel(new BorderLayout(0, 5));
        pnlRec.setOpaque(false);
        chkRecorrencia = new JCheckBox("Gerar doses futuras (Esquema)");
        chkRecorrencia.setFont(new Font("Segoe UI", Font.BOLD, 12));
        chkRecorrencia.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel pnlDetRec = new JPanel(new GridLayout(1, 2, 10, 0));
        pnlDetRec.setOpaque(false);
        cbQtdDoses = new JComboBox<>(new String[]{"1 Dose", "2 Doses", "3 Doses", "4 Doses", "5 Doses"});
        configurarCombo(cbQtdDoses);
        txtIntervaloDias = new JTextField();
        txtIntervaloDias.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ex: 30");
        configurarCampo(txtIntervaloDias);

        pnlDetRec.add(montarBloco("QTD EXTRAS", null, cbQtdDoses));
        pnlDetRec.add(montarBloco("DIAS INTERVALO", null, txtIntervaloDias));
        pnlDetRec.setVisible(false);

        chkRecorrencia.addActionListener(e -> pnlDetRec.setVisible(chkRecorrencia.isSelected()));
        pnlRec.add(chkRecorrencia, BorderLayout.NORTH);
        pnlRec.add(pnlDetRec, BorderLayout.CENTER);
        pnlCardForm.add(pnlRec);
        pnlCardForm.add(Box.createVerticalStrut(20));

        JPanel pnlBotoesAcao = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlBotoesAcao.setOpaque(false);

        btnCancelarItem = new JButton(" Cancelar");
        btnCancelarItem.setIcon(carregarIcone("note.svg", 14, Cores.CINZA_GRAFITE));
        btnCancelarItem.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btnCancelarItem.setPreferredSize(new Dimension(110, 36));
        btnCancelarItem.setBackground(new Color(240, 243, 245));
        btnCancelarItem.setForeground(Cores.CINZA_GRAFITE);
        btnCancelarItem.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnCancelarItem.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancelarItem.setVisible(false);
        btnCancelarItem.addActionListener(e -> limparFormularioItem());

        btnAdicionarItem = new JButton(" Adicionar");
        btnAdicionarItem.setIcon(carregarIcone("adicionar.svg", 14, Color.WHITE));
        btnAdicionarItem.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btnAdicionarItem.setPreferredSize(new Dimension(130, 36));
        btnAdicionarItem.setBackground(Cores.VERDE_AQUA);
        btnAdicionarItem.setForeground(Color.WHITE);
        btnAdicionarItem.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnAdicionarItem.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAdicionarItem.addActionListener(e -> salvarItem());

        pnlBotoesAcao.add(btnCancelarItem);
        pnlBotoesAcao.add(btnAdicionarItem);
        pnlCardForm.add(pnlBotoesAcao);

        JScrollPane scrollEsquerdo = new JScrollPane(pnlCardForm);
        scrollEsquerdo.setBorder(null);
        scrollEsquerdo.setOpaque(false);
        scrollEsquerdo.getViewport().setOpaque(false);
        scrollEsquerdo.getVerticalScrollBar().setUnitIncrement(16);
        pnlEsquerdoContainer.add(scrollEsquerdo, BorderLayout.CENTER);
        splitPane.setLeftComponent(pnlEsquerdoContainer);

        JPanel pnlDireitoWrapper = new JPanel(new BorderLayout(0, 15));
        pnlDireitoWrapper.setOpaque(false);
        pnlDireitoWrapper.setBorder(new EmptyBorder(5, 10, 20, 30));

        JPanel pnlCardTabela = new JPanel(new BorderLayout());
        pnlCardTabela.putClientProperty(FlatClientProperties.STYLE, "arc: 16; background: #ffffff; border: 1,1,1,1,#E1E5E9,16;");
        pnlCardTabela.setBorder(new EmptyBorder(15, 20, 15, 20));

        modeloCarrinho = new DefaultTableModel(new Object[]{"Paciente", "Vacina", "Data", "Status", "Valor"}, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tabelaCarrinho = new JTable(modeloCarrinho);
        tabelaCarrinho.putClientProperty(FlatClientProperties.STYLE, "rowHeight: 35; showHorizontalLines: true; showVerticalLines: false; intercellSpacing: 0,0; selectionBackground: $Table.selectionBackground;");
        tabelaCarrinho.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabelaCarrinho.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabelaCarrinho.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "background: #F8FAFC; separatorColor: #E1E5E9;");

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer(); centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        tabelaCarrinho.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        tabelaCarrinho.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        JLabel lblTituloResumo = new JLabel(" Resumo do Atendimento");
        lblTituloResumo.setIcon(carregarIcone("report.svg", 18, Cores.CINZA_GRAFITE));
        lblTituloResumo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTituloResumo.setBorder(new EmptyBorder(0, 0, 10, 0));
        pnlCardTabela.add(lblTituloResumo, BorderLayout.NORTH);

        JScrollPane scrollTabela = new JScrollPane(tabelaCarrinho);
        scrollTabela.setBorder(new LineBorder(new Color(235, 235, 235)));
        pnlCardTabela.add(scrollTabela, BorderLayout.CENTER);

        JButton btnEditar = new JButton("Editar");
        btnEditar.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btnEditar.setIcon(carregarIcone("lapis-de-blog.svg", 14, Cores.CINZA_GRAFITE));
        btnEditar.setBackground(new Color(240, 243, 245));
        btnEditar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnEditar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEditar.addActionListener(e -> {
            int row = tabelaCarrinho.getSelectedRow();
            if (row >= 0) carregarItemParaEdicao(row); else JOptionPane.showMessageDialog(this, "Selecione um item na tabela.", "Aviso", JOptionPane.WARNING_MESSAGE);
        });

        JButton btnRemover = new JButton("Remover");
        btnRemover.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btnRemover.setIcon(carregarIcone("trash.svg", 14, Cores.ROSA_KAROL));
        btnRemover.setForeground(Cores.ROSA_KAROL);
        btnRemover.setBackground(new Color(255, 240, 240));
        btnRemover.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRemover.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRemover.addActionListener(e -> removerItemCarrinho());

        JPanel pnlAcoesTabela = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        pnlAcoesTabela.setOpaque(false);
        pnlAcoesTabela.add(btnEditar);
        pnlAcoesTabela.add(btnRemover);
        pnlCardTabela.add(pnlAcoesTabela, BorderLayout.SOUTH);

        pnlDireitoWrapper.add(pnlCardTabela, BorderLayout.CENTER);

        // FINANCEIRO
        JPanel pnlCardFinanceiro = new JPanel(new BorderLayout(15, 10));
        pnlCardFinanceiro.putClientProperty(FlatClientProperties.STYLE, "arc: 16; background: #F8FAFC; border: 1,1,1,1,#E1E5E9,16;");
        pnlCardFinanceiro.setBorder(new EmptyBorder(15, 25, 15, 25));

        txtDesconto = new JTextField("0");
        configurarCampo(txtDesconto);
        rbPorcentagem = new JRadioButton("%", true); rbPorcentagem.setOpaque(false); rbPorcentagem.setCursor(new Cursor(Cursor.HAND_CURSOR));
        rbReais = new JRadioButton("R$"); rbReais.setOpaque(false); rbReais.setCursor(new Cursor(Cursor.HAND_CURSOR));
        ButtonGroup group = new ButtonGroup(); group.add(rbPorcentagem); group.add(rbReais);

        JPanel pnlDesc = new JPanel(new BorderLayout()); pnlDesc.setOpaque(false);
        JPanel pnlRad = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); pnlRad.setOpaque(false);
        pnlRad.add(rbPorcentagem); pnlRad.add(rbReais);
        pnlDesc.add(txtDesconto, BorderLayout.CENTER); pnlDesc.add(pnlRad, BorderLayout.EAST);

        // ATUALIZADO: Agora suporta múltiplas formas (Misto)
        cbPagamento = new JComboBox<>(new String[]{"Pendente", "PIX", "Cartão de Crédito", "Cartão de Débito", "Dinheiro", "Múltiplas Formas..."});
        configurarCombo(cbPagamento);
        cbPagamento.addActionListener(e -> {
            if (cbPagamento.getSelectedItem() != null && cbPagamento.getSelectedItem().toString().equals("Múltiplas Formas...")) {
                abrirModalPagamentoMisto();
            }
        });

        txtValorTotal = new JTextField("R$ 0,00");
        txtValorTotal.setEditable(false);
        txtValorTotal.setFont(new Font("Segoe UI", Font.BOLD, 28));
        txtValorTotal.setForeground(new Color(39, 174, 96));
        txtValorTotal.setHorizontalAlignment(JTextField.RIGHT);
        txtValorTotal.setBorder(null);
        txtValorTotal.setOpaque(false);

        JPanel pnlGridFin = new JPanel(new GridLayout(1, 2, 20, 0));
        pnlGridFin.setOpaque(false);
        pnlGridFin.add(montarBloco("DESCONTO GLOBAL", null, pnlDesc));
        pnlGridFin.add(montarBloco("PAGAMENTO", "usd-circle.svg", cbPagamento));

        JPanel pnlTotalFinal = new JPanel(new BorderLayout());
        pnlTotalFinal.setOpaque(false);
        pnlTotalFinal.setBorder(new EmptyBorder(10, 0, 0, 0));
        JLabel lblTot = new JLabel("TOTAL LÍQUIDO:");
        lblTot.setFont(new Font("Segoe UI", Font.BOLD, 15));
        pnlTotalFinal.add(lblTot, BorderLayout.WEST);
        pnlTotalFinal.add(txtValorTotal, BorderLayout.EAST);

        pnlCardFinanceiro.add(pnlGridFin, BorderLayout.NORTH);
        pnlCardFinanceiro.add(pnlTotalFinal, BorderLayout.SOUTH);

        JButton btnSalvarTudo = new JButton(" Finalizar Atendimento");
        btnSalvarTudo.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btnSalvarTudo.setIcon(carregarIcone("disco.svg", 20, Color.WHITE));
        btnSalvarTudo.setBackground(Cores.ROSA_KAROL);
        btnSalvarTudo.setForeground(Color.WHITE);
        btnSalvarTudo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnSalvarTudo.setPreferredSize(new Dimension(0, 50));
        btnSalvarTudo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSalvarTudo.addActionListener(e -> finalizarAtendimento());

        JPanel pnlSulDir = new JPanel(new BorderLayout(0, 12));
        pnlSulDir.setOpaque(false);
        pnlSulDir.add(pnlCardFinanceiro, BorderLayout.CENTER);
        pnlSulDir.add(btnSalvarTudo, BorderLayout.SOUTH);
        pnlDireitoWrapper.add(pnlSulDir, BorderLayout.SOUTH);

        splitPane.setRightComponent(pnlDireitoWrapper);
        add(splitPane, BorderLayout.CENTER);

        txtDesconto.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { atualizarFinanceiro(); } });
        rbPorcentagem.addActionListener(e -> atualizarFinanceiro()); rbReais.addActionListener(e -> atualizarFinanceiro());

        if (appEdicao != null) {
            itensCarrinho.add(appEdicao);
            adicionarLinhaTabela(appEdicao);
            cbPagamento.setSelectedItem(appEdicao.getFormaPagamento());
            txtDesconto.setText(String.format(new Locale("pt", "BR"), "%.2f", appEdicao.getDesconto()));
            rbReais.setSelected(true);
            atualizarFinanceiro();
            tabelaCarrinho.setRowSelectionInterval(0, 0);
            SwingUtilities.invokeLater(() -> carregarItemParaEdicao(0));
        }
    }

    // MODAL DE PAGAMENTO DIVIDIDO
    private void abrirModalPagamentoMisto() {
        double totalApagar = 0;
        try { totalApagar = Double.parseDouble(txtValorTotal.getText().replaceAll("[^0-9,]", "").replace(",", ".")); } catch (Exception ex) {}

        if (totalApagar <= 0) {
            JOptionPane.showMessageDialog(frame, "O carrinho está vazio ou o valor total está zerado.", "Aviso", JOptionPane.WARNING_MESSAGE);
            cbPagamento.setSelectedIndex(1); // Volta pro PIX padrão
            return;
        }

        JDialog diag = new JDialog(frame, "Dividir Pagamento", true);
        diag.setSize(600, 500);
        diag.setLocationRelativeTo(frame);
        diag.setLayout(new BorderLayout());
        diag.getContentPane().setBackground(Color.WHITE);

        JPanel pnlTopo = new JPanel(new GridLayout(2, 1));
        pnlTopo.setBackground(Color.WHITE);
        pnlTopo.setBorder(new EmptyBorder(20, 30, 10, 30));

        Locale br = new Locale("pt", "BR");
        JLabel lblTotal = new JLabel(String.format(br, "Total da Venda: R$ %,.2f", totalApagar));
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTotal.setForeground(Cores.CINZA_GRAFITE);

        JLabel lblFalta = new JLabel(String.format(br, "Falta Pagar: R$ %,.2f", totalApagar));
        lblFalta.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblFalta.setForeground(new Color(220, 53, 69));

        pnlTopo.add(lblTotal);
        pnlTopo.add(lblFalta);
        diag.add(pnlTopo, BorderLayout.NORTH);

        JPanel pnlMeio = new JPanel(new BorderLayout(0, 15));
        pnlMeio.setBackground(Color.WHITE);
        pnlMeio.setBorder(new EmptyBorder(10, 30, 10, 30));

        JPanel pnlAdd = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pnlAdd.setBackground(Color.WHITE);

        JComboBox<String> cbMetodo = new JComboBox<>(new String[]{"PIX", "Cartão de Crédito", "Cartão de Débito", "Dinheiro"});
        configurarCombo(cbMetodo);
        cbMetodo.setPreferredSize(new Dimension(180, 40));

        JTextField txtValPag = new JTextField();
        configurarCampo(txtValPag);
        txtValPag.setPreferredSize(new Dimension(120, 40));
        txtValPag.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "0,00");

        JButton btnAddPag = new JButton("Adicionar");
        btnAddPag.setBackground(Cores.VERDE_AQUA);
        btnAddPag.setForeground(Color.WHITE);
        btnAddPag.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnAddPag.setPreferredSize(new Dimension(100, 40));

        pnlAdd.add(cbMetodo);
        pnlAdd.add(txtValPag);
        pnlAdd.add(btnAddPag);

        pnlMeio.add(pnlAdd, BorderLayout.NORTH);

        DefaultTableModel modPag = new DefaultTableModel(new Object[]{"Método Selecionado", "Valor Pago"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabPag = new JTable(modPag);
        tabPag.setRowHeight(35);
        tabPag.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tabPag.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        JPanel pnlTabela = new JPanel(new BorderLayout(0, 5));
        pnlTabela.setOpaque(false);
        pnlTabela.add(new JScrollPane(tabPag), BorderLayout.CENTER);
        JLabel lblDica = new JLabel("Dica: Dê um duplo clique na linha para remover um pagamento errado.");
        lblDica.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblDica.setForeground(Cores.CINZA_LABEL);
        pnlTabela.add(lblDica, BorderLayout.SOUTH);

        pnlMeio.add(pnlTabela, BorderLayout.CENTER);
        diag.add(pnlMeio, BorderLayout.CENTER);

        JButton btnConfirmar = new JButton("Confirmar Pagamento Misto");
        btnConfirmar.setBackground(Cores.ROSA_KAROL);
        btnConfirmar.setForeground(Color.WHITE);
        btnConfirmar.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnConfirmar.setPreferredSize(new Dimension(0, 50));
        btnConfirmar.setEnabled(false);

        final double[] falta = {totalApagar};
        final boolean[] confirmado = {false};

        btnAddPag.addActionListener(e -> {
            try {
                double v = Double.parseDouble(txtValPag.getText().replace(".", "").replace(",", "."));
                if (v <= 0) return;
                if (v > falta[0] + 0.01) {
                    JOptionPane.showMessageDialog(diag, "O valor digitado é maior que o restante a pagar!", "Aviso", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                modPag.addRow(new Object[]{cbMetodo.getSelectedItem(), v});
                falta[0] -= v;
                if(falta[0] < 0) falta[0] = 0;

                lblFalta.setText(String.format(br, "Falta Pagar: R$ %,.2f", falta[0]));
                if (falta[0] <= 0.01) {
                    lblFalta.setText("Falta Pagar: R$ 0,00");
                    lblFalta.setForeground(new Color(39, 174, 96));
                    btnConfirmar.setEnabled(true);
                    btnAddPag.setEnabled(false);
                }
                txtValPag.setText("");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(diag, "Digite um valor numérico válido (Ex: 150,00).", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        tabPag.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int r = tabPag.getSelectedRow();
                    if (r >= 0) {
                        double v = (double) modPag.getValueAt(r, 1);
                        falta[0] += v;
                        modPag.removeRow(r);
                        lblFalta.setText(String.format(br, "Falta Pagar: R$ %,.2f", falta[0]));
                        lblFalta.setForeground(new Color(220, 53, 69));
                        btnConfirmar.setEnabled(false);
                        btnAddPag.setEnabled(true);
                    }
                }
            }
        });

        JPanel pnlBot = new JPanel(new BorderLayout());
        pnlBot.setBorder(new EmptyBorder(10, 30, 20, 30));
        pnlBot.setBackground(Color.WHITE);
        pnlBot.add(btnConfirmar, BorderLayout.CENTER);
        diag.add(pnlBot, BorderLayout.SOUTH);

        btnConfirmar.addActionListener(e -> {
            StringBuilder sb = new StringBuilder("Misto: ");
            for (int i=0; i<modPag.getRowCount(); i++) {
                if (i>0) sb.append(" + ");
                sb.append(modPag.getValueAt(i, 0)).append(" (R$ ").append(String.format(br, "%.2f", modPag.getValueAt(i,1))).append(")");
            }
            confirmado[0] = true;
            cbPagamento.removeItem(sb.toString()); // Remove se ja existir um igual
            cbPagamento.addItem(sb.toString());
            cbPagamento.setSelectedItem(sb.toString());
            diag.dispose();
        });

        diag.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (!confirmado[0]) cbPagamento.setSelectedIndex(1); // Volta pro PIX se fechar no X
            }
        });

        diag.setVisible(true);
    }

    private String formatarNomeComIdade(Paciente p) {
        if (p.getDataNascimento() == null) return p.getNome() + " (Idade não informada)";
        LocalDate hoje = LocalDate.now();
        Period periodo = Period.between(p.getDataNascimento(), hoje);
        int a = periodo.getYears(); int m = periodo.getMonths(); int d = periodo.getDays();
        StringBuilder idade = new StringBuilder();
        if (a > 0) idade.append(a).append(a == 1 ? " ano" : " anos");
        if (m > 0) { if (idade.length() > 0) idade.append(", "); idade.append(m).append(m == 1 ? " mês" : " meses"); }
        if (d > 0) { if (idade.length() > 0) idade.append(", "); idade.append(d).append(d == 1 ? " dia" : " dias"); }
        if (idade.length() == 0) idade.append("Recém-nascido");
        return p.getNome() + " (" + idade.toString() + ")";
    }

    private void carregarItemParaEdicao(int rowIndex) {
        indiceItemEditado = rowIndex;
        Aplicacao app = itensCarrinho.get(rowIndex);

        for (int i = 0; i < listaPacientesCache.size(); i++) {
            if (listaPacientesCache.get(i).getId() == app.getPaciente().getId()) {
                cbPacientePrincipal.setSelectedIndex(i + 1);
                break;
            }
        }

        for(int i=0; i<cbVacina.getItemCount(); i++) { if (cbVacina.getItemAt(i).contains(app.getVacina().getLote())) { cbVacina.setSelectedIndex(i); break; } }
        txtData.setText(app.getDataHora().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        txtHora.setText(app.getDataHora().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        cbStatus.setSelectedItem(app.getStatus());

        String loc = app.getLocalAplicacao();
        if(loc == null || loc.isEmpty()) cbLocalAplicacao.setSelectedIndex(0);
        else cbLocalAplicacao.setSelectedItem(loc);

        if (chkRecorrencia != null) chkRecorrencia.getParent().setVisible(false);
        btnAdicionarItem.setText(" Salvar Alteração");
        btnAdicionarItem.setBackground(Cores.ROSA_KAROL);
        btnCancelarItem.setVisible(true);
    }

    private void limparFormularioItem() {
        indiceItemEditado = -1;
        cbVacina.setSelectedIndex(0);
        cbStatus.setSelectedIndex(0);
        cbLocalAplicacao.setSelectedIndex(0);
        if (chkRecorrencia != null) chkRecorrencia.getParent().setVisible(true);
        btnAdicionarItem.setText(" Adicionar");
        btnAdicionarItem.setBackground(Cores.VERDE_AQUA);
        btnCancelarItem.setVisible(false);
        tabelaCarrinho.clearSelection();
    }

    private void processarFamiliares() {
        if (cbPacientePrincipal.getSelectedIndex() > 0) {
            Paciente p = listaPacientesCache.get(cbPacientePrincipal.getSelectedIndex() - 1);
            listaFamiliaresAtuais = new PacienteDAO().buscarFamiliaresPorCpfResponsavel(p.getCpf(), p.getCpfResponsavel(), p.getCpfResponsavel2(), p.getId());
            if (!listaFamiliaresAtuais.isEmpty()) {
                chkFamilia.setVisible(true);
                cbFamiliar.removeAllItems();
                cbFamiliar.addItem("Selecione o familiar...");
                for (Paciente fam : listaFamiliaresAtuais) cbFamiliar.addItem(formatarNomeComIdade(fam));
                return;
            }
        }
        chkFamilia.setVisible(false); chkFamilia.setSelected(false); cbFamiliar.setVisible(false);
    }

    private void salvarItem() {
        if (cbPacientePrincipal.getSelectedIndex() <= 0 || cbVacina.getSelectedIndex() <= 0) return;
        Vacina v = vacinaDAO.buscarPorLoteCombo(cbVacina.getSelectedItem().toString());
        if (v == null) return;

        LocalDate dataDef;
        try { dataDef = LocalDate.parse(txtData.getText(), DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
        catch (Exception ex) { return; }

        LocalTime horaDef = LocalTime.of(8, 0);
        try { String hStr = txtHora.getText().replaceAll("[^0-9]", ""); if (hStr.length() == 4) horaDef = LocalTime.parse(txtHora.getText(), DateTimeFormatter.ofPattern("HH:mm")); } catch(Exception ex){}

        Paciente pAlvo = listaPacientesCache.get(cbPacientePrincipal.getSelectedIndex() - 1);
        if (chkFamilia != null && chkFamilia.isSelected() && cbFamiliar.getSelectedIndex() > 0) pAlvo = listaFamiliaresAtuais.get(cbFamiliar.getSelectedIndex() - 1);

        String status = cbStatus.getSelectedItem().toString();
        String localApp = cbLocalAplicacao.getSelectedIndex() > 0 ? cbLocalAplicacao.getSelectedItem().toString() : "";

        if (indiceItemEditado == -1) {
            Aplicacao app = new Aplicacao();
            app.setPaciente(pAlvo); app.setVacina(v); app.setDataHora(LocalDateTime.of(dataDef, horaDef)); app.setStatus(status); app.setValor(v.getValorVenda()); app.setValorBruto(v.getValorVenda()); app.setLocalAplicacao(localApp);
            itensCarrinho.add(app); adicionarLinhaTabela(app);

            if (chkRecorrencia != null && chkRecorrencia.isSelected()) {
                int dias = Integer.parseInt(txtIntervaloDias.getText().replaceAll("[^0-9]", ""));
                int qtdExtras = cbQtdDoses.getSelectedIndex() + 1;
                for (int i = 1; i <= qtdExtras; i++) {
                    LocalDate dFutura = dataDef.plusDays((long) dias * i);
                    if (dFutura.getDayOfWeek() == DayOfWeek.SUNDAY) dFutura = dFutura.plusDays(1);
                    Aplicacao appRec = new Aplicacao();
                    appRec.setPaciente(pAlvo); appRec.setVacina(v); appRec.setDataHora(LocalDateTime.of(dFutura, horaDef)); appRec.setStatus("Agendado"); appRec.setValor(v.getValorVenda()); appRec.setValorBruto(v.getValorVenda()); appRec.setLocalAplicacao(""); // Recorrente não tem local definido ainda
                    itensCarrinho.add(appRec); adicionarLinhaTabela(appRec);
                }
            }
        } else {
            Aplicacao app = itensCarrinho.get(indiceItemEditado);
            double valAntigo = app.getValorBruto() > 0 ? app.getValorBruto() : app.getValor();
            valorBrutoCarrinho -= valAntigo;

            app.setPaciente(pAlvo); app.setVacina(v); app.setDataHora(LocalDateTime.of(dataDef, horaDef)); app.setStatus(status); app.setValor(v.getValorVenda()); app.setValorBruto(v.getValorVenda()); app.setLocalAplicacao(localApp);
            valorBrutoCarrinho += app.getValorBruto();

            modeloCarrinho.setValueAt(pAlvo.getNome().split(" ")[0], indiceItemEditado, 0);
            modeloCarrinho.setValueAt(v.getNomeVacina(), indiceItemEditado, 1);
            modeloCarrinho.setValueAt(app.getDataHora().format(DateTimeFormatter.ofPattern("dd/MM/yy")), indiceItemEditado, 2);
            modeloCarrinho.setValueAt(app.getStatus(), indiceItemEditado, 3);
            modeloCarrinho.setValueAt(String.format("R$ %.2f", app.getValorBruto()), indiceItemEditado, 4);
        }
        atualizarFinanceiro(); limparFormularioItem();
    }

    private void adicionarLinhaTabela(Aplicacao app) {
        double valBase = app.getValorBruto() > 0 ? app.getValorBruto() : app.getValor();
        valorBrutoCarrinho += valBase;
        modeloCarrinho.addRow(new Object[]{ app.getPaciente().getNome().split(" ")[0], app.getVacina().getNomeVacina(), app.getDataHora().format(DateTimeFormatter.ofPattern("dd/MM/yy")), app.getStatus(), String.format("R$ %.2f", valBase) });
    }

    private void removerItemCarrinho() {
        int row = tabelaCarrinho.getSelectedRow();
        if (row >= 0 && indiceItemEditado == -1) {
            double val = itensCarrinho.get(row).getValorBruto() > 0 ? itensCarrinho.get(row).getValorBruto() : itensCarrinho.get(row).getValor();
            valorBrutoCarrinho -= val; itensCarrinho.remove(row); modeloCarrinho.removeRow(row); atualizarFinanceiro();
        } else if (row >= 0 && indiceItemEditado != -1) {
            JOptionPane.showMessageDialog(this, "Não é possível remover enquanto o item está sendo editado.", "Aviso", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void atualizarFinanceiro() {
        try {
            String dTexto = txtDesconto.getText().replace(",", "."); if (dTexto.isEmpty()) dTexto = "0";
            double desc = Double.parseDouble(dTexto);
            double total = rbPorcentagem.isSelected() ? valorBrutoCarrinho - (valorBrutoCarrinho * (desc / 100)) : valorBrutoCarrinho - desc;

            String novoTotalTexto = String.format(new Locale("pt", "BR"), "R$ %,.2f", Math.max(0.00, total));

            // Segurança Anti-Fraude: Se alteraram o desconto e o pagamento atual é Misto, ele cancela o Misto.
            if (!txtValorTotal.getText().equals(novoTotalTexto) && cbPagamento.getSelectedItem() != null && cbPagamento.getSelectedItem().toString().startsWith("Misto:")) {
                cbPagamento.setSelectedIndex(1); // Reseta para PIX
            }

            txtValorTotal.setText(novoTotalTexto);
        } catch (Exception e) { txtValorTotal.setText(String.format("R$ %.2f", valorBrutoCarrinho)); }
    }

    private void finalizarAtendimento() {
        if (itensCarrinho.isEmpty()) { JOptionPane.showMessageDialog(this, "O carrinho está vazio.", "Atenção", JOptionPane.WARNING_MESSAGE); return; }
        String pag = cbPagamento.getSelectedItem().toString();

        double totFinal = 0.0;
        try { totFinal = Double.parseDouble(txtValorTotal.getText().replaceAll("[^0-9,]", "").replace(",", ".")); } catch (Exception e) { totFinal = valorBrutoCarrinho; }
        double prop = valorBrutoCarrinho > 0 ? totFinal / valorBrutoCarrinho : 1.0;

        List<Aplicacao> novasApps = new ArrayList<>();
        AplicacaoDAO appDao = new AplicacaoDAO();

        for (Aplicacao a : itensCarrinho) {
            a.setFormaPagamento(a.getStatus().equals("Agendado") ? "Pendente" : pag);
            if(a.getStatus().equals("Aplicado") && pag.equals("Pendente")) { JOptionPane.showMessageDialog(this, "Status 'Aplicado' não pode ficar com pagamento 'Pendente'.", "Erro Financeiro", JOptionPane.ERROR_MESSAGE); return; }
            double vB = a.getValorBruto() > 0 ? a.getValorBruto() : a.getValor();
            a.setValorBruto(vB); a.setValor(vB * prop); a.setDesconto(vB - a.getValor());
            if (a.getId() > 0) { if (!appDao.atualizar(a)) { JOptionPane.showMessageDialog(this, "Erro Crítico.", "Erro", JOptionPane.ERROR_MESSAGE); return; } }
            else { novasApps.add(a); }
        }

        if (!novasApps.isEmpty()) { if (!appDao.salvarEmLote(novasApps)) { JOptionPane.showMessageDialog(this, "Erro no BD", "Erro no BD", JOptionPane.ERROR_MESSAGE); return; } }
        JOptionPane.showMessageDialog(this, "Atendimento salvo com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        frame.trocarTelaCentral(new PainelAplicacoes(frame));
    }

    private void configurarCombo(JComboBox<?> cb) {
        cb.setPreferredSize(new Dimension(0, 36));
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cb.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc: 5;"
                + "focusWidth: 1;"
                + "focusColor: #20b2aa;"
                + "hoverBackground: #f4f6f8;"
                + "buttonHoverArrowColor: #20b2aa"
        );
    }

    private void configurarCampo(JComponent c) {
        c.setPreferredSize(new Dimension(0, 36));
        c.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        c.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc: 5;"
                + "focusWidth: 1;"
                + "focusColor: #20b2aa;"
                + "hoverBackground: #f4f6f8;"
                + "padding: 4,8,4,8"
        );
    }

    private JPanel montarBloco(String titulo, String iconeSvg, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel l = new JLabel(titulo);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(new Color(110, 120, 130));
        if (iconeSvg != null) {
            l.setIcon(carregarIcone(iconeSvg, 14, Cores.VERDE_AQUA));
            l.setIconTextGap(6);
        }
        mapLabels.put(comp, l);
        p.add(l, BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private FlatSVGIcon carregarIcone(String n, int t, Color c) {
        try { return (FlatSVGIcon) new FlatSVGIcon("icons/" + n, t, t).setColorFilter(new FlatSVGIcon.ColorFilter(cor -> c)); }
        catch(Exception e) { return null; }
    }
}