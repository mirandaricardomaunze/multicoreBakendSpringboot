package mz.multicore.erp.modules.users.dto;

/** Vista de leitura de um utilizador para a UI (sem password). */
public record AppUserDTO(
        Long id,
        String username,
        String name,
        String role,
        boolean active,
        boolean hasManagerPin,
        String email
) {
    public AppUserDTO(Long id, String username, String name, String role, boolean active) {
        this(id, username, name, role, active, false, null);
    }
}
