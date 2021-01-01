package com.github.archturion64.procstatparser.model;

/**
 * class designed to contain a pair of values
 * representing the current CPU load and idle time in jiffies.
 */
public class CpuLoad {

    private final long cpuLoadTotal;
    private final long cpuIdleValue;

    public CpuLoad(long cpuLoadTotal, long cpuIdleValue) {
        this.cpuLoadTotal = cpuLoadTotal;
        this.cpuIdleValue = cpuIdleValue;
    }

    public long getCpuLoadTotal() {
        return cpuLoadTotal;
    }

    public long getCpuIdleValue() {
        return cpuIdleValue;
    }
}
