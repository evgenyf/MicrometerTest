package com.gigaspaces.micrometer;

import com.gigaspaces.internal.utils.GsEnv;
import com.gigaspaces.micrometer.hsqldb.HsqldbMetricsRegistry;
import com.gigaspaces.micrometer.hsqldb.HsqldbRegistryConfig;
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
import java.util.Properties;

public class ReportVmAndSystemMetricsToHsqlDb {

    private final MeterRegistry hsqldbMeterRegistry;

    public ReportVmAndSystemMetricsToHsqlDb(){
        hsqldbMeterRegistry = createRegistry();
        addFilter();

        ClassLoaderMetrics classLoaderMetrics = new ClassLoaderMetrics();
        classLoaderMetrics.bindTo(hsqldbMeterRegistry);

        JvmMemoryMetrics jvmMemoryMetrics = new JvmMemoryMetrics();
        jvmMemoryMetrics.bindTo(hsqldbMeterRegistry);

        JvmGcMetrics jvmGcMetrics = new JvmGcMetrics();
        jvmGcMetrics.bindTo(hsqldbMeterRegistry);

        ProcessorMetrics processorMetrics = new ProcessorMetrics();
        processorMetrics.bindTo(hsqldbMeterRegistry);

        JvmThreadMetrics jvmThreadMetrics = new JvmThreadMetrics();
        jvmThreadMetrics.bindTo(hsqldbMeterRegistry);
    }

    private HsqldbMetricsRegistry createRegistry(){
        return new HsqldbMetricsRegistry( new HsqldbRegistryConfig( createHsqlDbProperties() ), Clock.SYSTEM);
    }

    private void addFilter(){
        hsqldbMeterRegistry.config().meterFilter(new MeterFilter() {
            @Override
            public MeterFilterReply accept(Meter.Id id ) {
                final MeterFilterReply reply =
                         id.getName().startsWith("process.cpu.usage") ? MeterFilterReply.ACCEPT : MeterFilterReply.DENY;
                //System.out.println( "filter reply of meter " + id + ": " + reply );
                return reply;
            }
        });
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
        new ReportVmAndSystemMetricsToHsqlDb();
        try {
            Thread.sleep( 30_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}