package com.gigaspaces.micrometer;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.influx.InfluxConfig;
import io.micrometer.influx.InfluxMeterRegistry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ReportVmAndSystemMetricsToInfluxDbUsingFilter {

    private final MeterRegistry influxMeterRegistry;

    public ReportVmAndSystemMetricsToInfluxDbUsingFilter(){

        influxMeterRegistry = createRegistry();

        addFilter();

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

    private void addFilter() {
        influxMeterRegistry.config().meterFilter(new MeterFilter() {
            @Override
            public MeterFilterReply accept( Meter.Id id ) {
                final MeterFilterReply reply =
                        ( id.getName().startsWith("jvm.gc.max.data.size") ||
                          id.getName().startsWith("jvm.gc.live.data.size") ||
                          id.getName().startsWith("jvm.gc.memory.allocated") ||
                          id.getName().startsWith( "jvm.gc.memory.promoted" ) )
                                ? MeterFilterReply.ACCEPT : MeterFilterReply.DENY;
                System.out.println( "filter reply of meter " + id + ": " + reply );
                return reply;
            }
        });
    }

    private InfluxMeterRegistry createRegistry(){
        InfluxConfig config = new InfluxConfig() {

            @Override
            public Duration step() {
                return Duration.ofSeconds(10);
            }

            @Override
            public String db() {
                return "statsdbf";
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
        new ReportVmAndSystemMetricsToInfluxDbUsingFilter();
        try {
            Thread.sleep( 20_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}