package auction;

import blockchain.Blockchain;

import java.util.HashMap;


public class AuctionManager {
    private final HashMap<String, Auction> auctions;
    private final Blockchain blockchain;
    private HashMap<String, Auction> allAuctions;

    public AuctionManager() {
        this.auctions = new HashMap<>();
        this.allAuctions = new HashMap<>();
        this.blockchain = new Blockchain();
    }


    public void createNewAuction(String name, int basePrice) {
        Auction auction = new Auction(name, basePrice);
        // @ TODO
    }


}
