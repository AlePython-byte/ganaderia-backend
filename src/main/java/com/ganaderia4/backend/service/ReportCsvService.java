package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.AlertReportFilterDTO;
import com.ganaderia4.backend.dto.AlertResponseDTO;
import com.ganaderia4.backend.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportCsvService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String DANGEROUS_CSV_PREFIXES = "=+-@";

    private final AlertReportService alertReportService;
    private final int maxRows;

    public ReportCsvService(AlertReportService alertReportService,
                            @Value("${reports.alerts.csv-max-rows:5000}") int maxRows) {
        this.alertReportService = alertReportService;
        this.maxRows = maxRows;
    }

    public byte[] exportAlertsReportCsv(AlertReportFilterDTO filter) {
        long totalRows = alertReportService.countAlertReport(filter);
        if (totalRows > maxRows) {
            throw new BadRequestException(
                    "El reporte supera el maximo de " + maxRows + " filas. Ajusta los filtros antes de exportar"
            );
        }

        List<AlertResponseDTO> alerts = alertReportService.getAlertReport(filter);

        StringBuilder csv = new StringBuilder();
        csv.append("id,type,status,message,createdAt,observations,cowId,cowToken,cowName,locationId")
                .append("\n");

        for (AlertResponseDTO alert : alerts) {
            csv.append(value(alert.getId()))
                    .append(",")
                    .append(value(alert.getType()))
                    .append(",")
                    .append(value(alert.getStatus()))
                    .append(",")
                    .append(value(alert.getMessage()))
                    .append(",")
                    .append(value(alert.getCreatedAt() != null
                            ? alert.getCreatedAt().format(DATE_TIME_FORMATTER)
                            : null))
                    .append(",")
                    .append(value(alert.getObservations()))
                    .append(",")
                    .append(value(alert.getCowId()))
                    .append(",")
                    .append(value(alert.getCowToken()))
                    .append(",")
                    .append(value(alert.getCowName()))
                    .append(",")
                    .append(value(alert.getLocationId()))
                    .append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String value(Object rawValue) {
        if (rawValue == null) {
            return "";
        }

        String text = neutralizePotentialFormula(String.valueOf(rawValue));
        String escaped = text.replace("\"", "\"\"");

        return "\"" + escaped + "\"";
    }

    private String neutralizePotentialFormula(String text) {
        if (text.isEmpty()) {
            return text;
        }

        String trimmed = text.stripLeading();
        if (trimmed.isEmpty()) {
            return text;
        }

        char firstCharacter = trimmed.charAt(0);
        if (DANGEROUS_CSV_PREFIXES.indexOf(firstCharacter) >= 0) {
            return "'" + text;
        }

        return text;
    }
}
