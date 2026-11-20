package mz.multicore.erp.architecture.changes;

/**
 * Change counter of the active company, polled by the desktop to refresh visible screens.
 *
 * @param epoch   identifies the running server instance; a new value means the server restarted
 * @param version grows on every successful write of the company; compare, never interpret
 */
public record ChangeVersionDTO(String epoch, long version) {
}
