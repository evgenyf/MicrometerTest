package com.gigaspaces.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Assert;
import org.junit.Test;

public class MyFirstTest {

    @Test
    public void givenGlobalRegistry_whenIncrementAnywhere_thenCounted() {
        class CountedObject {
            private CountedObject() {
                Metrics.counter("objects.instance").increment(1.0);
            }
        }

        Metrics.addRegistry(new SimpleMeterRegistry());

        Metrics.counter("objects.instance").increment();
        new CountedObject();

        Counter counter = Metrics.globalRegistry
                .find("objects.instance").counter();
        System.out.println( "Counter=" + counter.count() );
        Assert.assertTrue(counter != null);
        Assert.assertTrue(counter.count() == 2.0);



    }
}
