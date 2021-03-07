package pizza;

import com.gigaspaces.micrometer.InfluxTest;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.influx.InfluxConfig;
import io.micrometer.influx.InfluxMeterRegistry;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/*
@RestController
@RequestMapping(“/v1/”)
*/
public class PizzaOrderController {
    private final MeterRegistry influxMeterRegistry;
/*    @RequestMapping(value = “create”, method = RequestMethod.POST)
    public long createOrder(@RequestParam String partnerId,
                            @RequestParam String type,@RequestParam String location) {
        long orderId = createOrder(type);
        increaseCount(partnerId, “received”);
        return orderId;
    }
    private void increaseCount(String partnerId, String state) {
        // Counter class stores the measurement name and the tags and
        // their values
        Counter counter =Metrics.counter(“request.orders”,  “partnerId”,
                partnerId, “state”, state);
        counter.increment();
    }
    private long createOrder(String type) {
        // create order
        return (long) Math.random();
    }
    private void processOrders() {
        List<Order> orders = getReceivedOrders();
        int processedOrders = 0;
        orders.forEach(order -> {
            bool processed = processOrder(order);
            if(processed){
                increaseCount(order.getPartnerId(), “processed”);
            }
        });
    }*/

/*
    private Counter featureCounter;
    private Counter feature2Counter;*/

    public PizzaOrderController(){

        influxMeterRegistry = createRegistry();
        //featureCounter = Metrics.counter("feature", "region", "test"); //(1)
    }

    public long createOrder(int customerId/*, String type*/) {
        long orderId = createOrder();
        increaseCount(customerId, "received");
        return orderId;
    }

    private void processOrder( int customerId ) {
        increaseCount(customerId, "processed");
    }

/*    private void processOrders() {
        List<Order> orders = getReceivedOrders();
        int processedOrders = 0;
        orders.forEach(order -> {
            boolean processed = processOrder(order);
            if(processed){
                increaseCount(order.getPartnerId(), "processed");
            }
        });
    }
    */

    private void increaseCount(int customerId, String state) {
        // Counter class stores the measurement name and the tags and
        // their values
        Counter counter = influxMeterRegistry.counter("request.orders",  "customerId", String.valueOf( customerId ), "state", state );
        counter.increment();
    }

    private long createOrder() {
        // create order
        return (long) Math.random();
    }


    private InfluxMeterRegistry createRegistry(){
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

        return new InfluxMeterRegistry(config, Clock.SYSTEM);
        //feature2Counter = registry.counter("feature", "region1", "kkk");
    }

/*    public void increment(){
        featureCounter.increment();
        feature2Counter.increment();
    }*/

    public static void main( String[] args ){

        PizzaOrderController pizzaOrderController = new PizzaOrderController();

        for( int customerCounter = 0; customerCounter < 10; customerCounter++ ){
            System.out.println( "Before create order " + customerCounter );
            pizzaOrderController.createOrder( customerCounter );
            System.out.println( "After create order " + customerCounter );
            try {
                Thread.sleep( 10_000 );
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println( "Before process order " + customerCounter );
            pizzaOrderController.processOrder( customerCounter );
            System.out.println( "After process order " + customerCounter );
            try {
                Thread.sleep( 10_000 );
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

/*        influxTest.increment();
        influxTest.increment();*/

        try {
            Thread.sleep( TimeUnit.MINUTES.toMillis(10) );
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}