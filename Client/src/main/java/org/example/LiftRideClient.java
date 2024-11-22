package org.example;



import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.io.FileWriter;
import java.io.IOException;

public class LiftRideClient {
    public static void main(String[] args) throws InterruptedException, IOException {
        int totalEvents = 20000; // Adjusted for higher load
        int initialThreads = 64;
        int subsequentThreads = 300;
        int totalThreads = initialThreads + subsequentThreads;

        // Calculate the base number of requests per thread and the remainder
        int baseRequestsPerThread = totalEvents / totalThreads;
        int remainingRequests = totalEvents % totalThreads;

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();
        BlockingQueue<LiftRide> queue = new LinkedBlockingQueue<>();

        // Replace with your server URL
        String serverUrl = "http://54.234.241.63:8080/Assignment2Server";

        ConcurrentLinkedQueue<RequestRecord> requestRecords = new ConcurrentLinkedQueue<>();

        // Start the Lift Ride Generator Thread
        Thread generatorThread = new Thread(new LiftRideGenerator(queue, totalEvents));
        generatorThread.start();

        long startTime = System.currentTimeMillis();

        // Create and start initial threads
        ExecutorService executor = Executors.newFixedThreadPool(initialThreads);

        for (int i = 0; i < initialThreads; i++) {
            int requestsForThisThread = baseRequestsPerThread + (i < remainingRequests ? 1 : 0);
            executor.execute(new LiftRidePoster(queue, requestsForThisThread, successCount, failureCount, serverUrl, requestRecords));
        }

        // Wait for initial threads to complete
        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        // Start subsequent threads
        ExecutorService subsequentExecutor = Executors.newFixedThreadPool(subsequentThreads);

        for (int i = 0; i < subsequentThreads; i++) {
            int threadIndex = initialThreads + i;
            int requestsForThisThread = baseRequestsPerThread + (threadIndex < remainingRequests ? 1 : 0);
            subsequentExecutor.execute(new LiftRidePoster(queue, requestsForThisThread, successCount, failureCount, serverUrl, requestRecords));
        }

        // Wait for subsequent threads to complete
        subsequentExecutor.shutdown();
        subsequentExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        long endTime = System.currentTimeMillis();
        long wallTime = endTime - startTime;
        double throughput = (double) successCount.get() / (wallTime / 1000.0);

        // Write the requestRecords to a CSV file
        writeRecordsToCSV(requestRecords, "records.csv");

        // Process the records to calculate statistics
        List<Long> latencies = requestRecords.stream()
                .map(RequestRecord::getLatency)
                .collect(Collectors.toList());

        double mean = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double median = calculateMedian(latencies);
        long max = latencies.stream().mapToLong(Long::longValue).max().orElse(0L);
        long min = latencies.stream().mapToLong(Long::longValue).min().orElse(0L);
        long p99 = calculatePercentile(latencies, 99);

        System.out.println("Number of successful requests: " + successCount.get());
        System.out.println("Number of unsuccessful requests: " + failureCount.get());
        System.out.println("Total run time (ms): " + wallTime);
        System.out.println("Throughput (requests/second): " + throughput);
        System.out.println("Mean response time (ms): " + mean);
        System.out.println("Median response time (ms): " + median);
        System.out.println("P99 response time (ms): " + p99);
        System.out.println("Min response time (ms): " + min);
        System.out.println("Max response time (ms): " + max);
        // Print out the threads used
        System.out.println("Initial threads: " + initialThreads);
        System.out.println("Subsequent threads: " + subsequentThreads);
    }

    private static void writeRecordsToCSV(Collection<RequestRecord> records, String filename) throws IOException {
        FileWriter csvWriter = new FileWriter(filename);
        csvWriter.append("Start Time,Request Type,Latency,Response Code\n");
        for (RequestRecord record : records) {
            csvWriter.append(record.getStartTime() + "," + record.getRequestType() + "," + record.getLatency() + "," + record.getResponseCode() + "\n");
        }
        csvWriter.flush();
        csvWriter.close();
    }

    private static double calculateMedian(List<Long> latencies) {
        Collections.sort(latencies);
        int size = latencies.size();
        if (size % 2 == 0) {
            return (latencies.get(size / 2 - 1) + latencies.get(size / 2)) / 2.0;
        } else {
            return latencies.get(size / 2);
        }
    }

    private static long calculatePercentile(List<Long> latencies, double percentile) {
        Collections.sort(latencies);
        int index = (int) Math.ceil(percentile / 100.0 * latencies.size());
        return latencies.get(index - 1);
    }
}