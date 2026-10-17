package com.fischgesichtert.timemod.future;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.TreeMap;

/**
 * Ein Wert, der mit den Jahren wächst oder fällt: Stützpunkte wie {@code {"50": 0.15, "500": 0.8}},
 * dazwischen logarithmisch interpoliert (die Epochen liegen logarithmisch: +1, +5, +50, +500 …),
 * vor dem ersten Punkt 0 bzw. der erste Wert, nach dem letzten der letzte.
 */
public final class Curve {
    public static final Curve ZERO = new Curve(new double[] {1}, new double[] {0}, 0.0);

    private final double[] logYears;
    private final double[] values;
    private final double before;

    private Curve(double[] years, double[] values, double before) {
        this.logYears = new double[years.length];
        for (int i = 0; i < years.length; i++) {
            this.logYears[i] = Math.log10(Math.max(1.0, years[i]));
        }
        this.values = values;
        this.before = before;
    }

    /** Eine Zahl gilt für alle Jahre; ein Objekt sind Stützpunkte. Vor dem ersten Stützpunkt gilt {@code before}. */
    public static Curve parse(JsonElement json, double before) {
        if (json == null || json.isJsonNull()) {
            return ZERO;
        }
        if (json.isJsonPrimitive()) {
            double value = json.getAsDouble();
            return new Curve(new double[] {1}, new double[] {value}, value);
        }
        JsonObject object = json.getAsJsonObject();
        TreeMap<Double, Double> points = new TreeMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            points.put(Double.parseDouble(entry.getKey()), entry.getValue().getAsDouble());
        }
        double[] years = new double[points.size()];
        double[] values = new double[points.size()];
        int i = 0;
        for (Map.Entry<Double, Double> point : points.entrySet()) {
            years[i] = point.getKey();
            values[i++] = point.getValue();
        }
        return new Curve(years, values, before);
    }

    public static Curve parse(JsonElement json) {
        return parse(json, 0.0);
    }

    public double at(long years) {
        if (years <= 0) {
            return before;
        }
        double log = Math.log10(years);
        if (log < logYears[0] - 1e-9) {
            return before;
        }
        for (int i = 1; i < logYears.length; i++) {
            if (log <= logYears[i]) {
                double t = (log - logYears[i - 1]) / (logYears[i] - logYears[i - 1]);
                return values[i - 1] + (values[i] - values[i - 1]) * t;
            }
        }
        return values[values.length - 1];
    }
}
