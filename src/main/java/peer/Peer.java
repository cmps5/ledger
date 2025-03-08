package peer;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import kademlia.Kademlia;
import kademlia.KademliaService;

import java.io.IOException;

import static java.lang.Thread.sleep;

public class Peer {
    String IP;
    int port;

    public Peer(String IP, String port) {
        this.IP = IP;
        this.port = Integer.parseInt(port);
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public static void main(String[] args) {

        Peer peer = new Peer(args[0], args[1]);

        Wallet wallet = Wallet.getInstance();
        wallet.setIP(peer.IP);
        wallet.setPort(Integer.toString(peer.port));

        Kademlia kademlia = Kademlia.getInstance();
        kademlia.setIP(peer.IP);
        kademlia.setPort(Integer.toString(peer.port));

        new Thread(() -> {
            Server server = ServerBuilder.forPort(peer.port)
                    .addService(new KademliaService(Kademlia.getInstance()))
                    .build();

            try {
                server.start();
                server.awaitTermination();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                server.shutdown();
            }
        }).start();


        try {
            sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        new Thread(new Client()).start();

    }
}
