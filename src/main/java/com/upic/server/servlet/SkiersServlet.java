package com.upic.server.servlet;

import static com.squareup.okhttp.internal.Internal.logger;

import com.rabbitmq.client.MessageProperties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import org.json.JSONObject;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

import org.apache.commons.pool2.impl.GenericObjectPool;

@WebServlet(name = "SkiersServlet", value = "/skiers")
public class SkiersServlet extends HttpServlet {
    private final Integer NUM_CHANNELS = 5;
    private final static String QUEUE_NAME = "LiftServer";
    private GenericObjectPool<Channel> pool;

    public void init() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("172.31.15.42");
        factory.setUsername("admin");
        factory.setPassword("s3crEt");
        factory.setVirtualHost("/");
        factory.setPort(5672);
        try {
            Connection connection = factory.newConnection();
            this.pool = new GenericObjectPool<>(new ChannelBufferFactory(connection));
            this.pool.setMaxTotal(NUM_CHANNELS);
        } catch (Exception e) {
            System.out.println("Error in creating pool.");
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("text/plain");
        if (isValidatedURL(request.getPathInfo())) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("It works!");
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("Missing parameters");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("text/plain");
        String urlPath = request.getPathInfo();
        if (urlPath == null || urlPath.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("Missing parameters");
        }
        String requestJson = request.getReader().lines().collect(Collectors.joining());
        if (!isValidatedURL(urlPath) || !isJsonValidated(requestJson)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            Channel chan = null;
            try {
                chan = pool.borrowObject();
                String[] urlPathArr = urlPath.split("/");
                JSONObject jsonObject = new JSONObject(requestJson);
                JSONObject message = new JSONObject();
                message.put("resort", Integer.valueOf(urlPathArr[1]));
                message.put("season", Integer.valueOf(urlPathArr[3]));
                message.put("day", Integer.valueOf(urlPathArr[5]));
                message.put("skier", Integer.valueOf(urlPathArr[7]));
                message.put("time", jsonObject.get("time"));
                message.put("liftID", jsonObject.get("liftID"));
                chan.queueDeclare(QUEUE_NAME, false, false, false, null);
                chan.basicPublish("", QUEUE_NAME, MessageProperties.PERSISTENT_TEXT_PLAIN,
                        message.toString().getBytes());
            } catch (Exception e) {
                logger.info("failed to send message to RabbitMQ");
                e.printStackTrace();
            } finally {
                try {
                    if(chan != null) {
                        pool.returnObject(chan);
                    }
                } catch (Exception e) {
                    logger.info("failed to return object to pool");
                    e.printStackTrace();
                }
            }
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write("It works!");
        }
    }

    private boolean isValidatedURL(String urlPath) {
        String urlPattern = "^/\\d+/seasons/\\d+/days/\\d+/skiers/\\d+$";
        Pattern pattern = Pattern.compile(urlPattern, Pattern.CASE_INSENSITIVE);
        return pattern.matcher(urlPath).matches();
    }

    private boolean isJsonValidated(String jsonRequest) {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(jsonRequest);
            Object timeObject = jsonObject.get("time");
            Object liftIDObject = jsonObject.get("liftID");
            return timeObject instanceof Integer && ((Integer) timeObject <= 360) &&
                    liftIDObject instanceof Integer && ((Integer) liftIDObject >= 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}