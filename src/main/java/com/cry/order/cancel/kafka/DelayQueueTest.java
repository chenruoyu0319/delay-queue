package com.cry.order.cancel.kafka;

import org.apache.htrace.shaded.fasterxml.jackson.core.JsonProcessingException;
import org.apache.htrace.shaded.fasterxml.jackson.databind.JsonNode;
import org.apache.htrace.shaded.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * @author cry777
 * @program order-cancel
 * @description
 * @create 2021-10-25
 */
@SpringBootTest
public class DelayQueueTest {
    private KafkaConsumer<String, String> consumer;
    private KafkaProducer<String, String> producer;
    private volatile Boolean exit = false;
    private final Object lock = new Object();
    private final String servers = "10.50.51.72:9092";

    @Before
    public void initConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "d");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "5000");
        consumer = new KafkaConsumer<>(props, new StringDeserializer(), new StringDeserializer());
    }

    @Before
    public void initProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producer = new KafkaProducer<>(props);
    }

    @Test
    public void testDelayQueue() throws IOException, InterruptedException {
        // 1分钟延迟队列
        String topic = "delay-minutes-1";
        List<String> topics = Collections.singletonList(topic);
        consumer.subscribe(topics);

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (lock) {
                    consumer.resume(consumer.paused());
                    lock.notify();
                }
            }
        }, 0, 1000);

        do {

            synchronized (lock) {
                // 拉取数据
                ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(200));

                if (consumerRecords.isEmpty()) {
                    lock.wait();
                    continue;
                }

                boolean timed = false;
                for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                    long timestamp = consumerRecord.timestamp();
                    TopicPartition topicPartition = new TopicPartition(consumerRecord.topic(), consumerRecord.partition());
                    // 判断任务是否超时
                    if (timestamp + 5 * 1000 < System.currentTimeMillis()) {

                        String value = consumerRecord.value();
                        ObjectMapper objectMapper = new ObjectMapper();
                        JsonNode jsonNode = objectMapper.readTree(value);
                        JsonNode jsonNodeTopic = jsonNode.get("topic");

                        String appTopic = null, appKey = null, appValue = null;

                        if (jsonNodeTopic != null) {
                            appTopic = jsonNodeTopic.asText();
                        }
                        if (appTopic == null) {
                            continue;
                        }
                        JsonNode jsonNodeKey = jsonNode.get("key");
                        if (jsonNodeKey != null) {
                            appKey = jsonNode.asText();
                        }

                        JsonNode jsonNodeValue = jsonNode.get("value");
                        if (jsonNodeValue != null) {
                            appValue = jsonNodeValue.asText();
                        }
                        // send to application topic
                        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(appTopic, appKey, appValue);
                        try {
                            producer.send(producerRecord).get();
                            // success. commit message
                            OffsetAndMetadata offsetAndMetadata = new OffsetAndMetadata(consumerRecord.offset() + 1);
                            HashMap<TopicPartition, OffsetAndMetadata> metadataHashMap = new HashMap<>();
                            metadataHashMap.put(topicPartition, offsetAndMetadata);
                            consumer.commitSync(metadataHashMap);
                        } catch (ExecutionException e) {
                            consumer.pause(Collections.singletonList(topicPartition));
                            consumer.seek(topicPartition, consumerRecord.offset());
                            timed = true;
                            break;
                        }
                    } else {
                        consumer.pause(Collections.singletonList(topicPartition));
                        consumer.seek(topicPartition, consumerRecord.offset());
                        timed = true;
                        break;
                    }
                }

                if (timed) {
                    lock.wait();
                }
            }
        } while (!exit);
    }

    @Test
    public void produceTest() throws ExecutionException, InterruptedException {
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>("target", "key1", "value1");
        RecordMetadata recordMetadata = producer.send(producerRecord).get();
        System.out.println(recordMetadata);
    }

    @Test
    public void consumeTest(){
        while (true){
            consumer.subscribe(Collections.singleton("target"));
        }
    }
}
