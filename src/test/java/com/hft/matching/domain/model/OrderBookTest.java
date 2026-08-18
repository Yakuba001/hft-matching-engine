package com.hft.matching.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OrderBookTest {

    private OrderBook orderBook;

    @BeforeEach
    void setUp() {
        orderBook = new OrderBook(10, 5);
    }

    @Test
    void shouldMaintainBestBidAndAskCorrectly() {
        orderBook.addOrder(1, 98, 10, Order.SIDE_BUY, Order.TYPE_LIMIT);
        orderBook.addOrder(2, 100, 10, Order.SIDE_BUY, Order.TYPE_LIMIT);
        orderBook.addOrder(3, 105, 10, Order.SIDE_SELL, Order.TYPE_LIMIT);
        orderBook.addOrder(4, 102, 10, Order.SIDE_SELL, Order.TYPE_LIMIT);

        assertEquals(100, orderBook.getBestBidPrice());
        assertEquals(102, orderBook.getBestAskPrice());
    }

    @Test
    void shouldRemoveLevelWhenAllOrdersCancelled() {
        orderBook.addOrder(1, 100, 10, Order.SIDE_BUY, Order.TYPE_LIMIT);
        int secondBid = orderBook.addOrder(2, 105, 10, Order.SIDE_BUY, Order.TYPE_LIMIT);

        boolean isCancel = orderBook.cancelOrder(secondBid);

        assertEquals(100, orderBook.getBestBidPrice());
        assertTrue(isCancel);
        assertFalse(orderBook.cancelOrder(secondBid));
    }

    @Test
    void shouldMaintainFifoOrderWithinSamePriceLevel() {
        int firstIdx = orderBook.addOrder(1, 100, 10, Order.SIDE_BUY, Order.TYPE_LIMIT);
        int secondIdx = orderBook.addOrder(2, 100, 10, Order.SIDE_BUY, Order.TYPE_LIMIT);
        int thirdIdx = orderBook.addOrder(3, 100, 10, Order.SIDE_BUY, Order.TYPE_LIMIT);

        Order firstOrder = orderBook.getOrder(firstIdx);
        Order secondOrder = orderBook.getOrder(secondIdx);
        Order thirdOrder = orderBook.getOrder(thirdIdx);

        assertEquals(-1, firstOrder.getPrevIndex());
        assertEquals(secondIdx, firstOrder.getNextIndex());
        assertEquals(firstIdx, secondOrder.getPrevIndex());
        assertEquals(thirdIdx, secondOrder.getNextIndex());
        assertEquals(secondIdx, thirdOrder.getPrevIndex());
        assertEquals(-1, thirdOrder.getNextIndex());
    }

    @Test
    void shouldRecyclePoolsWithoutAllocations() {
        int[] orderIndices = new int[10];
        for (int i = 0; i < 10; i++) {
            int idx = orderBook.addOrder(i, 100, 10, Order.SIDE_BUY, Order.TYPE_LIMIT);
            assertNotEquals(-1, idx);
            orderIndices[i] = idx;
        }
        int overflowIdx = orderBook.addOrder(99, 200, 10, Order.SIDE_BUY, Order.TYPE_LIMIT);
        assertEquals(-1, overflowIdx);

        for (int i = 0; i < 10; i++) {
            boolean cancelled = orderBook.cancelOrder(orderIndices[i]);
            assertTrue(cancelled);
        }

        for (int i = 0; i < 10; i++) {
            int recycledIdx = orderBook.addOrder(i + 100, 300, 10, Order.SIDE_BUY, Order.TYPE_LIMIT);
            assertNotEquals(-1, recycledIdx);
        }
    }

    @Test
    void shouldHandlePoolExhaustionGracefully() {
        OrderBook exhaustedOrderBook = new OrderBook(2, 1);

        int firstIdx = exhaustedOrderBook.addOrder(1, 100, 10, Order.SIDE_BUY, Order.TYPE_LIMIT);
        assertNotEquals(-1, firstIdx);

        int secondIdx = exhaustedOrderBook.addOrder(2, 101, 10, Order.SIDE_BUY, Order.TYPE_LIMIT);
        assertEquals(-1, secondIdx);

        int thirdIdx = exhaustedOrderBook.addOrder(3, 100, 15, Order.SIDE_BUY, Order.TYPE_LIMIT);
        assertNotEquals(-1, thirdIdx);
    }
}
