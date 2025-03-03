package kademlia;

import java.math.BigInteger;
import java.util.Comparator;

public class Node {

    private String ip;
    private String port;
    private String id; /* Binary mode */

    public Node(String ip, String port, String id) {
        this.ip = ip;
        this.port = port;
        this.id = id;
    }

    public String getPort() {
        return port;
    }

    public String getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public boolean compare(Node otherNode) {
        return this.getId().equals(otherNode.getId()) &&
                this.getIp().equals(otherNode.getIp()) &&
                this.getPort().equals(otherNode.getPort());
    }

}

class NodeWrap {

    private Node node;
    private BigInteger distance;

    public NodeWrap(Node node, String targetID) {
        this.node = node;
        this.distance = Kademlia.getInstance().xorDistance(targetID, node.getId());
    }

    public Node getNode() {
        return node;
    }

    public BigInteger getDistance() {
        return distance;
    }

    public boolean compare(NodeWrap otherNode) {
        return this.node.getId().equals(otherNode.node.getId()) &&
                this.node.getIp().equals(otherNode.node.getIp()) &&
                this.node.getPort().equals(otherNode.node.getPort()) &&
                this.getDistance().equals(otherNode.getDistance());
    }

}

class NodeWrapComparator implements Comparator<NodeWrap> {
    @Override
    public int compare(NodeWrap node1, NodeWrap node2) {
        return node1.getDistance().compareTo(node2.getDistance());
    }
}