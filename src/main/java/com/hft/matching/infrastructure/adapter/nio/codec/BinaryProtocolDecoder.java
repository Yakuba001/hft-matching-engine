package com.hft.matching.infrastructure.adapter.nio.codec;

import com.hft.matching.infrastructure.concurrency.CommandRingBuffer;

import java.nio.ByteBuffer;

public class BinaryProtocolDecoder {

    private static final int FRAME_SIZE = 26;
    private final CommandRingBuffer commandRingBuffer;

    public BinaryProtocolDecoder(CommandRingBuffer commandRingBuffer) {
        this.commandRingBuffer = commandRingBuffer;
    }

    public boolean decode(ByteBuffer buffer) {
        if (buffer.remaining() < FRAME_SIZE) return false;
        long orderId = buffer.getLong();
        byte type = buffer.get();
        byte side = buffer.get();
        long price = buffer.getLong();
        long quantity = buffer.getLong();
        commandRingBuffer.push(orderId, type, side, price, quantity);
        return true;
    }
}
