package com.upic.consumer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ConnectionPoolFactory {
    private final BlockingQueue<Consumer> pool;

    public ConnectionPoolFactory(String host, int port, String username, String password, int poolSize) {
        pool = new LinkedBlockingQueue<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            pool.add(new Consumer());
        }
    }

    public Consumer getConnection() throws InterruptedException {
        return pool.take();
    }

    public void returnConnection(Consumer connection) {
        pool.add(connection);
    }
}
