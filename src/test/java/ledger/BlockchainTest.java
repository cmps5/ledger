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
        assertTrue("Block hash should meet the difficulty requirement.", block.getHash().startsWith(prefix));
    }
}
