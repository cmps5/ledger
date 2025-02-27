package auction;

import java.time.LocalDateTime;
import java.util.HashMap;

public class Auction {

    private final String name;
    private final int basePrice;
    private final LocalDateTime countdown;
    boolean active;
    private int currentBid;
    private String currentBidder;
    private HashMap<String, Integer> bids = null;

    public Auction(String name, int basePrice) {

        this.bids = new HashMap<>();

        this.countdown = LocalDateTime.now().plusSeconds(300); // 5min countdown (300 secs)

        this.basePrice = basePrice;
        this.currentBid = basePrice;

        this.name = name;
        this.active = true;

    }

    public void printAuction() {
        System.out.println("\n\n");
        System.out.println("Name: " + this.getName());
        System.out.println("Base Price: " + this.getBasePrice());
        System.out.println("Deadline: " + this.getCountdown().toString());
        System.out.println("Bid State: " + this.active);
        System.out.println("Actual bid: " + this.getCurrentBid());
        System.out.println("Bidder ID: " + this.getCurrentBidder());
        System.out.println("\n\n");
    }

    public void closeAuction() {
        System.out.println("\n\n");
        System.out.println("Close Auction");
        System.out.println(name + ": " + currentBid);
        System.out.println("\n\n");
    }

    public void registerBid(int amount, String bidderID) {
        if (amount > currentBid && LocalDateTime.now().isBefore(this.countdown) && this.active) {
            currentBid = amount;
            currentBidder = bidderID;
            bids.put(bidderID, amount);
        }
    }

    // Getters & Setters

    public void isActive(boolean active) {
        this.active = active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCountdown() {
        return countdown;
    }

    public String getName() {
        return name;
    }

    public int getBasePrice() {
        return basePrice;
    }

    public int getCurrentBid() {
        return currentBid;
    }

    public void setCurrentBid(int currentBid) {
        this.currentBid = currentBid;
    }

    public String getCurrentBidder() {
        return currentBidder;
    }

    public void setCurrentBidder(String currentBidder) {
        this.currentBidder = currentBidder;
    }
}
