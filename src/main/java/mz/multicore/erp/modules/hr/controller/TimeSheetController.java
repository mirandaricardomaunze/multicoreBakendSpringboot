package mz.multicore.erp.modules.hr.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.hr.dto.CreateTimeEntryRequest;
import mz.multicore.erp.modules.hr.dto.TimeEntryDTO;
import mz.multicore.erp.modules.hr.dto.TimeSheetDTO;
import mz.multicore.erp.modules.hr.service.TimeSheetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Ponto e assiduidade. Ver docs/RH_COMPLETO_SPEC.md §B2. */
@RestController
@RequestMapping("/api/hr/timesheet")
public class TimeSheetController {

    private final TimeSheetService timeSheetService;

    public TimeSheetController(TimeSheetService timeSheetService) {
        this.timeSheetService = timeSheetService;
    }

    /** Folha de ponto apurada do mês: uma linha por colaborador, extra por escalão. */
    @GetMapping("/{year}/{month}")
    public ResponseEntity<TimeSheetDTO> monthlySheet(@PathVariable int year, @PathVariable int month) {
        return ResponseEntity.ok(timeSheetService.getMonthlySheet(year, month));
    }

    @GetMapping("/{year}/{month}/entries")
    public ResponseEntity<List<TimeEntryDTO>> entries(@PathVariable int year, @PathVariable int month) {
        return ResponseEntity.ok(timeSheetService.getEntries(year, month));
    }

    @PostMapping("/entries")
    public ResponseEntity<TimeEntryDTO> record(@RequestBody @Valid CreateTimeEntryRequest request) {
        return ResponseEntity.ok(timeSheetService.recordEntry(request));
    }

    @DeleteMapping("/entries/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        timeSheetService.deleteEntry(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{year}/{month}/close")
    public ResponseEntity<TimeSheetDTO> close(@PathVariable int year, @PathVariable int month) {
        return ResponseEntity.ok(timeSheetService.closePeriod(year, month));
    }

    /** Reabrir exige motivo — senão o fecho não significa nada. */
    @PostMapping("/{year}/{month}/reopen")
    public ResponseEntity<TimeSheetDTO> reopen(@PathVariable int year, @PathVariable int month,
                                               @RequestParam String reason) {
        return ResponseEntity.ok(timeSheetService.reopenPeriod(year, month, reason));
    }
}
