package com.syntricdb.net;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * OpenTelemetry & Prometheus metrics collector delivering real-time metrics at /metrics endpoint.
 */
public class MetricsCollector {
    private static final AtomicLong queryCounter = new AtomicLong(0);
    private static final AtomicLong activeConnections = new AtomicLong(0);

    public static void incrementQueryCount() {
        queryCounter.incrementAndGet();
    }

    public static void incrementConnections() {
        activeConnections.incrementAndGet();
    }

    public static void decrementConnections() {
        activeConnections.decrementAndGet();
    }

    public static String getPrometheusMetrics() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();

        StringBuilder sb = new StringBuilder();
        sb.append("# HELP syntricdb_queries_total Total executed SQL and AI queries\n");
        sb.append("# TYPE syntricdb_queries_total counter\n");
        sb.append("syntricdb_queries_total ").append(queryCounter.get()).append("\n\n");

        sb.append("# HELP syntricdb_active_connections Current active Netty network connections\n");
        sb.append("# TYPE syntricdb_active_connections gauge\n");
        sb.append("syntricdb_active_connections ").append(Math.max(0, activeConnections.get())).append("\n\n");

        sb.append("# HELP syntricdb_jvm_heap_bytes_used JVM Heap Memory Used\n");
        sb.append("# TYPE syntricdb_jvm_heap_bytes_used gauge\n");
        sb.append("syntricdb_jvm_heap_bytes_used ").append(heapUsed).append("\n\n");

        sb.append("# HELP syntricdb_jvm_heap_bytes_max JVM Heap Memory Max\n");
        sb.append("# TYPE syntricdb_jvm_heap_bytes_max gauge\n");
        sb.append("syntricdb_jvm_heap_bytes_max ").append(heapMax).append("\n");

        return sb.toString();
    }
}
