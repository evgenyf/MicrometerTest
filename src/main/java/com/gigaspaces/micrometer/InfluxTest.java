package com.gigaspaces.micrometer;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.influx.InfluxConfig;
import io.micrometer.influx.InfluxMeterRegistry;

import java.time.*;
import java.util.concurrent.TimeUnit;

public class InfluxTest {

    //private Counter featureCounter;
    private Counter feature2Counter;

    public InfluxTest(){

        //featureCounter = Metrics.counter("feature1", "region", "test"); //(1)
    }

    public void createRegistry(){
        InfluxConfig config = new InfluxConfig() {

            @Override
            public Duration step() {
                return Duration.ofSeconds(10);
            }

            @Override
            public String db() {
                return "mydb";
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

        MeterRegistry registry = new InfluxMeterRegistry(config, Clock.SYSTEM);
        feature2Counter = registry.counter("feature1", "region1", "kkk");
    }

    public void increment(){
        //featureCounter.increment();
        feature2Counter.increment();
    }

    public static void main( String[] args ){

        InfluxTest influxTest = new InfluxTest();

        influxTest.createRegistry();

        influxTest.increment();
        influxTest.increment();

        try {
            Thread.sleep( TimeUnit.MINUTES.toMillis(10) );
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

/*
*
*   QUERIES
*   SELECT * FROM "mydb"."autogen"."feature"
*   SELECT * FROM "mydb"."autogen"."feature"
*
*
*
*/
