# Prerequisites
- Java 11 or higher
- Build Tool: Maven

# Dependencies
- Jackson Databind for JSON serialization/deserialization

# Project Structure
- LiftRide: Model class representing a lift ride event.
- RequestRecord: Class to store request details for latency measurements.
- LiftRideGenerator: Generates lift ride events in a separate thread.
- LiftRidePoster: Sends POST requests to the server and records latency.
- LiftRideClient: Main class that orchestrates the client application and calculates statistics.

# How to Run
1. **Configure the server URL in `LiftRideClient.java`.**
   - Locate the following line:
   
     `String serverUrl = "http://ec2-54-174-150-27.compute-1.amazonaws.com:8080/untitled";`
   - Replace the URL with the server URL.
2. **Adjust Client Settings (Optional)**

    In the `LiftRideClient.java` file, you can adjust the following settings:
    - Total Number of Events:
   
      `int totalEvents = 200000; // Total number of POST requests to send`
    - Initial Number of Threads:

      `int requestsPerThread = 1000; // Number of requests each initial thread sends`
    - Subsequent Number of Threads:

      After the initial threads complete, the client calculates the remaining requests and starts additional threads. You can adjust the number of subsequent threads:

      `int subsequentThreads = 64; // Number of subsequent threads to handle remaining requests`
3. **Run the Client Application**

    Run the `LiftRideClient.java` file. The client will start sending POST requests to the server and record latency measurements.