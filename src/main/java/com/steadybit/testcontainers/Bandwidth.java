package com.steadybit.testcontainers;

public final class Bandwidth {
    private final int value;
    private final String unit;

    private Bandwidth(int value, String unit) {
        this.value = value;
        this.unit = unit;
    }

    /**
     * Bits per second
     */
    public static Bandwidth bit(int value) {
        return new Bandwidth(value, "bit");
    }

    /**
     * Kilobits per second
     */
    public static Bandwidth kbit(int value) {
        return new Bandwidth(value, "kbit");
    }

    /**
     * Megabits per second
     */
    public static Bandwidth mbit(int value) {
        return new Bandwidth(value, "mbit");
    }

    /**
     * Gigabits per second
     */
    public static Bandwidth gbit(int value) {
        return new Bandwidth(value, "gbit");
    }

    /**
     * Terabits per second
     */
    public static Bandwidth tbit(int value) {
        return new Bandwidth(value, "tbit");
    }

    /**
     * bytes per second
     */
    public static Bandwidth bps(int value) {
        return new Bandwidth(value, "bps");
    }

    /**
     * Kilobytes per second
     */
    public static Bandwidth kbps(int value) {
        return new Bandwidth(value, "kbps");
    }

    /**
     * Megabytes per second
     */
    public static Bandwidth mbps(int value) {
        return new Bandwidth(value, "mbps");
    }

    /**
     * Gigabytes per second
     */
    public static Bandwidth gbps(int value) {
        return new Bandwidth(value, "gbps");
    }

    /**
     * Terabytes per second
     */
    public static Bandwidth tbps(int value) {
        return new Bandwidth(value, "tbps");
    }

    public int getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    @Override
    public String toString() {
        return value + unit;
    }
}
