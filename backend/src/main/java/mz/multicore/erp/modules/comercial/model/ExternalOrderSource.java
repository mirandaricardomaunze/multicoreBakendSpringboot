package mz.multicore.erp.modules.comercial.model;

/**
 * De onde vem uma encomenda criada por um sistema externo (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.4, §3.4.2).
 *
 * @param originName         nome da chave de API (ex.: "Loja online")
 * @param externalReference  referência da encomenda no sistema externo
 * @param idempotencyKey     chave única na empresa que impede a mesma encomenda externa de entrar duas vezes
 * @param requestFingerprint SHA-256 do corpo normalizado, para distinguir uma repetição de um conflito
 * @param contactPhone       telefone do Consumidor Final; {@code null} quando o cliente é registado
 */
public record ExternalOrderSource(String originName, String externalReference, String idempotencyKey,
                                  String requestFingerprint, String contactPhone) {
}
