package kademlia;

import auction.AuctionManager;
import blockchain.Blockchain;
import io.grpc.stub.StreamObserver;
import peer.Wallet;

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
        String myPort = request.getPort();
        long timestamp = request.getTimestamp();

        String hash = ip + myPort + timestamp;

        // id =

        // TODO
        JoinResponse.Builder response = JoinResponse.newBuilder();
        // response.setId(id);
        response.setId("010000011101011");

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

        if (!kademlia.isInsideNetwork()) return;

        // TODO
    }

    @Override
    public void findValue(FindValueRequest request, StreamObserver<FindValueResponse> responseObserver) {

        if (!kademlia.isInsideNetwork()) return;

        // TODO

    }

}
