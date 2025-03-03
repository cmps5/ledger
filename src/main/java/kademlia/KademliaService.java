package kademlia;

import auction.AuctionManager;
import blockchain.Blockchain;
import io.grpc.stub.StreamObserver;

public class KademliaService extends KademliaGrpc.KademliaImplBase {

    Kademlia kademlia;
    Blockchain blockChain;
    AuctionManager auctionApp;

    public KademliaService(Kademlia kademlia) {
        this.kademlia = kademlia;
        // @ TODO
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

}
