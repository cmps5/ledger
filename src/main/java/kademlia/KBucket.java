package kademlia;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class KBucket {

    private Node node;
    private ArrayList[] kbucket = new ArrayList[160];

    public KBucket(Node node) {
        this.node = node;

        for (int i = 0; i < 160; i++)
            kbucket[i] = new ArrayList<Node>();

        if (!node.getId().equals(Kademlia.getInstance().getBootstrapId())) {
            insertNode(Kademlia.getInstance().getBootstrapIp(), Kademlia.getInstance().getBootstrapId(),
                    Kademlia.getInstance().getBootstrapPort());
        }
    }

    public synchronized LinkedList<Node> getClosestNodes(String target) {
        LinkedList<Node> closestNodes = new LinkedList<>();

        int distance = Kademlia.getInstance().findDistancePos(target, node.getId());
        int alpha = Kademlia.getInstance().getAlpha();

        alpha = addNodesFromBucket(distance, alpha, closestNodes);

        for (int i = 1; alpha > 0 && (distance - i >= 0 || distance + i < 160); i++) {
            if (distance - i >= 0) {
                alpha = addNodesFromBucket(distance - i, alpha, closestNodes);
            }
            if (alpha == 0) break;
            if (distance + i < 160) {
                alpha = addNodesFromBucket(distance + i, alpha, closestNodes);
            }
        }

        return closestNodes;
    }

    private int addNodesFromBucket(int index, int remaining, LinkedList<Node> list) {
        List<Node> bucket = kbucket[index];
        for (int i = 0; i < bucket.size() && remaining > 0; i++, remaining--) {
            list.add(bucket.get(i));
        }
        return remaining;
    }


    public synchronized LinkedList<Node> getClosestNodes(String targetId, Node requesterNode) {
        LinkedList<Node> closestNodes = new LinkedList<>();

        int distance = Kademlia.getInstance().findDistancePos(targetId, node.getId());
        int alpha = Kademlia.getInstance().getAlpha();

        alpha = addNodesExcludingRequester(distance, alpha, closestNodes, requesterNode);

        for (int i = 1; alpha > 0 && (distance - i >= 0 || distance + i < 160); i++) {
            if (distance - i >= 0) {
                alpha = addNodesExcludingRequester(distance - i, alpha, closestNodes, requesterNode);
            }
            if (alpha == 0) break;
            if (distance + i < 160) {
                alpha = addNodesExcludingRequester(distance + i, alpha, closestNodes, requesterNode);
            }
        }

        return closestNodes;
    }

    private int addNodesExcludingRequester(int index, int remaining, LinkedList<Node> list, Node requesterNode) {
        List<Node> bucket = kbucket[index];
        for (int i = 0; i < bucket.size() && remaining > 0; i++) {
            Node node = bucket.get(i);
            if (node.compare(requesterNode)) continue;
            list.add(node);
            remaining--;
        }
        return remaining;
    }


    public synchronized void insertNode(String nodeIP, String nodeID, String nodePort) {
        Node nodeToInsert = new Node(nodeIP, nodePort, nodeID);
        int distance = Kademlia.getInstance().findDistancePos(nodeToInsert.getId(), node.getId());

        if (Kademlia.getInstance().searchList(kbucket[distance], nodeToInsert)) {
            int pos = Kademlia.getInstance().searchPos(kbucket[distance], nodeToInsert);
            kbucket[distance].remove(pos);
            kbucket[distance].add(nodeToInsert);
            return;
        }

        if (kbucket[distance].size() >= Kademlia.getInstance().getK()) {
            Node toPing = (Node) kbucket[distance].get(0);
            if (!doPing(toPing.getIp(), toPing.getPort())) {
                kbucket[distance].remove(0);
                kbucket[distance].add(nodeToInsert);
            }
            return;
        }

        kbucket[distance].add(nodeToInsert);
    }

    public boolean doPing(String targetIp, String targetPort) {

        NodeInfo sender = NodeInfo.newBuilder()
                .setId(node.getId())
                .setIp(node.getIp())
                .setPort(node.getPort())
                .build();

        ManagedChannel channel = ManagedChannelBuilder.forAddress(targetIp, Integer.parseInt(targetPort)).usePlaintext()
                .build();
        KademliaGrpc.KademliaBlockingStub kademliaStub = KademliaGrpc.newBlockingStub(channel); // Sync

        boolean result = false;

        try {
            NodeInfo response;

            response = kademliaStub.withDeadlineAfter(1000, TimeUnit.MILLISECONDS).ping(sender);

            if (node.getPort().equals(response.getPort())
                    && node.getIp().equals(response.getIp())
                    && node.getId().equals(response.getId())) {
                result = true;
            }

        } catch (StatusRuntimeException e) {

        }

        try {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {

        }

        return result;
    }

    public Node getNode() {
        return node;
    }

    public ArrayList[] getKbucket() {
        return kbucket;
    }
}
