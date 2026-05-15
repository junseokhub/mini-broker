package dev.minibroker;

import dev.minibroker.network.BrokerServer;

import java.io.IOException;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws IOException {
        BrokerServer server = new BrokerServer(9092, Path.of("data"));

        System.out.println("mini-broker started on port 9092");
        server.start();
    }

}