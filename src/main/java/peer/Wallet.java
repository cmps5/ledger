package peer;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

public class Wallet {
    private static Wallet instance;
    private KeyPair keyPair;
    private String ID;
    private String IP;
    private String Port;

    private Wallet() {
        try {
            keyPair = generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Algorithm not supported: " + e.getMessage());
        }
    }

    public static Wallet getInstance() {
        if (instance == null) {
            instance = new Wallet();
        }
        return instance;
    }


    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048);
        return keyPairGen.generateKeyPair();
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public PublicKey getPubKey() {
        return keyPair.getPublic();
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

    public String getPort() {
        return Port;
    }

    public void setPort(String port) {
        Port = port;
    }
}
