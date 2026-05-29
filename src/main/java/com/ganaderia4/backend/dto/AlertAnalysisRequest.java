package com.ganaderia4.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(
        description = "Datos operativos del hato para el análisis inteligente con DeepSeek AI",
        example = """
                {
                  "activeAlerts": 7,
                  "riskLevel": "HIGH",
                  "criticalEvents": [
                    "Vaca 42 fuera de geocerca por más de 2 horas",
                    "Collar 15 sin señal GPS desde las 06:30"
                  ],
                  "problems": [
                    "Temperatura ambiente superior a 35°C",
                    "Pozo de agua principal sin nivel mínimo"
                  ]
                }
                """
)
public class AlertAnalysisRequest {

    @NotNull(message = "activeAlerts es obligatorio")
    @Min(value = 0, message = "activeAlerts debe ser mayor o igual a cero")
    @Schema(
            description = "Número total de alertas activas en el momento del análisis",
            example = "7",
            minimum = "0"
    )
    private Integer activeAlerts;

    @NotBlank(message = "riskLevel es obligatorio")
    @Pattern(regexp = "LOW|MEDIUM|HIGH", message = "riskLevel debe ser LOW, MEDIUM o HIGH")
    @Schema(
            description = "Nivel de riesgo operativo actual del hato, determinado por reglas heurísticas del backend",
            example = "HIGH",
            allowableValues = {"LOW", "MEDIUM", "HIGH"}
    )
    private String riskLevel;

    @NotNull(message = "criticalEvents es obligatorio")
    @Size(max = 50, message = "criticalEvents no puede tener más de 50 elementos")
    @Schema(
            description = "Lista de eventos críticos detectados en el período de análisis. " +
                          "Cada elemento describe un hecho concreto (animal fuera de geocerca, collar sin señal, etc.). " +
                          "Máximo 50 eventos, cada uno hasta 300 caracteres.",
            example = "[\"Vaca 42 fuera de geocerca por más de 2 horas\", \"Collar 15 sin señal GPS desde las 06:30\"]"
    )
    private List<@NotBlank(message = "Cada evento crítico no puede estar vacío")
    @Size(max = 300, message = "Cada evento crítico no puede superar 300 caracteres") String> criticalEvents;

    @NotNull(message = "problems es obligatorio")
    @Size(max = 50, message = "problems no puede tener más de 50 elementos")
    @Schema(
            description = "Lista de problemas identificados en el hato que pueden estar relacionados con las alertas. " +
                          "Puede incluir condiciones ambientales, estado de infraestructura, salud animal, etc. " +
                          "Máximo 50 elementos, cada uno hasta 300 caracteres.",
            example = "[\"Temperatura ambiente superior a 35°C\", \"Pozo de agua principal sin nivel mínimo\"]"
    )
    private List<@NotBlank(message = "Cada problema no puede estar vacío")
    @Size(max = 300, message = "Cada problema no puede superar 300 caracteres") String> problems;

    public AlertAnalysisRequest() {
    }

    public AlertAnalysisRequest(Integer activeAlerts, String riskLevel,
                                List<String> criticalEvents, List<String> problems) {
        this.activeAlerts = activeAlerts;
        this.riskLevel = riskLevel;
        this.criticalEvents = criticalEvents;
        this.problems = problems;
    }

    public Integer getActiveAlerts() {
        return activeAlerts;
    }

    public void setActiveAlerts(Integer activeAlerts) {
        this.activeAlerts = activeAlerts;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public List<String> getCriticalEvents() {
        return criticalEvents;
    }

    public void setCriticalEvents(List<String> criticalEvents) {
        this.criticalEvents = criticalEvents;
    }

    public List<String> getProblems() {
        return problems;
    }

    public void setProblems(List<String> problems) {
        this.problems = problems;
    }
}
