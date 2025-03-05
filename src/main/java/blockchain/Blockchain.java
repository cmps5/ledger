package blockchain;

import java.util.ArrayList;
import java.util.List;

public class Blockchain {
    // The hash of the first block in the blockchain (genesis block) - 64 zeros (256 bits ÷ 4 bits)
    public static final String GENESIS_PREV_HASH = "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
    private static final int DIFFICULTY = 1;

    private static Blockchain instance;
    private ArrayList<Block> chain;

    public Blockchain() {
        chain = new ArrayList<>();
    }

    public static Blockchain getInstance() {
        if (instance == null) {
            instance = new Blockchain();
        }
        return instance;
    }

    public Block getLastBlock() {
        if (chain.isEmpty()) {
            return null;
        }
        return chain.get(chain.size() - 1);
    }

    public ArrayList<Block> getBlockchain (){
        return this.chain;
    }

    public long getChainSize(){
        return this.chain.size();
    }

    public void addBlock(Block block) {
        this.chain.add(block);
    }

    public String getLastBlockHash(List<Block> chain) {
        if (chain.isEmpty()) {
            return GENESIS_PREV_HASH;
        }
        return chain.get(chain.size() - 1).getHash();
    }

    public boolean addBlockPOW(Block block) {
        if (this.chain.isEmpty()) {
            block.setPreviousHash(GENESIS_PREV_HASH);
        } else {
            block.setPreviousHash(getLastBlockHash(this.chain));
        }

        Block minedBlock = mine(block);
        if (validateBlock(minedBlock)) {
            addBlock(minedBlock);
            return true;
        }
        return false;
    }

    /**
     * Validates the block's hash against difficulty requirements
     *
     * @param block The block to validate
     * @return true if the hash is valid
     */
    public boolean validateBlock(Block block) {
        String zeros = new String(new char[DIFFICULTY]).replace('\0', '0');
        String blockNonceHash = block.generateHash(block.getNonce());

        return blockNonceHash.startsWith(zeros)
                && blockNonceHash.equals(block.getHash());
    }

    /**
     * Mines the given block until a valid hash is found for difficulty requirement
     *
     * @param block The block to be mined
     * @return The mined block with a valid hash
     */
    public Block mine(Block block) {
        while (proofOfWork(block)) {
            block.incrementNonce();
            block.generateHash();
        }
        return block;
    }

    /**
     * Checks if the given block meets the Proof of Work requirement
     * The block's hash must start with a number of zeros defined by the difficulty
     *
     * @param block The block to be checked.
     * @return true if the block does not meet the PoW requirement
     */
    public boolean proofOfWork(Block block) {
        String zeros = new String(new char[DIFFICULTY]).replace('\0', '0');
        return !block.getHash().substring(0, DIFFICULTY).equals(zeros);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Blockchain:\n");
        builder.append("====================================\n");

        this.chain.forEach(block -> builder.append(block).append("\n"));

        builder.append("====================================\n");

        return builder.toString();
    }
}
