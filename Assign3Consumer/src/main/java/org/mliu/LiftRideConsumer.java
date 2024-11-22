package org.mliu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import javax.sql.DataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LiftRideConsumer {

    private static final String QUEUE_NAME = "LiftRideQueue";
    private static final String HOST = "3.230.249.177"; // Replace with your RabbitMQ host
    private static final int PORT = 5672; // Default RabbitMQ port
    private static final String USERNAME = "mliu"; // Replace with your RabbitMQ username
    private static final String PASSWORD = "hihbis-mUxgax-6derna"; // Replace with your RabbitMQ password

    private static final int THREAD_POOL_SIZE = 300; // Adjust based on your needs

    // Reusable ObjectMapper instance
    private static final ObjectMapper mapper = new ObjectMapper();

    // RabbitMQ connection
    private static com.rabbitmq.client.Connection connection;

    // Database connection pool
    private static DataSource dataSource;

    public static void main(String[] args) throws Exception {
        // Initialize resources
        init();

        // Create a fixed thread pool
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        // Create a consumer for each thread
        for (int i = 0; i < THREAD_POOL_SIZE; i++) {
            executor.submit(() -> {
                try {
                    Channel threadChannel = connection.createChannel();
                    // No need to declare the queue again
                    threadChannel.basicQos(1); // Fair dispatch

                    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                        String message = new String(delivery.getBody(), "UTF-8");
                        try {
                            processMessage(message);
                            // Manually acknowledge message after processing
                            threadChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                        } catch (Exception e) {
                            e.printStackTrace();
                            // Optionally, reject the message and requeue or send to a dead-letter queue
                            threadChannel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
                        }
                    };

                    // Start consuming messages
                    threadChannel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        // Add shutdown hook to close resources
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                executor.shutdown();
                executor.awaitTermination(5, TimeUnit.SECONDS);
                connection.close();
                if (dataSource instanceof HikariDataSource) {
                    ((HikariDataSource) dataSource).close();
                }
                System.out.println("Consumer shutdown gracefully.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }

    // Initialization method
    private static void init() throws Exception {
        // Initialize RabbitMQ connection factory
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST);
        factory.setPort(PORT);
        factory.setUsername(USERNAME);
        factory.setPassword(PASSWORD);

        // Create a RabbitMQ connection
        connection = factory.newConnection();

        // Declare the queue once
        Channel channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        channel.close();

        // Initialize the database connection pool
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://database-1.c6wlvkpkkqog.us-east-1.rds.amazonaws.com:3306/LiftRideDB");
        config.setUsername("admin");
        config.setPassword("hihbis-mUxgax-6derna");
        config.setMaximumPoolSize(THREAD_POOL_SIZE); // Match the thread pool size

        // Additional HikariCP settings for performance
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);
    }

    // Method to process each message
    private static void processMessage(String message) {
        java.sql.Connection dbConnection = null;
        PreparedStatement preparedStatement = null;
        try {
            LiftRide liftRide = mapper.readValue(message, LiftRide.class);

            // Get a connection from the pool
            dbConnection = dataSource.getConnection();

            // Prepare SQL insert statement
            String insertSQL = "INSERT INTO LiftRides (skierID, resortID, seasonID, dayID, time, liftID, vertical) VALUES (?, ?, ?, ?, ?, ?, ?)";
            preparedStatement = dbConnection.prepareStatement(insertSQL);
            preparedStatement.setInt(1, liftRide.getSkierID());
            preparedStatement.setInt(2, liftRide.getResortID());
            preparedStatement.setString(3, liftRide.getSeasonID());
            preparedStatement.setInt(4, liftRide.getDayID());
            preparedStatement.setInt(5, liftRide.getTime());
            preparedStatement.setInt(6, liftRide.getLiftID());
            preparedStatement.setInt(7, liftRide.getLiftID() * 10); // Calculate vertical

            // Execute the insert
            preparedStatement.executeUpdate();

            // For demonstration, print the processed message
            System.out.println("Inserted lift ride for skier ID: " + liftRide.getSkierID());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Close resources
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) { /* ignored */ }
            }
            if (dbConnection != null) {
                try {
                    dbConnection.close(); // Returns the connection to the pool
                } catch (SQLException e) { /* ignored */ }
            }
        }
    }
}



// LiftRide class to represent the data
class LiftRide {
    private int resortID;
    private String seasonID;
    private int dayID;
    private int skierID;
    private int time;
    private int liftID;

    // Getters and Setters
    public int getResortID() {
        return resortID;
    }

    public void setResortID(int resortID) {
        this.resortID = resortID;
    }

    public String getSeasonID() {
        return seasonID;
    }

    public void setSeasonID(String seasonID) {
        this.seasonID = seasonID;
    }

    public int getDayID() {
        return dayID;
    }

    public void setDayID(int dayID) {
        this.dayID = dayID;
    }

    public int getSkierID() {
        return skierID;
    }

    public void setSkierID(int skierID) {
        this.skierID = skierID;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getLiftID() {
        return liftID;
    }

    public void setLiftID(int liftID) {
        this.liftID = liftID;
    }
}
