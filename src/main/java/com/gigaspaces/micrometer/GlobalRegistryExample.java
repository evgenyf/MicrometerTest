package com.gigaspaces.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

public class GlobalRegistryExample {

    public GlobalRegistryExample(){
    }

    Counter featureCounter = Metrics.counter("feature", "region", "test"); //(1)

    void feature() {
        featureCounter.increment();
    }

    void feature2(String type) {
        Counter counter = Metrics.counter("feature.2", "type", type);
        counter.increment(); //(2)
    }

    static class MyApplication {
        void start() {
            // wire your monitoring system to global static state
            Metrics.addRegistry(new SimpleMeterRegistry()); //(3)
        }
    }

    public static void main( String[] args ){
        GlobalRegistryExample globalRegistryExample = new GlobalRegistryExample();
        globalRegistryExample.feature();
        globalRegistryExample.feature2("d");
        ( new MyApplication() ).start();
        System.out.println( "Registers count:" + Metrics.globalRegistry.getRegistries().size() );
        System.out.println( "Meters count:" + Metrics.globalRegistry.getMeters().size() );
    }
}
