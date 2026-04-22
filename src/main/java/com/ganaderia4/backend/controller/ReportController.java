package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.dto.AlertReportFilterDTO;
import com.ganaderia4.backend.dto.AlertResponseDTO;
import com.ganaderia4.backend.dto.CowIncidentReportDTO;
import com.ganaderia4.backend.dto.OfflineCollarReportDTO;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.service.AlertReportService;
import com.ganaderia4.backend.service.CollarReportService;
import com.ganaderia4.backend.service.CowIncidentReportService;
import com.ganaderia4.backend.service.ReportCsvService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final CowIncidentReportService cowIncidentReportService;
    private final ReportCsvService reportCsvService;

    public ReportController(AlertReportService alertReportService,
                            CollarReportService collarReportService,
                            CowIncidentReportService cowIncidentReportService,
                            ReportCsvService reportCsvService) {
        this.alertReportService = alertReportService;
        this.collarReportService = collarReportService;
        this.cowIncidentReportService = cowIncidentReportService;
        this.reportCsvService = reportCsvService;
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

    @GetMapping("/alerts/page")
    public Page<AlertResponseDTO> getAlertReportPage(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @RequestParam(required = false)
            AlertType type,

            @RequestParam(required = false)
            AlertStatus status,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "${app.pagination.default-size:20}")
            int size,

            @RequestParam(defaultValue = "createdAt")
            String sort,

            @RequestParam(defaultValue = "DESC")
            String direction
    ) {
        AlertReportFilterDTO filter = new AlertReportFilterDTO();
        filter.setFrom(from);
        filter.setTo(to);
        filter.setType(type);
        filter.setStatus(status);

        return alertReportService.getAlertReportPage(filter, page, size, sort, direction);
    }

    @GetMapping("/alerts/export.csv")
    public ResponseEntity<byte[]> exportAlertReportCsv(
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

        byte[] csvBytes = reportCsvService.exportAlertsReportCsv(filter);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=alert-report.csv")
                .body(csvBytes);
    }

    @GetMapping("/offline-collars")
    public List<OfflineCollarReportDTO> getOfflineCollarsReport() {
        return collarReportService.getOfflineCollarsReport();
    }

    @GetMapping("/cows-most-incidents")
    public List<CowIncidentReportDTO> getCowsMostIncidentsReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @RequestParam(required = false)
            AlertType type,

            @RequestParam(required = false)
            AlertStatus status,

            @RequestParam(required = false)
            Integer limit
    ) {
        AlertReportFilterDTO filter = new AlertReportFilterDTO();
        filter.setFrom(from);
        filter.setTo(to);
        filter.setType(type);
        filter.setStatus(status);

        return cowIncidentReportService.getCowsMostIncidentsReport(filter, limit);
    }
}
