package com.upic.consumer;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Consumer {
  private static final String QUEUE_NAME = "LiftServer";
  private static final Integer NUM_THREADS = 60;
  private static Map<String, String> map = new ConcurrentHashMap<>();

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("172.31.15.42");
    factory.setUsername("full_access");
    factory.setPassword("s3crEt");
    factory.setVirtualHost("/");
    factory.setPort(5672);
    Connection connection = factory.newConnection();
    ExecutorService pool = Executors.newFixedThreadPool(NUM_THREADS);
    for (int i = 0; i < NUM_THREADS; i++) {
      pool.execute(new ConsumerRunnable(connection, QUEUE_NAME, map));
    }
    pool.shutdown();
    pool.awaitTermination(10, TimeUnit.SECONDS);
  }
}
