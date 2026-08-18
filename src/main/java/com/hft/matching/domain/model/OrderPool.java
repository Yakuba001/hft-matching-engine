package com.hft.matching.domain.model;

public final class OrderPool {

    private final Order[] pool;
    private final int[] freeList;
    private int freeCount;

    public OrderPool(int capacity) {
        this.pool = new Order[capacity];
        this.freeList = new int[capacity];
        this.freeCount = capacity;

        for (int i = 0; i < capacity; i++) {
            this.pool[i] = new Order();
            freeList[i] = i;
        }
    }

    public int acquire() {
        if (freeCount == 0) {
            return -1;
        }
        return freeList[--freeCount];
    }

    public void release(int index) {
        freeList[freeCount++] = index;
    }

    public Order get(int index) {
        return pool[index];
    }

    public int getCapacity() {
        return pool.length;
    }
}
