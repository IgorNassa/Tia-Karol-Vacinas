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
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PainelFormulario extends JPanel {
    private TelaPrincipal frame;

    private JComboBox<String> cbPaciente, cbVacina, cbStatus, cbPagamento;
    private JFormattedTextField txtData, txtHora;
    private JTextField txtValorBase, txtDesconto, txtValorTotal;
    private JRadioButton rbPorcentagem, rbReais;

    // Campos de Recorrência
    private JCheckBox chkRecorrencia;
    private JComboBox<String> cbQtdDoses;
    private JTextField txtIntervaloDias;

    private List<Paciente> listaPacientesCache;
    private VacinaDAO vacinaDAO = new VacinaDAO();
    private double precoOriginal = 0.0;
    private Aplicacao aplicacaoEmEdicao = null;

    private Map<JComponent, JLabel> mapLabels = new HashMap<>();

    public PainelFormulario(TelaPrincipal frame) {
        this(frame, null);
    }

    public PainelFormulario(TelaPrincipal frame, Aplicacao appEdicao) {
        this.frame = frame;
        this.aplicacaoEmEdicao = appEdicao;
        setOpaque(false);
        setLayout(new GridBagLayout());

        GlassPanel cardVidro = new GlassPanel();
        cardVidro.setPreferredSize(new Dimension(1100, 750));
        cardVidro.setLayout(new GridBagLayout());
        cardVidro.setBorder(new EmptyBorder(30, 60, 30, 60));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // --- TOPO ---
        JPanel pnlTopo = new JPanel(new BorderLayout(0, 10));
        pnlTopo.setOpaque(false);
        pnlTopo.setBorder(new EmptyBorder(0, 0, 20, 0));

        JLabel btnVoltar = new JLabel(" Voltar");
        btnVoltar.setIcon(carregarIcone("seta-para-a-esquerda.svg", 16, Cores.VERDE_AQUA));
        btnVoltar.setForeground(Cores.VERDE_AQUA);
        btnVoltar.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnVoltar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnVoltar.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { frame.trocarTelaCentral(new PainelAplicacoes(frame)); }
        });

        JLabel lblTitulo = new JLabel(appEdicao == null ? "Registro de Aplicação" : "Editar Aplicação");
        lblTitulo.setFont(new Font("Segoe UI Semilight", Font.PLAIN, 36));
        lblTitulo.setForeground(Cores.CINZA_GRAFITE);

        pnlTopo.add(btnVoltar, BorderLayout.NORTH);
        pnlTopo.add(lblTitulo, BorderLayout.CENTER);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 30, 0);
        cardVidro.add(pnlTopo, gbc);

        // =========================================================
        // DADOS DO ATENDIMENTO
        // =========================================================
        cbPaciente = new JComboBox<>(); cbPaciente.addItem("Selecione o paciente...");
        listaPacientesCache = new PacienteDAO().listarTodos();
        for (Paciente p : listaPacientesCache) cbPaciente.addItem(p.getNome());
        configurarCombo(cbPaciente);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.6;
        gbc.insets = new Insets(0, 0, 20, 30);
        cardVidro.add(montarBloco("PACIENTE *", cbPaciente), gbc);

        // --- PAINEL DATA E HORA SEPARADOS ---
        JPanel pnlDataHora = new JPanel(new GridLayout(1, 2, 10, 0)); pnlDataHora.setOpaque(false);

        try {
            MaskFormatter maskData = new MaskFormatter("##/##/####"); maskData.setPlaceholderCharacter('_');
            txtData = new JFormattedTextField(maskData); txtData.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            configurarCampo(txtData);

            MaskFormatter maskHora = new MaskFormatter("##:##"); maskHora.setPlaceholderCharacter('_');
            txtHora = new JFormattedTextField(maskHora); txtHora.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
            configurarCampo(txtHora);
        } catch (Exception e) {}

        JPanel pnlDataCompleto = new JPanel(new BorderLayout(5, 0)); pnlDataCompleto.setOpaque(false);
        pnlDataCompleto.add(txtData, BorderLayout.CENTER);

        JButton btnAgora = new JButton("Hoje");
        btnAgora.setFont(new Font("Segoe UI", Font.BOLD, 12)); btnAgora.setBackground(new Color(245, 245, 245)); btnAgora.setForeground(Cores.CINZA_GRAFITE); btnAgora.setCursor(new Cursor(Cursor.HAND_CURSOR)); btnAgora.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(210, 210, 210), 1, true), new EmptyBorder(0, 10, 0, 10))); btnAgora.setFocusPainted(false);
        btnAgora.addActionListener(e -> {
            txtData.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            txtHora.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        });
        pnlDataCompleto.add(btnAgora, BorderLayout.EAST);

        pnlDataHora.add(montarBloco("DATA *", pnlDataCompleto, txtData));
        pnlDataHora.add(montarBloco("HORA (OPCIONAL)", txtHora));

        gbc.gridx = 1; gbc.weightx = 0.4; gbc.insets = new Insets(0, 0, 20, 0);
        cardVidro.add(pnlDataHora, gbc);

        // --- LINHA 2: VACINA E STATUS ---
        cbVacina = new JComboBox<>(); cbVacina.addItem("Selecione a vacina/lote...");
        for (String item : vacinaDAO.listarLotesParaCombo()) {
            if (item != null && !item.toLowerCase().contains("selecione")) cbVacina.addItem(item);
        }
        configurarCombo(cbVacina);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.6; gbc.insets = new Insets(0, 0, 20, 30);
        cardVidro.add(montarBloco("VACINA (LOTE ATUAL) *", cbVacina), gbc);

        cbStatus = new JComboBox<>(new String[]{"Aplicado", "Agendado"}); configurarCombo(cbStatus);
        gbc.gridx = 1; gbc.weightx = 0.4; gbc.insets = new Insets(0, 0, 20, 0);
        cardVidro.add(montarBloco("STATUS DA APLICAÇÃO", cbStatus), gbc);

        // =========================================================
        // NOVO: PAINEL DE RECORRÊNCIA (SÓ APARECE SE FOR NOVA APLICAÇÃO)
        // =========================================================
        if (appEdicao == null) {
            JPanel pnlRecorrencia = new JPanel(new BorderLayout()); pnlRecorrencia.setOpaque(false);
            pnlRecorrencia.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(new LineBorder(new Color(210, 210, 210)), " ESQUEMA VACINAL ", 0, 0, new Font("Segoe UI", Font.BOLD, 12), Cores.CINZA_LABEL),
                    new EmptyBorder(10, 15, 15, 15)
            ));

            chkRecorrencia = new JCheckBox("Gerar agendamento automático para as próximas doses");
            chkRecorrencia.setFont(new Font("Segoe UI", Font.BOLD, 14)); chkRecorrencia.setForeground(Cores.VERDE_AQUA); chkRecorrencia.setOpaque(false); chkRecorrencia.setCursor(new Cursor(Cursor.HAND_CURSOR));

            JPanel pnlDetalhesRec = new JPanel(new GridLayout(1, 2, 20, 0)); pnlDetalhesRec.setOpaque(false);
            cbQtdDoses = new JComboBox<>(new String[]{"1 Dose extra", "2 Doses extras", "3 Doses extras", "4 Doses extras", "5 Doses extras", "6 Doses extras", "7 Doses extras", "8 Doses extras"});
            txtIntervaloDias = new JTextField(); txtIntervaloDias.putClientProperty("JTextField.placeholderText", "Ex: 30"); configurarCampo(txtIntervaloDias);

            pnlDetalhesRec.add(montarBloco("QUANTIDADE", cbQtdDoses));
            pnlDetalhesRec.add(montarBloco("INTERVALO (DIAS) *", txtIntervaloDias));

            pnlDetalhesRec.setVisible(false); // Esconde por padrão

            chkRecorrencia.addActionListener(e -> pnlDetalhesRec.setVisible(chkRecorrencia.isSelected()));

            pnlRecorrencia.add(chkRecorrencia, BorderLayout.NORTH);
            pnlRecorrencia.add(pnlDetalhesRec, BorderLayout.CENTER);

            gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.insets = new Insets(0, 0, 20, 0);
            cardVidro.add(pnlRecorrencia, gbc);
        }

        // =========================================================
        // FINANCEIRO E PAGAMENTO
        // =========================================================
        JPanel pnlFinBorder = new JPanel(new BorderLayout()); pnlFinBorder.setOpaque(false);
        pnlFinBorder.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(new LineBorder(new Color(220, 220, 220)), " ACERTO FINANCEIRO ", 0, 0, new Font("Segoe UI", Font.BOLD, 12), Cores.CINZA_LABEL),
                new EmptyBorder(15, 20, 20, 20)
        ));

        JPanel gridFin = new JPanel(new GridLayout(1, 4, 20, 0)); gridFin.setOpaque(false);

        txtValorBase = criarCampoMoeda(false); gridFin.add(montarBloco("VALOR TABELA", txtValorBase));

        JPanel pnlDescComplex = new JPanel(new BorderLayout(0, 5)); pnlDescComplex.setOpaque(false);
        txtDesconto = new JTextField("0"); configurarCampo(txtDesconto);
        JPanel pnlRadios = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)); pnlRadios.setOpaque(false);
        rbPorcentagem = new JRadioButton("%"); rbReais = new JRadioButton("R$");
        rbPorcentagem.setSelected(true); rbPorcentagem.setOpaque(false); rbReais.setOpaque(false);
        ButtonGroup group = new ButtonGroup(); group.add(rbPorcentagem); group.add(rbReais);
        pnlRadios.add(rbPorcentagem); pnlRadios.add(rbReais);
        pnlDescComplex.add(txtDesconto, BorderLayout.CENTER); pnlDescComplex.add(pnlRadios, BorderLayout.SOUTH);
        gridFin.add(montarBloco("DESCONTO", pnlDescComplex));

        txtValorTotal = criarCampoMoeda(false); txtValorTotal.setFont(new Font("Segoe UI", Font.BOLD, 22)); txtValorTotal.setForeground(new Color(39, 174, 96));
        gridFin.add(montarBloco("TOTAL FINAL", txtValorTotal));

        cbPagamento = new JComboBox<>(new String[]{"Pendente", "PIX", "Cartão de Crédito", "Cartão de Débito", "Dinheiro"}); configurarCombo(cbPagamento);
        gridFin.add(montarBloco("FORMA PAGAMENTO", cbPagamento));

        pnlFinBorder.add(gridFin, BorderLayout.CENTER);

        gbc.gridy = 4; gbc.gridx = 0; gbc.gridwidth = 2; gbc.insets = new Insets(10, 0, 30, 0);
        cardVidro.add(pnlFinBorder, gbc);

        // =========================================================
        // PREENCHIMENTO SE FOR EDIÇÃO
        // =========================================================
        if (appEdicao != null) {
            cbPaciente.setSelectedItem(appEdicao.getPaciente().getNome());
            for(int i=0; i<cbVacina.getItemCount(); i++) {
                if (cbVacina.getItemAt(i).contains(appEdicao.getVacina().getLote())) { cbVacina.setSelectedIndex(i); break; }
            }
            txtData.setText(appEdicao.getDataHora().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            txtHora.setText(appEdicao.getDataHora().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            cbStatus.setSelectedItem(appEdicao.getStatus());
            cbPagamento.setSelectedItem(appEdicao.getFormaPagamento());
            txtValorTotal.setText(String.format(new Locale("pt", "BR"), "R$ %,.2f", appEdicao.getValor()));
        }

        // =========================================================
        // BOTÃO GIGANTE SALVAR
        // =========================================================
        JButton btnSalvar = new JButton(appEdicao == null ? " Confirmar e Finalizar Registro" : " Salvar Alterações");
        btnSalvar.setIcon(carregarIcone("disco.svg", 24, Color.WHITE)); btnSalvar.setPreferredSize(new Dimension(0, 80));
        btnSalvar.setBackground(appEdicao == null ? Cores.VERDE_AQUA : Cores.ROSA_KAROL); btnSalvar.setForeground(Color.WHITE);
        btnSalvar.setFont(new Font("Segoe UI", Font.BOLD, 20)); btnSalvar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSalvar.addActionListener(e -> salvarRegistro());

        gbc.gridy = 5; gbc.gridwidth = 2; gbc.insets = new Insets(0, 0, 0, 0);
        cardVidro.add(btnSalvar, gbc);

        cbVacina.addActionListener(e -> {
            if (cbVacina.getSelectedIndex() > 0) {
                Vacina v = vacinaDAO.buscarPorLoteCombo(cbVacina.getSelectedItem().toString());
                if (v != null) { precoOriginal = v.getValorVenda(); txtValorBase.setText(String.format("R$ %.2f", precoOriginal)); calcularTotal(); }
            }
        });

        txtDesconto.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { calcularTotal(); } });
        rbPorcentagem.addActionListener(e -> calcularTotal()); rbReais.addActionListener(e -> calcularTotal());
        add(cardVidro);
    }

    private void calcularTotal() {
        try {
            String dTexto = txtDesconto.getText().replace(",", "."); if (dTexto.isEmpty()) dTexto = "0"; double desc = Double.parseDouble(dTexto);
            double total = rbPorcentagem.isSelected() ? precoOriginal - (precoOriginal * (desc / 100)) : precoOriginal - desc;
            txtValorTotal.setText(String.format(new Locale("pt", "BR"), "R$ %,.2f", Math.max(0.01, total)));
        } catch (Exception e) { txtValorTotal.setText(String.format("R$ %.2f", precoOriginal)); }
    }

    private void salvarRegistro() {
        if (cbPaciente.getSelectedIndex() <= 0 || cbVacina.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Selecione Paciente e Vacina!", "Erro", JOptionPane.ERROR_MESSAGE); return;
        }

        // --- VALIDAÇÃO DE DATA E HORA ---
        String dataStr = txtData.getText().replaceAll("[^0-9]", "");
        if (dataStr.length() < 8) { JOptionPane.showMessageDialog(this, "Informe uma Data válida.", "Erro", JOptionPane.ERROR_MESSAGE); return; }

        String horaStr = txtHora.getText().replaceAll("[^0-9]", "");
        LocalTime horaDefinitiva = LocalTime.of(8, 0); // Padrão se não preencher
        if (horaStr.length() == 4) {
            try { horaDefinitiva = LocalTime.parse(txtHora.getText(), DateTimeFormatter.ofPattern("HH:mm")); }
            catch (Exception ex) { JOptionPane.showMessageDialog(this, "Hora inválida.", "Erro", JOptionPane.ERROR_MESSAGE); return; }
        }

        String statusSelecionado = cbStatus.getSelectedItem().toString();
        String pagamentoSelecionado = cbPagamento.getSelectedItem().toString();

        if (statusSelecionado.equals("Aplicado") && pagamentoSelecionado.equals("Pendente")) {
            JOptionPane.showMessageDialog(this, "A aplicação não pode ser salva como Aplicado com o pagamento Pendente.", "Atenção", JOptionPane.WARNING_MESSAGE); return;
        }

        try {
            Paciente p = listaPacientesCache.get(cbPaciente.getSelectedIndex() - 1);
            Vacina v = vacinaDAO.buscarPorLoteCombo(cbVacina.getSelectedItem().toString());
            LocalDate dataDefinitiva = LocalDate.parse(txtData.getText(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            LocalDateTime dataHoraFinal = LocalDateTime.of(dataDefinitiva, horaDefinitiva);

            double valorFinal = Double.parseDouble(txtValorTotal.getText().replaceAll("[^0-9,]", "").replace(",", ".")) ;

            // SALVANDO A DOSE PRINCIPAL
            Aplicacao app = aplicacaoEmEdicao == null ? new Aplicacao() : aplicacaoEmEdicao;
            app.setPaciente(p);
            app.setVacina(v);
            app.setDataHora(dataHoraFinal);
            app.setStatus(statusSelecionado);
            app.setFormaPagamento(pagamentoSelecionado);
            app.setValor(valorFinal);
            if(aplicacaoEmEdicao == null){ app.setReacoes(""); app.setObservacoesAdicionais(""); }

            if (aplicacaoEmEdicao == null) new AplicacaoDAO().salvar(app);
            else new AplicacaoDAO().atualizar(app);

            // =====================================================================
            // A MÁGICA DA RECORRÊNCIA E DO ALGORITMO DE FIM DE SEMANA
            // =====================================================================
            if (aplicacaoEmEdicao == null && chkRecorrencia != null && chkRecorrencia.isSelected()) {

                String intervaloTexto = txtIntervaloDias.getText().replaceAll("[^0-9]", "");
                if (intervaloTexto.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Você ativou as múltiplas doses, mas não informou o intervalo em dias.", "Atenção", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                int qtdDosesExtras = cbQtdDoses.getSelectedIndex() + 1;
                int intervaloDias = Integer.parseInt(intervaloTexto);

                for (int i = 1; i <= qtdDosesExtras; i++) {
                    // Calcula a data pura
                    LocalDate dataFutura = dataDefinitiva.plusDays((long) intervaloDias * i);

                    // ALGORITMO PULA FINAL DE SEMANA
                    if (dataFutura.getDayOfWeek() == DayOfWeek.SATURDAY) {
                        dataFutura = dataFutura.plusDays(2); // Vai pra Segunda
                    } else if (dataFutura.getDayOfWeek() == DayOfWeek.SUNDAY) {
                        dataFutura = dataFutura.plusDays(1); // Vai pra Segunda
                    }

                    // Cria e salva o agendamento
                    Aplicacao agendamentoFuturo = new Aplicacao();
                    agendamentoFuturo.setPaciente(p);
                    agendamentoFuturo.setVacina(v);
                    agendamentoFuturo.setDataHora(LocalDateTime.of(dataFutura, horaDefinitiva));
                    agendamentoFuturo.setStatus("Agendado");
                    agendamentoFuturo.setFormaPagamento("Pendente");
                    agendamentoFuturo.setValor(valorFinal);
                    agendamentoFuturo.setReacoes("");
                    agendamentoFuturo.setObservacoesAdicionais("");

                    new AplicacaoDAO().salvar(agendamentoFuturo);
                }
            }

            JOptionPane.showMessageDialog(this, "Registro salvo com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            frame.trocarTelaCentral(new PainelAplicacoes(frame));

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao processar os dados. Verifique as datas e valores.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void configurarCombo(JComboBox<?> cb) { cb.setPreferredSize(new Dimension(0, 50)); cb.setFont(new Font("Segoe UI", Font.PLAIN, 16)); cb.setBackground(Color.WHITE); cb.putClientProperty("JComponent.outline", null); }
    private void configurarCampo(JComponent c) { c.setPreferredSize(new Dimension(0, 50)); c.setFont(new Font("Segoe UI", Font.PLAIN, 16)); c.setBackground(Color.WHITE); }
    private JTextField criarCampoMoeda(boolean editavel) { JTextField f = new JTextField("R$ 0,00"); f.setEditable(editavel); f.setFocusable(editavel); f.setHorizontalAlignment(JTextField.RIGHT); configurarCampo(f); return f; }
    private JPanel montarBloco(String titulo, JComponent comp) { return montarBloco(titulo, comp, comp); }
    private JPanel montarBloco(String titulo, JComponent container, JComponent targetRef) { JPanel p = new JPanel(new BorderLayout(0, 8)); p.setOpaque(false); JLabel l = new JLabel(titulo); l.setFont(new Font("Segoe UI", Font.BOLD, 12)); l.setForeground(Cores.CINZA_LABEL); mapLabels.put(targetRef, l); targetRef.putClientProperty("tituloOriginal", titulo); p.add(l, BorderLayout.NORTH); p.add(container, BorderLayout.CENTER); return p; }
    private FlatSVGIcon carregarIcone(String nome, int tam, Color cor) { try { return (FlatSVGIcon) new FlatSVGIcon("icons/" + nome, tam, tam).setColorFilter(new FlatSVGIcon.ColorFilter(c -> cor)); } catch(Exception e) {return null;} }
}