package auction;

import blockchain.Blockchain;

import java.util.HashMap;


public class AuctionManager {
    private static AuctionManager instance;
    private final HashMap<String, Auction> auctions;
    private final Blockchain blockchain;
    private HashMap<String, Auction> allAuctions;

    public AuctionManager() {
        this.auctions = new HashMap<>();
        this.allAuctions = new HashMap<>();
        this.blockchain = new Blockchain();
    }

    public static AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }


    public void createNewAuction(String name, int basePrice) {
        Auction auction = new Auction(name, basePrice);
        // @ TODO
    }


    public void printAuctions() {
        // @TODO
    }

    public void publishBid(String topic, int i) {
        // @TODO
    }
}
