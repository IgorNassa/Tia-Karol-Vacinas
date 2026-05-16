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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PainelFormulario extends JPanel {
    private TelaPrincipal frame;
    private JComboBox<String> cbPacientePrincipal, cbFamiliar, cbVacina, cbStatus, cbPagamento;
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

        // HEADER COMPACTO
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

        // =========================================================
        // LADO ESQUERDO: FORMULÁRIO
        // =========================================================
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
        for (Paciente p : listaPacientesCache) cbPacientePrincipal.addItem(p.getNome());
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

        cbStatus = new JComboBox<>(new String[]{"Aplicado", "Agendado"});
        configurarCombo(cbStatus);
        pnlCardForm.add(montarBloco("STATUS *", "check-circle.svg", cbStatus));
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

        // =========================================================
        // LADO DIREITO: CARRINHO E FINANCEIRO
        // =========================================================
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

        cbPagamento = new JComboBox<>(new String[]{"Pendente", "PIX", "Cartão de Crédito", "Cartão de Débito", "Dinheiro"});
        configurarCombo(cbPagamento);

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

    private void carregarItemParaEdicao(int rowIndex) {
        indiceItemEditado = rowIndex;
        Aplicacao app = itensCarrinho.get(rowIndex);
        cbPacientePrincipal.setSelectedItem(app.getPaciente().getNome());
        for(int i=0; i<cbVacina.getItemCount(); i++) { if (cbVacina.getItemAt(i).contains(app.getVacina().getLote())) { cbVacina.setSelectedIndex(i); break; } }
        txtData.setText(app.getDataHora().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        txtHora.setText(app.getDataHora().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        cbStatus.setSelectedItem(app.getStatus());
        if (chkRecorrencia != null) chkRecorrencia.getParent().setVisible(false);
        btnAdicionarItem.setText(" Salvar Alteração");
        btnAdicionarItem.setBackground(Cores.ROSA_KAROL);
        btnCancelarItem.setVisible(true);
    }

    private void limparFormularioItem() {
        indiceItemEditado = -1;
        cbVacina.setSelectedIndex(0);
        cbStatus.setSelectedIndex(0);
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
                for (Paciente fam : listaFamiliaresAtuais) cbFamiliar.addItem(fam.getNome());
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

        if (indiceItemEditado == -1) {
            Aplicacao app = new Aplicacao();
            app.setPaciente(pAlvo); app.setVacina(v); app.setDataHora(LocalDateTime.of(dataDef, horaDef)); app.setStatus(status); app.setValor(v.getValorVenda()); app.setValorBruto(v.getValorVenda());
            itensCarrinho.add(app); adicionarLinhaTabela(app);

            if (chkRecorrencia != null && chkRecorrencia.isSelected()) {
                int dias = Integer.parseInt(txtIntervaloDias.getText().replaceAll("[^0-9]", ""));
                int qtdExtras = cbQtdDoses.getSelectedIndex() + 1;
                for (int i = 1; i <= qtdExtras; i++) {
                    LocalDate dFutura = dataDef.plusDays((long) dias * i);
                    if (dFutura.getDayOfWeek() == DayOfWeek.SUNDAY) dFutura = dFutura.plusDays(1);
                    Aplicacao appRec = new Aplicacao();
                    appRec.setPaciente(pAlvo); appRec.setVacina(v); appRec.setDataHora(LocalDateTime.of(dFutura, horaDef)); appRec.setStatus("Agendado"); appRec.setValor(v.getValorVenda()); appRec.setValorBruto(v.getValorVenda());
                    itensCarrinho.add(appRec); adicionarLinhaTabela(appRec);
                }
            }
        } else {
            Aplicacao app = itensCarrinho.get(indiceItemEditado);
            double valAntigo = app.getValorBruto() > 0 ? app.getValorBruto() : app.getValor();
            valorBrutoCarrinho -= valAntigo;

            app.setPaciente(pAlvo); app.setVacina(v); app.setDataHora(LocalDateTime.of(dataDef, horaDef)); app.setStatus(status); app.setValor(v.getValorVenda()); app.setValorBruto(v.getValorVenda());
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
            txtValorTotal.setText(String.format(new Locale("pt", "BR"), "R$ %,.2f", Math.max(0.00, total)));
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

    // =========================================================
    // MELHORIA ESTÉTICA COM FLATLAF CLIENT PROPERTIES
    // =========================================================

    private void configurarCombo(JComboBox<?> cb) {
        cb.setPreferredSize(new Dimension(0, 20));
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cb.putClientProperty(FlatClientProperties.STYLE, ""
                + "focusWidth: 1;"          // Borda fina e suave ao clicar
                + "hoverBackground: #f4f6f8;"
                + "buttonHoverArrowColor: #20b2aa"
                + "padding: 5,10,5,10"
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