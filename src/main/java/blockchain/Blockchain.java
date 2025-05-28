package blockchain;

import kademlia.Kademlia;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

public class Blockchain {
    // The hash of the first block in the blockchain (genesis block) - 64 zeros (256 bits ÷ 4 bits)
    public static final String GENESIS_PREV_HASH = "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
    private static final int DIFFICULTY = 1;
    private static final int CONFLICT_DEPTH = 5;

    private static Blockchain instance;
    private List<Block> chain;
    private List<List<Block>> conflictChains;

    private Kademlia kademlia;


    public Blockchain() {
        this.chain = new ArrayList<>();
        this.conflictChains = new ArrayList<>();

        this.kademlia = Kademlia.getInstance();
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

    public ArrayList<Block> getBlockchain() {
        return (ArrayList<Block>) this.chain;
    }

    public long getChainSize() {
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

    public boolean searchBlock(Block block) {
        boolean blockExists = false;
        if (conflictChains.isEmpty()) {
            return chain.contains(block);
        } else {
            for (List<Block> chain : conflictChains) {
                if (chain.contains(block))
                    blockExists = true;
            }
        }
        return blockExists;
    }

    public void storeBlock(Block block) {
        if (validateBlock(block)) {
            tryResolveForks();
        }
    }

    public synchronized void tryResolveForks() {
        Stack<List<Block>> forksToClose = new Stack<>();
        sortAllListsBySize();
        if (!conflictChains.isEmpty()) {
            int biggestForkSize = this.conflictChains.get(0).size();
            for (List<Block> fork : this.conflictChains) {
                if (biggestForkSize - fork.size() > CONFLICT_DEPTH) {
                    forksToClose.add(fork);
                }
            }
            if (!forksToClose.isEmpty()) {
                while (!forksToClose.isEmpty()) {
                    handleForkTerminate(forksToClose.pop());
                }
            } else {
                System.out.println("No forks can be resolved");
            }
        }
    }

    public synchronized void handleForkTerminate(List<Block> fork) {
        for (Block blockToDiscard : fork) {
            if (!conflictChains.get(0).contains(blockToDiscard)) {
                //Block not in the main chain
                try {
                    if (addBlockFromClosedForkPOW(blockToDiscard)) {
                        kademlia.storeBlock(blockToDiscard);
                    }
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        this.conflictChains.remove(fork);
    }

    public synchronized boolean addBlockFromClosedForkPOW(Block block) throws NoSuchAlgorithmException {
        if (conflictChains.isEmpty()) {
            if (this.chain.isEmpty()) {
                block.setPreviousHash(GENESIS_PREV_HASH);
            } else {
                block.setPreviousHash(getLastBlockHash(this.chain));
            }
        } else {
            sortAllListsBySize();
            //Pick the one that comes first
            String prevHash = getLastBlockHash(conflictChains.get(0));
            block.setPreviousHash(prevHash);
            block.generateHash();
        }

        //Mine Block
        Block minedBlock = mine(block);
        if (validateBlock(minedBlock)) {
            //Add Mined Block
            addLocalMinedBlock(minedBlock);
            return true;
        }
        return false;
    }

    public synchronized void addLocalMinedBlock(Block newMinedBlock) {
        if (conflictChains.isEmpty()) {
            this.chain.add(newMinedBlock);
        } else {

            conflictChains.get(0).add(newMinedBlock);
            sortAllListsBySize();
            tryResolveForks();
        }
    }

    public synchronized void sortAllListsBySize() {
        if (this.conflictChains != null && !this.conflictChains.isEmpty()) {
            this.conflictChains.sort(Comparator.comparingInt((List<Block> l) -> l.size()).reversed());
            this.chain = this.conflictChains.get(0);
        }
    }

    public List<Block> getChain() {
        return chain;
    }

    public void setChain(List<Block> chain) {
        this.chain = chain;
    }

    public List<List<Block>> getConflictChains() {
        return conflictChains;
    }

    public void setConflictChains(List<List<Block>> conflictChains) {
        this.conflictChains = conflictChains;
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
