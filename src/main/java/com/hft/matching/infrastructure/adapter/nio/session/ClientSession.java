package com.hft.matching.infrastructure.adapter.nio.session;

import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

@Getter
public class ClientSession {

    private SocketChannel channel;
    private final ByteBuffer readBuffer;
    private final ByteBuffer writeBuffer;
    @Setter
    private boolean isActive;
    private final int index;

    public ClientSession(int index, int bufferSize) {
        this.index = index;
        this.readBuffer = ByteBuffer.allocate(bufferSize);
        this.writeBuffer = ByteBuffer.allocate(bufferSize);
    }

    public void attach(SocketChannel channel) {
        this.channel = channel;
        this.readBuffer.clear();
        this.writeBuffer.clear();
    }

    public void detach() {
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException ignore) {}
            this.channel = null;
        }
        this.readBuffer.clear();
        this.writeBuffer.clear();
    }
}
