package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.dto.AlertReportFilterDTO;
import com.ganaderia4.backend.dto.AlertResponseDTO;
import com.ganaderia4.backend.dto.OfflineCollarReportDTO;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.service.AlertReportService;
import com.ganaderia4.backend.service.CollarReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final AlertReportService alertReportService;
    private final CollarReportService collarReportService;

    public ReportController(AlertReportService alertReportService,
                            CollarReportService collarReportService) {
        this.alertReportService = alertReportService;
        this.collarReportService = collarReportService;
    }

    @GetMapping("/alerts")
    public List<AlertResponseDTO> getAlertReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @RequestParam(required = false)
            AlertType type,

            @RequestParam(required = false)
            AlertStatus status
    ) {
        AlertReportFilterDTO filter = new AlertReportFilterDTO();
        filter.setFrom(from);
        filter.setTo(to);
        filter.setType(type);
        filter.setStatus(status);

        return alertReportService.getAlertReport(filter);
    }

    @GetMapping("/offline-collars")
    public List<OfflineCollarReportDTO> getOfflineCollarsReport() {
        return collarReportService.getOfflineCollarsReport();
    }
}