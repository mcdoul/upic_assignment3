package com.upic.client2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MultiServletPost {

    public static void main(String[] args) {
        // CSV file
        String csvFileName = "./records.csv";
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(csvFileName);
            List<String> header = new ArrayList<>();
            header.add("start_time");
            header.add("request_type");
            header.add("latency");
            header.add("response_code");
            header.add("\n");
            String collect = header.stream().collect(Collectors.joining(","));
            fileWriter.write(collect);
        } catch (IOException e) {
            e.printStackTrace();
        }

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
            // Each thread passes in a FileWriter object to write records to the CSV file
            ClientThread clientThread = new ClientThread(fileWriter);
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
        long throughput = Math.round(successRequest/wallTime);

        System.out.println("Multithreaded Client");
        System.out.println("The total run time: " + wallTime + " seconds to send " + maxRequest + " requests");
        System.out.println("The number of successful is: " + successRequest + ", in : " + maxRequest +" requests");
        System.out.println("The number of failed is: " + failedRequest + ", in : " + maxRequest +" requests");
        System.out.println("The total throughput is: " + throughput + ", in : " + maxRequest +" requests");
        System.out.println("It is predicted that sending 200K requests will take: " + (200*1000.0/throughput) +" seconds");
        System.out.println("------------seq--------------------------------------");

        try {
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Reading index latency time from CSV file
        List<Long> list = new ArrayList<>();
        try {
            try (BufferedReader br = new BufferedReader(new FileReader(csvFileName))) {
                String line;
                br.readLine();
                while ((line = br.readLine()) != null) {
                    String[] values = line.split(",");
                    list.add(Long.valueOf(values[2]));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Sort latency time
        Collections.sort(list);
        // get the min, max, mean, median of latency time
        long minLatencyTime = list.get(0);
        long maxLatencyTime = list.get(list.size() - 1);
        double meanLatencyTime = list.stream().mapToLong(Long::valueOf).average().getAsDouble();
        long medianLatencyTime = 0;
        if (list.size() % 2 == 0) {
            medianLatencyTime = (list.get(list.size() / 2) + list.get(list.size() / 2 - 1)) / 2;
        } else {
            medianLatencyTime = list.get(list.size() / 2);
        }
        // calc percentile time
        int i = (int) Math.round(list.size() * 0.95);
        long percentileTime = list.get(i);
        // print the result
        System.out.println("The minLatencyTime of "+ maxRequest +" requests is:" + minLatencyTime);
        System.out.println("The maxLatencyTime of "+ maxRequest +" requests is:" + maxLatencyTime);
        System.out.println("The meanLatencyTime of "+ maxRequest +" requests is:" +meanLatencyTime);
        System.out.println("The medianLatencyTime of "+ maxRequest +" requests is:" +medianLatencyTime);
        System.out.println("The percentileTime of "+ maxRequest +" requests is:" +percentileTime);
    }
}
