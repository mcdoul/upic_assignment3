package com.upic.client1;

public class MultiServletPost {

    public static void main(String[] args) {
        // Record number of requests
        int successRequest = 0;
        int failedRequest = 0;
        int maxRequest = 200 * 1000;
        // Set the number of requests for the thread
        ClientThread.maxSend=maxRequest;
        ClientThread.successRequest=successRequest;
        ClientThread.failedRequest=failedRequest;

        // Record time before sending request
        long startTime = System.currentTimeMillis();

        // Create 32 threads to send requests
        for (int i = 0; i < 32; i++) {
            ClientThread clientThread = new ClientThread();
            Thread thread = new Thread(clientThread);
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Record the time after all requests have been sent
        long endTime = System.currentTimeMillis();
        double wallTime = (endTime - startTime)*1.0/1000;

        successRequest = ClientThread.successRequest;
        failedRequest = ClientThread.failedRequest;
        // calc throughput
        long throughput = Math.round(successRequest/wallTime);
        // print the result
        System.out.println("Multithreaded Client");
        System.out.println("The total run time: " + wallTime + " seconds to send " + maxRequest + " requests");
        System.out.println("The number of successful is: " + successRequest + ", in : " + maxRequest +" requests");
        System.out.println("The number of failed is: " + failedRequest + ", in : " + maxRequest +" requests");
        System.out.println("The total throughput is: " + throughput + ", in : " + maxRequest +" requests");
        System.out.println("It is predicted that sending 200K requests will take: " + (200*1000.0/throughput) +" seconds");
    }
}
