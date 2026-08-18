package com.hft.matching.domain.service;

import com.hft.matching.domain.model.OrderPool;
import com.hft.matching.domain.model.PriceLevelPool;

public class OrderBook {

    private final OrderPool orderPool;
    private final PriceLevelPool priceLevelPool;
    private int headBidLevel = -1;
    private int headAskLevel = -1;

    public OrderBook(int orderPoolSize, int priceLevelPoolSize) {
        this.orderPool = new OrderPool(orderPoolSize);
        this.priceLevelPool = new PriceLevelPool(priceLevelPoolSize);
    }

    public int addOrder(long orderId, long price, long quantity, byte side, byte type) {
        return 0;
    }

    public boolean cancelOrder(int orderIndex) {
        return false;
    }

    public long getBestBidPrice() {
        return 0;
    }

    public long getBestAskPrice() {
        return 0;
    }
}
