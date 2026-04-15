package br.sistema.view.panels;

import br.sistema.model.Endereco;
import br.sistema.model.Paciente;
import br.sistema.repository.PacienteDAO;
import br.sistema.util.Cores;
import br.sistema.util.ServicoCEP;
import br.sistema.util.Validadores;
import br.sistema.view.TelaPrincipal;
import br.sistema.view.components.GlassPanel;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.HashMap;
import java.util.Map;

public class PainelFormularioPaciente extends JPanel {
    private TelaPrincipal frame;
    private CardLayout cardLayout;
    private JPanel pnlCards;
    private JButton btnNavPessoais, btnNavResponsavel, btnNavEndereco;

    private JTextField txtNome;
    private JFormattedTextField txtCpf;
    private JFormattedTextField txtDataNascimento;
    private JComboBox<String> cbSexo;
    private JTextField txtCartaoSus;
    private JFormattedTextField txtTelefone;
    private JFormattedTextField txtTelefone2;
    private JTextField txtAlergias;
    private JTextField txtMedicoEncaminhador;

    // RESPONSÁVEIS ATUALIZADOS
    private JTextField txtNomeResponsavel;
    private JFormattedTextField txtCpfResponsavel;
    private JTextField txtNomeResponsavel2;
    private JFormattedTextField txtCpfResponsavel2;

    private JFormattedTextField txtCep;
    private JTextField txtRua, txtNumero, txtComplemento, txtBairro, txtCidade, txtUf;
    private String codigoIbge = "";

    private PnlCircularAvatar pnlAvatar;
    private byte[] fotoBytes = null;
    private Map<JComponent, JLabel> mapLabels = new HashMap<>();
    private boolean isMenorDeIdade = false;
    private JCheckBox chkEstrangeiro;

    private Paciente pacienteEmEdicao = null;
    private JLabel lblTitulo;
    private JButton btnSalvar;

    public PainelFormularioPaciente(TelaPrincipal frame) {
        this.frame = frame; setOpaque(false); setLayout(new GridBagLayout());

        GlassPanel cardVidro = new GlassPanel(); cardVidro.setPreferredSize(new Dimension(950, 750));
        cardVidro.setLayout(new BorderLayout(0, 20)); cardVidro.setBorder(new EmptyBorder(30, 45, 30, 45));

        GridBagConstraints gbcMain = new GridBagConstraints(); gbcMain.anchor = GridBagConstraints.CENTER;
        add(cardVidro, gbcMain);

        // TOPO
        JPanel pnlTopo = new JPanel(new BorderLayout()); pnlTopo.setOpaque(false);
        JLabel btnVoltar = new JLabel(" Voltar"); btnVoltar.setIcon(carregarIcone("seta-para-a-esquerda.svg", 16, Cores.VERDE_AQUA));
        btnVoltar.setForeground(Cores.VERDE_AQUA); btnVoltar.setFont(new Font("Segoe UI", Font.BOLD, 15)); btnVoltar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnVoltar.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { frame.trocarTelaCentral(new PainelPacientes(frame)); } });
        pnlTopo.add(btnVoltar, BorderLayout.WEST);

        lblTitulo = new JLabel("Novo Paciente"); lblTitulo.setFont(new Font("Segoe UI Semilight", Font.PLAIN, 32)); lblTitulo.setForeground(Cores.ROSA_KAROL);
        pnlTopo.add(lblTitulo, BorderLayout.EAST); cardVidro.add(pnlTopo, BorderLayout.NORTH);

        // NAVBAR
        JPanel pnlCentro = new JPanel(new BorderLayout(0, 20)); pnlCentro.setOpaque(false);
        JPanel pnlNavbar = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 0)); pnlNavbar.setOpaque(false); pnlNavbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 210, 210)));
        btnNavPessoais = criarBotaoNav("Dados Pessoais", true); btnNavResponsavel = criarBotaoNav("Responsável Legal", false); btnNavEndereco = criarBotaoNav("Endereço", false);
        pnlNavbar.add(btnNavPessoais); pnlNavbar.add(btnNavResponsavel); pnlNavbar.add(btnNavEndereco);
        pnlCentro.add(pnlNavbar, BorderLayout.NORTH);

        cardLayout = new CardLayout(); pnlCards = new JPanel(cardLayout); pnlCards.setOpaque(false);
        pnlCards.add(criarAbaDadosPessoais(), "PESSOAIS"); pnlCards.add(criarAbaResponsavel(), "RESPONSAVEL"); pnlCards.add(criarAbaEndereco(), "ENDERECO");
        pnlCentro.add(pnlCards, BorderLayout.CENTER); cardVidro.add(pnlCentro, BorderLayout.CENTER);

        // RODAPÉ
        btnSalvar = new JButton(" Finalizar Cadastro"); btnSalvar.setIcon(carregarIcone("disco.svg", 20, Color.WHITE));
        btnSalvar.setPreferredSize(new Dimension(320, 55)); btnSalvar.setBackground(Cores.VERDE_AQUA); btnSalvar.setForeground(Color.WHITE); btnSalvar.setFont(new Font("Segoe UI", Font.BOLD, 16)); btnSalvar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSalvar.addActionListener(e -> salvarRegistro());
        JPanel pnlRodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); pnlRodape.setOpaque(false); pnlRodape.add(btnSalvar);
        cardVidro.add(pnlRodape, BorderLayout.SOUTH);

        btnNavPessoais.addActionListener(e -> trocarAba("PESSOAIS", btnNavPessoais)); btnNavResponsavel.addActionListener(e -> trocarAba("RESPONSAVEL", btnNavResponsavel)); btnNavEndereco.addActionListener(e -> trocarAba("ENDERECO", btnNavEndereco));
    }

    public PainelFormularioPaciente(TelaPrincipal frame, Paciente paciente) {
        this(frame);
        this.pacienteEmEdicao = paciente;

        lblTitulo.setText("Editar Paciente");
        btnSalvar.setText(" Salvar Alterações");
        btnSalvar.setBackground(Cores.ROSA_KAROL);

        preencherDadosParaEdicao();
    }

    private void preencherDadosParaEdicao() {
        boolean isEst = false;
        if (pacienteEmEdicao.getTelefone() != null && pacienteEmEdicao.getTelefone().startsWith("+")) isEst = true;
        if (pacienteEmEdicao.getCpf() != null && !pacienteEmEdicao.getCpf().isEmpty() && !pacienteEmEdicao.getCpf().matches("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}")) isEst = true;

        chkEstrangeiro.setSelected(isEst);
        atualizarObrigatoriedadesEstrangeiro();

        txtNome.setText(pacienteEmEdicao.getNome());
        txtCpf.setText(pacienteEmEdicao.getCpf());
        if (pacienteEmEdicao.getDataNascimento() != null) {
            txtDataNascimento.setText(pacienteEmEdicao.getDataNascimento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }
        cbSexo.setSelectedItem(pacienteEmEdicao.getSexo().isEmpty() ? "Selecione..." : pacienteEmEdicao.getSexo());
        txtCartaoSus.setText(pacienteEmEdicao.getCartaoSus());
        txtTelefone.setText(pacienteEmEdicao.getTelefone());
        txtTelefone2.setText(pacienteEmEdicao.getTelefone2());
        txtMedicoEncaminhador.setText(pacienteEmEdicao.getMedicoEncaminhador());
        txtAlergias.setText(pacienteEmEdicao.getAlergias());

        // CARREGA RESPONSÁVEIS
        txtNomeResponsavel.setText(pacienteEmEdicao.getNomeResponsavel());
        txtCpfResponsavel.setText(pacienteEmEdicao.getCpfResponsavel());
        txtNomeResponsavel2.setText(pacienteEmEdicao.getNomeResponsavel2());
        txtCpfResponsavel2.setText(pacienteEmEdicao.getCpfResponsavel2());

        Endereco end = pacienteEmEdicao.getEndereco();
        if (end != null) {
            txtCep.setText(end.getCep());
            txtRua.setText(end.getRua());
            txtNumero.setText(end.getNumero());
            txtComplemento.setText(end.getComplemento());
            txtBairro.setText(end.getBairro());
            txtCidade.setText(end.getCidade());
            txtUf.setText(end.getUf());
            codigoIbge = end.getCodigoIbge() != null ? end.getCodigoIbge() : "";
        }

        if (pacienteEmEdicao.getFoto() != null) {
            try {
                fotoBytes = pacienteEmEdicao.getFoto();
                Image img = new ImageIcon(fotoBytes).getImage();
                BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                Graphics2D bGr = bimage.createGraphics();
                bGr.drawImage(img, 0, 0, null);
                bGr.dispose();
                pnlAvatar.setImage(bimage);
            } catch (Exception e) {}
        }

        verificarIdadeEAdaptarUI();
    }

    private JPanel criarAbaDadosPessoais() {
        JPanel pnl = new JPanel(new GridBagLayout()); pnl.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints(); c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(8, 10, 15, 10);

        JPanel pnlFoto = new JPanel(); pnlFoto.setLayout(new BoxLayout(pnlFoto, BoxLayout.Y_AXIS)); pnlFoto.setOpaque(false);
        pnlAvatar = new PnlCircularAvatar(130); pnlAvatar.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton btnFoto = new JButton("Carregar Foto"); btnFoto.setAlignmentX(Component.CENTER_ALIGNMENT); btnFoto.setFont(new Font("Segoe UI", Font.BOLD, 11)); btnFoto.setBackground(new Color(235, 243, 243)); btnFoto.setForeground(Cores.VERDE_AQUA); btnFoto.setFocusPainted(false); btnFoto.setBorder(new EmptyBorder(6, 12, 6, 12)); btnFoto.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnFoto.addActionListener(e -> carregarFotoNativa());
        pnlFoto.add(pnlAvatar); pnlFoto.add(Box.createVerticalStrut(10)); pnlFoto.add(btnFoto);
        c.gridx = 0; c.gridy = 0; c.gridheight = 4; c.weightx = 0.0; pnl.add(pnlFoto, c);

        c.gridheight = 1; c.weightx = 1.0;

        JPanel pnlNome = new JPanel(new BorderLayout(0, 5)); pnlNome.setOpaque(false);

        chkEstrangeiro = new JCheckBox("Paciente Estrangeiro (Ignorar máscaras de CPF, CEP e Telefone)");
        chkEstrangeiro.setOpaque(false);
        chkEstrangeiro.setFont(new Font("Segoe UI", Font.BOLD, 12));
        chkEstrangeiro.setForeground(Cores.CINZA_GRAFITE);
        chkEstrangeiro.setCursor(new Cursor(Cursor.HAND_CURSOR));
        chkEstrangeiro.addActionListener(e -> atualizarObrigatoriedadesEstrangeiro());

        txtNome = criarCampoTexto("Nome completo do paciente");
        aplicarCapitalizacao(txtNome);
        adicionarValidacaoTempoReal(txtNome, true, "TEXTO");

        pnlNome.add(chkEstrangeiro, BorderLayout.NORTH);
        pnlNome.add(txtNome, BorderLayout.CENTER);

        c.gridx = 1; c.gridy = 0; c.gridwidth = 2; pnl.add(montarBloco("NOME COMPLETO *", pnlNome, txtNome), c);

        txtCpf = new JFormattedTextField(); aplicarMascara(txtCpf, "###.###.###-##"); configurarCampoFormatado(txtCpf); adicionarValidacaoTempoReal(txtCpf, false, "CPF");
        c.gridx = 1; c.gridy = 1; c.gridwidth = 1; pnl.add(montarBloco("CPF *", txtCpf), c);

        txtDataNascimento = new JFormattedTextField(); aplicarMascara(txtDataNascimento, "##/##/####"); configurarCampoFormatado(txtDataNascimento);
        txtDataNascimento.addFocusListener(new FocusAdapter() { public void focusLost(FocusEvent e) { verificarIdadeEAdaptarUI(); }});
        adicionarValidacaoTempoReal(txtDataNascimento, true, "DATA");
        c.gridx = 2; c.gridy = 1; pnl.add(montarBloco("DATA NASCIMENTO *", txtDataNascimento), c);

        cbSexo = new JComboBox<>(new String[]{"Selecione...", "Masculino", "Feminino", "Outro"}); cbSexo.setPreferredSize(new Dimension(0, 45)); cbSexo.setFont(new Font("Segoe UI", Font.PLAIN, 15)); cbSexo.setBackground(Color.WHITE);
        c.gridx = 1; c.gridy = 2; pnl.add(montarBloco("SEXO BIOLÓGICO", cbSexo), c);

        txtCartaoSus = criarCampoTexto("Nº CNS"); c.gridx = 2; c.gridy = 2; pnl.add(montarBloco("CARTÃO SUS", txtCartaoSus), c);

        txtTelefone = new JFormattedTextField(); aplicarMascara(txtTelefone, "(##) #####-####"); configurarCampoFormatado(txtTelefone); adicionarValidacaoTempoReal(txtTelefone, true, "TELEFONE");
        c.gridx = 1; c.gridy = 3; pnl.add(montarBloco("CELULAR PRINCIPAL *", txtTelefone), c);

        txtTelefone2 = new JFormattedTextField(); aplicarMascara(txtTelefone2, "(##) #####-####"); configurarCampoFormatado(txtTelefone2); adicionarValidacaoTempoReal(txtTelefone2, false, "TELEFONE");
        c.gridx = 2; c.gridy = 3; pnl.add(montarBloco("CELULAR SECUNDÁRIO", txtTelefone2), c);

        txtMedicoEncaminhador = criarCampoTexto("Dr(a). Solicitante"); aplicarCapitalizacao(txtMedicoEncaminhador);
        c.gridx = 0; c.gridy = 4; pnl.add(montarBloco("ENCAMINHAMENTO MÉDICO", txtMedicoEncaminhador), c);

        txtAlergias = criarCampoTexto("Ex: Dipirona..."); aplicarCapitalizacao(txtAlergias);
        c.gridx = 1; c.gridy = 4; c.gridwidth = 2; pnl.add(montarBloco("ALERGIAS", txtAlergias), c);

        c.gridy = 5; c.weighty = 1.0; pnl.add(Box.createVerticalGlue(), c);
        return pnl;
    }

    private JPanel criarAbaResponsavel() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 20)); pnl.setOpaque(false); pnl.setBorder(new EmptyBorder(10, 0, 0, 0));

        // Aumentei o Grid para suportar 4 campos em vez de 2
        JPanel grid = new JPanel(new GridLayout(4, 1, 0, 15)); grid.setOpaque(false); grid.setPreferredSize(new Dimension(550, 300));

        txtNomeResponsavel = criarCampoTexto("Mãe, Pai ou Responsável Principal");
        aplicarCapitalizacao(txtNomeResponsavel);
        adicionarValidacaoTempoReal(txtNomeResponsavel, false, "TEXTO");
        grid.add(montarBloco("NOME DO RESPONSÁVEL 1", txtNomeResponsavel));

        txtCpfResponsavel = new JFormattedTextField(); aplicarMascara(txtCpfResponsavel, "###.###.###-##"); configurarCampoFormatado(txtCpfResponsavel); adicionarValidacaoTempoReal(txtCpfResponsavel, false, "CPF");
        grid.add(montarBloco("CPF DO RESPONSÁVEL 1", txtCpfResponsavel));

        // NOVOS CAMPOS DO RESPONSÁVEL 2
        txtNomeResponsavel2 = criarCampoTexto("2º Responsável (Opcional)");
        aplicarCapitalizacao(txtNomeResponsavel2);
        grid.add(montarBloco("NOME DO RESPONSÁVEL 2 (OPCIONAL)", txtNomeResponsavel2));

        txtCpfResponsavel2 = new JFormattedTextField(); aplicarMascara(txtCpfResponsavel2, "###.###.###-##"); configurarCampoFormatado(txtCpfResponsavel2); adicionarValidacaoTempoReal(txtCpfResponsavel2, false, "CPF");
        grid.add(montarBloco("CPF DO RESPONSÁVEL 2", txtCpfResponsavel2));

        pnl.add(grid); return pnl;
    }

    private JPanel criarAbaEndereco() {
        JPanel pnl = new JPanel(new BorderLayout()); pnl.setOpaque(false);
        JPanel grid = new JPanel(new GridLayout(3, 1, 0, 15)); grid.setOpaque(false); grid.setBorder(new EmptyBorder(10, 0, 0, 0));
        JPanel linha1 = new JPanel(new GridLayout(1, 3, 20, 0)); linha1.setOpaque(false);
        txtCep = new JFormattedTextField(); aplicarMascara(txtCep, "#####-###"); configurarCampoFormatado(txtCep); adicionarValidacaoTempoReal(txtCep, true, "CEP");
        txtCep.addFocusListener(new FocusAdapter() { public void focusLost(FocusEvent evt) { buscarCepEPreencher(); } });
        linha1.add(montarBloco("CEP *", txtCep));
        txtRua = criarCampoTexto("Logradouro"); aplicarCapitalizacao(txtRua); linha1.add(montarBloco("RUA", txtRua));
        txtNumero = criarCampoTexto("Nº"); adicionarValidacaoTempoReal(txtNumero, true, "TEXTO"); linha1.add(montarBloco("NÚMERO *", txtNumero));
        grid.add(linha1);
        JPanel linha2 = new JPanel(new GridLayout(1, 3, 20, 0)); linha2.setOpaque(false);
        txtComplemento = criarCampoTexto("Apto..."); aplicarCapitalizacao(txtComplemento); linha2.add(montarBloco("COMPLEMENTO", txtComplemento));
        txtBairro = criarCampoTexto("Bairro"); aplicarCapitalizacao(txtBairro); linha2.add(montarBloco("BAIRRO", txtBairro));
        txtCidade = criarCampoTexto("Cidade"); aplicarCapitalizacao(txtCidade); linha2.add(montarBloco("CIDADE", txtCidade));
        grid.add(linha2);
        JPanel linha3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); linha3.setOpaque(false);
        txtUf = criarCampoTexto("UF"); txtUf.setPreferredSize(new Dimension(200, 45)); linha3.add(montarBloco("ESTADO (UF)", txtUf));
        grid.add(linha3); pnl.add(grid, BorderLayout.NORTH); return pnl;
    }

    private void aplicarCapitalizacao(JTextField campo) {
        campo.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                String t = campo.getText().trim();
                if (!t.isEmpty()) {
                    String[] palavras = t.toLowerCase().split("\\s+");
                    StringBuilder sb = new StringBuilder();
                    for (String p : palavras) {
                        if (p.length() > 2 || p.matches("^(dr|dra|sr|sra)$")) {
                            sb.append(p.substring(0, 1).toUpperCase()).append(p.substring(1));
                        } else {
                            sb.append(p);
                        }
                        sb.append(" ");
                    }
                    campo.setText(sb.toString().trim());
                }
            }
        });
    }

    private void atualizarObrigatoriedadesEstrangeiro() {
        boolean est = chkEstrangeiro.isSelected();
        JLabel lblCep = mapLabels.get(txtCep);
        JLabel lblNum = mapLabels.get(txtNumero);
        JLabel lblCpf = mapLabels.get(txtCpf);

        if (est) {
            if (lblCep != null) { lblCep.setText("ZIP CODE (OPCIONAL)"); txtCep.putClientProperty("tituloOriginal", "ZIP CODE (OPCIONAL)"); setErroComponente(txtCep, false, null); }
            if (lblNum != null) { lblNum.setText("NÚMERO (OPCIONAL)"); txtNumero.putClientProperty("tituloOriginal", "NÚMERO (OPCIONAL)"); setErroComponente(txtNumero, false, null); }
            if (lblCpf != null && !isMenorDeIdade) { lblCpf.setText("PASSAPORTE / ID"); txtCpf.putClientProperty("tituloOriginal", "PASSAPORTE / ID"); setErroComponente(txtCpf, false, null); }

            txtTelefone.setFormatterFactory(null);
            txtTelefone.putClientProperty("JTextField.placeholderText", "Ex: +54 9 11 1234...");
            if(limparMascara(txtTelefone.getText()).isEmpty()) txtTelefone.setText("");

            txtTelefone2.setFormatterFactory(null);
            txtTelefone2.putClientProperty("JTextField.placeholderText", "Ex: +1 555-0198");
            if(limparMascara(txtTelefone2.getText()).isEmpty()) txtTelefone2.setText("");

        } else {
            verificarIdadeEAdaptarUI();
            if (lblCep != null) { lblCep.setText("CEP *"); txtCep.putClientProperty("tituloOriginal", "CEP *"); validarCampo(txtCep, true, "CEP"); }
            if (lblNum != null) { lblNum.setText("NÚMERO *"); txtNumero.putClientProperty("tituloOriginal", "NÚMERO *"); validarCampo(txtNumero, true, "TEXTO"); }

            aplicarMascara(txtTelefone, "(##) #####-####");
            txtTelefone.putClientProperty("JTextField.placeholderText", "");
            if(limparMascara(txtTelefone.getText()).isEmpty()) txtTelefone.setText("");

            aplicarMascara(txtTelefone2, "(##) #####-####");
            txtTelefone2.putClientProperty("JTextField.placeholderText", "");
            if(limparMascara(txtTelefone2.getText()).isEmpty()) txtTelefone2.setText("");
        }
    }

    private void verificarIdadeEAdaptarUI() {
        if (isDataValida(txtDataNascimento.getText())) {
            LocalDate dataNasc = LocalDate.parse(txtDataNascimento.getText(), DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT));
            int idade = Period.between(dataNasc, LocalDate.now()).getYears();
            isMenorDeIdade = idade < 18;

            JLabel lblTel = mapLabels.get(txtTelefone); JLabel lblCpf = mapLabels.get(txtCpf);

            if (isMenorDeIdade) {
                lblTel.setText("CELULAR (RESPONSÁVEL) *"); txtTelefone.putClientProperty("tituloOriginal", "CELULAR (RESPONSÁVEL) *");
                lblCpf.setText("CPF (OPCIONAL P/ MENOR)"); txtCpf.putClientProperty("tituloOriginal", "CPF (OPCIONAL P/ MENOR)");
                btnNavResponsavel.setForeground(new Color(220, 53, 69));
                adicionarValidacaoTempoReal(txtNomeResponsavel, true, "TEXTO");
            } else {
                lblTel.setText("CELULAR PRINCIPAL *"); txtTelefone.putClientProperty("tituloOriginal", "CELULAR PRINCIPAL *");
                lblCpf.setText(chkEstrangeiro.isSelected() ? "PASSAPORTE / ID" : "CPF *");
                txtCpf.putClientProperty("tituloOriginal", chkEstrangeiro.isSelected() ? "PASSAPORTE / ID" : "CPF *");
                btnNavResponsavel.setForeground(Cores.CINZA_LABEL);
                adicionarValidacaoTempoReal(txtNomeResponsavel, false, "TEXTO");
            }
        }
    }

    private void carregarFotoNativa() {
        FileDialog fd = new FileDialog(frame, "Selecione a Foto do Paciente", FileDialog.LOAD);
        fd.setFile("*.jpg;*.jpeg;*.png"); fd.setVisible(true);
        String dir = fd.getDirectory(); String nome = fd.getFile();

        if (dir != null && nome != null) {
            try {
                BufferedImage imgOriginal = ImageIO.read(new File(dir, nome));
                if(imgOriginal != null) {
                    int tamanhoMenor = Math.min(imgOriginal.getWidth(), imgOriginal.getHeight());
                    int x = (imgOriginal.getWidth() - tamanhoMenor) / 2;
                    int y = (imgOriginal.getHeight() - tamanhoMenor) / 2;
                    BufferedImage imgRecortada = imgOriginal.getSubimage(x, y, tamanhoMenor, tamanhoMenor);

                    pnlAvatar.setImage(imgRecortada);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(); ImageIO.write(imgRecortada, "jpg", baos); fotoBytes = baos.toByteArray();
                }
            } catch (Exception ex) { JOptionPane.showMessageDialog(frame, "Erro ao carregar a imagem.", "Erro", JOptionPane.ERROR_MESSAGE); }
        }
    }

    private boolean isDataValida(String dataStr) {
        if (dataStr.length() != 10) return false;
        try {
            LocalDate data = LocalDate.parse(dataStr, DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT));
            return data.getYear() >= 1900 && !data.isAfter(LocalDate.now());
        } catch (Exception e) { return false; }
    }

    private boolean isTelefoneValido(String tel) {
        String num = tel.replaceAll("[^0-9]", "");
        return num.length() >= 10;
    }

    private void adicionarValidacaoTempoReal(JTextField campo, boolean obrigatorio, String tipo) {
        campo.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) { validarCampo(campo, obrigatorio, tipo); }
            public void focusGained(FocusEvent e) { setErroComponente(campo, false, null); }
        });

        campo.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                String numeros = campo.getText().replaceAll("[^0-9]", "");
                String limpo = limparMascara(campo.getText());

                if (obrigatorio && limpo.isEmpty()) return;

                boolean correto = false;
                if (tipo.equals("CPF") && Validadores.isCpfValido(numeros)) correto = true;
                else if (tipo.equals("DATA") && isDataValida(campo.getText())) correto = true;
                else if (tipo.equals("TELEFONE") && isTelefoneValido(campo.getText())) correto = true;
                else if (tipo.equals("CEP") && numeros.length() == 8) correto = true;
                else if (tipo.equals("TEXTO") && !limpo.isEmpty()) correto = true;

                if (correto) setErroComponente(campo, false, null);
            }
        });
    }

    private boolean validarCampo(JTextField campo, boolean obrigatorio, String tipo) {
        String limpo = limparMascara(campo.getText());
        String numeros = campo.getText().replaceAll("[^0-9]", "");

        if (obrigatorio && limpo.isEmpty()) { setErroComponente(campo, true, "Obrigatório"); return false; }
        if (!limpo.isEmpty() && !chkEstrangeiro.isSelected()) {
            if (tipo.equals("CPF") && !Validadores.isCpfValido(numeros)) { setErroComponente(campo, true, "Inválido"); return false; }
            if (tipo.equals("DATA") && !isDataValida(campo.getText())) { setErroComponente(campo, true, "Inválida"); return false; }
            if (tipo.equals("TELEFONE") && !isTelefoneValido(campo.getText())) { setErroComponente(campo, true, "Incompleto"); return false; }
            if (tipo.equals("CEP") && numeros.length() != 8) { setErroComponente(campo, true, "Incompleto"); return false; }
        }
        setErroComponente(campo, false, null); return true;
    }

    private void setErroComponente(JComponent comp, boolean comErro, String msg) {
        JLabel lbl = mapLabels.get(comp); String original = (String) comp.getClientProperty("tituloOriginal");
        if (comErro) {
            comp.putClientProperty("JComponent.outline", "error");
            if (lbl != null && msg != null) { lbl.setText(original + " - " + msg); lbl.setForeground(new Color(220, 53, 69)); }
        } else {
            comp.putClientProperty("JComponent.outline", null);
            if (lbl != null) { lbl.setText(original); lbl.setForeground(Cores.CINZA_LABEL); }
        } comp.repaint();
    }

    private void buscarCepEPreencher() {
        if(chkEstrangeiro.isSelected()) return;

        String cep = txtCep.getText().replaceAll("[^0-9]", "");
        if (cep.length() == 8) {
            new Thread(() -> { String[] dados = ServicoCEP.buscarCep(cep);
                SwingUtilities.invokeLater(() -> {
                    if (dados != null) { txtRua.setText(dados[0]); txtBairro.setText(dados[1]); txtCidade.setText(dados[2]); txtUf.setText(dados[3]); codigoIbge = dados[4]; txtNumero.requestFocus(); setErroComponente(txtCep, false, null); }
                    else { setErroComponente(txtCep, true, "Não encontrado no ViaCEP"); }
                });
            }).start();
        }
    }

    private void salvarRegistro() {
        boolean est = chkEstrangeiro.isSelected();
        boolean valid = true;

        valid &= validarCampo(txtNome, true, "TEXTO");
        valid &= validarCampo(txtDataNascimento, true, "DATA");
        valid &= validarCampo(txtTelefone, true, "TELEFONE");

        valid &= validarCampo(txtCep, !est, "CEP");
        valid &= validarCampo(txtNumero, !est, "TEXTO");

        valid &= validarCampo(txtCpfResponsavel, false, "CPF");
        valid &= validarCampo(txtCpfResponsavel2, false, "CPF"); // Adicionado Validação

        if (!isMenorDeIdade && !est) {
            valid &= validarCampo(txtCpf, true, "CPF");
        } else if (!isMenorDeIdade && est) {
            valid &= validarCampo(txtCpf, false, "TEXTO");
        } else {
            valid &= validarCampo(txtCpf, false, "CPF");
            valid &= validarCampo(txtNomeResponsavel, true, "TEXTO");
        }

        if (!valid) { JOptionPane.showMessageDialog(frame, "Verifique os campos obrigatórios marcados em vermelho.", "Atenção", JOptionPane.WARNING_MESSAGE); return; }

        LocalDate dataNasc = LocalDate.parse(txtDataNascimento.getText(), DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT));
        Endereco endereco = new Endereco(txtCep.getText(), txtRua.getText().trim(), txtNumero.getText().trim(), txtComplemento.getText().trim(), txtBairro.getText().trim(), txtCidade.getText().trim(), txtUf.getText().trim(), codigoIbge);
        String sexoSel = cbSexo.getSelectedIndex() == 0 ? "" : cbSexo.getSelectedItem().toString();

        // O NOVO CONSTRUTOR AQUI!
        Paciente pacienteAtualizado = new Paciente(
                txtNome.getText().trim(),
                txtCpf.getText(),
                dataNasc,
                sexoSel,
                txtCartaoSus.getText().trim(),
                txtTelefone.getText(),
                txtTelefone2.getText(),
                txtAlergias.getText().trim(),
                txtMedicoEncaminhador.getText().trim(),
                txtNomeResponsavel.getText().trim(),
                txtCpfResponsavel.getText(),
                txtNomeResponsavel2.getText().trim(),
                txtCpfResponsavel2.getText(),
                fotoBytes,
                endereco
        );

        if (pacienteEmEdicao == null) {
            new PacienteDAO().salvar(pacienteAtualizado);
            JOptionPane.showMessageDialog(frame, "Paciente cadastrado com sucesso!");
        } else {
            pacienteAtualizado.setId(pacienteEmEdicao.getId());
            new PacienteDAO().atualizar(pacienteAtualizado);
            JOptionPane.showMessageDialog(frame, "Cadastro atualizado com sucesso!");
        }

        frame.trocarTelaCentral(new PainelPacientes(frame));
    }

    private class PnlCircularAvatar extends JPanel {
        private Image image = null; private int size;
        public PnlCircularAvatar(int size) {
            this.size = size; setPreferredSize(new Dimension(size, size)); setMinimumSize(new Dimension(size, size)); setMaximumSize(new Dimension(size, size)); setOpaque(false);
            FlatSVGIcon icon = carregarIcone("member-list.svg", size - 40, Cores.CINZA_LABEL); if (icon != null) image = icon.getImage();
        }
        public void setImage(BufferedImage img) { this.image = img.getScaledInstance(size, size, Image.SCALE_SMOOTH); repaint(); }
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(242, 245, 248)); g2.fillOval(0, 0, size - 1, size - 1);
            if (image != null) { g2.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, size - 1, size - 1));
                if (image.getWidth(null) < size) g2.drawImage(image, 20, 20, size - 40, size - 40, this); else g2.drawImage(image, 0, 0, size - 1, size - 1, this); }
            g2.setClip(null); g2.setColor(new Color(210, 210, 210)); g2.setStroke(new BasicStroke(2f)); g2.drawOval(0, 0, size - 1, size - 1); g2.dispose();
        }
    }

    private String limparMascara(String texto) { String limpo = texto.replace("_", "").replace("/", "").replace("-", "").replace(".", "").replace("(", "").replace(")", "").replace("+", ""); return limpo.trim(); }
    private JButton criarBotaoNav(String texto, boolean ativo) { JButton btn = new JButton(texto); btn.setFont(new Font("Segoe UI", ativo ? Font.BOLD : Font.PLAIN, 16)); btn.setForeground(ativo ? Cores.VERDE_AQUA : Cores.CINZA_LABEL); btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false); btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); btn.setBorder(new EmptyBorder(10, 15, 10, 15)); return btn; }
    private void trocarAba(String aba, JButton btnAtivo) { cardLayout.show(pnlCards, aba); JButton[] botoes = {btnNavPessoais, btnNavResponsavel, btnNavEndereco}; for (JButton b : botoes) { b.setFont(new Font("Segoe UI", Font.PLAIN, 16)); b.setForeground(Cores.CINZA_LABEL); } btnAtivo.setFont(new Font("Segoe UI", Font.BOLD, 16)); btnAtivo.setForeground(Cores.VERDE_AQUA); }
    private FlatSVGIcon carregarIcone(String nomeArquivo, int tamanho, Color cor) { try { java.net.URL imgURL = getClass().getResource("/icons/" + nomeArquivo); if (imgURL != null) return (FlatSVGIcon) new FlatSVGIcon(imgURL).derive(tamanho, tamanho).setColorFilter(new FlatSVGIcon.ColorFilter(c -> cor)); return (FlatSVGIcon) new FlatSVGIcon("icons/" + nomeArquivo, tamanho, tamanho).setColorFilter(new FlatSVGIcon.ColorFilter(c -> cor)); } catch (Exception e) { return null; } }

    private void aplicarMascara(JFormattedTextField campo, String formato) {
        try {
            MaskFormatter mask = new MaskFormatter(formato);
            mask.setPlaceholderCharacter('_');
            campo.setFormatterFactory(new DefaultFormatterFactory(mask));
        } catch (Exception e) { }
    }

    private JPanel montarBloco(String titulo, JComponent comp) { return montarBloco(titulo, comp, comp); }
    private JPanel montarBloco(String titulo, JComponent container, JComponent targetRef) { JPanel p = new JPanel(new BorderLayout(0, 5)); p.setOpaque(false); JLabel l = new JLabel(titulo); l.setFont(new Font("Segoe UI", Font.BOLD, 12)); l.setForeground(Cores.CINZA_LABEL); l.setBorder(new EmptyBorder(0, 3, 0, 0)); mapLabels.put(targetRef, l); targetRef.putClientProperty("tituloOriginal", titulo); p.add(l, BorderLayout.NORTH); p.add(container, BorderLayout.CENTER); return p; }

    private JTextField criarCampoTexto(String placeholder) { JTextField f = new JTextField(); f.setPreferredSize(new Dimension(0, 45)); f.putClientProperty("JTextField.placeholderText", placeholder); f.setFont(new Font("Segoe UI", Font.PLAIN, 15)); f.setBackground(Color.WHITE); aplicarHoverMinimalista(f); return f; }
    private void configurarCampoFormatado(JFormattedTextField f) { f.setPreferredSize(new Dimension(0, 45)); f.setFont(new Font("Segoe UI", Font.PLAIN, 15)); f.setBackground(Color.WHITE); aplicarHoverMinimalista(f); f.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { if (limparMascara(f.getText()).isEmpty()) f.setCaretPosition(0); }}); f.addFocusListener(new FocusAdapter() { public void focusGained(FocusEvent e) { if (limparMascara(f.getText()).isEmpty()) SwingUtilities.invokeLater(() -> f.setCaretPosition(0)); setErroComponente(f, false, null); }}); }
    private void aplicarHoverMinimalista(JComponent comp) { comp.addMouseListener(new MouseAdapter() { public void mouseEntered(MouseEvent e) { if (comp.isEnabled() && !comp.hasFocus()) comp.setBackground(new Color(242, 248, 248)); } public void mouseExited(MouseEvent e) { if (!comp.hasFocus()) comp.setBackground(Color.WHITE); } }); }
}