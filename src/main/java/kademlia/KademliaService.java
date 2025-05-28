package kademlia;

import auction.Auction;
import auction.AuctionManager;
import blockchain.Block;
import blockchain.Blockchain;
import blockchain.Transaction;
import io.grpc.stub.StreamObserver;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import peer.Wallet;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.LinkedList;

public class KademliaService extends KademliaGrpc.KademliaImplBase {

    Kademlia kademlia;
    Blockchain blockchain;
    AuctionManager auctionManager;
    Wallet wallet;

    public KademliaService(Kademlia kademlia) {
        this.kademlia = kademlia;
        this.wallet = Wallet.getInstance();
        this.blockchain = Blockchain.getInstance();
        this.auctionManager = AuctionManager.getInstance();
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

        if (blockchain.getConflictChains().isEmpty()) {
            if (!blockchain.getBlockchain().isEmpty()) {
                int size = blockchain.getBlockchain().size();
                Block headhBlock = blockchain.getBlockchain().get(size - 1);
                response.setHeadBlock(Kademlia.blockToProto(headhBlock));
                response.setHasBlock(1);
            } else {
                response.setHasBlock(0);
            }
        } else {
            int size = blockchain.getConflictChains().get(0).size();
            Block headhBlock = blockchain.getConflictChains().get(0).get(size - 1);
            response.setHeadBlock(Kademlia.blockToProto(headhBlock));
            response.setHasBlock(1);
        }


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
    public void store(StoreRequest request, StreamObserver<StoreResponse> responseObserver) {

        if (!kademlia.isInsideNetwork()) return;

        if (request.getBlockOrAuctionCase() == StoreRequest.BlockOrAuctionCase.BLOCK) {

            Block recvBlock = protoToBlock(request.getBlock());

            if (Transaction.verifySignature(recvBlock.getTransaction().getSignature(), recvBlock.getTransaction().getHash(), recvBlock.getTransaction().getBuyerPublicKey())) {

                if (!blockchain.searchBlock(recvBlock)) {
                    //System.out.println("iubeagobareiemwPAERINWGE");
                    Auction auction = null;
                    Transaction test = recvBlock.getTransaction();
                    if (test.isActive()) {
                        if (auctionManager.getAllAuctions().containsKey(test.getItemName())) {
                            auction = auctionManager.getAuctions().get(test.getItemName());
                            auction.registerBid(recvBlock.getTransaction().getPrice(), recvBlock.getTransaction().getBuyerID());
                        } else if (auctionManager.getAllAuctions().containsKey(test.getItemName())) {
                            auction = auctionManager.getAllAuctions().get(test.getItemName());
                            auction.registerBid(recvBlock.getTransaction().getPrice(), recvBlock.getTransaction().getBuyerID());
                        }
                    }

                    blockchain.storeBlock(recvBlock);
                    kademlia.disseminateStore(recvBlock, request.getKey());

                } else {
                    System.out.println("Block not found");
                }

            }
        } else if (request.getBlockOrAuctionCase() == StoreRequest.BlockOrAuctionCase.AUCTION) {

            SignatureProto signatureProto = request.getAuction().getSignature();

            String hash = signatureProto.getHash();
            PublicKey publicKey;
            try {
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(signatureProto.getPublicKey().toByteArray());
                KeyFactory keyFactory = KeyFactory.getInstance("RSA", new BouncyCastleProvider());
                publicKey = keyFactory.generatePublic(keySpec);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                throw new RuntimeException(e);
            }

            if (Transaction.verifySignature(signatureProto.getSignature().toByteArray(), hash, publicKey)) {

                if (!auctionManager.getAllAuctions().containsKey(request.getAuction().getTopic()) && !request.getAuction().getSellerID().equals(wallet.getID())) {

                    Auction recvAuction = protoToAuction(request.getAuction());
                    auctionManager.registerAuction(recvAuction);

                    kademlia.disseminateStore(recvAuction, request.getKey());
                }

            }
        }

        NodeInfo nodeInfo = NodeInfo.newBuilder().setId(kademlia.getID())
                .setIp(kademlia.getIP())
                .setPort(kademlia.getPort())
                .build();

        StoreResponse response = StoreResponse.newBuilder()
                .setSender(nodeInfo)
                .setSuccess(true)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private Block protoToBlock(BlockProto blockProto) {
        Block block = new Block(blockProto.getPrevioushash());
        block.setTransaction(new Transaction(blockProto.getTransaction()));
        block.setHash(blockProto.getHash());
        block.setNonce((int) blockProto.getNonce());
        block.setTimeStamp(blockProto.getTimestamp());
        return block;
    }

    private Auction protoToAuction(AuctionProto auctionProto) {

        PublicKey key;
        try {
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(auctionProto.getSellerPubKey().toByteArray());
            KeyFactory keyFactory = KeyFactory.getInstance("RSA", new BouncyCastleProvider());
            key = keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }

        Auction auction = new Auction(auctionProto.getName(), auctionProto.getBasePrice());
        auction.setCurrentBid(auctionProto.getCurrentBid());
        auction.setCurrentBidder(auctionProto.getCurrentBidder());
        auction.setSellerID(auctionProto.getSellerID());
        auction.setSellerPubKey(key);

        return auction;
    }

    @Override
    public void findNode(FindNodeRequest request, StreamObserver<FindNodeResponse> responseObserver) {
        //System.out.println("Received findNode request");
        if (!kademlia.isInsideNetwork()) return;

        String targetId = request.getTargetId();
        Node senderRequest = new Node(request.getSender().getIp(), request.getSender().getPort(), request.getSender().getId());

        LinkedList<Node> closestNodes = kademlia.getClosestNodes(targetId, senderRequest);

        for (Node closestNode : closestNodes) {
            NodeInfo.Builder nodeInfo = NodeInfo.newBuilder();
            nodeInfo.setPort(closestNode.getPort());
            nodeInfo.setId(closestNode.getId());
            nodeInfo.setIp(closestNode.getIp());

            FindNodeResponse response = FindNodeResponse.newBuilder()
                    .setNode(nodeInfo)
                    .build();

            responseObserver.onNext(response);
        }

        responseObserver.onCompleted();

        kademlia.insertNode(request.getSender().getIp(), request.getSender().getId(), request.getSender().getPort());
    }

    @Override
    public void findValue(FindValueRequest request, StreamObserver<FindValueResponse> responseObserver) {

        if (!kademlia.isInsideNetwork()) return;

        // TODO

    }

}
