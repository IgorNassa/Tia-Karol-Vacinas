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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.prefs.Preferences;

public class PainelPacientes extends JPanel {
    private TelaPrincipal frame;
    private JTable tabelaPacientes;
    private DefaultTableModel modeloTabela;
    private PacienteDAO dao;
    private List<Paciente> listaPacientes;
    private int hoveredRow = -1;

    public PainelPacientes(TelaPrincipal frame) {
        this.frame = frame;
        this.dao = new PacienteDAO();
        setOpaque(false);
        setLayout(new BorderLayout(0, 20));
        setBorder(new EmptyBorder(30, 40, 30, 40));

        GlassPanel cardVidro = new GlassPanel();
        cardVidro.setLayout(new BorderLayout(0, 15));
        cardVidro.setBorder(new EmptyBorder(25, 35, 30, 35));

        // HEADER
        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setOpaque(false);
        JLabel titulo = new JLabel("Pacientes Registrados");
        titulo.setFont(new Font("Segoe UI Semilight", Font.PLAIN, 32));
        titulo.setForeground(Cores.CINZA_GRAFITE);
        header.add(titulo, BorderLayout.WEST);

        JButton btnNovo = new JButton(" Novo Paciente");
        btnNovo.setIcon(carregarIcone("adicionar.svg", 18, Color.WHITE));
        btnNovo.setBackground(Cores.VERDE_AQUA);
        btnNovo.setForeground(Color.WHITE);
        btnNovo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnNovo.setPreferredSize(new Dimension(180, 45));
        btnNovo.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // CHAMA O PAINEL DE FORMULÁRIO (MODO: NOVO PACIENTE)
        btnNovo.addActionListener(e -> {
            frame.trocarTelaCentral(new PainelFormularioPaciente(frame));
        });

        header.add(btnNovo, BorderLayout.EAST);
        cardVidro.add(header, BorderLayout.NORTH);

        JPanel pnlCentro = new JPanel(new BorderLayout(0, 20));
        pnlCentro.setOpaque(false);

        // TABELA
        String[] colunas = {"ID", "Nome", "CPF", "Nascimento", "Telefone", "Alergias"};
        modeloTabela = new DefaultTableModel(new Object[][]{}, colunas) {
            public boolean isCellEditable(int row, int column) { return false; }
        };

        tabelaPacientes = new JTable(modeloTabela);
        tabelaPacientes.setRowHeight(50);
        tabelaPacientes.setShowVerticalLines(false);
        tabelaPacientes.setShowHorizontalLines(false);
        tabelaPacientes.setIntercellSpacing(new Dimension(0, 5));
        tabelaPacientes.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        tabelaPacientes.setDefaultRenderer(Object.class, new CustomTableRenderer());

        JPopupMenu menuContexto = criarMenuOpcoesPacientes();

        tabelaPacientes.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                int row = tabelaPacientes.rowAtPoint(e.getPoint());
                if (hoveredRow != row) { hoveredRow = row; tabelaPacientes.repaint(); }
            }
        });

        tabelaPacientes.addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent e) { hoveredRow = -1; tabelaPacientes.repaint(); }
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int linhaClicada = tabelaPacientes.rowAtPoint(e.getPoint());
                    if (linhaClicada >= 0) {
                        tabelaPacientes.setRowSelectionInterval(linhaClicada, linhaClicada);
                        menuContexto.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tabelaPacientes);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setOpaque(false);
        pnlCentro.add(scroll, BorderLayout.CENTER);

        cardVidro.add(pnlCentro, BorderLayout.CENTER);
        add(cardVidro, BorderLayout.CENTER);

        carregarDadosTabela();
    }

    private void carregarDadosTabela() {
        modeloTabela.setRowCount(0);
        listaPacientes = dao.listarTodos();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (Paciente p : listaPacientes) {
            String nas = p.getDataNascimento() != null ? p.getDataNascimento().format(fmt) : "N/A";
            String alergias = (p.getAlergias() == null || p.getAlergias().trim().isEmpty()) ? "Nenhuma" : p.getAlergias();

            modeloTabela.addRow(new Object[]{
                    String.format("%03d", p.getId()),
                    p.getNome(),
                    p.getCpf(),
                    nas,
                    p.getTelefone(),
                    alergias
            });
        }
        hoveredRow = -1;
    }

    private Paciente buscarPacientePorIdNaLista(int id) {
        for (Paciente p : listaPacientes) { if (p.getId() == id) return p; }
        return null;
    }

    // =========================================================
    // MENU DE CONTEXTO PREMIUM
    // =========================================================
    private JPopupMenu criarMenuOpcoesPacientes() {
        JPopupMenu popup = new JPopupMenu();

        popup.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(230, 235, 240), 1, true), new EmptyBorder(10, 5, 10, 5)));
        popup.setBackground(Color.WHITE);

        JMenuItem itemEditar = criarItemMenu("Editar Dados do Paciente", "lapis-de-blog.svg", Cores.CINZA_GRAFITE);
        JMenuItem itemFicha = criarItemMenu("Imprimir Ficha Completa", "imprimir.svg", Cores.CINZA_GRAFITE);
        JMenuItem itemExcluir = criarItemMenu("Excluir Cadastro do Paciente", "trash.svg", new Color(220, 53, 69));

        // CHAMA O PAINEL DE FORMULÁRIO (MODO: EDITAR PACIENTE)
        itemEditar.addActionListener(e -> {
            int l = tabelaPacientes.getSelectedRow();
            if (l >= 0) {
                // Busca os dados do paciente clicado
                Paciente pEmEdicao = buscarPacientePorIdNaLista(Integer.parseInt(tabelaPacientes.getValueAt(l, 0).toString()));
                // Troca a tela central para o formulário, enviando o paciente junto
                frame.trocarTelaCentral(new PainelFormularioPaciente(frame, pEmEdicao));
            }
        });

        itemFicha.addActionListener(e -> {
            int l = tabelaPacientes.getSelectedRow();
            if (l >= 0) gerarEImprimirFichaPaciente(buscarPacientePorIdNaLista(Integer.parseInt(tabelaPacientes.getValueAt(l, 0).toString())));
        });

        itemExcluir.addActionListener(e -> {
            int l = tabelaPacientes.getSelectedRow();
            if (l >= 0) {
                int id = Integer.parseInt(tabelaPacientes.getValueAt(l, 0).toString());
                String nome = tabelaPacientes.getValueAt(l, 1).toString();

                int opt1 = JOptionPane.showConfirmDialog(frame, "ATENÇÃO: Você está prestes a excluir o cadastro de:\n\n" + nome + " (ID: "+id+")\n\nIsso removerá o histórico deste paciente.\nDeseja continuar?", "1ª Confirmação", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if(opt1 == JOptionPane.YES_OPTION) {
                    int opt2 = JOptionPane.showConfirmDialog(frame, "CONFIRMAÇÃO FINAL:\nTem certeza absoluta que deseja apagar permanentemente este registro?\nEsta ação NÃO PODE SER DESFEITA.", "2ª Confirmação - PERIGO", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
                    if(opt2 == JOptionPane.YES_OPTION) {
                        dao.excluir(id); carregarDadosTabela(); JOptionPane.showMessageDialog(frame, "Paciente excluído com sucesso.");
                    }
                }
            }
        });

        popup.add(itemEditar); JSeparator sep1 = new JSeparator(); sep1.setForeground(new Color(240,240,240)); popup.add(sep1);
        popup.add(itemFicha); JSeparator sep2 = new JSeparator(); sep2.setForeground(new Color(240,240,240)); popup.add(sep2);
        popup.add(itemExcluir);

        return popup;
    }

    private JMenuItem criarItemMenu(String texto, String arquivoSvg, Color corAcao) {
        JMenuItem item = new JMenuItem(texto);
        item.setFont(new Font("Segoe UI", Font.PLAIN, 14)); item.setBorder(new EmptyBorder(10, 20, 10, 30)); item.setCursor(new Cursor(Cursor.HAND_CURSOR)); item.setForeground(Cores.CINZA_GRAFITE); item.setBackground(Color.WHITE); item.setOpaque(true);
        String corHex = String.format("#%02x%02x%02x", corAcao.getRed(), corAcao.getGreen(), corAcao.getBlue());
        item.putClientProperty("FlatLaf.style", "selectionBackground: fade(" + corHex + ", 12%); selectionArc: 10; selectionForeground: " + corHex + ";");
        try {
            FlatSVGIcon icon = carregarIcone(arquivoSvg, 18, Cores.CINZA_GRAFITE); item.setIcon(icon); item.setIconTextGap(15);
            item.addChangeListener(e -> { if (item.isArmed()) icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> corAcao)); else icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> Cores.CINZA_GRAFITE)); });
        } catch (Exception ignored) {}
        return item;
    }

    private FlatSVGIcon carregarIcone(String nomeArquivo, int tamanho, Color cor) { try { return (FlatSVGIcon) new FlatSVGIcon("icons/" + nomeArquivo, tamanho, tamanho).setColorFilter(new FlatSVGIcon.ColorFilter(c -> cor)); } catch (Exception e) { return null; } }

    private class CustomTableRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column); setBorder(new EmptyBorder(0, 15, 0, 15));
            if (isSelected) { c.setBackground(Cores.VERDE_AQUA); c.setForeground(Color.WHITE); } else if (row == hoveredRow) { c.setBackground(new Color(242, 248, 248)); c.setForeground(Cores.CINZA_GRAFITE); } else { c.setBackground(Color.WHITE); c.setForeground(Cores.CINZA_GRAFITE); }
            return c;
        }
    }

    // =========================================================
    // IMPRESSÃO DE FICHA A4 (PREMIUM COM ROSA E AZUL + DADOS REAIS)
    // =========================================================
    private void gerarEImprimirFichaPaciente(Paciente paciente) {
        if (paciente == null) return;
        Preferences prefs = Preferences.userNodeForPackage(PainelConfiguracoes.class);
        String clinicaNome = prefs.get("clinica_nome", "CLÍNICA DE VACINAÇÃO"); String clinicaCnpj = prefs.get("clinica_cnpj", "00.000.000/0000-00"); String clinicaEnd = prefs.get("clinica_endereco", "Endereço não configurado"); String clinicaTel = prefs.get("clinica_telefone", "(00) 00000-0000"); String clinicaLogo = prefs.get("clinica_logo_path", "");
        String tagLogoClinica = ""; if (clinicaLogo != null && !clinicaLogo.isEmpty() && new java.io.File(clinicaLogo).exists()) { String uri = "file:///" + clinicaLogo.replace("\\", "/"); tagLogoClinica = "<img src='" + uri + "' style='max-width: 100px; max-height: 100px; object-fit: contain;' />"; }
        List<Aplicacao> todasAplicacoes = new AplicacaoDAO().listarTodas(); StringBuilder linhasHistorico = new StringBuilder(); DateTimeFormatter fmtData = DateTimeFormatter.ofPattern("dd/MM/yyyy"); boolean temHistorico = false;
        for (Aplicacao app : todasAplicacoes) { if (app.getPaciente() != null && app.getPaciente().getId() == paciente.getId()) { linhasHistorico.append("<tr>").append("<td>").append(app.getDataHora().format(fmtData)).append("</td>").append("<td>").append(app.getVacina().getNomeVacina()).append("</td>").append("<td>").append(app.getVacina().getLote()).append("</td>").append("<td>Enf Karoline Gulchinski</td>").append("</tr>"); temHistorico = true; } }
        if (!temHistorico) linhasHistorico.append("<tr><td colspan='4' align='center' style='color: #999; font-style: italic;'>Nenhuma aplicação registrada.</td></tr>");

        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>");
        html.append("body { font-family: 'Segoe UI', sans-serif; background-color: #e6e9ec; margin: 0; display: flex; justify-content: center; padding: 20px; }");
        html.append(".folha-a4 { background: white; width: 210mm; min-height: 297mm; padding: 15mm 20mm; box-sizing: border-box; box-shadow: 0 5px 15px rgba(0,0,0,0.2); display: flex; flex-direction: column; }");
        html.append("@media print { body { background: white; padding: 0; } .folha-a4 { box-shadow: none; width: 100%; min-height: 100vh; padding: 0; } }");
        html.append("table { width: 100%; border-collapse: collapse; font-size: 13px; color: #333; } td, th { padding: 8px; }");
        html.append(".tabela-cabecalho { border-bottom: 3px solid #D88C9A; padding-bottom: 15px; margin-bottom: 30px; }");
        html.append(".nome-clinica { font-size: 22px; font-weight: bold; color: #1E6669; margin: 0; text-transform: uppercase; letter-spacing: 1px;}");
        html.append(".info-clinica { font-size: 12px; color: #666; margin-top: 5px; line-height: 1.4; }");
        html.append(".titulo-secao { color: #1E6669; font-weight: bold; font-size: 16px; margin-bottom: 10px; border-left: 4px solid #D88C9A; padding-left: 8px; text-transform: uppercase;}");
        html.append(".tabela-dados { margin-bottom: 40px; background-color: #fcfcfc; border: 1px solid #eee; border-radius: 5px;} .tabela-dados td { border-bottom: 1px dashed #eee; }");
        html.append(".table-historico th { background-color: #1E6669; color: white; border: 1px solid #1E6669; text-align: left; font-weight: 500;} .table-historico td { border: 1px solid #ddd; } .table-historico tr:nth-child(even) { background-color: #f9f9f9; }");
        html.append(".danger { color: #D88C9A; font-weight: bold; }");
        html.append(".foto-paciente { width: 110px; height: 140px; border: 2px dashed #D88C9A; text-align: center; color: #D88C9A; font-size: 12px; background-color: #fff9fa; }");
        html.append(".espacador { flex-grow: 1; } .assinatura-container { text-align: center; margin-top: 50px; padding-top: 20px; }");
        html.append(".linha-assinatura { display: inline-block; width: 400px; border-top: 1px solid #1E6669; padding-top: 8px; color: #1E6669; font-weight: bold; }");
        html.append("</style></head><body>");

        html.append("<div class='folha-a4'><table class='tabela-cabecalho'><tr>");
        html.append("<td width='110' align='center'>").append(tagLogoClinica).append("</td><td valign='middle'><p class='nome-clinica'>").append(clinicaNome).append("</p><p class='info-clinica'>CNPJ: ").append(clinicaCnpj).append("<br>").append(clinicaEnd).append("<br>Telefone: ").append(clinicaTel).append("</p></td>");
        html.append("<td width='150' align='right' valign='middle'><h2 style='color: #1E6669; margin: 0;'>PRONTUÁRIO</h2><p style='color: #D88C9A; margin: 0; font-weight: bold;'>Via do Paciente</p></td></tr></table>");

        html.append("<div class='titulo-secao'>Dados do Paciente</div><table class='tabela-dados'><tr><td valign='top'><table>");
        html.append("<tr><td width='120'><b>Nome Completo:</b></td><td>").append(paciente.getNome()).append("</td></tr>");
        html.append("<tr><td><b>CPF:</b></td><td>").append(paciente.getCpf() == null || paciente.getCpf().isEmpty() ? "N/A" : paciente.getCpf()).append("</td></tr>");
        html.append("<tr><td><b>Nascimento:</b></td><td>").append(paciente.getDataNascimento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</td></tr>");
        html.append("<tr><td><b>Telefone:</b></td><td>").append(paciente.getTelefone()).append("</td></tr>");
        html.append("<tr><td><b>Aplicador(a):</b></td><td><b>Enf Karoline Gulchinski</b></td></tr>");
        html.append("<tr><td><b>Alergias:</b></td><td><span class='danger'>").append(paciente.getAlergias() == null || paciente.getAlergias().trim().isEmpty() ? "Nenhuma alergia relatada" : paciente.getAlergias()).append("</span></td></tr>");
        html.append("</table></td><td width='130' align='center' valign='middle'><table class='foto-paciente'><tr><td valign='middle'>Foto<br>3x4</td></tr></table></td></tr></table>");

        html.append("<div class='titulo-secao'>Histórico de Vacinação</div><table class='table-historico'><tr><th>Data da Aplicação</th><th>Imunizante</th><th>Lote</th><th>Aplicador(a)</th></tr>");
        html.append(linhasHistorico.toString()).append("</table><div class='espacador'></div>");
        html.append("<div class='assinatura-container'><div class='linha-assinatura'>Enf Karoline Gulchinski<br><span style='font-size: 11px; font-weight: normal; color: #666;'>Responsável Técnica - ").append(clinicaNome).append("</span></div></div></div></body></html>");

        try {
            String nomeArquivo = "Prontuario_" + paciente.getNome().replace(" ", "_");
            java.io.File arquivoFicha = java.io.File.createTempFile(nomeArquivo, ".html");
            java.nio.file.Files.writeString(arquivoFicha.toPath(), html.toString(), java.nio.charset.StandardCharsets.UTF_8);
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(arquivoFicha);
        } catch (Exception ex) { JOptionPane.showMessageDialog(frame, "Erro ao gerar a ficha: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE); }
    }
}