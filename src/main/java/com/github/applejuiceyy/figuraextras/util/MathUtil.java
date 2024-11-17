package com.github.applejuiceyy.figuraextras.util;

public class MathUtil {
    public static double map(double value, double min, double max, double rmin, double rmax) {
        return (value - min) / (max - min) * (rmax - rmin) + rmin;
    }

    public static double length(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(Math.abs(x1 - x2), 2) + Math.pow(Math.abs(y1 - y2), 2));
    }

    public static double constrain(double v, double min, double max) {
        return Math.min(max, Math.max(min, v));
    }

    public static double angleTo(double x1, double y1, double x2, double y2) {
        return Math.atan2(y2 - y1, x2 - x1);
    }
}
