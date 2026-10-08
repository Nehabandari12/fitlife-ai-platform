package com.fitness.aiservice.service;

/** A model that turns a prompt into text. GeminiService in production; a stub in tests. */
public interface TextGenerator {

    /**
     * @return the model's text reply
     * @throws AiResponseException when the model can't be reached or gives no usable reply
     */
    String generate(String prompt);
}
