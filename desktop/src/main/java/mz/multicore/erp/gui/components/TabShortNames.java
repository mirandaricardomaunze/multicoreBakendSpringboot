package mz.multicore.erp.gui.components;

import java.util.Map;

/**
 * Short labels drawn on tabs whose full names would push other tabs out of view. Only the drawing
 * changes: {@code getTitleAt} keeps the full name, so code, tests, the remembered tab and the top bar
 * path ("Stock › Transferências entre Armazéns") still use it, and the tab's tooltip shows it.
 * One place for every module, so the same idea always gets the same word.
 */
public final class TabShortNames {

    private static final Map<String, String> SHORT = Map.ofEntries(
            // Configurações
            Map.entry("Log de Auditoria Geral", "Auditoria"),
            Map.entry("Cópias de Segurança & Backups", "Cópias de Segurança"),
            Map.entry("Utilizadores & Permissões", "Utilizadores"),
            Map.entry("Colunas dos Documentos", "Documentos"),
            Map.entry("Suporte à Plataforma", "Suporte"),
            Map.entry("A Minha Assinatura", "Assinatura"),
            Map.entry("Perfil de Actividade", "Actividade"),
            Map.entry("Balança deste Posto", "Balança"),
            Map.entry("Gaveta deste Posto", "Gaveta"),
            // Compras
            Map.entry("Faturas de Compra (V/FT)", "Faturas de Compra"),
            Map.entry("Encomendas a Fornecedor (EC-F)", "Encomendas"),
            Map.entry("Gestão de Fornecedores", "Fornecedores"),
            Map.entry("Gestão de Negócios", "Negócios"),
            Map.entry("Conta Corrente & Reconciliação", "Conta Corrente"),
            // Stock
            Map.entry("Movimentos & Rastreabilidade", "Movimentos"),
            Map.entry("Transferências entre Armazéns", "Transferências"),
            Map.entry("Quebras & Desperdício", "Quebras"),
            Map.entry("Registo & Validação de Quebras", "Registo de Quebras"),
            Map.entry("Radar de Validades & Prevenção", "Radar de Validades"),
            Map.entry("Métricas & Relatório Executivo", "Métricas"),
            Map.entry("Validade (expirados / a expirar)", "Validade"),
            // Clientes
            Map.entry("Directório de Clientes", "Directório"),
            Map.entry("Risco de Crédito & Cobrança (Aging)", "Risco & Cobrança"),
            // Tesouraria
            Map.entry("Reconciliação Bancária", "Reconciliação"),
            Map.entry("Projeção Previsional", "Previsão"),
            // Plataforma
            Map.entry("Assinaturas & Pagamentos", "Assinaturas"),
            Map.entry("Versões dos Clientes", "Versões"),
            Map.entry("Saúde & Diagnóstico", "Diagnóstico"));

    private TabShortNames() {}

    /** The label drawn on the tab: the short name when one is defined, otherwise the name itself. */
    public static String of(String fullTitle) {
        if (fullTitle == null) return null;
        return SHORT.getOrDefault(fullTitle.trim(), fullTitle);
    }

    static Map<String, String> all() {
        return SHORT;
    }
}
