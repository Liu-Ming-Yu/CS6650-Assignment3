package org.example;    //package org.example means that the class is part of the org.example package


import java.util.concurrent.BlockingQueue;
import java.util.Random;

public class LiftRideGenerator implements Runnable {
    private final BlockingQueue<LiftRide> queue;
    private final int totalEvents;

    public LiftRideGenerator(BlockingQueue<LiftRide> queue, int totalEvents) {
        this.queue = queue;
        this.totalEvents = totalEvents;
    }

    @Override
    public void run() {
        Random random = new Random();
        for (int i = 0; i < totalEvents; i++) {
            LiftRide liftRide = new LiftRide();
            liftRide.setSkierID(random.nextInt(100000) + 1);
            liftRide.setResortID(random.nextInt(10) + 1);
            liftRide.setLiftID(random.nextInt(40) + 1);
            liftRide.setSeasonID("2024");
            liftRide.setDayID("1");
            liftRide.setTime(random.nextInt(360) + 1);
            try {
                queue.put(liftRide);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}

