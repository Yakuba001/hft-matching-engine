package com.hft.matching.domain.model;

public final class PriceLevelPool {

    private final PriceLevel[] pool;
    private final int[] freeList;
    private int freeCount;

    public PriceLevelPool(int capacity) {
        this.pool = new PriceLevel[capacity];
        this.freeList = new int[capacity];
        this.freeCount = capacity;

        for (int i = 0; i < capacity; i++) {
            this.pool[i] = new PriceLevel();
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

    public PriceLevel get(int index) {
        return pool[index];
    }
}
