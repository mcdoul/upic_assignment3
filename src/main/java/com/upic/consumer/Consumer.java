package com.upic.consumer;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

public class Consumer {
  private static final String QUEUE_NAME = "LiftServer";
  private static final Integer NUM_THREADS = 50;

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("172.31.15.42");
    factory.setUsername("admin");
    factory.setPassword("s3crEt");
    factory.setVirtualHost("/");
    factory.setPort(5672);

    final JedisPoolConfig poolConfig = buildPoolConfig();
    JedisPool jedisPool = new JedisPool(poolConfig, Protocol.DEFAULT_HOST, 6379);
    Connection connection = factory.newConnection();
    ExecutorService pool = Executors.newFixedThreadPool(NUM_THREADS);
    for (int i = 0; i < NUM_THREADS; i++) {
      pool.execute(new ConsumerRunnable(connection, jedisPool, QUEUE_NAME));
    }
//
    pool.shutdown();
    pool.awaitTermination(10, TimeUnit.SECONDS);
  }

  private static JedisPoolConfig buildPoolConfig() {
    final JedisPoolConfig poolConfig = new JedisPoolConfig();
    poolConfig.setMaxTotal(128);
    poolConfig.setMaxIdle(128);
    poolConfig.setMinIdle(16);
    poolConfig.setTestOnBorrow(true);
    poolConfig.setTestOnReturn(true);
    poolConfig.setTestWhileIdle(true);
    poolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
    poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
    poolConfig.setNumTestsPerEvictionRun(3);
    poolConfig.setBlockWhenExhausted(true);
    return poolConfig;
  }
}
