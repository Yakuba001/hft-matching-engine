package com.hft.matching.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
public final class PriceLevel {

    private long price;
    @Setter
    private int headIndex = -1;
    @Setter
    private int tailIndex = -1;
    @Setter
    private long totalQuantity;
    @Setter
    private int nextLevelIndex = -1;
    @Setter
    private int prevLevelIndex = -1;

    public void reset(long price) {
        this.price = price;
        this.totalQuantity = 0;
        this.headIndex = -1;
        this.tailIndex = -1;
        this.nextLevelIndex = -1;
        this.prevLevelIndex = -1;
    }

    public void addOrder(int orderIndex, OrderPool pool) {
        Order order = pool.get(orderIndex);
        if (headIndex == -1) {
            headIndex = orderIndex;
        } else {
            pool.get(tailIndex).setNextIndex(orderIndex);
            order.setPrevIndex(tailIndex);
        }
        tailIndex = orderIndex;
        totalQuantity += order.getQuantity();
    }

    public void removeOrder(int orderIndex, OrderPool pool) {
        Order order = pool.get(orderIndex);
        if (orderIndex == headIndex && orderIndex == tailIndex) {
            headIndex = -1;
            tailIndex = -1;
        } else if (orderIndex == headIndex) {
            headIndex = order.getNextIndex();
            pool.get(headIndex).setPrevIndex(-1);
        } else if (orderIndex == tailIndex) {
            tailIndex = order.getPrevIndex();
            pool.get(tailIndex).setNextIndex(-1);
        } else {
            pool.get(order.getPrevIndex()).setNextIndex(order.getNextIndex());
            pool.get(order.getNextIndex()).setPrevIndex(order.getPrevIndex());
        }
        totalQuantity -= order.getQuantity();
    }
}
