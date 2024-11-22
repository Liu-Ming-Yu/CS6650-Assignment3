package org.example;


import java.util.concurrent.BlockingQueue;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentLinkedQueue;


public class LiftRidePoster implements Runnable {
    private final BlockingQueue<LiftRide> queue;
    private final int numRequests;
    private final AtomicInteger successCount;
    private final AtomicInteger failureCount;
    private final String serverUrl;
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final ConcurrentLinkedQueue<RequestRecord> requestRecords;

    public LiftRidePoster(BlockingQueue<LiftRide> queue, int numRequests,
                          AtomicInteger successCount, AtomicInteger failureCount,
                          String serverUrl, ConcurrentLinkedQueue<RequestRecord> requestRecords) {
        this.queue = queue;
        this.numRequests = numRequests;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.serverUrl = serverUrl;
        this.client = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
        this.requestRecords = requestRecords;
    }

    @Override
    /*
    public void run() {
        for (int i = 0; i < numRequests; i++) {
            LiftRide liftRide = queue.poll();
            if (liftRide != null) {
                sendPostRequest(liftRide);
            } else {
                // No more lift rides to send
                break;
            }
        }
    }
    */
    public void run() {
        for (int i = 0; i < numRequests; i++) {
            try {
                LiftRide liftRide = queue.take(); // Use take() instead of poll()
                sendPostRequest(liftRide);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }


    private void sendPostRequest(LiftRide liftRide) {
        int attempts = 0;
        boolean success = false;
        while (attempts < 2 && !success) {
            attempts++;
            long startTime = System.currentTimeMillis();
            try {
                // Prepare URL with path parameters
                String url = serverUrl + "/skiers/" + liftRide.getResortID() + "/seasons/" +
                        liftRide.getSeasonID() + "/days/" + liftRide.getDayID() + "/skiers/" +
                        liftRide.getSkierID();

                // Prepare JSON payload with time and liftID only
                LiftRidePayload payload = new LiftRidePayload(liftRide.getTime(), liftRide.getLiftID());
                String requestBody = mapper.writeValueAsString(payload);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .header("Content-Type", "application/json")
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                long endTime = System.currentTimeMillis();
                long latency = endTime - startTime;
                int statusCode = response.statusCode();

                // Record the request
                RequestRecord record = new RequestRecord(startTime, "POST", latency, statusCode);
                requestRecords.add(record);

                if (statusCode == 201 || statusCode == 200) {
                    successCount.incrementAndGet();
                    success = true;
                } else if (statusCode >= 400) {
                    //failureCount.incrementAndGet();
                    // Retry on server or client error
                    Thread.sleep(1); // Brief pause before retrying
                } else {
                    // Other non-retryable errors
                    failureCount.incrementAndGet();
                    success = true; // Stop retrying
                }
            } catch (Exception e) {
                long endTime = System.currentTimeMillis();
                long latency = endTime - startTime;

                // Record the request with exception (response code 0)
                RequestRecord record = new RequestRecord(startTime, "POST", latency, 0);
                requestRecords.add(record);
                //failureCount.incrementAndGet();

                // Handle exceptions (e.g., network errors)
                try {
                    //failureCount.incrementAndGet();
                    Thread.sleep(1); // Brief pause before retrying
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        if (!success) {
            failureCount.incrementAndGet();
        }
    }

    // Inner class for payload
    private static class LiftRidePayload {
        private int time;
        private int liftID;

        public LiftRidePayload(int time, int liftID) {
            this.time = time;
            this.liftID = liftID;
        }

        // Getters and setters
        public int getTime() { return time; }
        public void setTime(int time) { this.time = time; }

        public int getLiftID() { return liftID; }
        public void setLiftID(int liftID) { this.liftID = liftID; }
    }
}