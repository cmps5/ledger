package ledger;

import blockchain.*;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class BlockchainTest {
    private Blockchain blockchain;

    @Before
    public void setUp() {
        blockchain = new Blockchain();
    }

    @Test
    public void testAddBlock() {
        Block block = new Block("Block");
        assertTrue(blockchain.addBlockPOW(block));
        assertEquals(1,blockchain.getChainSize());
        assertEquals(Blockchain.GENESIS_PREV_HASH, block.getPreviousHash());

        Block block1 = new Block(null);
        blockchain.addBlockPOW(block1);
        assertEquals(2, blockchain.getChainSize());
        assertEquals(block.getHash(), block1.getPreviousHash());
    }

    @Test
    public void testBlockValidation() {
        Block block = new Block("Block");
        blockchain.addBlockPOW(block);

        assertTrue(blockchain.validateBlock(block));
    }

    @Test
    public void testProofOfWork() {
        Block block = new Block("Mining test");
        blockchain.mine(block);

        String prefix = "0"; // DIFFICULTY = 1
        assertTrue(block.getHash().startsWith(prefix));
    }
}
