package com.example.consistenthash.model;

import java.util.Objects;

public class Node {
    private final String id;
    private final String host;
    private final int port;

    public Node(String id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
    }

    public Node(String id) {
        this(id, "localhost", 8080);
    }

    public String getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getAddress() {
        return host + ":" + port;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Node node = (Node) obj;
        return port == node.port &&
                Objects.equals(id, node.id) &&
                Objects.equals(host, node.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, host, port);
    }

    @Override
    public String toString() {
        return String.format("Node{id='%s', address='%s'}", id, getAddress());
    }
}