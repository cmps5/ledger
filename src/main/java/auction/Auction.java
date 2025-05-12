package auction;

import peer.Wallet;

import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.HashMap;

public class Auction {

    private final String name;
    private final int basePrice;
    private final LocalDateTime countdown;
    boolean active;
    private String sellerID;
    private PublicKey sellerPubKey;
    private int currentBid;
    private String currentBidder;
    private HashMap<String, Integer> bids = null;

    private Wallet wallet;

    public Auction(String name, int basePrice) {
        wallet = Wallet.getInstance();
        this.bids = new HashMap<>();

        this.name = name;
        this.basePrice = basePrice;
        this.countdown = LocalDateTime.now().plusSeconds(300); // 5min countdown (300 secs)
        this.active = true;

        this.currentBid = basePrice;
        this.currentBidder = wallet.getID();
        this.sellerID = wallet.getID();
        this.sellerPubKey = wallet.getPubKey();

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

    public boolean registerBid(int amount, String bidderID) {
        if (amount > currentBid && LocalDateTime.now().isBefore(this.countdown) && this.active) {
            currentBid = amount;
            currentBidder = bidderID;
            bids.put(bidderID, amount);
            return true;
        }
        return false;
    }

    // Getters & Setters
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

    public PublicKey getSellerPubKey() {
        return sellerPubKey;
    }

    public void setSellerPubKey(PublicKey sellerPubKey) {
        this.sellerPubKey = sellerPubKey;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getSellerID() {
        return sellerID;
    }

    public void setSellerID(String sellerID) {
        this.sellerID = sellerID;
    }

    public HashMap<String, Integer> getBids() {
        return bids;
    }

    public void setBids(HashMap<String, Integer> bids) {
        this.bids = bids;
    }
}
