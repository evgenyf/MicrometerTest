package com.gigaspaces.micrometer;

import com.gigaspaces.internal.utils.GsEnv;
import com.gigaspaces.micrometer.hsqldb.HsqldbMetricsRegistry;
import com.gigaspaces.micrometer.hsqldb.HsqldbRegistryConfig;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
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
import java.util.Properties;

public class ReportVmAndSystemMetricsToFewRegisters {

    private final MeterRegistry hsqldbMeterRegistry;
    private final InfluxMeterRegistry influxDbRegistry;

    public ReportVmAndSystemMetricsToFewRegisters(){
        hsqldbMeterRegistry = createHsqldbRegistry();
        influxDbRegistry = createInfluxDbRegistry();

        ClassLoaderMetrics classLoaderMetrics = new ClassLoaderMetrics();
        classLoaderMetrics.bindTo(hsqldbMeterRegistry);
        classLoaderMetrics.bindTo(influxDbRegistry);

        JvmMemoryMetrics jvmMemoryMetrics = new JvmMemoryMetrics();
        jvmMemoryMetrics.bindTo(hsqldbMeterRegistry);
        jvmMemoryMetrics.bindTo(influxDbRegistry);

        JvmGcMetrics jvmGcMetrics = new JvmGcMetrics();
        jvmGcMetrics.bindTo(hsqldbMeterRegistry);
        jvmGcMetrics.bindTo(influxDbRegistry);

        ProcessorMetrics processorMetrics = new ProcessorMetrics();
        processorMetrics.bindTo(hsqldbMeterRegistry);
        processorMetrics.bindTo(influxDbRegistry);

        JvmThreadMetrics jvmThreadMetrics = new JvmThreadMetrics();
        jvmThreadMetrics.bindTo(hsqldbMeterRegistry);
        jvmThreadMetrics.bindTo(influxDbRegistry);
    }

    private HsqldbMetricsRegistry createHsqldbRegistry(){
        HsqldbMetricsRegistry hsqldbMetricsRegistry =
                new HsqldbMetricsRegistry( new HsqldbRegistryConfig( createHsqlDbProperties() ), Clock.SYSTEM);

        hsqldbMetricsRegistry.config().meterFilter(new MeterFilter() {
            @Override
            public MeterFilterReply accept(Meter.Id id ) {
                final MeterFilterReply reply =
                        id.getName().startsWith("process.cpu.usage") ? MeterFilterReply.ACCEPT : MeterFilterReply.DENY;
                //System.out.println( "filter reply of meter " + id + ": " + reply );
                return reply;
            }
        });

        return hsqldbMetricsRegistry;
    }

    private InfluxMeterRegistry createInfluxDbRegistry(){
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

    private Properties createHsqlDbProperties() {
        Properties properties = new Properties();
        properties.setProperty("driverClassName", "org.hsqldb.jdbc.JDBCDriver");
        properties.setProperty("dbname", "metricsdb");
        properties.setProperty("username", "sa");
        properties.setProperty("password", "");
        properties.setProperty("host", GsEnv.property("com.gs.ui.metrics.db.host").get( "localhost" /*firstHost*/ ));
        properties.setProperty("port", GsEnv.property("com.gs.ui.metrics.db.port").get( "9101" ));

        return properties;
    }

    public static void main( String[] args ){
        new ReportVmAndSystemMetricsToFewRegisters();
        try {
            Thread.sleep( 30_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}