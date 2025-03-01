package blockchain;

import auction.Auction;

public class Transaction {

    private String itemName;
    private int price;
    private boolean isActive;

    public Transaction(Auction auction, int amount, boolean isActive) {
        this.isActive = isActive;
        this.itemName = auction.getName();
        this.price = amount;
    }
}
