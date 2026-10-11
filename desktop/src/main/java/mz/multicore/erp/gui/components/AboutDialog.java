package mz.multicore.erp.gui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Diálogo executivo canónico "Sobre o Multicore ERP" com identificação detalhada
 * do autor e arquiteto do sistema: Miranda Ricardo Maunze.
 */
public final class AboutDialog {

    private AboutDialog() {}

    /**
     * Apresenta o diálogo de Sobre o Sistema centrado na janela pai informada.
     */
    public static void show(Component parent) {
        ExecutiveDetailDialog dialog = ExecutiveDetailDialog.create(parent, "Sobre o Multicore ERP");

        // 1. Cabeçalho rico
        dialog.setTitle("Multicore Enterprise ERP");
        dialog.setSubtitle("Sistema Integrado de Gestão Empresarial • Moçambique");
        dialog.setHeaderIcon("fas-layer-group", 36, UIHelper.ACCENT_BLUE);
        dialog.setStatusBadge("AUTOR & ARQUITETO: MIRANDA RICARDO MAUNZE", ExecutiveDetailDialog.StatusSeverity.SUCCESS);

        // 2. Faixa horizontal de KPIs com cartões modernos
        dialog.addKpi(
                "Autor & Concepção",
                "Miranda Ricardo Maunze",
                "Liderança Técnica & Arquitetura",
                UIHelper.ACCENT_BLUE,
                "fas-user-tie"
        );
        dialog.addKpi(
                "Versão do Sistema",
                "v1.0.0 Enterprise",
                "Build de Produção Estável",
                UIHelper.APPROVED_GREEN,
                "fas-code-branch"
        );
        dialog.addKpi(
                "Mercado & Jurisdição",
                "Moçambique",
                "Conformidade Fiscal AT / IVA",
                UIHelper.PENDING_YELLOW,
                "fas-globe-africa"
        );
        dialog.addKpi(
                "Engenharia",
                "Hexagonal / Reactor",
                "Java 21 • Spring Boot 3 • Swing",
                UIHelper.MODULE_ACCOUNTING,
                "fas-cubes"
        );

        // 3. Abas de Detalhes
        dialog.addTab(
                "Autoria & Concepção",
                "fas-user-shield",
                UIHelper.ACCENT_BLUE,
                buildAuthorTab()
        );
        dialog.addTab(
                "Módulos Integrados",
                "fas-th-large",
                UIHelper.APPROVED_GREEN,
                buildModulesTab()
        );
        dialog.addTab(
                "Arquitetura & Engenharia",
                "fas-microchip",
                UIHelper.MODULE_ACCOUNTING,
                buildArchitectureTab()
        );
        dialog.addTab(
                "Licenciamento & Direitos",
                "fas-certificate",
                UIHelper.PENDING_YELLOW,
                buildLicenseTab()
        );

        dialog.showDialog();
    }

    private static JComponent buildAuthorTab() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Topo da aba: Avatar estilizado + dados do autor
        JPanel authorHeader = new JPanel(new BorderLayout(16, 0));
        authorHeader.setOpaque(false);

        // Monograma MRM
        JPanel avatarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setPaint(new GradientPaint(0, 0, UIHelper.ACCENT_BLUE, getWidth(), getHeight(), UIHelper.ACCENT_BLUE_HOVER));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font(UIHelper.FONT, Font.BOLD, 22));
                    FontMetrics fm = g2.getFontMetrics();
                    String text = "MRM";
                    int tx = (getWidth() - fm.stringWidth(text)) / 2;
                    int ty = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                    g2.drawString(text, tx, ty);
                } finally {
                    g2.dispose();
                }
            }
        };
        avatarPanel.setPreferredSize(new Dimension(68, 68));
        avatarPanel.setOpaque(false);
        authorHeader.add(avatarPanel, BorderLayout.WEST);

        JPanel authorMeta = new JPanel();
        authorMeta.setOpaque(false);
        authorMeta.setLayout(new BoxLayout(authorMeta, BoxLayout.Y_AXIS));

        JLabel nameLabel = new JLabel("Miranda Ricardo Maunze");
        nameLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 18));
        nameLabel.setForeground(UIHelper.TEXT_LIGHT);

        JLabel roleLabel = new JLabel("Autor, Fundador & Arquiteto de Software Principal");
        roleLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        roleLabel.setForeground(UIHelper.ACCENT_BLUE);

        JLabel locationLabel = new JLabel("Maputo, Moçambique • Engenharia de Sistemas Empresariais");
        locationLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        locationLabel.setForeground(UIHelper.TEXT_MUTED);

        authorMeta.add(nameLabel);
        authorMeta.add(Box.createRigidArea(new Dimension(0, 3)));
        authorMeta.add(roleLabel);
        authorMeta.add(Box.createRigidArea(new Dimension(0, 3)));
        authorMeta.add(locationLabel);
        authorHeader.add(authorMeta, BorderLayout.CENTER);

        card.add(authorHeader, BorderLayout.NORTH);

        // Centro: Manifesto de Autoria e Ficha Técnica
        JPanel body = new JPanel(new GridLayout(1, 2, 16, 0));
        body.setOpaque(false);

        // Bloco 1: Biografia e Visão
        ModernPanel visionPanel = new ModernPanel(12);
        visionPanel.setLayout(new BoxLayout(visionPanel, BoxLayout.Y_AXIS));
        visionPanel.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel visionTitle = new JLabel("Concepção & Liderança de Engenharia");
        visionTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        visionTitle.setForeground(UIHelper.TEXT_LIGHT);

        JTextArea visionText = new JTextArea(
                "O Multicore ERP foi integralmente concebido, desenhado e desenvolvido por "
                + "Miranda Ricardo Maunze como uma solução empresarial de alta performance e robustez industrial, "
                + "feita à medida das exigências de conformidade fiscal e eficiência operacional do mercado corporativo de Moçambique.\n\n"
                + "O sistema reúne décadas de boas práticas de engenharia de software, garantindo tempo de resposta "
                + "instantâneo no terminal desktop, consistência transacional absoluta e controlo total das operações empresariais."
        );
        visionText.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        visionText.setForeground(UIHelper.TEXT_MUTED);
        visionText.setWrapStyleWord(true);
        visionText.setLineWrap(true);
        visionText.setEditable(false);
        visionText.setOpaque(false);

        visionPanel.add(visionTitle);
        visionPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        visionPanel.add(visionText);
        body.add(visionPanel);

        // Bloco 2: Ficha Técnica de Autoria
        ModernPanel infoPanel = new ModernPanel(12);
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel infoTitle = new JLabel("Ficha de Identificação do Autor");
        infoTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        infoTitle.setForeground(UIHelper.TEXT_LIGHT);
        infoPanel.add(infoTitle);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        addMetaRow(infoPanel, "Autor & Criador:", "Miranda Ricardo Maunze");
        addMetaRow(infoPanel, "Propriedade Intelectual:", "Miranda Ricardo Maunze");
        addMetaRow(infoPanel, "Origem & Desenvolvimento:", "Maputo, Moçambique");
        addMetaRow(infoPanel, "Especialidade:", "Sistemas ERP, POS & Finanças");
        addMetaRow(infoPanel, "Filosofia de Design:", "Enterprise Tier-1 sem bloqueio de UI");
        addMetaRow(infoPanel, "Estado da Plataforma:", "Produção Estável & Suportada");

        body.add(infoPanel);
        card.add(body, BorderLayout.CENTER);

        return card;
    }

    private static JComponent buildModulesTab() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel title = new JLabel("Ecossistema de Módulos Integrados — Multicore ERP");
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        title.setForeground(UIHelper.TEXT_LIGHT);
        card.add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(3, 3, 12, 12));
        grid.setOpaque(false);

        grid.add(createModuleCard("Facturação & Vendas", "Facturas, cotações, guias de transporte e comunicação AT.", "fas-file-invoice", UIHelper.APPROVED_GREEN));
        grid.add(createModuleCard("POS & Frente de Caixa", "Vendas balcão, caixa cego, sangria, talhões e múltiplos meios.", "fas-cash-register", UIHelper.ACCENT_BLUE));
        grid.add(createModuleCard("Gestão de Stock", "Estrutura Caixa → Embalagem → Unidade, lotes e armazéns.", "fas-boxes", UIHelper.MODULE_STOCK));
        grid.add(createModuleCard("Financeiro & Bancos", "Contas a pagar/receber, conciliação bancária e extratos.", "fas-university", UIHelper.MODULE_FINANCEIRO));
        grid.add(createModuleCard("Contabilidade & Razão", "Plano de contas geral, diários, balancetes e fecho de exercício.", "fas-calculator", UIHelper.MODULE_ACCOUNTING));
        grid.add(createModuleCard("Crédito & Cobrança", "Plafonds por cliente, prazos médios, scoring e risco financeiro.", "fas-balance-scale", UIHelper.PENDING_YELLOW));
        grid.add(createModuleCard("Motor de Aprovações", "Políticas de alçada executiva, aprovações duplas e alertas.", "fas-check-double", UIHelper.MODULE_APPROVALS));
        grid.add(createModuleCard("Auditoria Forense", "Deteção de anomalias, cancelamentos, logs e controlo de fraude.", "fas-shield-alt", UIHelper.REJECTED_RED));
        grid.add(createModuleCard("Recursos Humanos", "Processamento salarial, IRPS, INSS e mapa de remunerações.", "fas-users", UIHelper.ACCENT_CYAN));

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private static JComponent buildArchitectureTab() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel title = new JLabel("Padrões de Engenharia & Arquitetura de Software");
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        title.setForeground(UIHelper.TEXT_LIGHT);
        card.add(title);
        card.add(Box.createRigidArea(new Dimension(0, 10)));

        ModernPanel techGrid = new ModernPanel(12);
        techGrid.setLayout(new GridLayout(2, 2, 16, 12));
        techGrid.setBorder(new EmptyBorder(14, 16, 14, 16));

        techGrid.add(createTechFeature(
                "Multi-Módulo Reactor",
                "Divisão física estrita em contracts (agnóstico), backend (Spring Boot 3 headless) e desktop (cliente gráfico Java 21).",
                "fas-network-wired",
                UIHelper.ACCENT_BLUE
        ));
        techGrid.add(createTechFeature(
                "Arquitetura Hexagonal & DDD",
                "Domínio limpo isolado de infraestrutura, com DTOs records imutáveis em todas as fronteiras de comunicação.",
                "fas-layer-group",
                UIHelper.APPROVED_GREEN
        ));
        techGrid.add(createTechFeature(
                "Segurança & Resiliência",
                "Autenticação por tokens, RBAC de granularidade fina e integridade de base de dados com migrações Flyway.",
                "fas-lock",
                UIHelper.PENDING_YELLOW
        ));
        techGrid.add(createTechFeature(
                "Design Ergonómico Swing",
                "Interface nativa ultra-rápida sem dependência de browser, suporte a temas Claro/Escuro e navegação rápida por teclado.",
                "fas-desktop",
                UIHelper.MODULE_ACCOUNTING
        ));

        card.add(techGrid);
        return card;
    }

    private static JComponent buildLicenseTab() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("Direitos de Autor & Termos de Uso");
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 15));
        title.setForeground(UIHelper.TEXT_LIGHT);
        card.add(title);
        card.add(Box.createRigidArea(new Dimension(0, 12)));

        JTextArea licenseText = new JTextArea(
                "MULTICORE ENTERPRISE ERP — SISTEMA INTEGRADO DE GESTÃO EMPRESARIAL\n\n"
                + "Copyright © 2026 Miranda Ricardo Maunze. Todos os direitos reservados.\n\n"
                + "1. TITULARIDADE DOS DIREITOS:\n"
                + "Todos os direitos de propriedade intelectual, código-fonte, arquitetura, bases de dados, "
                + "layout e documentação deste software pertencem com exclusividade ao autor Miranda Ricardo Maunze.\n\n"
                + "2. CONFORMIDADE LEGAL:\n"
                + "O sistema foi projetado em estrita conformidade com a legislação fiscal da República de Moçambique, "
                + "assegurando transparência, auditabilidade e rigor contabilístico nas operações dos utilizadores licenciados.\n\n"
                + "3. SUPORTE & AUTORIA:\n"
                + "Para informações sobre licenciamento corporativo, integrações ou expansão de módulos, "
                + "contacte diretamente a equipa Multicore sob liderança técnica de Miranda Ricardo Maunze."
        );
        licenseText.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        licenseText.setForeground(UIHelper.TEXT_MUTED);
        licenseText.setLineWrap(true);
        licenseText.setWrapStyleWord(true);
        licenseText.setEditable(false);
        licenseText.setOpaque(false);

        card.add(licenseText);
        return card;
    }

    private static void addMetaRow(JPanel container, String label, String value) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        lbl.setForeground(UIHelper.TEXT_MUTED);
        lbl.setPreferredSize(new Dimension(160, 20));

        JLabel val = new JLabel(value);
        val.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        val.setForeground(UIHelper.TEXT_LIGHT);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        container.add(row);
        container.add(Box.createRigidArea(new Dimension(0, 4)));
    }

    private static JPanel createModuleCard(String title, String desc, String icon, Color color) {
        ModernPanel p = new ModernPanel(10);
        p.setLayout(new BorderLayout(10, 0));
        p.setBorder(new EmptyBorder(10, 12, 10, 12));

        JLabel ic = new JLabel(UIHelper.icon(icon, 18, color));
        p.add(ic, BorderLayout.WEST);

        JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
        text.setOpaque(false);

        JLabel t = new JLabel(title);
        t.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        t.setForeground(UIHelper.TEXT_LIGHT);

        JLabel d = new JLabel(desc);
        d.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        d.setForeground(UIHelper.TEXT_MUTED);

        text.add(t);
        text.add(d);
        p.add(text, BorderLayout.CENTER);
        return p;
    }

    private static JPanel createTechFeature(String title, String desc, String icon, Color color) {
        ModernPanel p = new ModernPanel(10);
        p.setLayout(new BorderLayout(12, 0));
        p.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel ic = new JLabel(UIHelper.icon(icon, 22, color));
        p.add(ic, BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel t = new JLabel(title);
        t.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        t.setForeground(UIHelper.TEXT_LIGHT);

        JLabel d = new JLabel("<html>" + desc + "</html>");
        d.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        d.setForeground(UIHelper.TEXT_MUTED);

        text.add(t);
        text.add(Box.createRigidArea(new Dimension(0, 3)));
        text.add(d);
        p.add(text, BorderLayout.CENTER);
        return p;
    }
}
