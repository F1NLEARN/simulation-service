package com.finlearn.simulationservice.domain.analysis.entity;

public enum AnalysisStatus {

    READY {
        @Override public boolean canComplete() { return true; }
        @Override public boolean canFail() { return true; }
    },
    COMPLETED {
        @Override public boolean canComplete() { return false; }
        @Override public boolean canFail() { return false; }
    },
    FAILED {
        @Override public boolean canComplete() { return false; }
        @Override public boolean canFail() { return false; }
    };

    public abstract boolean canComplete();
    public abstract boolean canFail();
}