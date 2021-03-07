package com.gigaspaces.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.List;
import java.util.Set;

public class CompositeMeterRegistryExample {

    public CompositeMeterRegistryExample(){

    }

    public static void main( String[] args ){
        CompositeMeterRegistry composite = new CompositeMeterRegistry();

        Counter compositeCounter = composite.counter("counter");
        compositeCounter.increment(); //(1)

        Integer gaugeCounter1 = composite.gauge("gaugecounter1", 1);
        Integer gaugeCounter2 = composite.gauge("gaugecounter2", 2);

        SimpleMeterRegistry simple1 = new SimpleMeterRegistry();
        composite.add(simple1); //(2)

        SimpleMeterRegistry simple2 = new SimpleMeterRegistry();
        composite.add(simple2); //(2)

        compositeCounter.increment();
        compositeCounter.increment();

        List<Meter> meters = composite.getMeters();
        Set<MeterRegistry> registries = composite.getRegistries();

        System.out.println( "meters size=" + meters.size() );
        System.out.println( "registers size=" + registries.size() );

        System.out.println( "Meters size=" + meters.size() );
        System.out.println( "Registers size=" + registries.size() );

/*        Counter counter = Metrics.globalRegistry
                .find("counter").counter();
        System.out.println( ">>> Counter=" + counter.count() );*/


        System.out.println( ">>> compositeCounter=" + compositeCounter.count() );
    }
}
