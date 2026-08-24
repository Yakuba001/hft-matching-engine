package com.hft.matching.domain.model;

import lombok.Getter;

@Getter
public class OrderCommand {

    public static final byte ADD = 1;
    public static final byte CANCEL = 2;

    public static final byte SIDE_BUY = 1;
    public static final byte SIDE_SELL = 2;

    private long orderId;
    private byte type;
    private byte side;
    private long price;
    private long quantity;

    public void reset(long orderId, byte type, byte side, long price, long quantity) {
        this.orderId = orderId;
        this.type = type;
        this.side = side;
        this.price = price;
        this.quantity = quantity;
    }
}
