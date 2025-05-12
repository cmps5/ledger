package peer;

import auction.Auction;
import auction.AuctionManager;
import blockchain.Block;
import blockchain.Blockchain;
import blockchain.Transaction;
import kademlia.Kademlia;

import java.util.Scanner;

public class Client implements Runnable {

    private final Scanner scanner;
    private final Kademlia kademlia;
    private final Blockchain blockchain;
    private final AuctionManager auctionManager;
    private final Wallet wallet;

    public Client() {
        this.kademlia = Kademlia.getInstance();
        this.blockchain = Blockchain.getInstance();
        this.auctionManager = AuctionManager.getInstance();
        this.scanner = new Scanner(System.in);
        this.wallet = Wallet.getInstance();
    }

    private void menu() {
        System.out.println("\n");
        System.out.println("1. Enter Kademlia");
        System.out.println("2. Search Value in Kademlia");
        System.out.println("3. Store Block");
        System.out.println("4. Print Blockchain");
        System.out.println("5. Start Auction");
        System.out.println("6. Print Auctions");
        System.out.println("7. Send Bid");
    }

    @Override
    public void run() {

        while (true) {

            menu();
            System.out.print("\u001B[34m" + "\n$ " + "\u001B[0m");
            String command = scanner.nextLine();

            if (!kademlia.isInsideNetwork() && !command.equals("1")) {
                System.out.println("Error - Node needs to enter in the network");
                continue;
            }

            switch (command) {
                case "1": {
                    kademlia.enterKademlia();
                    break;
                }
                case "2": { //search node
                    String key;
                    System.out.print("Key: ");
                    key = scanner.nextLine();
                    kademlia.search(key);
                    break;
                }
                case "3": {
                    Block block;
                    String name;
                    String basePrice;

                    System.out.print("Name: ");
                    name = scanner.nextLine();
                    System.out.print("Price: ");
                    basePrice = scanner.nextLine();

                    Auction auction = new Auction(name, Integer.parseInt(basePrice));
                    auction.setCurrentBid(Integer.parseInt(basePrice) + 100);
                    auction.setCurrentBidder(wallet.getID());

                    Transaction transaction = new Transaction(auction, Integer.parseInt(basePrice) + 100, true);

                    if (blockchain.getBlockchain().isEmpty())
                        block = new Block(Blockchain.GENESIS_PREV_HASH);
                    else
                        block = new Block(blockchain.getLastBlock().getHash());

                    block.setTransaction(transaction);

                    blockchain.addBlockPOW(block);
                    kademlia.storeBlock(block);
                    break;
                }
                case "4": {
                    System.out.println(blockchain.toString());
                    break;
                }
                case "5": { //startAuction
                    String name;
                    String basePrice;

                    System.out.print("Name: ");
                    name = scanner.nextLine();

                    System.out.print("Price: ");
                    basePrice = scanner.nextLine();

                    auctionManager.createNewAuction(name, Integer.parseInt(basePrice));
                    break;
                }
                case "6": {
                    auctionManager.printAuctions();
                    break;
                }
                case "7": {
                    System.out.print("Name: ");
                    String topic = scanner.nextLine();

                    System.out.print("Bid: ");
                    String amount = scanner.nextLine();

                    auctionManager.publishBid(topic, Integer.parseInt(amount));
                    break;
                }
                default:
                    break;
            }
        }
    }
}





