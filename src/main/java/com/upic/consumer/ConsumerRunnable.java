package com.upic.consumer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ConsumerRunnable implements Runnable {

  private final Connection connection;
  private final Channel channel;
  private final String queueName;
  private final Map map;

  public ConsumerRunnable(Connection connection, String queue_name, Map map)
      throws IOException {
    this.connection = connection;
    this.channel = connection.createChannel();
    this.queueName = queue_name;
    this.map = map;
  }

  @Override
  public void run() {
    try {
      this.channel.queueDeclare(this.queueName, false, false, false, null);
      this.channel.basicQos(1);
      DeliverCallback threadCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
        String[] arrOfMessage = message.split("/");
        this.map.put(arrOfMessage[0], arrOfMessage[1]);
        this.channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
      };
      boolean autoAck = false;
      this.channel.basicConsume(this.queueName, autoAck, threadCallback, consumerTag -> { });
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
