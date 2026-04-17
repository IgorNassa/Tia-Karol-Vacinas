package br.sistema.view.panels;

import br.sistema.model.Aplicacao;
import br.sistema.model.Paciente;
import br.sistema.model.Vacina;
import br.sistema.repository.AplicacaoDAO;
import br.sistema.repository.PacienteDAO;
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

    // Carrinho
    private JTable tabelaCarrinho;
    private DefaultTableModel modeloCarrinho;
    private List<Aplicacao> itensCarrinho = new ArrayList<>();

    private List<Paciente> listaPacientesCache;
    private List<Paciente> listaFamiliaresAtuais = new ArrayList<>();
    private VacinaDAO vacinaDAO = new VacinaDAO();
    private double valorBrutoCarrinho = 0.0;
    private Aplicacao aplicacaoEmEdicao = null;

    private Map<JComponent, JLabel> mapLabels = new HashMap<>();

    public PainelFormulario(TelaPrincipal frame) { this(frame, null); }

    public PainelFormulario(TelaPrincipal frame, Aplicacao appEdicao) {
        this.frame = frame;
        this.aplicacaoEmEdicao = appEdicao;
        setOpaque(false); setLayout(new BorderLayout());

        JPanel pnlTopo = new JPanel(new BorderLayout(0, 10)); pnlTopo.setOpaque(false); pnlTopo.setBorder(new EmptyBorder(25, 50, 15, 40));
        JLabel btnVoltar = new JLabel(" Voltar");
        FlatSVGIcon iconeVoltar = carregarIcone("seta-para-a-esquerda.svg", 16, Cores.VERDE_AQUA);
        if(iconeVoltar != null) btnVoltar.setIcon(iconeVoltar);

        btnVoltar.setForeground(Cores.VERDE_AQUA); btnVoltar.setFont(new Font("Segoe UI", Font.BOLD, 15)); btnVoltar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnVoltar.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { frame.trocarTelaCentral(new PainelAplicacoes(frame)); } });
        JLabel lblTitulo = new JLabel(appEdicao == null ? "Novo Atendimento (Carrinho)" : "Editar Aplicação"); lblTitulo.setFont(new Font("Segoe UI Semilight", Font.PLAIN, 34)); lblTitulo.setForeground(Cores.CINZA_GRAFITE);
        pnlTopo.add(btnVoltar, BorderLayout.NORTH); pnlTopo.add(lblTitulo, BorderLayout.CENTER);
        add(pnlTopo, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); splitPane.setOpaque(false); splitPane.setBorder(null); splitPane.setDividerSize(15); splitPane.setDividerLocation(540);

        GlassPanel pnlEsquerdo = new GlassPanel(); pnlEsquerdo.setLayout(new BoxLayout(pnlEsquerdo, BoxLayout.Y_AXIS)); pnlEsquerdo.setBorder(new EmptyBorder(30, 50, 30, 30));

        cbPacientePrincipal = new JComboBox<>(); cbPacientePrincipal.addItem("Selecione o paciente principal...");
        listaPacientesCache = new PacienteDAO().listarTodos();
        for (Paciente p : listaPacientesCache) cbPacientePrincipal.addItem(p.getNome());
        configurarCombo(cbPacientePrincipal);
        pnlEsquerdo.add(montarBloco("PACIENTE PRINCIPAL *", cbPacientePrincipal)); pnlEsquerdo.add(Box.createVerticalStrut(20));

        JPanel pnlFamilia = new JPanel(new BorderLayout(0, 8)); pnlFamilia.setOpaque(false);
        chkFamilia = new JCheckBox("Atendimento Múltiplo (Incluir Familiares)"); chkFamilia.setFont(new Font("Segoe UI", Font.BOLD, 13)); chkFamilia.setForeground(Cores.VERDE_AQUA); chkFamilia.setOpaque(false); chkFamilia.setVisible(false); chkFamilia.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cbFamiliar = new JComboBox<>(); cbFamiliar.addItem("Selecione o membro da família..."); configurarCombo(cbFamiliar); cbFamiliar.setVisible(false);
        pnlFamilia.add(chkFamilia, BorderLayout.NORTH); pnlFamilia.add(cbFamiliar, BorderLayout.CENTER);
        pnlEsquerdo.add(pnlFamilia); pnlEsquerdo.add(Box.createVerticalStrut(20));

        cbPacientePrincipal.addActionListener(e -> processarFamiliares());
        chkFamilia.addActionListener(e -> cbFamiliar.setVisible(chkFamilia.isSelected()));

        cbVacina = new JComboBox<>(); cbVacina.addItem("Selecione a vacina/lote...");
        for (String item : vacinaDAO.listarLotesParaCombo()) if (item != null && !item.toLowerCase().contains("selecione")) cbVacina.addItem(item);
        configurarCombo(cbVacina);
        pnlEsquerdo.add(montarBloco("VACINA *", cbVacina)); pnlEsquerdo.add(Box.createVerticalStrut(20));

        JPanel pnlDataHora = new JPanel(new GridLayout(1, 2, 15, 0)); pnlDataHora.setOpaque(false);
        try { MaskFormatter maskData = new MaskFormatter("##/##/####"); maskData.setPlaceholderCharacter('_'); txtData = new JFormattedTextField(maskData); txtData.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))); configurarCampo(txtData);
            MaskFormatter maskHora = new MaskFormatter("##:##"); maskHora.setPlaceholderCharacter('_'); txtHora = new JFormattedTextField(maskHora); txtHora.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))); configurarCampo(txtHora);
        } catch (Exception e) {}
        pnlDataHora.add(montarBloco("DATA *", txtData)); pnlDataHora.add(montarBloco("HORA (OPC. OU HOJE)", txtHora));
        pnlEsquerdo.add(pnlDataHora); pnlEsquerdo.add(Box.createVerticalStrut(20));

        cbStatus = new JComboBox<>(new String[]{"Aplicado", "Agendado"}); configurarCombo(cbStatus);
        pnlEsquerdo.add(montarBloco("STATUS DA DOSE ATUAL", cbStatus)); pnlEsquerdo.add(Box.createVerticalStrut(20));

        if (appEdicao == null) {
            JPanel pnlRec = new JPanel(new BorderLayout(0, 8)); pnlRec.setOpaque(false);
            chkRecorrencia = new JCheckBox("Gerar esquema vacinal (Doses extras)"); chkRecorrencia.setFont(new Font("Segoe UI", Font.BOLD, 13)); chkRecorrencia.setForeground(Cores.CINZA_GRAFITE); chkRecorrencia.setOpaque(false); chkRecorrencia.setCursor(new Cursor(Cursor.HAND_CURSOR));
            JPanel pnlDetRec = new JPanel(new GridLayout(1, 2, 15, 0)); pnlDetRec.setOpaque(false);
            cbQtdDoses = new JComboBox<>(new String[]{"1 Dose", "2 Doses", "3 Doses", "4 Doses", "5 Doses", "6 Doses", "7 Doses", "8 Doses", "9 Doses", "10 Doses"}); configurarCombo(cbQtdDoses);
            txtIntervaloDias = new JTextField(); txtIntervaloDias.putClientProperty("JTextField.placeholderText", "Ex: 30 dias"); configurarCampo(txtIntervaloDias);
            pnlDetRec.add(montarBloco("QTD EXTRAS", cbQtdDoses)); pnlDetRec.add(montarBloco("INTERVALO (DIAS)", txtIntervaloDias)); pnlDetRec.setVisible(false);
            chkRecorrencia.addActionListener(e -> pnlDetRec.setVisible(chkRecorrencia.isSelected()));
            pnlRec.add(chkRecorrencia, BorderLayout.NORTH); pnlRec.add(pnlDetRec, BorderLayout.CENTER);
            pnlEsquerdo.add(pnlRec); pnlEsquerdo.add(Box.createVerticalStrut(30));
        }

        JButton btnAdicionarItem = new JButton(appEdicao == null ? " + Adicionar ao Carrinho" : " ✔ Atualizar Dados");
        btnAdicionarItem.putClientProperty("JButton.buttonType", "roundRect");
        btnAdicionarItem.setPreferredSize(new Dimension(0, 48)); btnAdicionarItem.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        btnAdicionarItem.setBackground(Cores.VERDE_AQUA); btnAdicionarItem.setForeground(Color.WHITE); btnAdicionarItem.setFont(new Font("Segoe UI", Font.BOLD, 15)); btnAdicionarItem.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAdicionarItem.addActionListener(e -> adicionarItemCarrinho());
        pnlEsquerdo.add(btnAdicionarItem);

        splitPane.setLeftComponent(pnlEsquerdo);

        // DIREITO
        JPanel pnlDireitoWrapper = new JPanel(new BorderLayout(0, 20)); pnlDireitoWrapper.setOpaque(false); pnlDireitoWrapper.setBorder(new EmptyBorder(0, 10, 0, 40));
        GlassPanel pnlTabela = new GlassPanel(); pnlTabela.setLayout(new BorderLayout()); pnlTabela.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(225, 225, 225), 1, true), new EmptyBorder(15, 15, 15, 15)));
        modeloCarrinho = new DefaultTableModel(new Object[]{"Paciente", "Vacina", "Data", "Status", "Valor"}, 0) { public boolean isCellEditable(int row, int column) { return false; } };
        tabelaCarrinho = new JTable(modeloCarrinho); tabelaCarrinho.setRowHeight(38); tabelaCarrinho.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tabelaCarrinho.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13)); tabelaCarrinho.getTableHeader().setBackground(new Color(245, 248, 248)); tabelaCarrinho.getTableHeader().setForeground(Cores.CINZA_GRAFITE);
        tabelaCarrinho.setSelectionBackground(new Color(230, 245, 245));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer(); centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        tabelaCarrinho.getColumnModel().getColumn(2).setCellRenderer(centerRenderer); tabelaCarrinho.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        JLabel lblTituloResumo = new JLabel(" RESUMO DO ATENDIMENTO"); lblTituloResumo.setFont(new Font("Segoe UI", Font.BOLD, 14)); lblTituloResumo.setForeground(Cores.CINZA_GRAFITE); lblTituloResumo.setBorder(new EmptyBorder(0, 0, 10, 0));
        pnlTabela.add(lblTituloResumo, BorderLayout.NORTH);

        JScrollPane scrollTabela = new JScrollPane(tabelaCarrinho); scrollTabela.setBorder(new LineBorder(new Color(230, 230, 230)));
        pnlTabela.add(scrollTabela, BorderLayout.CENTER);

        JButton btnRemover = new JButton("Remover Selecionado"); btnRemover.putClientProperty("JButton.buttonType", "roundRect");
        btnRemover.setForeground(Cores.ROSA_KAROL); btnRemover.setBackground(new Color(255, 240, 240)); btnRemover.setFont(new Font("Segoe UI", Font.BOLD, 13)); btnRemover.setCursor(new Cursor(Cursor.HAND_CURSOR)); btnRemover.setFocusPainted(false);
        JPanel pnlRemover = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 10)); pnlRemover.setOpaque(false); pnlRemover.add(btnRemover);
        btnRemover.addActionListener(e -> removerItemCarrinho());
        pnlTabela.add(pnlRemover, BorderLayout.SOUTH);
        pnlDireitoWrapper.add(pnlTabela, BorderLayout.CENTER);

        GlassPanel pnlFinanceiro = new GlassPanel(); pnlFinanceiro.setLayout(new BorderLayout(15, 15)); pnlFinanceiro.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(225, 225, 225), 1, true), new EmptyBorder(20, 25, 20, 25)));

        JPanel pnlDesconto = new JPanel(new BorderLayout(5, 0)); pnlDesconto.setOpaque(false);
        txtDesconto = new JTextField("0"); configurarCampo(txtDesconto);
        JPanel pnlRadios = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0)); pnlRadios.setOpaque(false);
        rbPorcentagem = new JRadioButton("%"); rbReais = new JRadioButton("R$"); rbPorcentagem.setSelected(true); rbPorcentagem.setOpaque(false); rbReais.setOpaque(false);
        ButtonGroup group = new ButtonGroup(); group.add(rbPorcentagem); group.add(rbReais); pnlRadios.add(rbPorcentagem); pnlRadios.add(rbReais);
        pnlDesconto.add(txtDesconto, BorderLayout.CENTER); pnlDesconto.add(pnlRadios, BorderLayout.EAST);

        cbPagamento = new JComboBox<>(new String[]{"Pendente", "PIX", "Cartão de Crédito", "Cartão de Débito", "Dinheiro"}); configurarCombo(cbPagamento);
        txtValorTotal = new JTextField("R$ 0,00"); txtValorTotal.setEditable(false); txtValorTotal.setFont(new Font("Segoe UI", Font.BOLD, 32)); txtValorTotal.setForeground(new Color(39, 174, 96)); txtValorTotal.setHorizontalAlignment(JTextField.RIGHT); txtValorTotal.setBorder(null); txtValorTotal.setOpaque(false);

        JPanel pnlGridFin = new JPanel(new GridLayout(1, 2, 25, 0)); pnlGridFin.setOpaque(false);
        pnlGridFin.add(montarBloco("DESCONTO NO TOTAL", pnlDesconto)); pnlGridFin.add(montarBloco("FORMA DE PAGAMENTO", cbPagamento));

        JPanel pnlTotalFinal = new JPanel(new BorderLayout()); pnlTotalFinal.setOpaque(false); pnlTotalFinal.setBorder(new EmptyBorder(10, 0, 0, 0));
        JLabel lblTot = new JLabel("TOTAL A PAGAR:"); lblTot.setFont(new Font("Segoe UI", Font.BOLD, 16)); lblTot.setForeground(Cores.CINZA_GRAFITE);
        pnlTotalFinal.add(lblTot, BorderLayout.WEST); pnlTotalFinal.add(txtValorTotal, BorderLayout.EAST);

        pnlFinanceiro.add(pnlGridFin, BorderLayout.NORTH); pnlFinanceiro.add(pnlTotalFinal, BorderLayout.SOUTH);

        JButton btnSalvarTudo = new JButton(appEdicao == null ? " Confirmar e Finalizar Atendimento" : " Salvar Alteração");
        btnSalvarTudo.putClientProperty("JButton.buttonType", "roundRect");
        FlatSVGIcon iconeDisco = carregarIcone("disco.svg", 24, Color.WHITE);
        if(iconeDisco != null) btnSalvarTudo.setIcon(iconeDisco);

        btnSalvarTudo.setPreferredSize(new Dimension(0, 60)); btnSalvarTudo.setBackground(Cores.ROSA_KAROL); btnSalvarTudo.setForeground(Color.WHITE); btnSalvarTudo.setFont(new Font("Segoe UI", Font.BOLD, 18)); btnSalvarTudo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSalvarTudo.addActionListener(e -> finalizarAtendimento());

        JPanel pnlSulDir = new JPanel(new BorderLayout(0, 20)); pnlSulDir.setOpaque(false);
        pnlSulDir.add(pnlFinanceiro, BorderLayout.CENTER); pnlSulDir.add(btnSalvarTudo, BorderLayout.SOUTH);
        pnlDireitoWrapper.add(pnlSulDir, BorderLayout.SOUTH);

        splitPane.setRightComponent(pnlDireitoWrapper); add(splitPane, BorderLayout.CENTER);

        txtDesconto.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { atualizarFinanceiro(); } });
        rbPorcentagem.addActionListener(e -> atualizarFinanceiro()); rbReais.addActionListener(e -> atualizarFinanceiro());

        if (appEdicao != null) preencherParaEdicao();
    }

    private void processarFamiliares() {
        if (cbPacientePrincipal.getSelectedIndex() > 0) {
            Paciente p = listaPacientesCache.get(cbPacientePrincipal.getSelectedIndex() - 1);
            if ((p.getCpfResponsavel() != null && !p.getCpfResponsavel().isEmpty()) || (p.getCpfResponsavel2() != null && !p.getCpfResponsavel2().isEmpty()) || (p.getCpf() != null && !p.getCpf().isEmpty())) {
                listaFamiliaresAtuais = new PacienteDAO().buscarFamiliaresPorCpfResponsavel(p.getCpfResponsavel(), p.getCpfResponsavel2(), p.getId());
                if (!listaFamiliaresAtuais.isEmpty()) {
                    chkFamilia.setVisible(true); cbFamiliar.removeAllItems(); cbFamiliar.addItem("Selecione o membro da família...");
                    for (Paciente fam : listaFamiliaresAtuais) cbFamiliar.addItem(fam.getNome());
                    return;
                }
            }
        }
        chkFamilia.setVisible(false); chkFamilia.setSelected(false); cbFamiliar.setVisible(false);
    }

    private void adicionarItemCarrinho() {
        if (cbPacientePrincipal.getSelectedIndex() <= 0 || cbVacina.getSelectedIndex() <= 0) { JOptionPane.showMessageDialog(this, "Selecione o Paciente e a Vacina."); return; }
        String dataStr = txtData.getText().replaceAll("[^0-9]", ""); if (dataStr.length() < 8) { JOptionPane.showMessageDialog(this, "Data inválida."); return; }

        Vacina v = vacinaDAO.buscarPorLoteCombo(cbVacina.getSelectedItem().toString());

        // LÓGICA DE ESTOQUE PURA E SIMPLES
        long qtdNoCarrinho = itensCarrinho.stream().filter(i -> i.getVacina().getId() == v.getId()).count();
        int qtdExtras = (aplicacaoEmEdicao == null && chkRecorrencia != null && chkRecorrencia.isSelected()) ? (cbQtdDoses.getSelectedIndex() + 1) : 0;
        int totalTentandoAdicionar = 1 + qtdExtras;

        if (v.getQtdDisponivel() - qtdNoCarrinho - totalTentandoAdicionar < 0) {
            JOptionPane.showMessageDialog(this,
                    "ESTOQUE INSUFICIENTE!\n\nVocê tem apenas " + v.getQtdDisponivel() + " doses desta vacina no estoque.\nNão é possível adicionar mais doses do que o existente na geladeira.",
                    "Estoque Esgotado", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Paciente pAlvo = listaPacientesCache.get(cbPacientePrincipal.getSelectedIndex() - 1);
        if (chkFamilia.isSelected() && cbFamiliar.getSelectedIndex() > 0) pAlvo = listaFamiliaresAtuais.get(cbFamiliar.getSelectedIndex() - 1);

        LocalTime horaDefinitiva = LocalTime.of(8, 0);
        try { if (txtHora.getText().replaceAll("[^0-9]", "").length() == 4) horaDefinitiva = LocalTime.parse(txtHora.getText(), DateTimeFormatter.ofPattern("HH:mm")); } catch(Exception ex){}
        LocalDate dataDef = LocalDate.parse(txtData.getText(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String status = cbStatus.getSelectedItem().toString();

        if (aplicacaoEmEdicao != null) { itensCarrinho.clear(); modeloCarrinho.setRowCount(0); valorBrutoCarrinho = 0; }

        Aplicacao app = aplicacaoEmEdicao == null ? new Aplicacao() : aplicacaoEmEdicao;
        app.setPaciente(pAlvo); app.setVacina(v); app.setDataHora(LocalDateTime.of(dataDef, horaDefinitiva)); app.setStatus(status); app.setValor(v.getValorVenda());
        itensCarrinho.add(app); adicionarLinhaTabela(app);

        if (aplicacaoEmEdicao == null && chkRecorrencia != null && chkRecorrencia.isSelected()) {
            String intervaloTexto = txtIntervaloDias.getText().replaceAll("[^0-9]", "");
            if (intervaloTexto.isEmpty()) { JOptionPane.showMessageDialog(this, "Informe o intervalo em dias da recorrência."); return; }
            int qtd = cbQtdDoses.getSelectedIndex() + 1; int dias = Integer.parseInt(intervaloTexto);
            for (int i = 1; i <= qtd; i++) {
                LocalDate dataFutura = dataDef.plusDays((long) dias * i);
                if (dataFutura.getDayOfWeek() == DayOfWeek.SUNDAY) dataFutura = dataFutura.plusDays(1);
                Aplicacao appRec = new Aplicacao(); appRec.setPaciente(pAlvo); appRec.setVacina(v); appRec.setDataHora(LocalDateTime.of(dataFutura, horaDefinitiva)); appRec.setStatus("Agendado"); appRec.setValor(v.getValorVenda());
                itensCarrinho.add(appRec); adicionarLinhaTabela(appRec);
            }
        }
        atualizarFinanceiro(); cbVacina.setSelectedIndex(0);
    }

    private void adicionarLinhaTabela(Aplicacao app) {
        valorBrutoCarrinho += app.getValor();
        modeloCarrinho.addRow(new Object[]{ app.getPaciente().getNome().split(" ")[0], app.getVacina().getNomeVacina(), app.getDataHora().format(DateTimeFormatter.ofPattern("dd/MM/yy")), app.getStatus(), String.format("R$ %.2f", app.getValor()) });
    }

    private void removerItemCarrinho() {
        int row = tabelaCarrinho.getSelectedRow();
        if (row >= 0 && aplicacaoEmEdicao == null) {
            valorBrutoCarrinho -= itensCarrinho.get(row).getValor(); itensCarrinho.remove(row); modeloCarrinho.removeRow(row); atualizarFinanceiro();
        }
    }

    private void atualizarFinanceiro() {
        try {
            String dTexto = txtDesconto.getText().replace(",", "."); if (dTexto.isEmpty()) dTexto = "0"; double desc = Double.parseDouble(dTexto);
            double total = rbPorcentagem.isSelected() ? valorBrutoCarrinho - (valorBrutoCarrinho * (desc / 100)) : valorBrutoCarrinho - desc;
            txtValorTotal.setText(String.format(new Locale("pt", "BR"), "R$ %,.2f", Math.max(0.00, total)));
        } catch (Exception e) { txtValorTotal.setText(String.format("R$ %.2f", valorBrutoCarrinho)); }
    }

    private void finalizarAtendimento() {
        if (itensCarrinho.isEmpty()) { JOptionPane.showMessageDialog(this, "O carrinho está vazio. Adicione vacinas antes de finalizar.", "Atenção", JOptionPane.WARNING_MESSAGE); return; }
        String pagamento = cbPagamento.getSelectedItem().toString();

        double totalFinal = Double.parseDouble(txtValorTotal.getText().replaceAll("[^0-9,]", "").replace(",", ".")) ;
        double proporcao = valorBrutoCarrinho > 0 ? totalFinal / valorBrutoCarrinho : 1.0;

        for (Aplicacao a : itensCarrinho) {
            a.setFormaPagamento(a.getStatus().equals("Agendado") ? "Pendente" : pagamento);
            if(a.getStatus().equals("Aplicado") && pagamento.equals("Pendente")) { JOptionPane.showMessageDialog(this, "Não é possível salvar status 'Aplicado' com pagamento 'Pendente'."); return; }
            a.setValor(a.getValor() * proporcao);
        }

        if (aplicacaoEmEdicao == null) {
            if (new AplicacaoDAO().salvarEmLote(itensCarrinho)) { JOptionPane.showMessageDialog(this, "Atendimento finalizado e estoque atualizado!"); frame.trocarTelaCentral(new PainelAplicacoes(frame)); }
            else { JOptionPane.showMessageDialog(this, "Erro ao salvar no banco de dados."); }
        } else {
            if (new AplicacaoDAO().atualizar(itensCarrinho.get(0))) { JOptionPane.showMessageDialog(this, "Alteração salva!"); frame.trocarTelaCentral(new PainelAplicacoes(frame)); }
        }
    }

    private void preencherParaEdicao() {
        cbPacientePrincipal.setSelectedItem(aplicacaoEmEdicao.getPaciente().getNome());
        for(int i=0; i<cbVacina.getItemCount(); i++) if (cbVacina.getItemAt(i).contains(aplicacaoEmEdicao.getVacina().getLote())) { cbVacina.setSelectedIndex(i); break; }
        txtData.setText(aplicacaoEmEdicao.getDataHora().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        txtHora.setText(aplicacaoEmEdicao.getDataHora().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        cbStatus.setSelectedItem(aplicacaoEmEdicao.getStatus()); cbPagamento.setSelectedItem(aplicacaoEmEdicao.getFormaPagamento());
        adicionarItemCarrinho();
    }

    private void configurarCombo(JComboBox<?> cb) { cb.setPreferredSize(new Dimension(0, 40)); cb.setFont(new Font("Segoe UI", Font.PLAIN, 15)); cb.setBackground(Color.WHITE); cb.putClientProperty("JComponent.roundRect", true); cb.putClientProperty("JComponent.outline", null); }
    private void configurarCampo(JComponent c) { c.setPreferredSize(new Dimension(0, 40)); c.setFont(new Font("Segoe UI", Font.PLAIN, 15)); c.setBackground(Color.WHITE); c.putClientProperty("JComponent.roundRect", true); }
    private JPanel montarBloco(String titulo, JComponent comp) { JPanel p = new JPanel(new BorderLayout(0, 6)); p.setOpaque(false); JLabel l = new JLabel(titulo); l.setFont(new Font("Segoe UI", Font.BOLD, 12)); l.setForeground(Cores.CINZA_LABEL); mapLabels.put(comp, l); p.add(l, BorderLayout.NORTH); p.add(comp, BorderLayout.CENTER); return p; }
    private FlatSVGIcon carregarIcone(String nome, int tam, Color cor) { try { return (FlatSVGIcon) new FlatSVGIcon("icons/" + nome, tam, tam).setColorFilter(new FlatSVGIcon.ColorFilter(c -> cor)); } catch(Exception e) {return null;} }
}