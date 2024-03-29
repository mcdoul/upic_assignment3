package com.upic.skiers.servlet;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.upic.server.model.ResponseMessage;
import com.upic.server.model.RideInfo;
import com.upic.consumer.Consumer;
import com.upic.consumer.ConnectionPoolFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Implement the POST API for the /skiers endpoint using Java servlets
 * This servlet only process Post request, return 404 status code for Get request
 */
@WebServlet(name = "SkiersServlet", urlPatterns = "/skiers")
public class SkiersServlet extends HttpServlet {

    private Connection connection;

    private Consumer rabbitMQConnection;

    private ConnectionPoolFactory connectionPool;

    @Override
    public void init() {
        connectionPool =
                new ConnectionPoolFactory();
        try {
            // get mq connection from pool
            rabbitMQConnection = connectionPool.getConnection();
            connection = rabbitMQConnection.getConnection();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // process Post request
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Response media format is json
        response.setContentType("application/json");
        Gson gson = new Gson();
        try {
            // Handle JSon Request
            StringBuilder stringBuilder = new StringBuilder();
            String temp;
            while ((temp = request.getReader().readLine()) != null) {
                stringBuilder.append(temp);
            }
            // Convert the requested json data into an object
            RideInfo rideInfo = gson.fromJson(stringBuilder.toString(), RideInfo.class);

            if (rideInfo == null || rideInfo.getSkierID() == null || rideInfo.getResortID() == null
                    || rideInfo.getLiftID() == null || rideInfo.getSeasonID() == null
                    || rideInfo.getDayID() == null || rideInfo.getTime() == null) {
                // The request does not contain all request parameters
                response.setStatus(404);
                response.getOutputStream().print(gson.toJson(new ResponseMessage("Data not found")));
            } if (rideInfo.getSkierID() < 1 || rideInfo.getSkierID() > 100000
                    || rideInfo.getResortID() < 1 || rideInfo.getResortID() > 10
                    || rideInfo.getLiftID() < 1 || rideInfo.getLiftID() > 40
                    || !rideInfo.getSeasonID().equals("2024") || !rideInfo.getDayID().equals("1")
                    || rideInfo.getTime() < 1 || rideInfo.getTime() > 360) {
                response.setStatus(400);
                response.getOutputStream().print(gson.toJson(new ResponseMessage("Invalid inputs")));
                } else {
                    // Request parameter validation successfully
                    // send message to rabbitMQ
                    Channel channel = connection.createChannel();
                    channel.queueDeclare("query", false, false, false, null);
                    // send message to mq
                    channel.basicPublish("", "query", null,
                            stringBuilder.toString().getBytes());

                    // close channel
                    try {
                        channel.close();
                    } catch (TimeoutException e) {
                        e.printStackTrace();
                    }
                    // response status code
                    response.setStatus(201);
                    // response message
                    response.getOutputStream().print(1);
                }
        } catch (IOException e) {
            // There are some errors in code
            e.printStackTrace();
            response.setStatus(500);
            response.getOutputStream().print(gson.toJson(new ResponseMessage("Server Error")));
        } finally {
            // commit the response
            response.getOutputStream().flush();
        }
    }

    @Override
    public void destroy() {
        if (connectionPool != null) {
            connectionPool.returnConnection(rabbitMQConnection);
        }
    }

    // process Get request
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        // return 404 status code for Get request
        response.setContentType("application/json");
        response.getOutputStream().print(gson.toJson(new ResponseMessage("Error Request Method")));
        response.setStatus(404);
        response.getOutputStream().flush();
    }
}
