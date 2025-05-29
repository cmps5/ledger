package kademlia;

import auction.Auction;
import blockchain.Block;
import blockchain.Transaction;
import com.google.common.math.BigIntegerMath;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import peer.Wallet;

import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.*;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;

public class Kademlia {
    public static final int DIFFICULTY = 1;
    public static Kademlia instance;
    private static KBucket kBucket;
    private static Wallet wallet;

    // Number max of elements in kbucket
    private final int k = 20;
    // Degree of parallelism in network
    private final int alpha = 3;
    private final String bootstrapId = "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
    private final String bootstrapPort = "8000";
    private final String bootstrapIp = "localhost";
    private String IP;
    private String ID;
    private String port;
    private boolean insideNetwork;

    public Kademlia() {
        wallet = Wallet.getInstance();
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
            throw new RuntimeException(e);
        }
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
            throw new RuntimeException(e);
        }
        closeChannel(channel);
        return responseNodes;
    }

    public static void sendStoreRequest(Node toSend, String key, Object dataToSend) {

        Node node = kBucket.getNode();
        NodeInfo sender = NodeInfo.newBuilder()
                .setId(node.getId())
                .setIp(node.getIp())
                .setPort(node.getPort())
                .build();

        ManagedChannel channel = ManagedChannelBuilder.forAddress(toSend.getIp(), Integer.parseInt(toSend.getPort()))
                .usePlaintext().build();

        KademliaGrpc.KademliaBlockingStub kademliaStub = KademliaGrpc.newBlockingStub(channel); //Sync

        StoreRequest request = null;
        if (dataToSend.getClass() == Block.class) {
            request = StoreRequest.newBuilder()
                    .setSender(sender)
                    .setKey(key)
                    .setBlock(blockToProto((Block) dataToSend))
                    .build();
        } else if (dataToSend.getClass() == Auction.class) {
            request = StoreRequest.newBuilder()
                    .setSender(sender)
                    .setKey(key)
                    .setAuction(auctionToProto((Auction) dataToSend))
                    .build();
        }

        if (request == null) {
            closeChannel(channel);
        }

        StoreResponse response = null;
        try {
            response = kademliaStub.withDeadlineAfter(300000, TimeUnit.MILLISECONDS).store(request);

        } catch (StatusRuntimeException e) {
            e.printStackTrace();
        }

        closeChannel(channel);
    }

    private static AuctionProto auctionToProto(Auction auction) {
        long timestampMillis = auction.getCountdown()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        byte[] auctionSignature;

        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(wallet.getPrivKey());
            signature.update(new BigInteger(auction.getName(), 16).toByteArray());
            auctionSignature = signature.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new RuntimeException("Failed to sign hash", e);
        }

        byte[] pubKeyBytes = wallet.getPubKey().getEncoded();

        SignatureProto signatureProto = SignatureProto.newBuilder()
                .setSignature(ByteString.copyFrom(auctionSignature))
                .setHash(auction.getName())
                .setPublicKey(ByteString.copyFrom(pubKeyBytes))
                .build();

        return AuctionProto.newBuilder()
                .setName(auction.getName())
                .setCurrentBid(auction.getCurrentBid())
                .setCurrentBidder(auction.getCurrentBidder())
                .setBasePrice(auction.getBasePrice())
                .setDeadline(timestampMillis)
                .setSignature(signatureProto)
                .setSellerID(auction.getSellerID())
                .setSellerPubKey(ByteString.copyFrom(auction.getSellerPubKey().getEncoded()))
                .build();
    }

    static BlockProto blockToProto(Block block) {
        return BlockProto.newBuilder()
                .setHash(block.getHash())
                .setNonce(block.getNonce())
                .setPrevioushash(block.getPreviousHash())
                .setTimestamp(block.getTimeStamp())
                .setTransaction(transactionToProto(block.getTransaction()))
                .build();
    }

    private static TransactionProto transactionToProto(Transaction transaction) {
        SignatureProto signatureProto = SignatureProto.newBuilder()
                .setSignature(ByteString.copyFrom(transaction.getSignature()))
                .setHash(transaction.getHash())
                .setPublicKey(ByteString.copyFrom(transaction.getBuyerPublicKey().getEncoded()))
                .build();

        NodeInfo buyerInfo = NodeInfo.newBuilder()
                .setId(transaction.getBuyerID())
                .setIp(transaction.getBuyerIP())
                .setPort(transaction.getBuyerPort())
                .build();

        return TransactionProto.newBuilder()
                .setName(transaction.getItemName())
                .setFinalPrice(transaction.getPrice())
                .setTimestamp(transaction.getTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .setAtive(transaction.isActive())
                .setAuctionPublicKey(ByteString.copyFrom(transaction.getAuctioneerPublicKey().getEncoded()))
                .setSignature(signatureProto)
                .setBuyerInfo(buyerInfo)
                .build();
    }

    public boolean isInsideNetwork() {
        return insideNetwork;
    }

    public void findClosestNodes(String targetId) {

        PriorityQueue<NodeWrap> shortlist = new PriorityQueue<>(new NodeWrapComparator());

        LinkedList<Node> bucketClosestNodes = kBucket.getClosestNodes(targetId);
        Node node;
        while (!bucketClosestNodes.isEmpty()) {
            node = bucketClosestNodes.remove();
            shortlist.add(new NodeWrap(node, targetId));
        }

        if (shortlist.isEmpty()) {
            return;
        }

        LinkedList<Node> probedNodes = new LinkedList<>();
        PriorityQueue<NodeWrap> closestNodes = new PriorityQueue<>(new NodeWrapComparator());

        int alpha = this.alpha;
        int k = this.k;

        while (probedNodes.size() < k && !shortlist.isEmpty() && alpha > 0) {

            // Select alpha nodes from shortlist that they have not already been contacted
            LinkedList<Node> toSend = new LinkedList<>();

            while (toSend.size() != getAlpha() && !shortlist.isEmpty()) {
                Node toInsert = shortlist.poll().getNode();
                if (!searchList(probedNodes, toInsert)) {
                    toSend.add(toInsert);
                }
            }

            while (!toSend.isEmpty()) {
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

        for (NodeWrap closestNode : closestNodes) {
            Node toAdd = closestNode.getNode();
            kBucket.insertNode(toAdd.getIp(), toAdd.getId(), toAdd.getPort());
        }
    }

    public String doJoin(long timestamp) {

        ManagedChannel channel = ManagedChannelBuilder.forAddress(bootstrapIp, Integer.parseInt(bootstrapPort))
                .usePlaintext().build();
        KademliaGrpc.KademliaBlockingStub kademliaStub = KademliaGrpc.newBlockingStub(channel); // Sync


        String hash = IP + port + timestamp;
        JoinRequest request = JoinRequest.newBuilder().setHash(hash).setIp(IP).setPort(port).setTimestamp(timestamp)
                .build();

        JoinResponse response;

        try {
            response = kademliaStub.join(request);
        } catch (StatusRuntimeException e) {
            throw new RuntimeException(e);
        }

        closeChannel(channel);

        return (response != null) ? response.getId() : null;
    }

    public void enterKademlia() {

        if (!port.equals(getBootstrapPort())) {

            long timestamp = System.currentTimeMillis();

            ID = doJoin(timestamp);

            if (ID.isEmpty()) {
                System.out.print("--empty id--");
                return;
            }

            System.out.println("Generated ID: " + ID);
            wallet.setID(ID);

            this.insideNetwork = true;
            kBucket = new KBucket(new Node(IP, port, ID));
            findClosestNodes(ID);

        } else {
            this.insideNetwork = true;
            kBucket = new KBucket(new Node(IP, port, bootstrapId));

            ID = bootstrapId;
            wallet.setID(bootstrapId);
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

    public void storeBlock(Block block) {
        // Hash the block's hash
        byte[] key;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            key = digest.digest(block.getHash().getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        // Convert the hash to a binary string
        BigInteger inputBigInt = new BigInteger(1, key);
        String keyConverted = inputBigInt.toString(2);
        while (keyConverted.length() % 8 != 0) {
            keyConverted = "0" + keyConverted;
        }

        // Do the lookup process
        findClosestNodes(keyConverted);
        // Search the closest Nodes on the bucket
        LinkedList<Node> closestNodes = kBucket.getClosestNodes(keyConverted);

        // Send STORE gRPC
        /*
        Iterator iterator = closestNodes.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            System.out.println("sending store request " + i++);
            sendStoreRequest((Node) iterator.next(), keyConverted, block);
        }
        //*/
    }

    public void storeAuction(Auction auction) {
        // Convert auction name (hex string) to byte array
        BigInteger bigInteger = new BigInteger(auction.getName(), 16);
        byte[] auctionNameBytes = bigInteger.toByteArray();

        // Hash the byte array using SHA-1
        byte[] key;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            key = digest.digest(auctionNameBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        // Convert the hash to a binary string
        BigInteger inputBigInt = new BigInteger(1, key);
        String keyConverted = inputBigInt.toString(2);
        while (keyConverted.length() % 8 != 0) {
            keyConverted = "0" + keyConverted;
        }

        // Do the lookup process
        findClosestNodes(keyConverted);
        // Search the closest Nodes on the bucket
        LinkedList<Node> closestNodes = kBucket.getClosestNodes(keyConverted);

        if (auction.isActive()) {
            // Send STORE gRPC
            Iterator iterator = closestNodes.iterator();
            int i = 0;
            while (iterator.hasNext()) {
                sendStoreRequest((Node) iterator.next(), keyConverted, auction);
                System.out.println("sending store request " + i++);
            }
        }
    }

    public void disseminateStore(Object dataToStore, String key) {
        ArrayList[] allNodes = kBucket.getKbucket();
        for (int i = 0; i < 160; i++) {
            if (!allNodes[i].isEmpty()) {
                for (int x = 0; x < allNodes[i].size(); x++) {
                    Node node = (Node) allNodes[i].get(x);
                    sendStoreRequest(node, key, dataToStore);
                }
            }
        }
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

        while (true) {
            assert iterator != null;
            if (!iterator.hasNext()) break;
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
        while (true) {
            assert iterator != null;
            if (!iterator.hasNext()) break;
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

        while (true) {
            assert iterator != null;
            if (!iterator.hasNext()) break;
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

    public void insertNode(String ip, String id, String port) {
        kBucket.insertNode(ip, id, port);
    }

    public LinkedList<Node> getClosestNodes(String target, Node requesterNode) {
        return kBucket.getClosestNodes(target, requesterNode);
    }
}
