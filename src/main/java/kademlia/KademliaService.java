package kademlia;

import auction.AuctionManager;
import blockchain.Blockchain;
import io.grpc.stub.StreamObserver;
import org.bouncycastle.crypto.digests.SHA256Digest;
import peer.Wallet;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class KademliaService extends KademliaGrpc.KademliaImplBase {

    Kademlia kademlia;
    Blockchain blockChain;
    AuctionManager auctionApp;
    Wallet wallet;

    public KademliaService(Kademlia kademlia) {
        this.kademlia = kademlia;
        this.wallet = Wallet.getInstance();
        this.blockChain = Blockchain.getInstance();
        this.auctionApp = AuctionManager.getInstance();
    }

    @Override
    public void join(JoinRequest request, StreamObserver<JoinResponse> responseObserver) {
        String id = "";
        String ip = request.getIp();
        String port = request.getPort();
        long timestamp = request.getTimestamp();

        String toHash = ip + port + timestamp;

        SHA256Digest digest = new SHA256Digest();
        byte[] inputBytes = toHash.getBytes(StandardCharsets.UTF_8);
        digest.update(inputBytes, 0, inputBytes.length);

        byte[] hashBytes = new byte[digest.getDigestSize()];
        digest.doFinal(hashBytes, 0);

        // Hexadecimal string
        StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));
        }
        String hash = hexString.toString();

        // Binary string (com padding para 256 bits)
        BigInteger bigInt = new BigInteger(1, hashBytes);

        id = bigInt.toString(2);

        while (id.length() % 8 != 0) {
            id = "0" + id;
        }

        // Truncate to the first 160 bits
        id = id.substring(0, 160);


        //while (kademlia.searchKbucket(id)) {

        //}

        // TODO
        JoinResponse.Builder response = JoinResponse.newBuilder();
        response.setId(id);
        //response.setId("010000011101011");


        responseObserver.onNext(response.build()); // Build response and send
        responseObserver.onCompleted(); // End connection
    }

    @Override
    public void ping(NodeInfo request, StreamObserver<NodeInfo> responseObserver) {

        if (!kademlia.isInsideNetwork()) return;

        NodeInfo.Builder response = NodeInfo.newBuilder();

        response.setId(request.getId());
        response.setIp(request.getIp());
        response.setPort(request.getPort());

        responseObserver.onNext(response.build()); // Build response and send
        responseObserver.onCompleted(); // End connection
    }

    @Override
    public void findNode(FindNodeRequest request, StreamObserver<FindNodeResponse> responseObserver) {
        System.out.println("Received findNode request");
        if (!kademlia.isInsideNetwork()) return;

        // TODO
    }

    @Override
    public void findValue(FindValueRequest request, StreamObserver<FindValueResponse> responseObserver) {

        if (!kademlia.isInsideNetwork()) return;

        // TODO

    }

}
