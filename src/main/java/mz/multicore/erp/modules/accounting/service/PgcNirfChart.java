package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.modules.accounting.model.AccountNature;

import java.util.List;

/**
 * Plano de contas de origem — <b>PGC-NIRF</b> (Plano Geral de Contabilidade de Moçambique),
 * no recorte que uma loja/mercearia usa de facto.
 *
 * <p>Não é o plano oficial completo (que tem centenas de contas): é o subconjunto que cobre o
 * que este ERP regista — vendas, compras, IVA, caixa, banco, clientes, fornecedores, existências
 * e pessoal. O contabilista acrescenta o resto pelo ecrã do plano de contas; a estrutura de
 * códigos é a mesma, pelo que nada tem de ser refeito.
 *
 * <p><b>Natureza por conta, não por classe:</b> Clientes (21) e Fornecedores (22) são ambos
 * classe 2 e têm naturezas opostas. Ver {@code AccountClass.defaultNature()}.
 */
public final class PgcNirfChart {

    private PgcNirfChart() {}

    /** Uma linha do plano semeado. {@code postable} falso = conta-mãe, só agrega. */
    public record Seed(String code, String name, AccountNature nature, boolean postable) {}

    /** Contas usadas pelos lançamentos automáticos — referidas por código, nunca por nome. */
    public static final String CAIXA = "1101";
    public static final String BANCO = "1201";
    public static final String CLIENTES = "2101";
    public static final String FORNECEDORES = "2201";
    public static final String IVA_LIQUIDADO = "2431";      // IVA a entregar ao Estado (vendas)
    public static final String IVA_DEDUTIVEL = "2432";      // IVA suportado (compras)
    public static final String PESSOAL_REMUNERACOES = "2601";
    public static final String MERCADORIAS = "3201";
    public static final String CMVMC = "6101";              // custo das mercadorias vendidas
    public static final String CUSTOS_PESSOAL = "6301";
    public static final String VENDAS = "7101";

    public static List<Seed> accounts() {
        return List.of(
                // Classe 1 — Meios circulantes financeiros
                new Seed("1", "Meios circulantes financeiros", AccountNature.DEVEDORA, false),
                new Seed("11", "Caixa", AccountNature.DEVEDORA, false),
                new Seed(CAIXA, "Caixa geral", AccountNature.DEVEDORA, true),
                new Seed("12", "Depósitos à ordem", AccountNature.DEVEDORA, false),
                new Seed(BANCO, "Banco — conta à ordem", AccountNature.DEVEDORA, true),

                // Classe 2 — Terceiros
                new Seed("2", "Terceiros", AccountNature.DEVEDORA, false),
                new Seed("21", "Clientes", AccountNature.DEVEDORA, false),
                new Seed(CLIENTES, "Clientes — conta corrente", AccountNature.DEVEDORA, true),
                new Seed("22", "Fornecedores", AccountNature.CREDORA, false),
                new Seed(FORNECEDORES, "Fornecedores — conta corrente", AccountNature.CREDORA, true),
                new Seed("24", "Estado e outros entes públicos", AccountNature.CREDORA, false),
                new Seed("243", "Imposto sobre o valor acrescentado", AccountNature.CREDORA, false),
                new Seed(IVA_LIQUIDADO, "IVA liquidado (vendas)", AccountNature.CREDORA, true),
                new Seed(IVA_DEDUTIVEL, "IVA dedutível (compras)", AccountNature.DEVEDORA, true),
                new Seed("26", "Pessoal", AccountNature.CREDORA, false),
                new Seed(PESSOAL_REMUNERACOES, "Remunerações a pagar", AccountNature.CREDORA, true),

                // Classe 3 — Existências
                new Seed("3", "Existências", AccountNature.DEVEDORA, false),
                new Seed("32", "Mercadorias", AccountNature.DEVEDORA, false),
                new Seed(MERCADORIAS, "Mercadorias em armazém", AccountNature.DEVEDORA, true),

                // Classe 5 — Capital
                new Seed("5", "Capital, reservas e resultados transitados", AccountNature.CREDORA, false),
                new Seed("51", "Capital social", AccountNature.CREDORA, false),
                new Seed("5101", "Capital social", AccountNature.CREDORA, true),

                // Classe 6 — Custos e perdas
                new Seed("6", "Custos e perdas", AccountNature.DEVEDORA, false),
                new Seed("61", "Custo das mercadorias vendidas e matérias consumidas", AccountNature.DEVEDORA, false),
                new Seed(CMVMC, "Custo das mercadorias vendidas", AccountNature.DEVEDORA, true),
                new Seed("62", "Fornecimentos e serviços de terceiros", AccountNature.DEVEDORA, false),
                new Seed("6201", "Fornecimentos e serviços de terceiros", AccountNature.DEVEDORA, true),
                new Seed("63", "Custos com o pessoal", AccountNature.DEVEDORA, false),
                new Seed(CUSTOS_PESSOAL, "Remunerações do pessoal", AccountNature.DEVEDORA, true),

                // Classe 7 — Proveitos e ganhos
                new Seed("7", "Proveitos e ganhos", AccountNature.CREDORA, false),
                new Seed("71", "Vendas", AccountNature.CREDORA, false),
                new Seed(VENDAS, "Vendas de mercadorias", AccountNature.CREDORA, true),
                new Seed("72", "Prestações de serviços", AccountNature.CREDORA, false),
                new Seed("7201", "Prestações de serviços", AccountNature.CREDORA, true),

                // Classe 8 — Resultados
                new Seed("8", "Resultados", AccountNature.CREDORA, false),
                new Seed("81", "Resultados operacionais", AccountNature.CREDORA, false),
                new Seed("8101", "Resultados operacionais", AccountNature.CREDORA, true));
    }
}
