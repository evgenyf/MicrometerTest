package com.gigaspaces.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.search.Search;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

public class MyGaugeTest {

    @Test
    public void givenGlobalRegistry_whenIncrementAnywhere_thenCounted() {


        class CountedObject {
            private CountedObject() {
                Metrics.gauge("gauge.objects.count", new AtomicInteger( 111 )).set(123456);
            }
        }

        AtomicInteger myGauge = Metrics.globalRegistry.gauge("numberGauge", new AtomicInteger(0));

        myGauge.set( 123 );
        myGauge.set( 323 );

        System.out.println(myGauge.get());

        new CountedObject();
        Gauge gauge = Metrics.globalRegistry.find("gauge.objects.count").gauge();

        Gauge gauge2 = Metrics.globalRegistry.find("numberGauge").gauge();
        double value2 = gauge2.value();

        Iterable<Measurement> measure = gauge.measure();
        Iterator<Measurement> iterator = measure.iterator();
        System.out.println(iterator.hasNext());
        System.out.println(iterator.next());

        System.out.println(gauge.value());

    }
}
