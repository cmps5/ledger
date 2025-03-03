package kademlia;

import blockchain.Block;
import com.google.common.math.BigIntegerMath;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;

public class Kademlia {
    public static final int DIFFICULTY = 1;
    public static Kademlia instance;
    private static KBucket kBucket;

    // Number max of elements in kbucket
    private final int k = 20;
    // Degree of parallelism in network
    private final int alpha = 3;
    private String bootstrapId = "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
    private String IP;
    private String ID;
    private String port;
    private String bootstrapPort = "9090";

    private String bootstrapIp = "localhost";
    private boolean insideNetwork;

    public Kademlia() {
        // @ TODO
    }

    public static Kademlia getInstance() {
        if (instance == null) {
            instance = new Kademlia();
        }

        return instance;
    }

    private static void closeChannel(ManagedChannel channel) {
        try {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
    }

    private static PriorityQueue<NodeWrap> convertListToPriorityQueue(LinkedList<Node> linkedList, String targetId) {

        PriorityQueue<NodeWrap> result = new PriorityQueue<>(new NodeWrapComparator());

        Node node;
        while (!linkedList.isEmpty()) {
            node = linkedList.remove();
            result.add(new NodeWrap(node, targetId));
        }

        return result;
    }

    private static LinkedList sendFindRequest(String host, String port, String targetId) {

        Node myNode = kBucket.getNode();

        NodeInfo sender = NodeInfo.newBuilder()
                .setId(myNode.getId())
                .setIp(myNode.getIp())
                .setPort(myNode.getPort())
                .build();

        FindNodeRequest findRequest = FindNodeRequest.newBuilder()
                .setTargetId(targetId)
                .setSender(sender)
                .build();

        Node node = new Node(host, port, "");

        ManagedChannel channel = ManagedChannelBuilder.forAddress(node.getIp(), Integer.parseInt(node.getPort()))
                .usePlaintext().build();

        KademliaGrpc.KademliaBlockingStub kademliaStub = KademliaGrpc.newBlockingStub(channel); // Sync

        Iterator<FindNodeResponse> response;

        LinkedList<Node> responseNodes = new LinkedList<>();

        try {
            response = kademliaStub.withDeadlineAfter(1000, TimeUnit.MILLISECONDS).findNode(findRequest);
            while (response.hasNext()) {
                NodeInfo nodeResponse = response.next().getNode();
                Node toInsert = new Node(nodeResponse.getIp(), nodeResponse.getPort(), nodeResponse.getId());
                responseNodes.add(toInsert);
            }
        } catch (StatusRuntimeException e) {

        }
        closeChannel(channel);
        return responseNodes;
    }

    public boolean isInsideNetwork() {
        return insideNetwork;
    }

    public void findClosestNodes(String targetId) {

        PriorityQueue<NodeWrap> shortlist = convertListToPriorityQueue(kBucket.getClosestNodes(targetId), targetId);

        if (shortlist.isEmpty()) {
            return;
        }

        LinkedList<Node> probedNodes = new LinkedList<>();
        PriorityQueue<NodeWrap> closestNodes = new PriorityQueue<>(new NodeWrapComparator());

        int alpha = this.alpha;
        int k = this.k;

        while (probedNodes.size() < k && shortlist.size() > 0 && alpha > 0) {

            // Select alpha nodes from shortlist that they haven´t already been contacted
            LinkedList<Node> toSend = new LinkedList<>();

            while (toSend.size() != getAlpha() && shortlist.size() > 0) {
                Node toInsert = shortlist.poll().getNode();
                if (!searchList(probedNodes, toInsert)) {
                    toSend.add(toInsert);
                }
            }

            while (toSend.size() > 0) {
                Node nodeToSend = toSend.removeFirst();

                LinkedList<Node> response = sendFindRequest(nodeToSend.getIp(), nodeToSend.getPort(), targetId);

                // If response success update probedNodes()
                probedNodes.add(nodeToSend);

                while (!response.isEmpty()) {
                    NodeWrap nodeWrap = new NodeWrap(response.remove(), targetId);

                    shortlist.add(nodeWrap);

                    if (!searchListNodeWrap(closestNodes, nodeWrap))
                        closestNodes.add(nodeWrap);
                }
            }

            alpha--;
        }

        Iterator<NodeWrap> iterator = closestNodes.iterator();
        while (iterator.hasNext()) {
            Node toAdd = iterator.next().getNode();
            kBucket.insertNode(toAdd.getIp(), toAdd.getId(), toAdd.getPort());
        }
    }

    public String doJoin(long timestamp) {

        ManagedChannel channel = ManagedChannelBuilder.forAddress(bootstrapIp, Integer.parseInt(bootstrapPort))
                .usePlaintext().build();
        KademliaGrpc.KademliaBlockingStub kademliaStub = KademliaGrpc.newBlockingStub(channel); // Sync

        int nonce = 0;

        String hash = IP + port + timestamp + nonce;
        JoinRequest request = JoinRequest.newBuilder().setHash(hash).setIp(IP).setPort(port).setTimestamp(timestamp)
                .build();

        JoinResponse response = null;

        try {
            response = kademliaStub.join(request);
        } catch (StatusRuntimeException e) {

        }

        closeChannel(channel);

        return (response != null) ? response.getId() : null;
    }

    public void enterKademlia() {

        if (!port.equals(getBootstrapPort())) {

            ID = doJoin(/* timestamp */ 2323);

            if (ID.equals("")) {
                System.out.print(ID + "--hfxgmdzjfhag--");
                return;
            }

            // utils.setID(ID); @TODO

            this.insideNetwork = true;
            kBucket = new KBucket(new Node(IP, port, ID));
            findClosestNodes(ID);

        } else {
            this.insideNetwork = true;
            kBucket = new KBucket(new Node(IP, port, bootstrapId));

            ID = bootstrapId;
            // utils.setID(bootstrapId); @TODO
        }
    }

    public String getBootstrapIp() {
        return bootstrapIp;
    }

    public String getBootstrapPort() {
        return bootstrapPort;
    }

    public String getBootstrapId() {
        return bootstrapId;
    }

    public void doStore(Block newBlock) {
        // TODO
    }

    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public BigInteger xorDistance(String targetId, String myId) {
        BigInteger target = new BigInteger(targetId, 2);
        BigInteger my = new BigInteger(myId, 2);
        return my.xor(target);
    }

    public int findDistancePos(String targetId, String myId) {
        BigInteger distance = xorDistance(targetId, myId);
        int pos = 0;

        if (BigInteger.valueOf(pos).compareTo(distance) != 0) {
            pos = BigIntegerMath.log2(distance, RoundingMode.DOWN);
        }
        return pos;
    }

    public <T> boolean searchList(T list, Node toSearch) {

        Iterator<Node> iterator = null;

        if (list instanceof LinkedList) {
            iterator = ((LinkedList) list).iterator();
        } else if (list instanceof PriorityQueue) {
            iterator = ((PriorityQueue) list).iterator();
        } else if (list instanceof ArrayList) {
            iterator = ((ArrayList) list).iterator();
        }

        while (iterator.hasNext()) {
            Node next = iterator.next();
            if (next.compare(toSearch))
                return true;
        }

        return false;
    }

    public <T> int searchPos(T list, Node toSearch) {
        Iterator<Node> iterator = null;

        if (list instanceof LinkedList) {
            iterator = ((LinkedList) list).iterator();
        } else if (list instanceof PriorityQueue) {
            iterator = ((PriorityQueue) list).iterator();
        } else if (list instanceof ArrayList) {
            iterator = ((ArrayList) list).iterator();
        }

        int pos = -1;
        while (iterator.hasNext()) {
            Node next = iterator.next();
            pos += 1;
            if (next.compare(toSearch))
                return pos;
        }
        return -1;
    }

    public <T> boolean searchListNodeWrap(T list, NodeWrap toSearch) {

        Iterator<NodeWrap> iterator = null;

        if (list instanceof LinkedList) {
            iterator = ((LinkedList) list).iterator();
        } else if (list instanceof PriorityQueue) {
            iterator = ((PriorityQueue) list).iterator();
        } else if (list instanceof ArrayList) {
            iterator = ((ArrayList) list).iterator();
        }

        while (iterator.hasNext()) {
            NodeWrap next = iterator.next();
            if (next.compare(toSearch))
                return true;
        }

        return false;
    }

    public int getAlpha() {
        return alpha;
    }

    public int getK() {
        return k;
    }

    // TODO
    public void search(String key) {
        //
    }
}
