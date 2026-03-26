package org.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class Consumer {

    public static void main(String[] args) {

        String groupId = "my_group_id";
        // connect to server
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers","127.0.0.1:9092");
        properties.setProperty("security.protocol","PLAINTEXT");

        // create consumer config based on type of data sent
        properties.setProperty("key.deserializer", StringDeserializer.class.getName());
        properties.setProperty("value.deserializer", StringDeserializer.class.getName());

        properties.setProperty("group.id",groupId);
        // values for this can be none/earliest/latest
        properties.setProperty("auto.offset.reset","earliest");
        // To set partition assignment strategy
        properties.setProperty("partition.assignment.strategy", CooperativeStickyAssignor.class.getName());
        // To set group instance id
//        properties.setProperty("group.instance.id","id");
        // create a consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);

        // For graceful shutdown
        final Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread(){
            public void run(){
                consumer.wakeup();
                // Join main thread to allow execution from main thread
                try {
                    mainThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });


        try {
            // Subscribe for data
            consumer.subscribe(Arrays.asList("topic_name"));

            // Poll for data
            while (true) {
                System.out.println("Polling");
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, String> record : records) {
                    System.out.println(record.key());
                    System.out.println(record.value());
                }
            }
        }catch (WakeupException e){
            System.out.println("Consumer starting to shut down");
        } catch (Exception e) {
            System.out.println("Exception occured");
        } finally {
            consumer.close(); // close consumer, this also commit offsets
            System.out.println("Consumer gracefully shoutdown");
            // Now consumer gracefully shutdown
        }
    }
}
