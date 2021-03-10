package com.gigaspaces.micrometer;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.search.Search;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.influx.InfluxConfig;
import io.micrometer.influx.InfluxMeterRegistry;

import java.time.Duration;

public class VmAndSystemMetrics {

    private final SimpleMeterRegistry simpleMeterRegistry = new SimpleMeterRegistry();

    private final ClassLoaderMetrics classLoaderMetrics = new ClassLoaderMetrics();
    private final JvmMemoryMetrics jvmMemoryMetrics = new JvmMemoryMetrics();
    private final JvmGcMetrics jvmGcMetrics = new JvmGcMetrics();
    private final ProcessorMetrics processorMetrics = new ProcessorMetrics();
    private final JvmThreadMetrics jvmThreadMetrics = new JvmThreadMetrics();

    //TODO check os metrics: exposed only: cpu.usage, cpu.count, no memory - v
    //TODO network statistics: NO - v
    //TODO compare with oshi metrics - done ( heap max is still different ) -
    //TODO check how to pull metrics data from client, how do another users implement it
    //TODO check Prometheus, how to report to it
    //TODO extension for hsqldb
    //TODO add pid and IP as an additional tags


    public VmAndSystemMetrics(){

        Metrics.addRegistry( simpleMeterRegistry );

/*        classLoaderMetrics.bindTo(simpleMeterRegistry);
        jvmMemoryMetrics.bindTo(simpleMeterRegistry);
        jvmGcMetrics.bindTo(simpleMeterRegistry);
        processorMetrics.bindTo(simpleMeterRegistry);
        jvmThreadMetrics.bindTo(simpleMeterRegistry);*/

        classLoaderMetrics.bindTo(Metrics.globalRegistry);
        jvmMemoryMetrics.bindTo(Metrics.globalRegistry);
        jvmGcMetrics.bindTo(Metrics.globalRegistry);
        processorMetrics.bindTo(Metrics.globalRegistry);
        jvmThreadMetrics.bindTo(Metrics.globalRegistry);
    }


    public static void main( String[] args ){
        VmAndSystemMetrics vmAndSystemMetrics = new VmAndSystemMetrics();
        try {
            Thread.sleep( 3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long startTimeFind = System.currentTimeMillis();
        Gauge memoryUsedGauge = Metrics.globalRegistry.find("jvm.memory.used").gauge();
        System.out.println( "Find took:" + ( System.currentTimeMillis() - startTimeFind ) + " msec." );

        Gauge processCpuUsedGauge = Metrics.globalRegistry.find("process.cpu.usage").gauge();
        Counter vmGcMemoryAllocatedCounter = Metrics.globalRegistry.find( "jvm.gc.memory.allocated" ).counter();
        Gauge vmClassesLoadedGauge = Metrics.globalRegistry.find("jvm.classes.loaded").gauge();


        long startTimeGetValue = System.currentTimeMillis();
        double memoryUsedValue = memoryUsedGauge.value();
        //System.out.println( "Getting value took:" + ( System.currentTimeMillis() - startTimeGetValue ) + " msec." );

        double vmClassesLoaded = vmClassesLoadedGauge.value();
        double processCpuUsed = processCpuUsedGauge.value();
        double vmGcMemoryAllocatedCount = vmGcMemoryAllocatedCounter.count();

        System.out.println( "vmClassesLoaded=" + vmClassesLoaded );
        System.out.println( "memoryUsedValue=" + memoryUsedValue );
        System.out.println( "vmGcMemoryAllocatedCount=" + vmGcMemoryAllocatedCount );
        System.out.println( "processCpuUsed=" + processCpuUsed );
    }
}