package com.ali.mobileclassification;

public class PredictionTuple implements  Comparable<PredictionTuple>{
    float probability;
    int index;
    public PredictionTuple(float probability, int index) {
        this.probability = probability;
        this.index = index;
    }

    public float getProbability() {
        return probability;
    }

    public int getIndex() {
        return index;
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
