package ledger;

import blockchain.Block;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestBlock {
    private Block block;

    @Before
    public void setUp() {
        block = new Block("previousHashDummy");
    }

    @Test
    public void testCreateBlock() {
        assertNotNull(block);
        assertNotNull(block.getHash());
        assertEquals("previousHashDummy", block.getPreviousHash());
        assertTrue(block.getTimeStamp() > 0);
    }

    @Test
    public void testHash() {
        Block anotherBlock = new Block(block.getHash());
        assertNotEquals(block.getHash(), anotherBlock.getHash());
    }

    @Test
    public void testTimestamp() {
        Block newBlock = new Block("previousHashDummy");
        assertTrue(newBlock.getTimeStamp() > block.getTimeStamp());
    }

}
