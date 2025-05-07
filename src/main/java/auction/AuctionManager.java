package auction;

import blockchain.Block;
import blockchain.Blockchain;
import blockchain.Transaction;
import kademlia.Kademlia;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Thread.sleep;


public class AuctionManager {
    private static AuctionManager instance;
    private final HashMap<String, Auction> auctions;
    private final Blockchain blockchain;
    private final Kademlia kademlia;
    private final HashMap<String, Auction> allAuctions;

    public AuctionManager() {
        this.auctions = new HashMap<>();
        this.allAuctions = new HashMap<>();
        this.blockchain = Blockchain.getInstance();
        this.kademlia = Kademlia.getInstance();
    }

    public static AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }


    public void createNewAuction(String name, int basePrice) {
        Auction auction = new Auction(name, basePrice);

        this.auctions.put(name, auction);
        this.allAuctions.put(name, auction);

        //Close auction after deadline
        Timer timer = new Timer();

        long duration = Duration.between(LocalDateTime.now(), auction.getCountdown()).toMillis();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                closeAuction(name);
            }
        }, duration);

        auction.printAuction();
        kademlia.storeAuction(auction);
    }

    public synchronized void closeAuction(String name) {
        Auction auctionToClose = this.auctions.get(name);

        auctionToClose.closeAuction();
        auctionToClose.setActive(false);

        publishLastBid(auctionToClose);

        this.auctions.remove(name);
        this.allAuctions.remove(name);
    }

    public void publishBid(String name, int value) {
        Auction auction = allAuctions.get(name);

        if (auction == null) {
            System.out.println("Auction not found!");
            return;
        }

        if (auction.getCurrentBid() >= value || value <= auction.getBasePrice()) {
            System.out.println("Bid value too low!");
            return;
        }

        Transaction transaction = new Transaction(auction, value, true);

        Block block = new Block(blockchain.getLastBlockHash(blockchain.getBlockchain()));
        block.setTransaction(transaction);

        if (blockchain.addBlockPOW(block)) {
            kademlia.storeBlock(block);
            registerBid(transaction);
        }
    }

    public void registerBid(Transaction transaction) {
        // TODO
        System.out.println("Working on it");
    }

    private void publishLastBid(Auction auction) {
        SecureRandom rand = new SecureRandom();
        try {
            int randomTime = rand.nextInt(1000);

            //System.out.println("Lets wait " + randomTime + " milliseconds");
            sleep(randomTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        //System.out.println("Sending end transaction " + LocalDateTime.now());
        Transaction transaction = new Transaction(auction, auction.getCurrentBid(), false);

        Block block;
        block = new Block(blockchain.getLastBlockHash(blockchain.getBlockchain()));
        block.setTransaction(transaction);

        if (blockchain.addBlockPOW(block)) {
            kademlia.storeBlock(block);
        }
    }

    public void registerAuction(Auction auction) {
        allAuctions.put(auction.getName(), auction);

        Timer timer = new Timer();
        long duration = Duration.between(LocalDateTime.now(), auction.getCountdown()).toMillis();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                closeAuction(auction.getName());
            }
        }, duration);
    }

    public HashMap<String, Auction> getAuctions() {
        return this.auctions;
    }

    public HashMap<String, Auction> getAllAuctions() {
        return this.allAuctions;
    }

    public void printAuctions() {
        System.out.println("My auctions");
        for (Map.Entry<String, Auction> entry : auctions.entrySet()) {
            entry.getValue().printAuction();
        }
    }

    public void printAllAuctions() {
        System.out.println("All auctions");
        for (Map.Entry<String, Auction> entry : allAuctions.entrySet()) {
            entry.getValue().printAuction();
        }
    }
}
