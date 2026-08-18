package com.hft.matching.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
public final class Order {

    public static final byte SIDE_BUY = 1;
    public static final byte SIDE_SELL = 2;

    public static final byte TYPE_LIMIT = 1;
    public static final byte TYPE_MARKET = 2;

    private long orderId;
    private long price;
    @Setter
    private long quantity;
    private long initialQty;
    private byte side;
    private byte type;

    @Setter
    private int nextIndex = -1;
    @Setter
    private int prevIndex = -1;

    @Setter
    private int priceLevelIndex = -1;

    public void reset(long orderId, long price, long quantity, byte side, byte type) {
        this.orderId = orderId;
        this.price = price;
        this.quantity = quantity;
        this.initialQty = quantity;
        this.side = side;
        this.type = type;
        this.nextIndex = -1;
        this.prevIndex = -1;
        this.priceLevelIndex = -1;
    }
}
