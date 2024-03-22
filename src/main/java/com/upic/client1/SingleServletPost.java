package com.upic.client1;

import com.google.gson.Gson;
import com.upic.server.model.RideInfo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Single thread sends all requests to Servlet server
 */
public class SingleServletPost {

    public static int sendRequest(HttpClient client){
        Gson gson = new Gson();
        Random random = new Random();
        // Randomly generate request parameters
        RideInfo rideInfo = new RideInfo();
        rideInfo.setDayID("1");
        rideInfo.setSeasonID("2024");
        rideInfo.setResortID(random.nextInt(10) + 1);
        rideInfo.setLiftID(random.nextInt(40) + 1);
        rideInfo.setTime(random.nextInt(360) + 1);
        rideInfo.setSkierID(random.nextInt(100000) + 1);
        // Wrapper request body
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://54.184.82.70:8080/servletskiers/skiers"))
                .timeout(Duration.ofMinutes(2))
                .version(HttpClient.Version.HTTP_1_1)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(rideInfo)))
                .build();
        try {
            // Number of failed retries
            int retryTime = 5;
            // Record the time before sending
            HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            while (statusCode!=201 && retryTime>0){
                // Received non-write success status code, continue to try again
                retryTime--;
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                statusCode = response.statusCode();
            }
            if (retryTime>0){
                // Sent successfully (including five retries)
                return 1;
            }else{
                // Failure occurs (including after five retries)
                return 0;
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        // Use Java11's HttpClient to send requests. Creates a client and sends all requests.
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(1000))
                .executor(executorService)
                .build();
        // Set the initial number of requests
        int successRequest = 0;
        int failedRequest = 0;
        int maxRequest = 200 * 1000;

        // Record time before sending request
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < maxRequest; i++) {
            int res = sendRequest(client);
            if (res>0){
                successRequest++;
            }else{
                failedRequest++;
            }
        }
        // Record the time after all requests have been sent
        long endTime = System.currentTimeMillis();
        double wallTime = (endTime - startTime)*1.0/1000;
        long throughput = Math.round(successRequest/wallTime);
        // print result
        System.out.println("The total run time: " + wallTime + " seconds to send " + maxRequest + " requests");
        System.out.println("The number of successful is: " + successRequest + ", in : " + maxRequest +" requests");
        System.out.println("The number of failed is: " + failedRequest + ", in : " + maxRequest +" requests");
        System.out.println("The total throughput is: " + throughput + ", in : " + maxRequest +" requests");
        System.out.println("It is predicted that sending 200K requests will take: " + (200*1000/(throughput*1.0)) +" seconds");
    }

}
