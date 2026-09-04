package com.md287.risk.model;

import com.md287.risk.domain.ModelStatus;

public record ModelScore(
        ModelStatus status,
        Integer score,
        String modelName,
        String modelVersion
) {
    public static ModelScore ok(int score, String modelName, String modelVersion) {
        return new ModelScore(ModelStatus.OK, score, modelName, modelVersion);
    }

    public static ModelScore timeout() {
        return new ModelScore(ModelStatus.TIMEOUT, null, null, null);
    }

    public static ModelScore unavailable() {
        return new ModelScore(ModelStatus.UNAVAILABLE, null, null, null);
    }
}
