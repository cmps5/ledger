package peer;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import kademlia.Kademlia;
import kademlia.KademliaService;

import java.io.IOException;

import static java.lang.Thread.sleep;

public class BootStrap {

    String IP;
    int port;

    public BootStrap() {
        this.IP = "localhost";
        this.port = 8000;
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public static void main(String[] args) {

        BootStrap peer = new BootStrap();

        Wallet wallet = Wallet.getInstance();
        wallet.setIP(peer.IP);
        wallet.setPort(Integer.toString(peer.port));


        Kademlia kademlia = Kademlia.getInstance();
        kademlia.setIP(peer.IP);
        kademlia.setPort(Integer.toString(peer.port));

        new Thread(() -> {
            Server server;
            server = ServerBuilder.forPort(peer.port)
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
