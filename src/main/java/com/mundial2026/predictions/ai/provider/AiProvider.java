package com.mundial2026.predictions.ai.provider;

/**
 * Contrato genérico de IA — implementa este interface para cualquier proveedor.
 * Para cambiar de modelo: ai.provider=gemini | chatgpt | claude | grok
 */
public interface AiProvider {

    /**
     * Envía un prompt y retorna la respuesta en texto plano.
     */
    String complete(String prompt);

    /**
     * Nombre del proveedor (para logs y respuestas de la API).
     */
    String providerName();
}
