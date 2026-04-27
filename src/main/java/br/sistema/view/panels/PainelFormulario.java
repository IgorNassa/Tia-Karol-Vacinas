package br.sistema.view.panels;

import br.sistema.model.Aplicacao;
import br.sistema.model.Paciente;
import br.sistema.model.Vacina;
import br.sistema.repository.AplicacaoDAO;
import br.sistema.repository.PacienteDAO;
import br.sistema.repository.VacinaDAO;
import br.sistema.util.Cores;
import br.sistema.view.TelaPrincipal;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.ui.FlatLineBorder;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.*;
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
    private JCheckBox chkFamilia;
    private JFormattedTextField txtData, txtHora;
    private JTextField txtDesconto, txtValorTotal;
    private JRadioButton rbPorcentagem, rbReais;
    private JCheckBox chkRecorrencia;
    private JComboBox<String> cbQtdDoses;
    private JTextField txtIntervaloDias;

    // Botoes de Ação
    private JButton btnAdicionarItem, btnCancelarItem;
    private int indiceItemEditado = -1;

    // Carrinho
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
        setOpaque(false); setLayout(new BorderLayout());

        // =========================================================
        // CABEÇALHO DA TELA
        // =========================================================
        JPanel pnlTopo = new JPanel(new BorderLayout(15, 10)); pnlTopo.setOpaque(false); pnlTopo.setBorder(new EmptyBorder(25, 40, 15, 40));
        JLabel btnVoltar = new JLabel(" Voltar para Aplicações");
        btnVoltar.setIcon(carregarIcone("seta-para-a-esquerda.svg", 18, Cores.VERDE_AQUA));
        btnVoltar.setForeground(Cores.VERDE_AQUA); btnVoltar.setFont(new Font("Segoe UI", Font.BOLD, 15)); btnVoltar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnVoltar.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { frame.trocarTelaCentral(new PainelAplicacoes(frame)); } });

        JLabel lblTitulo = new JLabel(" Gerenciador de Atendimento");
        lblTitulo.setIcon(carregarIcone("report.svg", 36, Cores.CINZA_GRAFITE));
        lblTitulo.setFont(new Font("Segoe UI Semilight", Font.PLAIN, 34)); lblTitulo.setForeground(Cores.CINZA_GRAFITE);

        pnlTopo.add(btnVoltar, BorderLayout.NORTH); pnlTopo.add(lblTitulo, BorderLayout.CENTER);
        add(pnlTopo, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setOpaque(false); splitPane.setBorder(null); splitPane.setDividerSize(0); // Divisor invisível
        splitPane.setDividerLocation(520);

        // =========================================================
        // LADO ESQUERDO: FORMULÁRIO (ESTILO CARD)
        // =========================================================
        JPanel pnlEsquerdoContainer = new JPanel(new BorderLayout()); pnlEsquerdoContainer.setOpaque(false); pnlEsquerdoContainer.setBorder(new EmptyBorder(10, 40, 30, 20));

        JPanel pnlCardForm = new JPanel(); pnlCardForm.setLayout(new BoxLayout(pnlCardForm, BoxLayout.Y_AXIS)); pnlCardForm.setBackground(Color.WHITE);
        pnlCardForm.setBorder(BorderFactory.createCompoundBorder(new FlatLineBorder(new Insets(1,1,1,1), new Color(230,230,230), 1.5f, 25), new EmptyBorder(30, 35, 30, 35)));

        cbPacientePrincipal = new JComboBox<>(); cbPacientePrincipal.addItem("Selecione o paciente principal...");
        listaPacientesCache = new PacienteDAO().listarTodos();
        for (Paciente p : listaPacientesCache) cbPacientePrincipal.addItem(p.getNome());
        configurarCombo(cbPacientePrincipal);
        pnlCardForm.add(montarBloco("PACIENTE PRINCIPAL *", "member-list.svg", cbPacientePrincipal)); pnlCardForm.add(Box.createVerticalStrut(25));

        JPanel pnlFamilia = new JPanel(new BorderLayout(0, 8)); pnlFamilia.setOpaque(false);
        chkFamilia = new JCheckBox("Atendimento Múltiplo (Incluir Familiares)"); chkFamilia.setFont(new Font("Segoe UI", Font.BOLD, 13)); chkFamilia.setForeground(Cores.VERDE_AQUA); chkFamilia.setOpaque(false); chkFamilia.setVisible(false); chkFamilia.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cbFamiliar = new JComboBox<>(); cbFamiliar.addItem("Selecione o membro da família..."); configurarCombo(cbFamiliar); cbFamiliar.setVisible(false);
        pnlFamilia.add(chkFamilia, BorderLayout.NORTH); pnlFamilia.add(cbFamiliar, BorderLayout.CENTER);
        pnlCardForm.add(pnlFamilia); pnlCardForm.add(Box.createVerticalStrut(15));

        cbPacientePrincipal.addActionListener(e -> processarFamiliares());
        chkFamilia.addActionListener(e -> cbFamiliar.setVisible(chkFamilia.isSelected()));

        cbVacina = new JComboBox<>(); cbVacina.addItem("Selecione a vacina / lote na geladeira...");
        for (String item : vacinaDAO.listarLotesParaCombo()) if (item != null && !item.toLowerCase().contains("selecione")) cbVacina.addItem(item);
        configurarCombo(cbVacina);
        pnlCardForm.add(montarBloco("VACINA *", "vacinas.svg", cbVacina)); pnlCardForm.add(Box.createVerticalStrut(25));

        JPanel pnlDataHora = new JPanel(new GridLayout(1, 2, 20, 0)); pnlDataHora.setOpaque(false);
        try { MaskFormatter maskData = new MaskFormatter("##/##/####"); maskData.setPlaceholderCharacter('_'); txtData = new JFormattedTextField(maskData); txtData.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))); configurarCampo(txtData);
            MaskFormatter maskHora = new MaskFormatter("##:##"); maskHora.setPlaceholderCharacter('_'); txtHora = new JFormattedTextField(maskHora); txtHora.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))); configurarCampo(txtHora);
        } catch (Exception e) {}
        pnlDataHora.add(montarBloco("DATA *", "calendar-clock.svg", txtData)); pnlDataHora.add(montarBloco("HORA (OPCIONAL)", null, txtHora));
        pnlCardForm.add(pnlDataHora); pnlCardForm.add(Box.createVerticalStrut(25));

        cbStatus = new JComboBox<>(new String[]{"Aplicado", "Agendado"}); configurarCombo(cbStatus);
        pnlCardForm.add(montarBloco("STATUS DA DOSE ATUAL *", "check-circle.svg", cbStatus)); pnlCardForm.add(Box.createVerticalStrut(25));

        JPanel pnlRec = new JPanel(new BorderLayout(0, 10)); pnlRec.setOpaque(false);
        chkRecorrencia = new JCheckBox("Gerar esquema vacinal (Doses extras no futuro)"); chkRecorrencia.setFont(new Font("Segoe UI", Font.BOLD, 13)); chkRecorrencia.setForeground(Cores.CINZA_GRAFITE); chkRecorrencia.setOpaque(false); chkRecorrencia.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JPanel pnlDetRec = new JPanel(new GridLayout(1, 2, 20, 0)); pnlDetRec.setOpaque(false);
        cbQtdDoses = new JComboBox<>(new String[]{"1 Dose", "2 Doses", "3 Doses", "4 Doses", "5 Doses", "6 Doses", "7 Doses", "8 Doses", "9 Doses", "10 Doses"}); configurarCombo(cbQtdDoses);
        txtIntervaloDias = new JTextField(); txtIntervaloDias.putClientProperty("JTextField.placeholderText", "Ex: 30 dias"); configurarCampo(txtIntervaloDias);
        pnlDetRec.add(montarBloco("QTD EXTRAS", "adicionar.svg", cbQtdDoses)); pnlDetRec.add(montarBloco("INTERVALO (DIAS)", null, txtIntervaloDias)); pnlDetRec.setVisible(false);
        chkRecorrencia.addActionListener(e -> pnlDetRec.setVisible(chkRecorrencia.isSelected()));
        pnlRec.add(chkRecorrencia, BorderLayout.NORTH); pnlRec.add(pnlDetRec, BorderLayout.CENTER);
        pnlCardForm.add(pnlRec); pnlCardForm.add(Box.createVerticalStrut(35));

        JPanel pnlBotoesAcao = new JPanel(new GridLayout(1, 2, 15, 0)); pnlBotoesAcao.setOpaque(false);

        btnAdicionarItem = new JButton(" Adicionar ao Carrinho");
        btnAdicionarItem.setIcon(carregarIcone("adicionar.svg", 18, Color.WHITE));
        btnAdicionarItem.putClientProperty("JButton.buttonType", "roundRect");
        btnAdicionarItem.setPreferredSize(new Dimension(0, 50)); btnAdicionarItem.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        btnAdicionarItem.setBackground(Cores.VERDE_AQUA); btnAdicionarItem.setForeground(Color.WHITE); btnAdicionarItem.setFont(new Font("Segoe UI", Font.BOLD, 15)); btnAdicionarItem.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAdicionarItem.addActionListener(e -> salvarItem());

        btnCancelarItem = new JButton(" Novo Item");
        btnCancelarItem.setIcon(carregarIcone("note.svg", 18, Cores.CINZA_GRAFITE));
        btnCancelarItem.putClientProperty("JButton.buttonType", "roundRect");
        btnCancelarItem.setPreferredSize(new Dimension(0, 50)); btnCancelarItem.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        btnCancelarItem.setBackground(new Color(240, 243, 245)); btnCancelarItem.setForeground(Cores.CINZA_GRAFITE); btnCancelarItem.setFont(new Font("Segoe UI", Font.BOLD, 15)); btnCancelarItem.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancelarItem.setVisible(false);
        btnCancelarItem.addActionListener(e -> limparFormularioItem());

        pnlBotoesAcao.add(btnAdicionarItem); pnlBotoesAcao.add(btnCancelarItem);
        pnlCardForm.add(pnlBotoesAcao);

        JScrollPane scrollEsquerdo = new JScrollPane(pnlCardForm);
        scrollEsquerdo.setBorder(null); scrollEsquerdo.setOpaque(false); scrollEsquerdo.getViewport().setOpaque(false); scrollEsquerdo.getVerticalScrollBar().setUnitIncrement(16);
        pnlEsquerdoContainer.add(scrollEsquerdo, BorderLayout.CENTER);
        splitPane.setLeftComponent(pnlEsquerdoContainer);

        // =========================================================
        // LADO DIREITO: CARRINHO & FINANCEIRO
        // =========================================================
        JPanel pnlDireitoWrapper = new JPanel(new BorderLayout(0, 20)); pnlDireitoWrapper.setOpaque(false); pnlDireitoWrapper.setBorder(new EmptyBorder(10, 20, 30, 40));

        JPanel pnlCardTabela = new JPanel(new BorderLayout()); pnlCardTabela.setBackground(Color.WHITE);
        pnlCardTabela.setBorder(BorderFactory.createCompoundBorder(new FlatLineBorder(new Insets(1,1,1,1), new Color(230,230,230), 1.5f, 25), new EmptyBorder(20, 25, 20, 25)));

        modeloCarrinho = new DefaultTableModel(new Object[]{"Paciente", "Vacina", "Data", "Status", "Valor"}, 0) { public boolean isCellEditable(int row, int column) { return false; } };
        tabelaCarrinho = new JTable(modeloCarrinho); tabelaCarrinho.setRowHeight(42); tabelaCarrinho.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        tabelaCarrinho.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14)); tabelaCarrinho.getTableHeader().setBackground(new Color(248, 250, 252)); tabelaCarrinho.getTableHeader().setForeground(Cores.CINZA_GRAFITE);
        tabelaCarrinho.setSelectionBackground(new Color(235, 245, 245));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer(); centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        tabelaCarrinho.getColumnModel().getColumn(2).setCellRenderer(centerRenderer); tabelaCarrinho.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        JLabel lblTituloResumo = new JLabel(" Resumo do Atendimento");
        lblTituloResumo.setIcon(carregarIcone("report.svg", 20, Cores.CINZA_GRAFITE));
        lblTituloResumo.setFont(new Font("Segoe UI", Font.BOLD, 15)); lblTituloResumo.setForeground(Cores.CINZA_GRAFITE); lblTituloResumo.setBorder(new EmptyBorder(0, 0, 15, 0));
        pnlCardTabela.add(lblTituloResumo, BorderLayout.NORTH);

        JScrollPane scrollTabela = new JScrollPane(tabelaCarrinho); scrollTabela.setBorder(new LineBorder(new Color(235, 235, 235)));
        pnlCardTabela.add(scrollTabela, BorderLayout.CENTER);

        // Botoes de Ação da Tabela (Editar e Remover)
        JButton btnEditar = new JButton("Editar Selecionado");
        btnEditar.putClientProperty("JButton.buttonType", "roundRect");
        btnEditar.setIcon(carregarIcone("lapis-de-blog.svg", 16, Cores.CINZA_GRAFITE));
        btnEditar.setForeground(Cores.CINZA_GRAFITE); btnEditar.setBackground(new Color(240, 243, 245)); btnEditar.setFont(new Font("Segoe UI", Font.BOLD, 13)); btnEditar.setCursor(new Cursor(Cursor.HAND_CURSOR)); btnEditar.setFocusPainted(false);
        btnEditar.addActionListener(e -> {
            int row = tabelaCarrinho.getSelectedRow();
            if (row >= 0) {
                carregarItemParaEdicao(row);
            } else {
                JOptionPane.showMessageDialog(this, "Selecione um item na tabela para editar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            }
        });

        JButton btnRemover = new JButton("Remover Selecionado");
        btnRemover.putClientProperty("JButton.buttonType", "roundRect");
        btnRemover.setIcon(carregarIcone("trash.svg", 16, Cores.ROSA_KAROL));
        btnRemover.setForeground(Cores.ROSA_KAROL); btnRemover.setBackground(new Color(255, 240, 240)); btnRemover.setFont(new Font("Segoe UI", Font.BOLD, 13)); btnRemover.setCursor(new Cursor(Cursor.HAND_CURSOR)); btnRemover.setFocusPainted(false);
        btnRemover.addActionListener(e -> removerItemCarrinho());

        JPanel pnlAcoesTabela = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 15)); pnlAcoesTabela.setOpaque(false);
        pnlAcoesTabela.add(btnEditar);
        pnlAcoesTabela.add(btnRemover);
        pnlCardTabela.add(pnlAcoesTabela, BorderLayout.SOUTH);

        pnlDireitoWrapper.add(pnlCardTabela, BorderLayout.CENTER);

        // FINANCEIRO
        JPanel pnlCardFinanceiro = new JPanel(new BorderLayout(15, 15)); pnlCardFinanceiro.setBackground(new Color(248, 250, 252));
        pnlCardFinanceiro.setBorder(BorderFactory.createCompoundBorder(new FlatLineBorder(new Insets(1,1,1,1), new Color(225,230,235), 1.5f, 25), new EmptyBorder(25, 30, 25, 30)));

        JPanel pnlDesconto = new JPanel(new BorderLayout(5, 0)); pnlDesconto.setOpaque(false);
        txtDesconto = new JTextField("0"); configurarCampo(txtDesconto);
        JPanel pnlRadios = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0)); pnlRadios.setOpaque(false);
        rbPorcentagem = new JRadioButton("%"); rbReais = new JRadioButton("R$"); rbPorcentagem.setSelected(true); rbPorcentagem.setOpaque(false); rbReais.setOpaque(false);
        ButtonGroup group = new ButtonGroup(); group.add(rbPorcentagem); group.add(rbReais); pnlRadios.add(rbPorcentagem); pnlRadios.add(rbReais);
        pnlDesconto.add(txtDesconto, BorderLayout.CENTER); pnlDesconto.add(pnlRadios, BorderLayout.EAST);

        cbPagamento = new JComboBox<>(new String[]{"Pendente", "PIX", "Cartão de Crédito", "Cartão de Débito", "Dinheiro"}); configurarCombo(cbPagamento);
        txtValorTotal = new JTextField("R$ 0,00"); txtValorTotal.setEditable(false); txtValorTotal.setFont(new Font("Segoe UI", Font.BOLD, 36)); txtValorTotal.setForeground(new Color(39, 174, 96)); txtValorTotal.setHorizontalAlignment(JTextField.RIGHT); txtValorTotal.setBorder(null); txtValorTotal.setOpaque(false);

        JPanel pnlGridFin = new JPanel(new GridLayout(1, 2, 25, 0)); pnlGridFin.setOpaque(false);
        pnlGridFin.add(montarBloco("DESCONTO", null, pnlDesconto)); pnlGridFin.add(montarBloco("FORMA DE PAGAMENTO", "usd-circle.svg", cbPagamento));

        JPanel pnlTotalFinal = new JPanel(new BorderLayout()); pnlTotalFinal.setOpaque(false); pnlTotalFinal.setBorder(new EmptyBorder(15, 0, 0, 0));
        JLabel lblTot = new JLabel("TOTAL A PAGAR:"); lblTot.setFont(new Font("Segoe UI", Font.BOLD, 18)); lblTot.setForeground(Cores.CINZA_GRAFITE);
        pnlTotalFinal.add(lblTot, BorderLayout.WEST); pnlTotalFinal.add(txtValorTotal, BorderLayout.EAST);

        pnlCardFinanceiro.add(pnlGridFin, BorderLayout.NORTH); pnlCardFinanceiro.add(pnlTotalFinal, BorderLayout.SOUTH);

        JButton btnSalvarTudo = new JButton(" Confirmar e Finalizar Atendimento");
        btnSalvarTudo.putClientProperty("JButton.buttonType", "roundRect");
        btnSalvarTudo.setIcon(carregarIcone("disco.svg", 24, Color.WHITE));

        btnSalvarTudo.setPreferredSize(new Dimension(0, 65)); btnSalvarTudo.setBackground(Cores.ROSA_KAROL); btnSalvarTudo.setForeground(Color.WHITE); btnSalvarTudo.setFont(new Font("Segoe UI", Font.BOLD, 18)); btnSalvarTudo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSalvarTudo.addActionListener(e -> finalizarAtendimento());

        JPanel pnlSulDir = new JPanel(new BorderLayout(0, 20)); pnlSulDir.setOpaque(false);
        pnlSulDir.add(pnlCardFinanceiro, BorderLayout.CENTER); pnlSulDir.add(btnSalvarTudo, BorderLayout.SOUTH);
        pnlDireitoWrapper.add(pnlSulDir, BorderLayout.SOUTH);

        splitPane.setRightComponent(pnlDireitoWrapper); add(splitPane, BorderLayout.CENTER);

        txtDesconto.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { atualizarFinanceiro(); } });
        rbPorcentagem.addActionListener(e -> atualizarFinanceiro()); rbReais.addActionListener(e -> atualizarFinanceiro());

        // SE VEIO DA TELA DE EDIÇÃO, COLOCA NO CARRINHO E SELECIONA
        if (appEdicao != null) {
            itensCarrinho.add(appEdicao);
            adicionarLinhaTabela(appEdicao);
            cbPagamento.setSelectedItem(appEdicao.getFormaPagamento());
            txtDesconto.setText(String.format(new Locale("pt", "BR"), "%.2f", appEdicao.getDesconto()));
            rbReais.setSelected(true);
            atualizarFinanceiro();
            tabelaCarrinho.setRowSelectionInterval(0, 0); // Seleciona a linha na tabela automaticamente
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

        btnAdicionarItem.setText(" Salvar Alteração"); btnAdicionarItem.setIcon(carregarIcone("check-circle.svg", 18, Color.WHITE)); btnAdicionarItem.setBackground(Cores.ROSA_KAROL);
        btnCancelarItem.setVisible(true);
    }

    private void limparFormularioItem() {
        indiceItemEditado = -1; cbVacina.setSelectedIndex(0); cbStatus.setSelectedIndex(0);
        if (chkRecorrencia != null) chkRecorrencia.getParent().setVisible(true);
        btnAdicionarItem.setText(" Adicionar ao Carrinho"); btnAdicionarItem.setIcon(carregarIcone("adicionar.svg", 18, Color.WHITE)); btnAdicionarItem.setBackground(Cores.VERDE_AQUA);
        btnCancelarItem.setVisible(false); tabelaCarrinho.clearSelection();
    }

    private void processarFamiliares() {
        if (cbPacientePrincipal.getSelectedIndex() > 0) {
            Paciente p = listaPacientesCache.get(cbPacientePrincipal.getSelectedIndex() - 1);
            listaFamiliaresAtuais = new PacienteDAO().buscarFamiliaresPorCpfResponsavel(p.getCpf(), p.getCpfResponsavel(), p.getCpfResponsavel2(), p.getId());
            if (!listaFamiliaresAtuais.isEmpty()) {
                chkFamilia.setVisible(true); cbFamiliar.removeAllItems(); cbFamiliar.addItem("Selecione o membro da família...");
                for (Paciente fam : listaFamiliaresAtuais) cbFamiliar.addItem(fam.getNome());
                return;
            }
        }
        chkFamilia.setVisible(false); chkFamilia.setSelected(false); cbFamiliar.setVisible(false);
    }

    private void salvarItem() {
        if (cbPacientePrincipal.getSelectedIndex() <= 0 || cbVacina.getSelectedIndex() <= 0) { JOptionPane.showMessageDialog(this, "Selecione o Paciente e a Vacina."); return; }
        Vacina v = vacinaDAO.buscarPorLoteCombo(cbVacina.getSelectedItem().toString());
        if (v == null) { JOptionPane.showMessageDialog(this, "Erro: Não foi possível carregar o lote desta vacina.", "Erro", JOptionPane.ERROR_MESSAGE); return; }

        LocalDate dataDef = null;
        try { dataDef = LocalDate.parse(txtData.getText(), DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
        catch (Exception ex) { JOptionPane.showMessageDialog(this, "Data inválida. Verifique o formato DD/MM/AAAA.", "Erro", JOptionPane.ERROR_MESSAGE); return; }

        LocalTime horaDefinitiva = LocalTime.of(8, 0);
        try { String hStr = txtHora.getText().replaceAll("[^0-9]", ""); if (hStr.length() == 4) horaDefinitiva = LocalTime.parse(txtHora.getText(), DateTimeFormatter.ofPattern("HH:mm")); } catch(Exception ex){}

        long qtdMesmaVacinaNovaNoCarrinho = 0;
        for (int i = 0; i < itensCarrinho.size(); i++) { if (i != indiceItemEditado && itensCarrinho.get(i).getVacina().getId() == v.getId() && itensCarrinho.get(i).getId() == 0) qtdMesmaVacinaNovaNoCarrinho++; }

        int qtdExtras = (indiceItemEditado == -1 && chkRecorrencia != null && chkRecorrencia.isSelected()) ? (cbQtdDoses.getSelectedIndex() + 1) : 0;
        long totalRequerido = 1 + qtdExtras + qtdMesmaVacinaNovaNoCarrinho;

        boolean isItemDB = (indiceItemEditado != -1 && itensCarrinho.get(indiceItemEditado).getId() > 0);
        if (isItemDB && itensCarrinho.get(indiceItemEditado).getVacina().getId() == v.getId()) totalRequerido--;

        if (v.getQtdDisponivel() - totalRequerido < 0) { JOptionPane.showMessageDialog(this, "ESTOQUE INSUFICIENTE!\n\nVocê tem apenas " + v.getQtdDisponivel() + " doses desta vacina na geladeira.", "Estoque Esgotado", JOptionPane.ERROR_MESSAGE); return; }

        Paciente pAlvo = listaPacientesCache.get(cbPacientePrincipal.getSelectedIndex() - 1);
        if (chkFamilia != null && chkFamilia.isSelected() && cbFamiliar.getSelectedIndex() > 0) pAlvo = listaFamiliaresAtuais.get(cbFamiliar.getSelectedIndex() - 1);

        String status = cbStatus.getSelectedItem().toString();

        if (indiceItemEditado == -1) {
            Aplicacao app = new Aplicacao(); app.setPaciente(pAlvo); app.setVacina(v); app.setDataHora(LocalDateTime.of(dataDef, horaDefinitiva)); app.setStatus(status);
            app.setValor(v.getValorVenda());
            app.setValorBruto(v.getValorVenda());
            itensCarrinho.add(app); adicionarLinhaTabela(app);

            if (chkRecorrencia != null && chkRecorrencia.isSelected()) {
                String intervaloTexto = txtIntervaloDias.getText().replaceAll("[^0-9]", "");
                if (intervaloTexto.isEmpty()) { JOptionPane.showMessageDialog(this, "Informe o intervalo em dias da recorrência."); return; }
                int dias = Integer.parseInt(intervaloTexto);
                for (int i = 1; i <= qtdExtras; i++) {
                    LocalDate dataFutura = dataDef.plusDays((long) dias * i); if (dataFutura.getDayOfWeek() == DayOfWeek.SUNDAY) dataFutura = dataFutura.plusDays(1);
                    Aplicacao appRec = new Aplicacao(); appRec.setPaciente(pAlvo); appRec.setVacina(v); appRec.setDataHora(LocalDateTime.of(dataFutura, horaDefinitiva)); appRec.setStatus("Agendado");
                    appRec.setValor(v.getValorVenda());
                    appRec.setValorBruto(v.getValorVenda());
                    itensCarrinho.add(appRec); adicionarLinhaTabela(appRec);
                }
            }
        } else {
            Aplicacao app = itensCarrinho.get(indiceItemEditado);
            double valAntigo = app.getValorBruto() > 0 ? app.getValorBruto() : app.getValor();
            valorBrutoCarrinho -= valAntigo;

            app.setPaciente(pAlvo); app.setVacina(v); app.setDataHora(LocalDateTime.of(dataDef, horaDefinitiva)); app.setStatus(status);
            app.setValor(v.getValorVenda());
            app.setValorBruto(v.getValorVenda());

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
        double valorBase = app.getValorBruto() > 0 ? app.getValorBruto() : app.getValor();
        valorBrutoCarrinho += valorBase;
        modeloCarrinho.addRow(new Object[]{ app.getPaciente().getNome().split(" ")[0], app.getVacina().getNomeVacina(), app.getDataHora().format(DateTimeFormatter.ofPattern("dd/MM/yy")), app.getStatus(), String.format("R$ %.2f", valorBase) });
    }

    private void removerItemCarrinho() {
        int row = tabelaCarrinho.getSelectedRow();
        if (row >= 0 && indiceItemEditado == -1) {
            double val = itensCarrinho.get(row).getValorBruto() > 0 ? itensCarrinho.get(row).getValorBruto() : itensCarrinho.get(row).getValor();
            valorBrutoCarrinho -= val;
            itensCarrinho.remove(row); modeloCarrinho.removeRow(row); atualizarFinanceiro();
        }
        else if (row >= 0 && indiceItemEditado != -1) { JOptionPane.showMessageDialog(this, "Não é possível remover enquanto o item está sendo editado. Clique em 'Novo Item' para cancelar a edição primeiro.", "Aviso", JOptionPane.WARNING_MESSAGE); }
        else { JOptionPane.showMessageDialog(this, "Selecione um item na tabela para remover.", "Aviso", JOptionPane.WARNING_MESSAGE); }
    }

    private void atualizarFinanceiro() {
        try {
            String dTexto = txtDesconto.getText().replace(",", "."); if (dTexto.isEmpty()) dTexto = "0"; double desc = Double.parseDouble(dTexto);
            double total = rbPorcentagem.isSelected() ? valorBrutoCarrinho - (valorBrutoCarrinho * (desc / 100)) : valorBrutoCarrinho - desc;
            txtValorTotal.setText(String.format(new Locale("pt", "BR"), "R$ %,.2f", Math.max(0.00, total)));
        } catch (Exception e) { txtValorTotal.setText(String.format("R$ %.2f", valorBrutoCarrinho)); }
    }

    private void finalizarAtendimento() {
        if (itensCarrinho.isEmpty()) { JOptionPane.showMessageDialog(this, "O carrinho está vazio.", "Atenção", JOptionPane.WARNING_MESSAGE); return; }
        String pagamento = cbPagamento.getSelectedItem().toString();

        double totalFinal = Double.parseDouble(txtValorTotal.getText().replaceAll("[^0-9,]", "").replace(",", ".")) ;
        double proporcao = valorBrutoCarrinho > 0 ? totalFinal / valorBrutoCarrinho : 1.0;

        List<Aplicacao> novasApps = new ArrayList<>();
        boolean sucesso = true; AplicacaoDAO appDao = new AplicacaoDAO();

        for (Aplicacao a : itensCarrinho) {
            a.setFormaPagamento(a.getStatus().equals("Agendado") ? "Pendente" : pagamento);
            if(a.getStatus().equals("Aplicado") && pagamento.equals("Pendente")) { JOptionPane.showMessageDialog(this, "Status 'Aplicado' não pode ficar com pagamento 'Pendente'."); return; }

            double vBrutoOriginal = a.getValorBruto() > 0 ? a.getValorBruto() : a.getValor();
            a.setValorBruto(vBrutoOriginal);
            a.setValor(vBrutoOriginal * proporcao);
            a.setDesconto(vBrutoOriginal - a.getValor());

            if (a.getId() > 0) { if (!appDao.atualizar(a)) sucesso = false; } else { novasApps.add(a); }
        }

        if (!novasApps.isEmpty()) { if (!appDao.salvarEmLote(novasApps)) sucesso = false; }

        if (sucesso) { JOptionPane.showMessageDialog(this, "Atendimento processado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE); frame.trocarTelaCentral(new PainelAplicacoes(frame)); }
        else { JOptionPane.showMessageDialog(this, "Ocorreu um erro no banco de dados.", "Erro", JOptionPane.ERROR_MESSAGE); }
    }

    private void configurarCombo(JComboBox<?> cb) { cb.setPreferredSize(new Dimension(0, 46)); cb.setFont(new Font("Segoe UI", Font.PLAIN, 15)); cb.setBackground(new Color(250, 250, 250)); cb.putClientProperty("JComponent.roundRect", true); }
    private void configurarCampo(JComponent c) { c.setPreferredSize(new Dimension(0, 46)); c.setFont(new Font("Segoe UI", Font.PLAIN, 15)); c.setBackground(new Color(250, 250, 250)); c.putClientProperty("JComponent.roundRect", true); c.putClientProperty("JTextField.padding", new Insets(5, 10, 5, 10)); }

    private JPanel montarBloco(String titulo, String iconeSvg, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 8)); p.setOpaque(false);
        JLabel l = new JLabel(titulo); l.setFont(new Font("Segoe UI", Font.BOLD, 12)); l.setForeground(new Color(130, 140, 150));
        if (iconeSvg != null) { l.setIcon(carregarIcone(iconeSvg, 16, Cores.VERDE_AQUA)); l.setIconTextGap(8); }
        mapLabels.put(comp, l); p.add(l, BorderLayout.NORTH); p.add(comp, BorderLayout.CENTER); return p;
    }

    private FlatSVGIcon carregarIcone(String nome, int tam, Color cor) { try { return (FlatSVGIcon) new FlatSVGIcon("icons/" + nome, tam, tam).setColorFilter(new FlatSVGIcon.ColorFilter(c -> cor)); } catch(Exception e) {return null;} }
}