package blockchain;

import auction.Auction;
import kademlia.TransactionProto;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import peer.Wallet;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;

public class Transaction {

    private static Wallet wallet;

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

    public Transaction(TransactionProto transaction) {
        this.itemName = transaction.getName();
        this.price = transaction.getFinalPrice();
        this.timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(transaction.getTimestamp()), ZoneId.systemDefault());
        this.buyerPublicKey = bytesToPubKey(transaction.getSignature().getPublicKey().toByteArray());
        this.hash = transaction.getSignature().getHash();
        this.auctioneerPublicKey = bytesToPubKey(transaction.getAuctionPublicKey().toByteArray());
        this.signature = transaction.getSignature().getSignature().toByteArray();
        this.buyerID = transaction.getBuyerInfo().getId();
        this.buyerIP = transaction.getBuyerInfo().getIp();
        this.buyerPort = transaction.getBuyerInfo().getPort();
        this.isActive = transaction.getAtive();
    }

    public static boolean verifySignature(byte[] signature, String hash, PublicKey pubKey) {
        if (signature == null || pubKey == null || hash == null) {
            return false;
        }

        try {
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initVerify(pubKey);
            sign.update(new BigInteger(hash, 16).toByteArray());
            return sign.verify(signature);
        } catch (NoSuchAlgorithmException | SignatureException e) {
            throw new RuntimeException("Failed to verify hash", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    private PublicKey bytesToPubKey(byte[] byteKey) {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(byteKey);
        // Get the RSA key factory
        KeyFactory keyFactory = null;
        PublicKey publicKey = null;
        try {
            keyFactory = KeyFactory.getInstance("RSA", new BouncyCastleProvider());
            // Generate the PublicKey object
            publicKey = keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
        return publicKey;
    }

    private byte[] generateSignature(String hash) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(wallet.getPrivKey());
            signature.update(new BigInteger(hash, 16).toByteArray());
            return signature.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new RuntimeException("Failed to sign hash", e);
        }
    }

    private String generateCheckSum(Auction auction, PublicKey auctioneerPublicKey, String buyerID, String buyerIP, String buyerPort) {
        String pubKeyHex;
        {
            byte[] pubKeyBytes = auctioneerPublicKey.getEncoded();
            StringBuilder sb = new StringBuilder();
            for (byte b : pubKeyBytes) {
                sb.append(String.format("%02x", b));
            }
            pubKeyHex = sb.toString();
        }

        String dataToHash = auction.getName() + pubKeyHex + buyerID + buyerIP + buyerPort;

        // Hashing with SHA-256
        String checksum;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(dataToHash.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            checksum = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        return checksum;
    }


    @Override
    public String toString() {
        return "Transaction:" +
                "\nItem name: " + itemName +
                ";\n Price: " + price +
                ";\n Timestamp: " + timestamp +
                ";\n Signature: " + Arrays.toString(signature) +
                ";\n Buyer ID: " + buyerID;
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
