package mz.multicore.erp.architecture.version;

/**
 * @param serverVersion    versão a correr no servidor — é também a última disponível para o cliente
 * @param minClientVersion versão mínima do desktop que este servidor ainda aceita
 */
public record ServerVersionDTO(String serverVersion, String minClientVersion) {}
