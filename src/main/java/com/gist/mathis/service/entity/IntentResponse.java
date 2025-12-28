package com.gist.mathis.service.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IntentResponse {

    @JsonProperty("intent")
    private String intentString;

    @JsonProperty("entities")
    private Map<String, String> entities;

    public Intent getIntentValue() {
        if (intentString == null) {
            return Intent.UNKNOWN;
        }
        try {
            return Intent.valueOf(intentString.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return Intent.UNKNOWN;
        }
    }

    public boolean isValid() {
        return getIntentValue() != Intent.UNKNOWN;
    }

    @Override
    public String toString() {
        return "IntentResponse{" +
                "intent=" + getIntentValue() +
                ", entities=" + entities +
                '}';
    }
}
