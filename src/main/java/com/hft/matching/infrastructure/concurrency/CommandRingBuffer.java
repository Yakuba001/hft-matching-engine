package com.hft.matching.infrastructure.concurrency;

import com.hft.matching.domain.model.OrderCommand;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class CommandRingBuffer {

    private final OrderCommand[] buffer;
    private long p1, p2, p3, p4, p5, p6, p7;
    private int head;
    private long p8, p9, p10, p11, p12, p13, p14;
    private int tail;
    private static VarHandle HEAD_HANDLE;
    private static VarHandle TAIL_HANDLE;
    private volatile boolean running = true;

    static {
        try {
            HEAD_HANDLE = MethodHandles.lookup().findVarHandle(CommandRingBuffer.class, "head", int.class);
            TAIL_HANDLE = MethodHandles.lookup().findVarHandle(CommandRingBuffer.class, "tail", int.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public CommandRingBuffer(int capacity) {
        if ((capacity & (capacity - 1)) != 0) {
            throw new IllegalArgumentException("Capacity must be a power of 2");
        }
        int adjustedCapacity = capacity == 1 ? 1 : Integer.highestOneBit(capacity - 1) << 1;
        this.buffer = new OrderCommand[adjustedCapacity];
        for (int i = 0; i < adjustedCapacity; i++) {
            buffer[i] = new OrderCommand();
        }
        this.head = 0;
        this.tail = 0;
    }

    public OrderCommand next() {
        int currentTail = 0;
        int currentHead = 0;
        while (running) {
            currentTail = (int) TAIL_HANDLE.getVolatile(this);
            currentHead = (int) HEAD_HANDLE.getAcquire(this);
            if (currentTail - currentHead >= buffer.length) {
                Thread.onSpinWait();
                continue;
            }
            break;
        }
        if (!running && (int) TAIL_HANDLE.getVolatile(this) == (int) HEAD_HANDLE.getAcquire(this)) {
            return null;
        }
        int index = currentTail & (buffer.length - 1);
        OrderCommand cmd = buffer[index];
        TAIL_HANDLE.setRelease(this, currentTail + 1);
        return cmd;
    }

    public OrderCommand poll() {
        int currentTail = 0;
        int currentHead = 0;
        while (running) {
            currentTail = (int) TAIL_HANDLE.getVolatile(this);
            currentHead = (int) HEAD_HANDLE.getAcquire(this);
            if (currentTail == currentHead) {
                Thread.onSpinWait();
                continue;
            }
            break;
        }
        if (!running && (int) TAIL_HANDLE.getVolatile(this) == (int) HEAD_HANDLE.getAcquire(this)) {
            return null;
        }
        int index = currentHead & (buffer.length - 1);
        OrderCommand cmd = buffer[index];
        HEAD_HANDLE.setRelease(this, currentHead + 1);
        return cmd;
    }

    public void shutdown() {
        running = false;
    }
}
