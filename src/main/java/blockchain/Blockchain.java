package blockchain;

import java.util.ArrayList;

public class Blockchain {
    private ArrayList<Block> chain;

    public Blockchain() {
        chain = new ArrayList<>();
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
