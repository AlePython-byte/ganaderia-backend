package com.ganaderia4.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Respuesta del análisis inteligente de alertas ganaderas")
public class AlertAnalysisResponse {

    @Schema(description = "Análisis narrativo generado por la IA sobre el estado operativo del hato")
    private String analysis;

    @Schema(description = "Proveedor de IA que generó el análisis", example = "DEEPSEEK")
    private String provider;

    @Schema(description = "Marca de tiempo UTC en que se generó el análisis")
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
