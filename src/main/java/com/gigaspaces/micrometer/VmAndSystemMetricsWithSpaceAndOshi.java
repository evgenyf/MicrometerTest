package com.gigaspaces.micrometer;

import com.gigaspaces.internal.jvm.JVMDetails;
import com.gigaspaces.internal.jvm.JVMInfoProvider;
import com.gigaspaces.internal.jvm.JVMStatistics;
import com.j_spaces.core.IJSpace;
import com.j_spaces.core.admin.IRemoteJSpaceAdmin;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.EmbeddedSpaceConfigurer;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VmAndSystemMetricsWithSpaceAndOshi {

    private final SimpleMeterRegistry simpleMeterRegistry = new SimpleMeterRegistry();

    private final ClassLoaderMetrics classLoaderMetrics = new ClassLoaderMetrics();
    private final JvmMemoryMetrics jvmMemoryMetrics = new JvmMemoryMetrics();
    private final JvmGcMetrics jvmGcMetrics = new JvmGcMetrics();
    private final ProcessorMetrics processorMetrics = new ProcessorMetrics();
    private final JvmThreadMetrics jvmThreadMetrics = new JvmThreadMetrics();

    //TODO check os metrics: exposed only: cpu.usage, cpu.count, no memory
    //TODO network statistics: NO
    //TODO compare with oshi metrics
    //TODO check Prometheus, how to report to it
    //TODO how to report vm metrics with host and pid as an additional tags
    //TODO check how to pull metrics data from client, how do another users implement it
    //TODO extension for hsqldb

    public VmAndSystemMetricsWithSpaceAndOshi(){

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

    private GigaSpace createEmbeddedSpace(){
        return new GigaSpaceConfigurer(new EmbeddedSpaceConfigurer("myTestSpace")).create();
    }

    private static JVMInfoProvider spaceJvmProvider = null;
    private static JVMDetails jvmDetails = null;
    private static JVMStatistics latestStatistics;
    public static void main( String[] args ){
        VmAndSystemMetricsWithSpaceAndOshi vmAndSystemMetrics = new VmAndSystemMetricsWithSpaceAndOshi();

        System.out.println( "Before creation of embedded space" );
        GigaSpace embeddedSpace = vmAndSystemMetrics.createEmbeddedSpace();
        IJSpace space = embeddedSpace.getSpace();

        try {
            IRemoteJSpaceAdmin spaceAdmin = (IRemoteJSpaceAdmin)space.getAdmin();
            if( spaceAdmin instanceof JVMInfoProvider ){
                System.out.println("=====JVMInfoProvider");
                spaceJvmProvider = ( JVMInfoProvider )spaceAdmin;
                jvmDetails = spaceJvmProvider.getJVMDetails();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        System.out.println( "Embedded space created" );

        try {
            Thread.sleep( 5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Gauge processCpuUsedGauge = Metrics.globalRegistry.find("process.cpu.usage").gauge();
        Counter vmGcMemoryAllocatedCounter = Metrics.globalRegistry.find( "jvm.gc.memory.allocated" ).counter();
        Gauge vmClassesLoadedGauge = Metrics.globalRegistry.find("jvm.classes.loaded").gauge();
        Gauge vmThreadsCountGauge = Metrics.globalRegistry.find("jvm.threads.live").gauge();

        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getPlatformMXBeans(MemoryPoolMXBean.class);

        List<String> heapMemoryPoolMXBeanIds = new ArrayList<>(3);
        List<String> nonheapMemoryPoolMXBeanIds = new ArrayList<>(3);

        for( MemoryPoolMXBean memoryPoolMXBean : memoryPoolMXBeans ){
            System.out.println( "NAME:" + memoryPoolMXBean.getName() + ", type=" + memoryPoolMXBean.getType());
            if( memoryPoolMXBean.getType() == MemoryType.HEAP ) {
                heapMemoryPoolMXBeanIds.add(memoryPoolMXBean.getName());
            }
            else if( memoryPoolMXBean.getType() == MemoryType.NON_HEAP ){
                nonheapMemoryPoolMXBeanIds.add(memoryPoolMXBean.getName());
            }
        }

        List<Gauge> memoryUsedHeapGauges = new ArrayList<>(3);
        for( String memoryPoolMXBeanName : heapMemoryPoolMXBeanIds ) {
            memoryUsedHeapGauges.add( Metrics.globalRegistry.find("jvm.memory.used").tag("area", "heap").tag("id", memoryPoolMXBeanName).gauge() );
        }

        List<Gauge> memoryMaxHeapGauges = new ArrayList<>(3);
        for( String memoryPoolMXBeanName : heapMemoryPoolMXBeanIds ) {
            memoryMaxHeapGauges.add( Metrics.globalRegistry.find("jvm.memory.max").tag("area", "heap").tag("id", memoryPoolMXBeanName).gauge() );
        }

        List<Gauge> memoryCommittedHeapGauges = new ArrayList<>(3);
        for( String memoryPoolMXBeanName : heapMemoryPoolMXBeanIds ) {
            memoryCommittedHeapGauges.add( Metrics.globalRegistry.find("jvm.memory.committed").tag("area", "heap").tag("id", memoryPoolMXBeanName).gauge() );
        }

        List<Gauge> memoryUsedNonheapGauges = new ArrayList<>(3);
        for( String memoryPoolMXBeanName : nonheapMemoryPoolMXBeanIds ) {
            memoryUsedNonheapGauges.add( Metrics.globalRegistry.find("jvm.memory.used").tag("area", "nonheap").tag("id", memoryPoolMXBeanName).gauge() );
        }

        List<Gauge> memoryCommittedNonheapGauges = new ArrayList<>(3);
        for( String memoryPoolMXBeanName : nonheapMemoryPoolMXBeanIds ) {
            memoryCommittedNonheapGauges.add( Metrics.globalRegistry.find("jvm.memory.committed").tag("area", "nonheap").tag("id", memoryPoolMXBeanName).gauge() );
        }

//        Gauge jvmMemoryMaxHeapGauge = Metrics.globalRegistry.find("jvm.memory.max").tag("area", "heap").gauge();
//        Gauge jvmMemoryMaxNonheapGauge = Metrics.globalRegistry.find("jvm.memory.max").tag("area", "nonheap").gauge();
//        Gauge jvmMemoryHeapCommittedGauge = Metrics.globalRegistry.find("jvm.memory.committed").tag("area", "heap").gauge();
//        Gauge jvmMemoryNonheapCommittedGauge = Metrics.globalRegistry.find("jvm.memory.committed").tag("area", "nonheap").gauge();
//        Gauge jvmBufferMemoryUsedGauge = Metrics.globalRegistry.find("jvm.buffer.memory.used").gauge();

        Runnable cpuFetchTask = () -> {
            try {
                JVMStatistics jvmStatistics = spaceJvmProvider.getJVMStatistics();
                if( latestStatistics != null ) {
                    double cpuFromXap = jvmStatistics.computeCpuPerc(latestStatistics);

                    System.out.println("\n============= CPU ==========");
                    System.out.println( "From Micrometer=" + processCpuUsedGauge.value() );
                    System.out.println( "From XAP=" + cpuFromXap );

                    System.out.println("\n============= MEMORY =============");

                    double usedAccumulatedMemoryHeap = 0;
                    for( Gauge gauge : memoryUsedHeapGauges ){
                        usedAccumulatedMemoryHeap += gauge.value();
                    }

                    double maxAccumulatedMemoryHeap = 0;
                    for( Gauge gauge : memoryMaxHeapGauges ){
                        maxAccumulatedMemoryHeap += gauge.value();
                    }

                    double committedAccumulatedMemoryHeap = 0;
                    for( Gauge gauge : memoryCommittedHeapGauges ){
                        committedAccumulatedMemoryHeap += gauge.value();
                    }

                    double committedAccumulatedMemoryNonheap = 0;
                    for( Gauge gauge : memoryCommittedNonheapGauges ){
                        committedAccumulatedMemoryNonheap += gauge.value();
                    }

                    double usedAccumulatedMemoryNonheap = 0;
                    for( Gauge gauge : memoryUsedNonheapGauges ){
                        usedAccumulatedMemoryNonheap += gauge.value();
                    }

                    System.out.println( "From XAP=" + toMB( jvmStatistics.getMemoryHeapUsed() ) + " MB" );
                    System.out.println( "From Micrometer, accumulated used heap=" + toMB( usedAccumulatedMemoryHeap ) + " MB" );
                    System.out.println();

                    System.out.println( "From XAP committed heap=" + toMB( jvmStatistics.getMemoryHeapCommitted() ) + " MB" );
                    System.out.println( "From Micrometer accumulated COMMITTED heap=" + toMB( committedAccumulatedMemoryHeap ) + " MB" );
                    System.out.println();

                    System.out.println( "From XAP max=" + toMB( jvmDetails.getMemoryHeapMax() ) + " MB" );
                    System.out.println( "From Micrometer accumulated MAX heap=" + toMB( maxAccumulatedMemoryHeap ) + " MB" );
                    System.out.println();

                    System.out.println( "From XAP memoryNonheapUsed=" + toMB( jvmStatistics.getMemoryNonHeapUsed() ) + " MB" );
                    System.out.println( "From Micrometer accumulated used nonheap=" + toMB( usedAccumulatedMemoryNonheap ) + " MB" );
                    System.out.println();

                    System.out.println( "From XAP memoryNonheapCommitted=" + toMB( jvmStatistics.getMemoryNonHeapCommitted() ) + " MB" );
                    System.out.println( "From Micrometer accumulated COMMITTED nonheap=" + toMB( committedAccumulatedMemoryNonheap ) + " MB" );
                    System.out.println();

                    System.out.println("============= END OF MEMORY =============");

                    /*
                    long memoryHeapUsed = jvmStatistics.getMemoryHeapUsed();
                    long memoryNonheapUsed = jvmStatistics.getMemoryNonHeapUsed();
                    long memoryHeapCommitted = jvmStatistics.getMemoryHeapCommitted();
                    long memoryNonheapCommitted = jvmStatistics.getMemoryNonHeapCommitted();

                    System.out.println( "From Micrometer=" + toMB( memoryUsedGauge.value() ) + " MB" );
                    System.out.println( "From Micrometer heap=" + toMB( memoryUsedHeapGauge.value() ) + " MB" );
                    System.out.println( "From Micrometer nonheap=" + toMB( memoryUsedNonheapGauge.value() ) + " MB" );

                    System.out.println( "From Micrometer PsEdenSpace heap=" + toMB( memoryUsedHeapPsEdenSpaceGauge.value() ) + " MB" );
                    System.out.println( "From Micrometer PsOldGen heap=" + toMB( memoryUsedHeapPsOldGenGauge.value() ) + " MB" );
                    System.out.println( "From Micrometer PsSurvivorSpace heap=" + toMB( memoryUsedHeapPsSurvivorSpaceGauge.value() ) + " MB" );
                    System.out.println( "From Micrometer all ids heap=" + toMB( memoryUsedHeapAllIdsGauge.value() ) + " MB" );

                    System.out.println( "From Micrometer jvmMemoryHeapCommitted=" + toMB( jvmMemoryHeapCommittedGauge.value() ) + " MB" );
                    System.out.println( "From Micrometer jvmMemoryNonheapCommitted=" + toMB( jvmMemoryNonheapCommittedGauge.value() ) + " MB" );
                    System.out.println( "From Micrometer jvmMemoryHeapMax=" + toMB(jvmMemoryMaxHeapGauge.value() ) + " MB" );
                    System.out.println( "From Micrometer jvmMemoryNonheapMax=" + toMB(jvmMemoryMaxNonheapGauge.value() ) + " MB" );*/
                    //System.out.println( "From Micrometer TOTAL jvmMemoryMax=" + toMB(jvmMemoryMaxNonheapGauge.value() + jvmMemoryMaxHeapGauge.value()) + " MB" );
                }

                latestStatistics = jvmStatistics;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        };

        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(cpuFetchTask,  0, 5, TimeUnit.SECONDS);

        try {
            JVMStatistics jvmStatistics = spaceJvmProvider.getJVMStatistics();
            int threadCount = jvmStatistics.getThreadCount();
            System.out.println( "====== oshi metrics =====" );
            System.out.println( "> threadCount=" + threadCount );
            System.out.println( "====== oshi metrics END=====\n\n" );
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        double vmClassesLoaded = vmClassesLoadedGauge.value();

        double vmGcMemoryAllocatedCount = vmGcMemoryAllocatedCounter.count();
        double threadsCount = vmThreadsCountGauge.value();

        System.out.println( "vmClassesLoaded=" + vmClassesLoaded );
        System.out.println( "vmGcMemoryAllocatedCount=" + vmGcMemoryAllocatedCount );
        System.out.println( "threadsCount=" + threadsCount );
    }

    public static double toMB( double bytes ){
        return bytes/1000/1000;
    }
}