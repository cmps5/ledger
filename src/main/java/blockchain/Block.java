package blockchain;

import org.bouncycastle.crypto.digests.SHA256Digest;

import java.nio.charset.StandardCharsets;
import java.util.Date;

public class Block {
    private Transaction transactions = null;
    private String hash;
    private String previousHash;
    private int nonce;
    private long timeStamp; // timestamp of the block in milliseconds.

    public Block(String previousHash) {
        this.previousHash = previousHash;
        this.timeStamp = new Date().getTime();
        generateHash();
    }

    /**
     * Generates the SHA-256 hash for the current block using its previous hash,
     * timestamp, and nonce.
     * 
     * The resulting hash is stored in the {@code hash} field.
     */

    public void generateHash() {
        String dataToHash = this.previousHash + this.timeStamp + this.nonce;
        this.hash = computeHash(dataToHash);
    }

    /**
     * Generates the SHA-256 hash for the block using the given nonce.
     *
     * @param nonce The nonce value to use for generating the hash.
     * @return A hexadecimal string representing the block's hash.
     * @see #generateHash()
     */
    public String generateHash(int nonce) {
        String dataToHash = this.previousHash + this.timeStamp + nonce;
        return computeHash(dataToHash);
    }

    /**
     * Computes the SHA-256 hash for the provided data.
     *
     * @param dataToHash The concatenated string containing the block's previous
     *                   hash, timestamp, and nonce.
     * @return A hexadecimal string representing the computed SHA-256 hash.
     */
    private String computeHash(String dataToHash) {
        SHA256Digest digest = new SHA256Digest();
        byte[] input = dataToHash.getBytes(StandardCharsets.UTF_8);
        digest.update(input, 0, input.length);

        byte[] hashBytes = new byte[digest.getDigestSize()];
        digest.doFinal(hashBytes, 0);

        StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));
        }

        return hexString.toString();
    }

    // getters and setters

    public Transaction getTransactions() {
        return transactions;
    }

    public void setTransactions(Transaction transactions) {
        this.transactions = transactions;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public int getNonce() {
        return nonce;
    }

    public void setNonce(int nonce) {
        this.nonce = nonce;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    @Override
    public String toString() {
        return "Hash:" + this.hash +
                "\nPrevHash:" + this.previousHash +
                "\nTime:" + this.timeStamp +
                "\nNonce:" + this.nonce +
                "\n" + this.transactions + "\n";
    }

    public void incrementNonce() {
        this.nonce++;
    }
}
