package mz.multicore.erp.modules.comercial.dto;

import jakarta.validation.constraints.Size;

/**
 * Dados de transporte da transferência gerada a partir de uma encomenda de reposição.
 *
 * <p>Todos opcionais: a origem, o destino e os artigos vêm da encomenda — o que falta é só quem
 * leva e em quê. Ver {@code docs/REPOSICAO_INTERNA_SPEC.md} §4.
 */
public record ConvertOrderToTransferRequest(
        @Size(max = 255, message = "O responsável deve ter no máximo 255 caracteres.")
        String responsible,

        @Size(max = 255, message = "O veículo deve ter no máximo 255 caracteres.")
        String vehicle,

        @Size(max = 400, message = "As observações devem ter no máximo 400 caracteres.")
        String notes,

        @Size(max = 120, message = "O nome do motorista deve ter no máximo 120 caracteres.")
        String driverName,

        @Size(max = 30, message = "A matrícula do veículo deve ter no máximo 30 caracteres.")
        String vehiclePlate
) {
    public ConvertOrderToTransferRequest {
        responsible = mz.multicore.erp.architecture.validation.InputSanitizer.sanitizeText(responsible);
        vehicle = mz.multicore.erp.architecture.validation.InputSanitizer.sanitizeText(vehicle);
        notes = mz.multicore.erp.architecture.validation.InputSanitizer.sanitizeNotes(notes, 400);
        driverName = mz.multicore.erp.architecture.validation.InputSanitizer.sanitizeText(driverName);
        vehiclePlate = mz.multicore.erp.architecture.validation.InputSanitizer.sanitizeText(vehiclePlate);
    }

    public ConvertOrderToTransferRequest(String responsible, String vehicle, String notes) {
        this(responsible, vehicle, notes, responsible, vehicle);
    }
}
