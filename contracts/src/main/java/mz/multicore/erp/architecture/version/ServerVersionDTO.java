package mz.multicore.erp.architecture.version;

/**
 * @param serverVersion    version running on the server and the latest available to the client
 * @param minClientVersion oldest desktop version still accepted by this server
 */
public record ServerVersionDTO(String serverVersion, String minClientVersion) {}
