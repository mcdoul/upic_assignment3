package com.upic.client2;

import com.google.gson.Gson;
import com.upic.server.model.RideInfo;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ClientThread implements Runnable {

    // the total number of requests need to send: 200K
    public static int maxSend = 200*1000;

    // the max number of requests that current thread needs to send
    private int needSend = 1000;

    // successful request count
    public static int successRequest = 0;
    // failed request count
    public static int failedRequest = 0;

    // Servlet or Spring
    public static String type;

    // write request to CSV file
    private FileWriter fileWriter;

    public ClientThread(FileWriter fileWriter) {
        this.fileWriter = fileWriter;
    }

    public synchronized void send(HttpClient client) {
        // The number of requests that all threads or current thread need to send a minus 1
        maxSend--;
        needSend--;

        Gson gson = new Gson();
        Random random = new Random();
        // Request address and parameters
        String url;
        String postData;
        // Send a request to the Servlet server
        url = "http://54.184.82.70:8080/servletskiers/skiers";
        RideInfo rideInfo = new RideInfo();
        rideInfo.setDayID("1");
        rideInfo.setSeasonID("2024");
        // Randomly generate request parameters
        rideInfo.setResortID(random.nextInt(10) + 1);
        rideInfo.setLiftID(random.nextInt(40) + 1);
        rideInfo.setTime(random.nextInt(360) + 1);
        rideInfo.setSkierID(random.nextInt(100000) + 1);
        postData = gson.toJson(rideInfo);

        // Wrapper request body
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(2))
                .version(HttpClient.Version.HTTP_1_1)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(postData))
                .build();
        HttpResponse response = null;
        try {
            // Number of failed retries
            int retryTime = 5;
            // Record the time before sending
            long startTime = System.currentTimeMillis();
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            while (statusCode!=201 && retryTime>0){
                // Received non-write success status code, continue to try again
                retryTime--;
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                statusCode = response.statusCode();
            }

            if (retryTime>0){
                // Sent successfully (including five retries)
                successRequest++;
            }else{
                // Failure occurs (including after five retries)
                failedRequest++;
            }
            // Record the time after sending
            long endTime = System.currentTimeMillis();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// format current time
            // write time to csv file
            List<String> row = new ArrayList<>();
            row.add(df.format(new Date()));
            row.add("POST");
            row.add(String.valueOf((endTime-startTime)));
            row.add(String.valueOf(statusCode));
            row.add("\n");
            String collect = row.stream().collect(Collectors.joining(","));
            fileWriter.append(collect);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void run() {
        // Use Java11's HttpClient to send requests. A thread creates a client and sends 1,000 requests.
        ExecutorService executor = Executors.newSingleThreadExecutor();
        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(2000))
                .executor(executor)
                .build();

        // The total number of requests sent has not reached 200K, and the current thread has not sent 1000 requests.
        while (maxSend > 0 && needSend > 0){
            send(client);
        }
        // After the current thread has sent 1,000 requests, the total number of requests sent has not reached 200K.
        if(maxSend>0){
            // Create a new thread to continue sending.
            ClientThread clientThread = new ClientThread(fileWriter);
            Thread newThread = new Thread(clientThread);
            newThread.start();
            try {
                newThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }




}
