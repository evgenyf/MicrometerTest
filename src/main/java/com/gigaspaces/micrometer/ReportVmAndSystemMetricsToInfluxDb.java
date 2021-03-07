package com.gigaspaces.micrometer;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.influx.InfluxConfig;
import io.micrometer.influx.InfluxMeterRegistry;

import java.time.Duration;

public class ReportVmAndSystemMetricsToInfluxDb {

    private final MeterRegistry influxMeterRegistry;

    public ReportVmAndSystemMetricsToInfluxDb(){

        influxMeterRegistry = createRegistry();

        ClassLoaderMetrics classLoaderMetrics = new ClassLoaderMetrics();
        classLoaderMetrics.bindTo(influxMeterRegistry);

        JvmMemoryMetrics jvmMemoryMetrics = new JvmMemoryMetrics();
        jvmMemoryMetrics.bindTo(influxMeterRegistry);

        JvmGcMetrics jvmGcMetrics = new JvmGcMetrics();
        jvmGcMetrics.bindTo(influxMeterRegistry);

        ProcessorMetrics processorMetrics = new ProcessorMetrics();
        processorMetrics.bindTo(influxMeterRegistry);

        JvmThreadMetrics jvmThreadMetrics = new JvmThreadMetrics();
        jvmThreadMetrics.bindTo(influxMeterRegistry);
    }

    private InfluxMeterRegistry createRegistry(){
        InfluxConfig config = new InfluxConfig() {

            @Override
            public Duration step() {
                return Duration.ofSeconds(10);
            }

            @Override
            public String db() {
                return "statsdb";
            }

            @Override
            public String uri(){
                return "http://localhost:8086";
            }

            @Override
            public String get(String k) {
                return null; // accept the rest of the defaults
            }
        };

        return new InfluxMeterRegistry(config, Clock.SYSTEM);
    }

    public static void main( String[] args ){
        new ReportVmAndSystemMetricsToInfluxDb();
        try {
            Thread.sleep( 20_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}