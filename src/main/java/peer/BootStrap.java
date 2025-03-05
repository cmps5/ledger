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
    String portString;


    public BootStrap() {
        this.IP = "localhost";
        this.port = 9090;
        this.portString = "9090";
    }

    public static void main(String[] args) {

        BootStrap peer = new BootStrap();


        try {
            sleep(600);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


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
