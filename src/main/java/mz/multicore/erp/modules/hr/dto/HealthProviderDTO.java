package mz.multicore.erp.modules.hr.dto;

/**
 * Prestador de saúde disponível para atribuir a um exame.
 *
 * <p>É uma leitura do <b>cadastro de fornecedores</b> — uma clínica é quem passa factura à empresa,
 * e criar-lhe um segundo cadastro só para o RH duplicaria NUIT, contacto e o histórico de
 * pagamentos. O que o RH mostra é a lista filtrada, não uma cópia dela.
 */
public record HealthProviderDTO(Long id, String name, String taxId) {}
