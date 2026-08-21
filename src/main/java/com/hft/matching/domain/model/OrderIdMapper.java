package com.hft.matching.domain.model;

public class OrderIdMapper {

    private final long[] keys;
    private final int[] values;
    private final int mask;

    public OrderIdMapper(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be positive: " + capacity);
        int adjustedCapacity = capacity == 1 ? 1 : Integer.highestOneBit(capacity - 1) << 1;
        this.keys = new long[adjustedCapacity];
        this.values = new int[adjustedCapacity];
        this.mask = adjustedCapacity - 1;
        for (int i = 0; i < adjustedCapacity; i++) {
            keys[i] = -1;
            values[i] = -1;
        }
    }

    public void put(long orderId, int orderIndex) {
        int index = Long.hashCode(orderId) & mask;
        while (keys[index] != -1 && keys[index] != orderId) index = (index + 1) & mask;
        keys[index] = orderId;
        values[index] = orderIndex;
    }

    public int get(long orderId) {
        int index = Long.hashCode(orderId) & mask;
        while (keys[index] != -1) {
            if (keys[index] == orderId) return values[index];
            index = (index + 1) & mask;
        }
        return -1;
    }

    public boolean remove(long orderId) {
        int index = Long.hashCode(orderId) & mask;
        while (keys[index] != -1 && keys[index] != orderId) {
            index = (index + 1) & mask;
        }
        if (keys[index] == orderId) {
            keys[index] = -1;
            values[index] = -1;
            int j = index;
            while (true) {
                j = (j + 1) & mask;
                if (keys[j] == -1) break;
                int k = Long.hashCode(keys[j]) & mask;
                boolean isBetween = (index <= j)
                        ? (k <= index || k > j)
                        : (k <= index && k > j);
                if (isBetween) {
                    keys[index] = keys[j];
                    values[index] = values[j];
                    index = j;
                }
            }
            keys[index] = -1;
            values[index] = -1;
            return true;
        }
        return false;
    }
}
