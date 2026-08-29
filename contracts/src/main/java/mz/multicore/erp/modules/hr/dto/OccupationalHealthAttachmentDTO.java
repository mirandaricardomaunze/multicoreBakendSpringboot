package mz.multicore.erp.modules.hr.dto;

/**
 * O comprovativo digitalizado de um exame, já decifrado, para quem tem permissão de o ver.
 *
 * <p>Até agora este ficheiro era gravado e nunca mais saía: não havia endpoint, serviço nem cliente
 * que o lesse. Ficava em claro na base de dados e nos backups sem servir para nada — o pior dos dois
 * mundos, exposto e inútil.
 */
public record OccupationalHealthAttachmentDTO(Long examId, String fileName, byte[] content) {}
