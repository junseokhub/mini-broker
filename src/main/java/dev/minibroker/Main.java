package dev.minibroker;

import dev.minibroker.network.BrokerServer;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        BrokerServer server = new BrokerServer(9092);

        System.out.println("mini-broker started on port 9092");
        server.start();
    }

}