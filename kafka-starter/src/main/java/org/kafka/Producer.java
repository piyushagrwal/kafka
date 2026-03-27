package org.kafka;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class Producer {

    public static void main(String[] args) {

        // connect to server
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers","127.0.0.1:9092");
        properties.setProperty("security.protocol","PLAINTEXT");
//        properties.setProperty("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"user-new\" password=\"password-new\";");
//        properties.setProperty("sasl.mechanism", "PLAIN");

        // Set producer properties
        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer", StringSerializer.class.getName());

        // can set batch size for producer (default batch size 16KB)
//        properties.setProperty("batch.size","400");
        // To set partitioner (Not Recommended)
//        properties.setProperty("partitoner.class", RoundRobinPartitioner.class.getName());
        // set safe producer configs
        properties.setProperty("acks","all");
        properties.setProperty("enable.idempotence", "true");
        properties.setProperty("retries", Integer.toString(Integer.MAX_VALUE));
        properties.setProperty("timeout delivery.timeout.ms", "120000");
        properties.setProperty("retry.backoff.ms", "100");

        // For high throughput producer
        properties.setProperty("linger.ms","20");
        properties.setProperty("batch.size",Integer.toString(32*1024));
        properties.setProperty("compression.type", "snappy");

        // create the producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        // Create a producer record
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>("topic_name","message");

        // Send Data
        producer.send(producerRecord);

        // Send Data with callback
        producer.send(producerRecord, new Callback() {
            @Override
            public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                // executes every time a record is successfully sent or exception is thrown
                if(e == null){
                    System.out.println("Received Metadata");
                    System.out.println("Topic: "+ recordMetadata.topic());
                    System.out.println("Partition: "+ recordMetadata.partition());
                    System.out.println("Offset: "+ recordMetadata.offset());
                    System.out.println("Timestamp: "+ recordMetadata.timestamp());
                }else{
                    System.out.println("Exception occured");
                }
            }
        });

        // Send Data with keys
        ProducerRecord<String, String> producerRecordWithKey = new ProducerRecord<>("topic_name","key","message_with_key");
        producer.send(producerRecordWithKey);

        // Tells producer to send all data and block until done -- Synchronous
        producer.flush();

        // Flush and close the producer
        producer.close();
    }
}
