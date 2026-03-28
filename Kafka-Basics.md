**Kafka**

Kafka is a data transporting system which sits between our systems. The systems which produce data send data to kafka and it then sends to consumers.  
Eg. Suppose there are 4 source systems and 6 consuming systems. Without kafka we need to create 24 subsystems to handle but with kafka only kafka is required between systems.

**Topics:** A particular stream of data. It is like a table in db without any constraints. Identified by name. Any kind of message format. Sequence of messages is called data stream. These can not be queries. Producers send and consumers read data.

**Partitions:** Topics are split into partitions like Partition 0, Partition 1 etc. Messages in each partition are ordered. Each message in the partition gets an incremental id called offset.  
Topics are immutable.Once data is written to partition, it can not be changed.  
Data is kept for limited time (default one week \- configurable)  
Offsets are not re-used if previous messages are deleted.  
Order is guaranteed only within a partition. Data is assigned randomly to the partition unless a key is provided.

**Producers:** Producers write data to topics. Producers know in advance to which partition they need to write data and which broker has it. In case of broker failure, producers automatically recover. Load is balanced and every partition gets blanched number of messages. So kafka scales.

**Message Keys:** Producers can send a key with a message. If not present, data is sent by round robin. If the key is not null, a message is sent using the hashing key (by kafka partitioner logic). By default, keys are hashed using the murmur2 algorithm.

Kafka Message Structure:

| Key- binary can be null | Value- binary can be null |
| :---: | :---: |
| Compression Type (none, gzip..) |  |
| Headers (key-value) |  |
| Partition \+ Offset |  |
| Timestamp (system or user set) |  |

Messages are created by Kafka Message Serializer. Kafka only accepts and returns bytes. So the serializer transforms objects/ data into bytes. They are used for the value and key.

**Consumers:** Consumers read data from topic (pull model). Consumers know which broker to read from. In case of failures, they know how to recover.  
Consumer Deserializer transforms bytes into objects/ data. Used on key and value. The serialisation / deserialization type must not change during a topic lifecycle (create a new topic if required).

**Consumer Groups:** All the consumers in an application read data as consumer groups. Each consumer within a group reads from exclusive partitions. If we have more consumers than partitions, then extra ones are inactive. We can have multiple consumer groups on the same topic.  
Within one consumer group, one partition can have only one consumer.  
In groups we can store the offsets consumer group has been reading. The offsets are committed in Kafka topic name \_\_consumer\_offsets.  
When consumer in a group has processed data, it periodically commits offsets. (Kafka broker writes to \_\_consumer\_offsets). If a consumer dies, it can read back from it.

**Delivery Semantics:** By default, at least once.  
If we commit manually:

* At least once \- offsets committed after message processed. Duplication possible  
* At most once \- offsets committed when message received.  
* Exactly once \- Transactional API, Idempotent consumer

**Brokers:** A kafka cluster is made up of multiple brokers(servers). Each broker is identified with its id. Each broker contains certain topic partitions. After connecting to broker, you will be connected to entire cluster.  
A good number to get started is 3 broker but some big clusters can have also 100 brokers.  
Topic, Partition combination can be inside any broker. Not necessary that one topic is inside only one broker.

**Kafka Broker Discovery:** Every kafka broker is bootstrap server. We need to connect to only one broker and kafka clients will know how to connect to other brokers inside cluster.

Client sends connection \+ metadata request \-\> receives list of all brokers from cluster \-\> can connect to the needed brokers

**Topic Replication Factor:** Topic should have \> 1 (between 2 to 3 in prod). If a broker is down, another can serve the data. It creates a copy of a partition of a topic in other brokers.  
So if one broker is down, other can serve data.   
At any time, only one broker can be leader of a given partition and consumers consume from the leader only. Producers can send data to the broker that is the leader of the partition. So each partition has one leader and other multiple ISR(In-sync replica).

Since kafka 2.4, it is possible to configure consumers to read from closest replica.

Producers can choose to receive acknowledgements of data writes.  
acks=0, Producer won’t wait for acknowledgement  
acks=1, Producer wait for leader broker acknowledgement  
acks=all or \-1, Producer wait for leader+replica acknowledgement (works with min.insync.replicas)  
**Zookeeper:** Zookeeper manages brokers. Helps in performing leader election for partitions. Sends notifications to kafka in case of changes.  
One  leader in multiple zookeeper instances and brokers attached to different instances of zookeeper.  
Till Kafka 2.x, kafka can’t work without zookeeper  
Kafka 3.x can work without zookeeper \- using kafka KRaft  
Kafka 4.x will not have zookeeper  
Zookeeper operates with odd number of servers. A leader and rest servers are followers (reads). Does not store consumer offsets with Kafka \> v0.10.

**Important:** Use zookeeper with kafka broker if kafka version \< 4.x  
Don’t use zookeeper with kafka clients as it is less secure.  
Zookeeper shows scaling issues  when clusters have \> 100000 partitions

**Kafka KRaft:** One leader in quorum controller in broker. Better performance than zookeeper.

Install kafka and start server by using below command in kafka installed folder.  
bin/kafka-server-start.sh config/server.properties

—- bootstrap-server (localhost:9092) or 127.0.0.1:9092

**Kafka Topic Management:** Using CLI, we can create kafka topics.   
In localhost we can have replication factor 1 only. For clusters in others we can have a replication factor up to the number of brokers.  
Kafka CLI commands are in another folder in repo.

**Kafka Producer using Java:**

* Create properties object and set server properties  
* Set producer properties (key and value serializer)  
* Create producer  
* Create producer record  
* Send data  
* Flush or close as per requirement

**Producer Callbacks:**  
Confirm the partition and offset the message was sent to using callbacks. StickyPartitioner (Performance improvement) better than Round Robin.  
Callback has oncompletion function which executes every time a record is successfully sent or exception is thrown.  
When multiple messages are sent very quickly, the producer batches these messages into one batch to make it more efficient. So one batch is sent to one partition and then other to next. This is default partitioner **UniformStickyPartitoner**.   
We can set batch size (default 16KB) and partitioner class(Not recommended)

We can send data with keys. Same key data sent to same partition.

**Consumer:** To create a consumer

* Create properties object and set server properties  
* Set consumer properties (key and value serializer, group id, offset)  
* Subscribe for data  
* Poll for data in a while loop  
* Close consumer (if want to, this will commit offsets)

For graceful shutdown, create main thread reference, create a shutdown hook (wakeup consumer in this)  
If we add multiple instances in the consumer group, then the consumers rebalance the partitions for a topic and then they are divided among the consumers.  
Moving partitions between consumers is called rebalancing. Reassignment happens when a consumer leaves or joins a group. It also happens when a new partition is added.

**Strategies for rebalance**:

1. Eager Rebalance: When a consumer joins, all consumers stop and give up their membership of partitions. They then rejoin the consumer group and get new assignment. During a short period of time, the entire group stops processing.   
2. Cooperative Rebalance (Incremental) : Reassign small subset of partition from one consumer to other. Other consumers can still process uninterrupted. Can go through several iterations to find “stable” assignment.

To use in KafkaConsumer, partition.assignment.strategy

* RangeAssigner: assigns based on per-topic basis  
* RoundRobin:   
* StickyAssignor: balanced like roundrobin, then minimises partition movements when consumer joins or leaves group  
* CooperativeStickyAssignor: StickyAssignor with support of cooperative rebalance

For Kafka Connect and Streams, CooperativeStickyAssignor is enabled by default.

If we user group.instance.id for a consumer, then if consumer leaves group, its partition is not assigned to anyone if it joins back within session.timeout.ms without rebalance. Used when cache is required in consumers.

**Offset Commiting:**  
In Java Consumer API, offsets are regularly committed which enables at least once reading scenario by default.  
Offsets are committed when .poll() or auto.commit.interval.ms has elapsed and enable.auto.commit=true  
Make sure all messages are processed before you call poll again. If you don’t at-least-once scenario not present. In that case, we disable auto commit, then from time to time call .commitAsync() or .commitSync() from a separate thread with correct offsets manually.

**Producer Retries**: In case of transient failures like not\_enough\_replicas, we need to handle exceptions and retries. Setting retries and retry.backoff.ms (default 100). For high no of retries, it is bound by timeout delivery.timeout.ms=120000.   
Use idempotent producer.  
To set how many producer requests can be in parallel, max.in.flight.requets.per.connection  
Default 5 set to 1 if ordering required.  
To create idempotent producers,  
properties.setProperties(“enable.idempotence”, true);

**Compression** is required at producer. Compression.type can be none, gzip, lz4, snappy, zstd.  
When producer sends batched data to kafka, it compresses to small size. It can also happen at broker level and topic level.   
Compression.type \= producer, broker takes compressed batch from producer and writes to topic’s log file without compressing the data (Best and default option)  
compression.type=none, all batches decompressed by broker  
compression.type=lz4, if same as producer, no changes else it decompresses and recompresses (consumer more CPU cycles)

To influence the batching mechanism and increase throughput,  
linger.ms \-\> default 0, how long to wait until sending batch. If increased, more messages in batch but latency increases.  
Batch.size \-\> can be increased to 32KB or 64KB to increase throughput.  
Add producer level compression

We can also **override the Partitioner behaviour** by using partitoner.class. When key=null, partitioner class is used to decide.

If a producer produces faster than the broker can consume, the records are buffered in memory. buffer.memory=33554432 (32 MB). If it fills completely, it blocks the code.  
max.block.ms=60000, time until .send() will block until throwing an exception. If the buffer is not cleared till this time after filling completely, it throws an exception.   
We can increase buffer memory.

Delivery Semantics: At least once (mostly used), At most once, exactly once.

**Consumer Offset commit strategies**: 2 patterns

1. Enable.auto.commit \= true and synchronous processing of batches.  
2. Enable.auto.commit \= false and manually commit offsets.

In case when autocommit is false, prepare a batch and call .commitAsync on the consumer. Used when accumulating records to buffer and then flushing to the database.  
while(true){  
	Batch \+= consumer.poll(Duration.ofMillis(100))  
	if(isReady(batch)){  
		Do batch processing synchronously  
		consumer.commitSync()  
}  
}  
In case when autocommit is false, we can also store offsets externally.  
Then we need to assign partitions using .seek() API. We need to model and store offsets in db. Need to handle cases when rebalances happen (ConsumerRebalanceListener).  
If idempotent not possible, process data \+ commit offsets in a single transaction.  
**Consumer Offset Reset Behaviour:**   
If consumer is down for 7 days, the offsets in kafka are marked invalid. This can be adjusted by offset.retention.minutes  
The reset behaviour can be:

* auto.offset.reset: latest \- will read from end of the log  
* auto.offset.reset: earliest \- will read from start of the log  
* auto.offset.reset: none \- will throw exception if no offset found

**Replaying Data for Consumers**:  
To replay data for a consumer group,

* Take all consumers from a specific group down  
* Use kafka-consumer-groups command to set offset to what you need  
* Restart consumers

**Controlling consumer liveliness**:   
Consumers in a group talk to the consumer group coordinator. To detect consumers, there is a heartbeat mechanism and a poll mechanism.  
Heartbeat thread is consumer sending message to brokers they are alive.  
Poll thread is other brokers thinking consumers are alive since they are requesting data from kafka.  
To avoid issue, consumers should process data fast and poll often.

heartbeat.interval.ms (default 3 sec) \- usually ⅓ of session.timeout.ms  
session.timeout.ms (default 45 sec, prev 10 sec) \- set lower for faster consumer rebalances

max.poll.interval.ms (default 5 minutes)   
max.poll.records (default 500 per request) \- can increase in case of small messages and high RAM.  
fetch.min.bytes (default 1\) \- min data pulled on each request  
fetch.max.wait.ms (default 500\) \- max time broker will block before answering fetch request  
max.partition.fetch.bytes (default 1MB) \- max data per partition server returns  
fetch.max.bytes (default 55MB) \- Max data returned for each fetch request

**Consumer behaviours with Partitions**:  
Since kafka 2.4, consumers can read from closest replica in case of multiple data centres. We need to setup **broker** for that. 

* rack.id config must be set to the data centre id.  
* Replica.selector.class must be set to org.apache.kafka.common.replica.RackAwareReplicaSelector

For consumer,

* client.rack \= data centre ID the consumer is launched on

**Kafka Connect:**  
Programmers use the same type of data sources for storing and fetching data.  
Source connectors get data from common data sources  
Sink connectors publish data to common data sources  
It becomes part of ETL. We can use utilities made by other users.  
These are present on Confluent. We can use it from there.  
Create app\_name.properties, connect-standalone.properties, elasticsearch.properties files and use them with kafka connect cli commands.

**Kafka Streams:**   
Data processing and transformation library within Kafka. It helps in Data Transformation, Data Enrichment, fraud Detection, Monitoring and Alerting.  
It can be used to analyse data.  
Exactly once. One record at a time (No batch processing)  
For stream application,

* Create kstream by using topic.  
* User SpecificStreamBuilder and use kstream instance as constructor  
* specificStreamBuilder.setup()  
* Create Topology \= streamsBuilder.build()  
* Create KafkaStreams using topology and properties  
* streams.start()

It adds topics for stats like app\_name.stats.parameter

Schema Registry: Kafka takes input as bytes and publishes without verification. So we need data to be self describable so if data type changes, consumers don’t break.  
So schema registry needs to be setup and producers and consumers should be able to talk to it.  
Schema registry store the schema and decrease the size of payload sent to kafka.   
The normal flow is:

* Source sends data to producer  
* Producer checks if schema exists else creates in registry  
* Registry verifies schema with kafka  
* The avro data is sent to kafka  
* Consumer retrieves schema from registry  
* Kafka sends data to consumer and then to target

The formats can be Apache Avro, Protobuf and JSON Schema.

To create schema registry

* Start kafka with schema registry  
* Create a schema for topic in schema registry  
* Send data using a producer  
* Consume using consumer

How to **select Kafka API**:

* If already have a source database \- User kafka connect  
* If data is being produced in real-time \- Use kafka Producer  
* Kafka to kafka transformations \- Kafka Streams or KSQL  
* Send data for storage and analysis \- Kafka Connect Sink  
* For not using data after processing \- Kafka Consumer

**Choosing Partition count:**  
Each partition can handle throughput of a few MB/s (measuring required for setup)  
Guidelines to choose are:

* Partitions per topic  
  * For small cluster (\< 6 brokers): 3x number of brokers  
  * For big cluster (\>12 brokers): 2x number of brokers  
  * Adjust for number of consumers we need to run in parallel at peak throughput  
  * Adjust for producer throughput (in case it increases too much)  
  * Test every cluster since it will have different performance

**Choosing Replication Factor**:  
At least 2, usually 3, max 4  
Guidelines are

* Set it 3 to get started  
* If replication performance gets an issue, get a better broker instead of less RF

**Cluster Guidelines:**  
Total no of partitions in cluster \< 200,000 (Zookeeper limit)  
If KRaft, millions but still avoid  
If more partitions are required, add brokers.   
Still if more required, follow netflix model and create more clusters.  
Start with a reasonable number for partitions. If req does not fulfill, then try adding.

**Topic Naming conventions:**  
Use snake case.  
Format : \<message\_type\>.\<dataset name\>.\<data name\>.\<data format\>  
Example: logging.database\_name.table\_name.json

Brokers have defaults for all topic configuration parameters. Some topics may need different values than defaults like replication factor, partitions, message size, compression level, log cleanup policy, min insync replicas etc.

To **set a topic configuration**:  
In CLI, use –entity-type topics –entity-name topic\_name –add-config min.insync.replicas=2  
In this way set other configs or can delete also.

Partitions are made up of segments. At once, only one segment is active. There are two segment settings:

* log.segment.bytes: Max size of single segment in bytes(default 1GB) then new created.  
* log.segment.ms: The time kafka waits before committing segment if not full (1 week)

Segment comes with two indexes (files)

* Offset to position index: helps kafka find where to read from  
* Timestamp to position index: helps kafka find messages with specific timestamp

A smaller log.segment.bytes means more segment per partition. Log compaction happens often but many files open error can come.   
Think how fast will I have new segments based on throughput.

A smaller log.segment.ms means we set a max frequency for log compaction.  
Think how often do I need log compaction to happen.

**Log Cleanup**: Making data expire as per policy. Happens on partition segments. Can be done by adding config while creating topic.

**Policy1**: log.cleanup.policy=delete (default) delete based on age of data, max size of log  
log.retention.hours (default 168\)  
log.retention.bytes (Max bytes in each partition def \-1 infinite)  
Common pairs used are 1 week and infinite bytes, infinite time and 500MB

**Policy2**: log.cleanup.policy=compact (default for topic \_\_consumer\_offsets). Delete based on keys of messages. Will delete old duplicate keys after active segment is committed. Infinite time and space retention.

Log compaction ensures that log contains at least the last known value for a specific key in a partition. Useful when we require Snapshot instead of full history.  
Any consumer reading from the tail of the log will still see all the messages sent to topic.  
Ordering of messages is kept. The offset is immutable.  
Deleted records can be seen by consumer for a period of delete.retention.ms  
log.cleanup.policy=compact is impacted by  
segment.ms \- def 7 days, Max amount of time to wait to close active segment  
segment.bytes \- def 1GB Max size of segment  
min.compaction.lag.ms \- def 0, how long to wait before message can be compacted  
delete.retention.ms  
min.cleanable.dirty.ratio \- def 0.5, high \-\> less, more efficient cleaning

unclean.leader.election.enable \-\> If all in sync replicas go offline, we can use this but this is dangerous. In this, non ISR becomes leader and kafka starts producing to non ISR partitions. If any ISR then comes online they are discarded and non ISR stays as leader.  
Use when availability required more than data loss.

To send **large messages** in Kafka:

1. Large message using external store \- store message outside kafka and send reference of that message to kafka. Write custom code for producer/consumer  
2. In kafka \- Topic wise, set max message size to 10MB.   
   1. Broker side: modify message.max.bytes  
   2. Topic side: modify max.message.bytes

Broker wise, set max replication fetch size to 10MB \-\> replica.fetch.max.bytes=10485880 in server.properties  
Consumer side, increase fetch size \-\> max.partition.fetch.bytes=10485880  
Producer side, increase max request size \-\> max.request.size=10485880

**Big Data Ingestion**:

Producers \-\> Kafka \-\> Spark, Flink etc. \-\> Real Time (Analytics, Dashboards, Consumers, Apps)  
			\-\> Hadoop, S3, RDBMS \-\> Batch(Audit, Reporting, Backup, Data Science)

For logging and monitoring, data sent to  
Topic application\_logs and application\_metrics which are sent to Splunk via Kafka Connect.