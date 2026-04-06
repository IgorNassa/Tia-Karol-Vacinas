package br.sistema.view.panels;

import br.sistema.model.Paciente;
import br.sistema.util.Cores;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Base64;
import java.time.LocalDate;
import java.time.Period;

public class DialogFichaPaciente extends JDialog {
    private Paciente paciente;
    private br.sistema.view.TelaPrincipal frame;

    public DialogFichaPaciente(Window owner, br.sistema.view.TelaPrincipal frame, Paciente paciente) {
        super(owner, "Prontuário Médico - " + paciente.getNome(), ModalityType.APPLICATION_MODAL);
        this.paciente = paciente;
        this.frame = frame;

        setSize(900, 700);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(245, 248, 250));

        // --- CABEÇALHO PERFIL ---
        add(criarCabecalho(), BorderLayout.NORTH);

        // --- CORPO (ABAS) ---
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabbedPane.setBackground(Color.WHITE);

        tabbedPane.addTab("Dados Cadastrais", criarAbaDados());
        tabbedPane.addTab("Histórico de Aplicações", criarAbaHistorico());
        tabbedPane.addTab("Anexos e Documentos", criarAbaAnexos());

        add(tabbedPane, BorderLayout.CENTER);

        // --- RODAPÉ (BOTÕES) ---
        add(criarRodape(), BorderLayout.SOUTH);
    }

    private JPanel criarCabecalho() {
        JPanel pnlHeader = new JPanel(new BorderLayout(20, 0));
        pnlHeader.setBackground(Color.WHITE);
        pnlHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)),
                new EmptyBorder(20, 30, 20, 30)
        ));

        // Foto Circular
        JLabel lblFoto = new JLabel();
        lblFoto.setPreferredSize(new Dimension(100, 100));
        if (paciente.getFoto() != null) {
            ImageIcon icon = new ImageIcon(paciente.getFoto());
            Image img = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            lblFoto.setIcon(new ImageIcon(img));
        } else {
            lblFoto.setIcon(carregarIcone("member-list.svg", 70, Cores.CINZA_LABEL));
        }
        lblFoto.setBorder(new LineBorder(new Color(230, 230, 230), 2, true));
        pnlHeader.add(lblFoto, BorderLayout.WEST);

        // Info Básica
        JPanel pnlInfo = new JPanel(new GridLayout(3, 1));
        pnlInfo.setOpaque(false);

        JLabel lblNome = new JLabel(paciente.getNome());
        lblNome.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblNome.setForeground(Cores.VERDE_AQUA);
        pnlInfo.add(lblNome);

        Period idade = Period.between(paciente.getDataNascimento(), LocalDate.now());
        String txtIdade = idade.getYears() == 0 ? idade.getMonths() + " meses" : idade.getYears() + " anos";
        JLabel lblSub = new JLabel(txtIdade + " | CPF: " + (paciente.getCpf().isEmpty() ? "Não informado" : paciente.getCpf()));
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblSub.setForeground(Cores.CINZA_LABEL);
        pnlInfo.add(lblSub);

        if (paciente.getAlergias() != null && !paciente.getAlergias().trim().isEmpty()) {
            JLabel lblAlergia = new JLabel("⚠️ ALERGIAS: " + paciente.getAlergias().toUpperCase());
            lblAlergia.setFont(new Font("Segoe UI", Font.BOLD, 14));
            lblAlergia.setForeground(new Color(220, 53, 69));
            pnlInfo.add(lblAlergia);
        }

        pnlHeader.add(pnlInfo, BorderLayout.CENTER);
        return pnlHeader;
    }

    private JPanel criarAbaDados() {
        JPanel pnl = new JPanel(new GridLayout(4, 2, 20, 20));
        pnl.setBackground(Color.WHITE);
        pnl.setBorder(new EmptyBorder(30, 40, 30, 40));

        pnl.add(criarItemDado("Data de Nascimento", paciente.getDataNascimento().toString()));
        pnl.add(criarItemDado("Sexo Biológico", paciente.getSexo().isEmpty() ? "Não informado" : paciente.getSexo()));
        pnl.add(criarItemDado("Celular Principal", paciente.getTelefone()));
        pnl.add(criarItemDado("Cartão SUS", paciente.getCartaoSus().isEmpty() ? "Não possui" : paciente.getCartaoSus()));
        pnl.add(criarItemDado("Responsável Legal", paciente.getNomeResponsavel().isEmpty() ? "N/A" : paciente.getNomeResponsavel()));
        pnl.add(criarItemDado("Médico Encaminhador", paciente.getMedicoEncaminhador().isEmpty() ? "Demanda Espontânea" : paciente.getMedicoEncaminhador()));

        String end = paciente.getEndereco() != null ? paciente.getEndereco().getRua() + ", " + paciente.getEndereco().getNumero() : "Não informado";
        pnl.add(criarItemDado("Endereço", end));

        return pnl;
    }

    private JPanel criarItemDado(String titulo, String valor) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitulo.setForeground(new Color(150, 150, 150));
        JLabel lblValor = new JLabel(valor);
        lblValor.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        lblValor.setForeground(new Color(50, 50, 50));
        p.add(lblTitulo, BorderLayout.NORTH);
        p.add(lblValor, BorderLayout.CENTER);
        return p;
    }

    private JPanel criarAbaHistorico() {
        JPanel pnl = new JPanel(new BorderLayout(0, 15));
        pnl.setBackground(Color.WHITE);
        pnl.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Tabela Falsa de Histórico (Até criarmos o Módulo de Estoque/Aplicações)
        String[] colunas = {"Data", "Vacina / Serviço", "Lote", "Aplicador", "Comprovante"};
        Object[][] dados = {
                {"10/03/2026", "BCG Infantil", "L-9982", "Enf. Karoline", "🖨️ Imprimir"},
                {"10/03/2026", "Hepatite B", "L-7741", "Enf. Karoline", "🖨️ Imprimir"}
        };
        JTable tbl = new JTable(new DefaultTableModel(dados, colunas));
        tbl.setRowHeight(35);
        tbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        pnl.add(new JScrollPane(tbl), BorderLayout.CENTER);
        return pnl;
    }

    private JPanel criarAbaAnexos() {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.setBackground(Color.WHITE);
        pnl.setBorder(new EmptyBorder(40, 40, 40, 40));

        JLabel lblMsg = new JLabel("Nenhum documento anexado.", SwingConstants.CENTER);
        lblMsg.setFont(new Font("Segoe UI", Font.ITALIC, 16));
        lblMsg.setForeground(Cores.CINZA_LABEL);
        pnl.add(lblMsg, BorderLayout.CENTER);

        return pnl;
    }

    private JPanel criarRodape() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        pnl.setBackground(new Color(250, 250, 250));
        pnl.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)));

        JButton btnAnexar = new JButton("Anexar Documento");
        btnAnexar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAnexar.addActionListener(e -> JOptionPane.showMessageDialog(this, "Módulo de Anexos na próxima etapa."));

        JButton btnImprimir = new JButton("Imprimir Ficha Completa");
        btnImprimir.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnImprimir.addActionListener(e -> imprimirFichaMedica()); // Chama o novo método de impressão!

        JButton btnAlterar = new JButton("Alterar Dados");
        btnAlterar.setBackground(Cores.ROSA_KAROL);
        btnAlterar.setForeground(Color.WHITE);
        btnAlterar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAlterar.addActionListener(e -> {
            dispose(); // Fecha o modal da ficha
            // Abre o painel principal em MODO EDIÇÃO!
            frame.trocarTelaCentral(new PainelFormularioPaciente(frame, paciente));
        });

        pnl.add(btnAnexar);
        pnl.add(btnImprimir);
        pnl.add(btnAlterar);
        return pnl;
    }

    // --- ENGENHARIA DE IMPRESSÃO VIA HTML (SEM BIBLIOTECAS EXTERNAS) ---
    private void imprimirFichaMedica() {
        try {
            // Cria um painel de texto invisível configurado para ler HTML
            JEditorPane editorPane = new JEditorPane();
            editorPane.setContentType("text/html");

            // Busca os dados personalizados do banco de dados
            br.sistema.model.Configuracao config = new br.sistema.repository.ConfiguracaoDAO().obterConfiguracao();
            String nomeClinica = (config != null && !config.getNomeClinica().isEmpty()) ? config.getNomeClinica() : "CLÍNICA DE VACINAÇÃO";
            String txtCnpj = (config != null && !config.getCnpj().isEmpty()) ? "CNPJ: " + config.getCnpj() : "";
            String txtEndereco = (config != null && !config.getEndereco().isEmpty()) ? config.getEndereco() : "";
            String txtContato = (config != null && !config.getTelefone().isEmpty()) ? " | Tel: " + config.getTelefone() : "";

// MÁGICA PARA A LOGO: Salva o byte[] em um arquivo temporário para o HTML do Swing conseguir ler
            String tagLogo = "";
            if (config != null && config.getLogo() != null) {
                try {
                    java.io.File tempFile = java.io.File.createTempFile("logo_clinica", ".png");
                    tempFile.deleteOnExit(); // O Windows apaga o arquivo sozinho quando fechar o sistema
                    java.nio.file.Files.write(tempFile.toPath(), config.getLogo());

                    String fileUrl = tempFile.toURI().toURL().toString();
                    tagLogo = "<img src='" + fileUrl + "' width='75' height='75' />"; // Tamanho controlado
                } catch (Exception ex) {
                    tagLogo = ""; // Se der erro, simplesmente não mostra a logo para não quebrar a ficha
                }
            }

// Constrói o documento com um layout "Laudo Médico Premium" usando micro-CSS
            StringBuilder html = new StringBuilder();
            html.append("<html><head><style>");
            html.append("body { font-family: sans-serif; color: #333333; margin: 15px; }");
            html.append("h1 { color: #1E6669; font-size: 22px; margin: 0; padding: 0; text-transform: uppercase; }");
            html.append("h2 { color: #555555; font-size: 15px; text-align: center; border-bottom: 2px solid #1E6669; padding-bottom: 8px; margin-top: 25px; margin-bottom: 20px;}");
            html.append("h3 { color: #1E6669; font-size: 12px; margin-top: 20px; margin-bottom: 8px; }");
            html.append("p.sub { font-size: 10px; color: #666666; margin: 3px 0px 0px 0px; }");
            html.append("table { width: 100%; border-collapse: collapse; font-size: 11px; }");
            html.append("td { padding: 6px 4px; vertical-align: middle; }");
            html.append(".linha-dado { border-bottom: 1px solid #EAEAEA; }");
            html.append(".hist-th { background-color: #1E6669; color: white; padding: 8px; text-align: left; font-weight: bold; }");
            html.append(".hist-td { border: 1px solid #DDDDDD; padding: 8px; }");
            html.append("</style></head><body>");

// Cabeçalho
            html.append("<table><tr>");
            if (!tagLogo.isEmpty()) {
                html.append("<td width='85' align='left'>").append(tagLogo).append("</td>");
            }
            html.append("<td align='left' valign='top'>");
            html.append("<h1>").append(nomeClinica).append("</h1>");
            html.append("<p class='sub'>").append(txtCnpj).append("</p>");
            html.append("<p class='sub'>").append(txtEndereco).append(txtContato).append("</p>");
            html.append("</td></tr></table>");

// Título do Documento
            html.append("<h2>PRONTUÁRIO MÉDICO DO PACIENTE</h2>");

// Dados Pessoais (Organizado em 4 colunas bem distribuídas)
            html.append("<h3>1. DADOS PESSOAIS</h3>");
            html.append("<table>");

            html.append("<tr class='linha-dado'>");
            html.append("<td width='12%'><b>Nome:</b></td><td width='38%'>").append(paciente.getNome()).append("</td>");
            html.append("<td width='12%'><b>CPF:</b></td><td width='38%'>").append(paciente.getCpf() == null || paciente.getCpf().isEmpty() ? "N/A" : paciente.getCpf()).append("</td>");
            html.append("</tr>");

            html.append("<tr class='linha-dado'>");
            html.append("<td><b>Data de Nasc.:</b></td><td>").append(paciente.getDataNascimento().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</td>");
            html.append("<td><b>Telefone:</b></td><td>").append(paciente.getTelefone()).append("</td>");
            html.append("</tr>");

            String alergiasStr = (paciente.getAlergias() == null || paciente.getAlergias().isEmpty()) ? "Nenhuma relatada" : paciente.getAlergias();
            String corAlergia = alergiasStr.equals("Nenhuma relatada") ? "#333333" : "#D32F2F";

            html.append("<tr class='linha-dado'>");
            html.append("<td><b>Alergias:</b></td><td><b style='color:").append(corAlergia).append(";'>").append(alergiasStr).append("</b></td>");
            html.append("<td><b>Médico:</b></td><td>").append(paciente.getMedicoEncaminhador() == null || paciente.getMedicoEncaminhador().isEmpty() ? "Demanda Espontânea" : paciente.getMedicoEncaminhador()).append("</td>");
            html.append("</tr>");
            html.append("</table>");

// Histórico
            html.append("<h3>2. HISTÓRICO DE APLICAÇÕES</h3>");
            html.append("<table style='margin-top: 5px;'>");
            html.append("<tr><th class='hist-th'>Data</th><th class='hist-th'>Vacina / Serviço</th><th class='hist-th'>Lote</th><th class='hist-th'>Aplicador</th></tr>");
            html.append("<tr><td class='hist-td'>--/--/----</td><td class='hist-td'>Aguardando Integração...</td><td class='hist-td'>---</td><td class='hist-td'>---</td></tr>");
            html.append("</table>");

// Assinatura
            html.append("<br><br><br><br>");
            html.append("<table style='text-align: center; margin-top: 30px;'><tr><td>");
            html.append("______________________________________________________________<br>");
            html.append("<span style='font-size: 10px; color: #555555;'>Assinatura e Carimbo do Responsável Técnico</span>");
            html.append("</td></tr></table>");

            html.append("</body></html>");

// Continua com o editorPane.setText(html.toString()); e a impressão...

            editorPane.setText(html.toString());

            // Abre a janela nativa de impressão do Windows
            boolean concluido = editorPane.print(null, null, true, null, null, true);
            if (concluido) {
                JOptionPane.showMessageDialog(this, "Impressão enviada com sucesso!", "Imprimir", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao tentar imprimir o documento.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private FlatSVGIcon carregarIcone(String nomeArquivo, int tamanho, Color cor) {
        try {
            java.net.URL imgURL = getClass().getResource("/icons/" + nomeArquivo);
            if (imgURL != null) return (FlatSVGIcon) new FlatSVGIcon(imgURL).derive(tamanho, tamanho).setColorFilter(new FlatSVGIcon.ColorFilter(c -> cor));
            return (FlatSVGIcon) new FlatSVGIcon("icons/" + nomeArquivo, tamanho, tamanho).setColorFilter(new FlatSVGIcon.ColorFilter(c -> cor));
        } catch (Exception e) { return null; }
    }
}