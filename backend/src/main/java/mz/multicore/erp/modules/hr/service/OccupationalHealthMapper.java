package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.modules.hr.dto.OccupationalHealthExamDTO;
import mz.multicore.erp.modules.hr.dto.OccupationalHealthSummaryDTO;
import mz.multicore.erp.modules.hr.model.OccupationalHealthExam;

import java.time.LocalDate;

final class OccupationalHealthMapper {

    private OccupationalHealthMapper() {}

    static String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static String describeProvider(OccupationalHealthExam exam) {
        String label = exam.providerLabel();
        return label == null || label.isBlank() ? "prestador não identificado" : label;
    }

    static String status(OccupationalHealthExam exam) {
        long days = exam.daysUntilExpiry(LocalDate.now());
        return days < 0 ? "EXPIRED" : days <= OccupationalHealthService.ALERT_DAYS ? "EXPIRING" : "VALID";
    }

    static OccupationalHealthSummaryDTO toSummary(OccupationalHealthExam exam) {
        long days = exam.daysUntilExpiry(LocalDate.now());
        return new OccupationalHealthSummaryDTO(exam.getEmployee().getId(), true,
                exam.getFitnessResult(), exam.getExamDate(), exam.getExpiryDate(), days, status(exam));
    }

    static OccupationalHealthExamDTO toDTO(OccupationalHealthExam exam) {
        return new OccupationalHealthExamDTO(exam.getId(), exam.getEmployee().getId(),
                exam.getEmployee().getName(), exam.getCardNumber(), exam.getExamDate(), exam.getExpiryDate(),
                exam.getFitnessResult(),
                exam.getProvider() == null ? null : exam.getProvider().getId(),
                exam.providerLabel(), exam.getClinic(), exam.getDoctorName(), exam.getRestrictions(),
                exam.getNotes(), exam.getCost(), exam.getInvoiceNumber(), exam.isPaid(), exam.getPaidAt(),
                exam.getAttachmentData() != null && exam.getAttachmentData().length > 0,
                exam.getAttachmentName(), exam.daysUntilExpiry(LocalDate.now()), status(exam));
    }
}
