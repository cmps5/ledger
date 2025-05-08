package blockchain;

import auction.Auction;
import peer.Wallet;

import java.security.PublicKey;
import java.time.LocalDateTime;

public class Transaction {

    private Wallet wallet;

    private String itemName;
    private int price;
    private boolean isActive;
    private LocalDateTime timestamp;

    private String buyerID;
    private String buyerIP;
    private String buyerPort;
    private PublicKey auctioneerPublicKey;
    private byte[] signature;
    private PublicKey buyerPublicKey;
    private String hash;

    public Transaction(Auction auction, int amount, boolean isActive) {
        this.wallet = Wallet.getInstance();

        this.itemName = auction.getName();
        this.price = amount;
        this.isActive = isActive;
        this.timestamp = LocalDateTime.now();

        this.buyerID = wallet.getID();
        this.buyerIP = wallet.getIP();
        this.buyerPort = wallet.getPort();

        this.buyerPublicKey = wallet.getPubKey();
        this.auctioneerPublicKey = auction.getSellerPubKey();
        this.hash = generateCheckSum(auction, auctioneerPublicKey, buyerID, buyerIP, buyerPort);
        this.signature = generateSignature(this.hash);
    }

    private byte[] generateSignature(String hash) {
        //@ TODO
        return null;
    }

    private String generateCheckSum(Auction auction, PublicKey auctioneerPublicKey, String buyerID, String buyerIP, String buyerPort) {
        // @ TODO
        return null;
    }

    @Override
    public String toString() {
        return "Transaction: {Item name: " + itemName + "; Price: " + price + "}";
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getBuyerID() {
        return buyerID;
    }

    public void setBuyerID(String buyerID) {
        this.buyerID = buyerID;
    }

    public String getBuyerIP() {
        return buyerIP;
    }

    public void setBuyerIP(String buyerIP) {
        this.buyerIP = buyerIP;
    }

    public String getBuyerPort() {
        return buyerPort;
    }

    public void setBuyerPort(String buyerPort) {
        this.buyerPort = buyerPort;
    }

    public PublicKey getAuctioneerPublicKey() {
        return auctioneerPublicKey;
    }

    public void setAuctioneerPublicKey(PublicKey auctioneerPublicKey) {
        this.auctioneerPublicKey = auctioneerPublicKey;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public PublicKey getBuyerPublicKey() {
        return buyerPublicKey;
    }

    public void setBuyerPublicKey(PublicKey buyerPublicKey) {
        this.buyerPublicKey = buyerPublicKey;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }
}
