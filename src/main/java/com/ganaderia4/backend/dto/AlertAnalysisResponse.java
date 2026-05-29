package com.ganaderia4.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(
        description = "Resultado del análisis inteligente generado por Claude AI (Anthropic) sobre el estado operativo del hato",
        example = """
                {
                  "analysis": "El hato presenta un nivel de riesgo ALTO con 7 alertas activas. La vaca 42 lleva más \
de 2 horas fuera de la geocerca asignada, lo cual puede indicar una ruptura en el cerco o una fuga deliberada. \
El collar 15 sin señal GPS desde las 06:30 impide el monitoreo de ese animal. Las condiciones ambientales \
con temperatura superior a 35°C y el déficit hídrico agravan el riesgo de estrés calórico en el hato. \
Acción prioritaria: verificar el estado del cerco perimetral y restablecer comunicación con el collar 15.",
                  "provider": "CLAUDE",
                  "generatedAt": "2026-05-29T14:32:00Z"
                }
                """
)
public class AlertAnalysisResponse {

    @Schema(
            description = "Diagnóstico narrativo generado por la IA en español. Incluye descripción del estado " +
                          "operativo actual, riesgos identificados y recomendaciones concretas para el operador. " +
                          "Basado exclusivamente en los datos enviados en el request, sin inventar hechos adicionales.",
            example = "El hato presenta un nivel de riesgo ALTO con 7 alertas activas. La vaca 42 lleva más de " +
                      "2 horas fuera de la geocerca asignada. Acción prioritaria: verificar el cerco perimetral."
    )
    private String analysis;

    @Schema(
            description = "Identificador del proveedor de IA que generó el análisis",
            example = "CLAUDE"
    )
    private String provider;

    @Schema(
            description = "Marca de tiempo UTC en que se generó el análisis en el servidor",
            example = "2026-05-29T14:32:00Z"
    )
    private Instant generatedAt;

    public AlertAnalysisResponse() {
    }

    public AlertAnalysisResponse(String analysis, String provider, Instant generatedAt) {
        this.analysis = analysis;
        this.provider = provider;
        this.generatedAt = generatedAt;
    }

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }
}
