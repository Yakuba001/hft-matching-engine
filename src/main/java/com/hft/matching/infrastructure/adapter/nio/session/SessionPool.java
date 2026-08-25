package com.hft.matching.infrastructure.adapter.nio.session;

public class SessionPool {

    private final ClientSession[] pool;
    private final int[] freeList;
    private int freeCount;

    public SessionPool(int capacity, int bufferSize) {
        this.pool = new ClientSession[capacity];
        this.freeList = new int[capacity];
        this.freeCount = capacity;
        for (int i = 0; i < capacity; i++) {
            this.pool[i] = new ClientSession(i, bufferSize);
            freeList[i] = i;
        }
    }

    public ClientSession acquire() {
        if (freeCount == 0) {
            return null;
        }
        int index = freeList[--freeCount];
        ClientSession session = pool[index];
        session.setActive(true);
        return session;
    }

    public void release(int index) {
        if (index < 0 || index >= pool.length) return;
        ClientSession session = pool[index];
        if (!session.isActive()) return;
        session.setActive(false);
        session.detach();
        freeList[freeCount++] = index;
    }
}
