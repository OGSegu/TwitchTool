package twitch;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class FollowSender extends Checkable {

    private final String channelName;
    private final int amount;
    private final Queue<String> followersQueue = new ArrayBlockingQueue<>(10000);

    /**
     * Constructor of FollowSender
     *
     * @param channelName - name of the channel which will be followed.
     * @param amount      - amount of follows
     */
    public FollowSender(File in, String channelName, int amount) {
        super(in);
        this.channelName = channelName;
        this.amount = amount;
    }

    /**
     * The method starts sending follows.
     */
    public void start() {
        try (Scanner sc = new Scanner(super.fileIn, "UTF-8")) {
            while (sc.hasNext()) {
                followersQueue.add(sc.next());
            }
        } catch (FileNotFoundException e) {
            System.out.println("File can not be found: " + e);
        }
        startExecution();
    }

    /**
     * Starts execution of Threads.
     */
    private void startExecution() {
        AtomicInteger subscribed = new AtomicInteger(0);
        while (!super.executor.isTerminated()) {
            super.executor.execute(() -> {
                if (subscribed.get() >= amount) {
                    super.executor.shutdown();
                    try {
                        super.executor.awaitTermination(3, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    String token = followersQueue.poll();
                    TwitchUser user = new TwitchUser(token);
                    if (!user.isValid()) {
                        return;
                    }
                    if (!user.follow(channelName)) {
                        return;
                    }
                    System.out.println(subscribed.incrementAndGet() + " / " + amount);
                }
            });
        }
    }
}
