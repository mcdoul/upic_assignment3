package com.upic.consumer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import com.upic.server.model.RideInfo;

public class ConsumerRunnable implements Runnable {

  private final Connection connection;
  private final Channel channel;
  private final String queueName;
  private final JedisPool jedisPool;

  public ConsumerRunnable(Connection connection, JedisPool jedisPool, String queue_name)
          throws IOException {
    this.connection = connection;
    this.channel = connection.createChannel();
    this.queueName = queue_name;
    this.jedisPool = jedisPool;
  }

  @Override
  public void run() {
    try {
      this.channel.queueDeclare(this.queueName, false, false, false, null);
      this.channel.basicQos(1);
      DeliverCallback threadCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
        JSONObject data = new JSONObject(message);
        RideInfo skier = new RideInfo(data.get("resort"), data.get("season"), data.get("day"),
                data.get("time"), data.get("liftID"));
        try (Jedis jedis = this.jedisPool.getResource()) {
          jedis.sadd("skier:" + data.get("skier").toString(), skier.toString());
        }
        this.channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
      };
      boolean autoAck = false;
      this.channel.basicConsume(this.queueName, autoAck, threadCallback, consumerTag -> { });
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}