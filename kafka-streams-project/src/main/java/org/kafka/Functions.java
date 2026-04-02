package org.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;

import java.util.Properties;

import static org.apache.kafka.streams.kstream.Materialized.as;

public class Functions {
    public static void main(String[] args) {
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "favourite-colour-java");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        // we disable the cache to demonstrate all the "steps" involved in the transformation - not recommended in prod
        config.put(StreamsConfig.STATESTORE_CACHE_MAX_BYTES_CONFIG, "0");

        // For exactly once semantics
        config.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> textLines = builder.stream("demo-topic");
        textLines.to("intermediate-topic");
        KTable<String, String> table = builder.table("intermediate-topic");

        // Using group by
        KGroupedTable<String, String> groupedTable = table.groupBy(
                (key, value) -> new KeyValue<>(value, value));
        KGroupedStream<String, String> groupedStream = textLines.groupByKey();

        // KGroupedStream Aggregate
        KTable<String, Long> aggregatedStream = groupedStream.aggregate(
                () -> 0L,
                (aggKey, newValue, aggValue) -> aggValue + newValue.length(),
                Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as("aggregated-stream-store")
                        .withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.Long()));

        // KGroupedTable Aggregate
        KTable<String, Long> aggregatedStreamTable = groupedTable.aggregate(
                () -> 0L,
                (aggKey, newValue, aggValue) -> aggValue + newValue.length(),
                (aggKey, oldValue, aggValue) -> aggValue - oldValue.length(),
                Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as("aggregated-stream-store")
                        .withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.Long()));

        // Reduce KGroupedStream
        KTable<String, String> reducedStream = groupedStream.reduce(
                (aggValue, newValue) -> aggValue + newValue,
                Materialized.<String, String, KeyValueStore<Bytes, byte[]>>as("reduced-stream-store")
                        .withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.String()));

        // Reduce KGroupedTable
        KTable<String, String> reducedStreamTable = groupedTable.reduce(
                (newValue, aggValue) -> aggValue + newValue, // Added
                (oldValue, aggValue) -> aggValue + oldValue, // Subtractor
                Materialized.<String, String, KeyValueStore<Bytes, byte[]>>as("aggregated-stream-store")
                        .withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.String()));

        // Peek
        KStream<String, String> peekStream = textLines.peek(
                (key, value) -> System.out.println("key= "+key+", value="+value));


    }
}
