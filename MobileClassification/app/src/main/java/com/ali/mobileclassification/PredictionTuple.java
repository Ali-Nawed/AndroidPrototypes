package com.ali.mobileclassification;

public class PredictionTuple implements  Comparable<PredictionTuple>{
    float probability;
    String label;
    public PredictionTuple(float probability, String label) {
        this.probability = probability;
        this.label = label;
    }

    public float getProbability() {
        return probability;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean equals(Object object) {

        if (object == this) {
            return true;
        }

        if (!(object instanceof PredictionTuple)) {
            return false;
        }

        return this.probability == ((PredictionTuple) object).probability;
    }

    @Override
    public int compareTo(PredictionTuple o) {
        if (this.probability > o.probability) {
            return 1;
        }
        if (this.probability < o.probability) {
            return -1;
        }
        if (this.probability == o.probability) {
            return 0;
        }
        return 0;
    }
}
