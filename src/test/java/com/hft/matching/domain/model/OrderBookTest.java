package com.hft.matching.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OrderBookTest {

    private OrderBook orderBook;

    @BeforeEach
    void setUp() {
        orderBook = new OrderBook(10, 5, 10);
    }

    @Test
    void shouldMaintainBestBidAndAskCorrectly() {
        orderBook.addOrder(1L, 98, 10, Order.SIDE_BUY, Order.TYPE_LIMIT);
        orderBook.addOrder(2L, 100, 10, Order.SIDE_BUY, Order.TYPE_LIMIT);
        orderBook.addOrder(3L, 105, 10, Order.SIDE_SELL, Order.TYPE_LIMIT);
        orderBook.addOrder(4L, 102, 10, Order.SIDE_SELL, Order.TYPE_LIMIT);

        assertEquals(100, orderBook.getBestBidPrice());
        assertEquals(102, orderBook.getBestAskPrice());
    }

    @Test
    void shouldRemoveLevelWhenAllOrdersCancelled() {
        orderBook.addOrder(1L, 100, 10, Order.SIDE_BUY, Order.TYPE_LIMIT);

        boolean isCancel = orderBook.cancelOrder(1L);

        assertFalse(orderBook.cancelOrder(1L));
        assertEquals(0, orderBook.getBestBidPrice());
        assertTrue(isCancel);
    }

    @Test
    void shouldMaintainFifoOrderWithinSamePriceLevel() {
        int firstIdx = orderBook.addOrder(1L, 100, 10, Order.SIDE_BUY, Order.TYPE_LIMIT);
        int secondIdx = orderBook.addOrder(2L, 100, 10, Order.SIDE_BUY, Order.TYPE_LIMIT);
        int thirdIdx = orderBook.addOrder(3L, 100, 10, Order.SIDE_BUY, Order.TYPE_LIMIT);

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
        for (long orderId = 0; orderId < 10; orderId++) {
            int idx = orderBook.addOrder(orderId, 100, 10, Order.SIDE_BUY, Order.TYPE_LIMIT);
            assertNotEquals(-1, idx, "Ордер должен успешно добавиться");
        }
        int overflowIdx = orderBook.addOrder(999L, 200, 10, Order.SIDE_BUY, Order.TYPE_LIMIT);
        assertEquals(-1, overflowIdx, "Пул переполнен, ожидаем -1");
        for (long orderId = 0; orderId < 10; orderId++) {
            boolean cancelled = orderBook.cancelOrder(orderId);
            assertTrue(cancelled, "Ордер " + orderId + " должен успешно отмениться");
        }
        for (long orderId = 100; orderId < 110; orderId++) {
            int recycledIdx = orderBook.addOrder(orderId, 300, 10, Order.SIDE_BUY, Order.TYPE_LIMIT);
            assertNotEquals(-1, recycledIdx, "Слот в пуле должен переиспользоваться");
        }
    }

    @Test
    void shouldHandlePoolExhaustionGracefully() {
        OrderBook exhaustedOrderBook = new OrderBook(2, 1, 10);

        int firstIdx = exhaustedOrderBook.addOrder(1, 100, 10, Order.SIDE_BUY, Order.TYPE_LIMIT);
        assertNotEquals(-1, firstIdx);

        int secondIdx = exhaustedOrderBook.addOrder(2, 101, 10, Order.SIDE_BUY, Order.TYPE_LIMIT);
        assertEquals(-1, secondIdx);

        int thirdIdx = exhaustedOrderBook.addOrder(3, 100, 15, Order.SIDE_BUY, Order.TYPE_LIMIT);
        assertNotEquals(-1, thirdIdx);
    }

    @Test
    void shouldMatchAggressiveLimitOrderPartiallyAndFully() {
        orderBook.addOrder(1L, 100, 50, Order.SIDE_SELL, Order.TYPE_LIMIT);
        int res2 = orderBook.addOrder(2L, 105, 20, Order.SIDE_BUY, Order.TYPE_LIMIT);

        boolean cancelSecond = orderBook.cancelOrder(2L);
        long bestAskPrice = orderBook.getBestAskPrice();
        boolean cancelFirst = orderBook.cancelOrder(1L);

        assertEquals(-2, res2);
        assertFalse(cancelSecond);
        assertTrue(cancelFirst);
        assertEquals(100, bestAskPrice);
    }

    @Test
    void shouldMatchMarketOrderAcrossMultipleLevels() {
        orderBook.addOrder(10L, 100, 10, Order.SIDE_SELL, Order.TYPE_LIMIT);
        orderBook.addOrder(20L, 101, 15, Order.SIDE_SELL, Order.TYPE_LIMIT);

        int matchedQty = orderBook.matchMarketOrder(Order.SIDE_BUY, 20);
        boolean cancelFirst = orderBook.cancelOrder(10L);
        long bestAskPrice = orderBook.getBestAskPrice();
        boolean cancelSecond = orderBook.cancelOrder(20L);

        assertEquals(20, matchedQty);
        assertFalse(cancelFirst);
        assertEquals(101, bestAskPrice);
        assertTrue(cancelSecond);
    }
}
