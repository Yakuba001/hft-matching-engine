package com.hft.matching.infrastructure.concurrency;

import com.hft.matching.domain.model.OrderBook;
import com.hft.matching.domain.model.OrderCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MatchingEngineTest {

    CommandRingBuffer buffer;
    OrderBook orderBook;
    MatchingEngine matchingEngine;

    @BeforeEach
    void setUp() {
        orderBook = new OrderBook(4, 4, 4);
        buffer = new CommandRingBuffer(4);
        matchingEngine = new MatchingEngine(buffer, orderBook);
    }

    @Test
    void consumerTest() {
        Thread consumer = new Thread(matchingEngine);
        consumer.start();
        OrderCommand cmd = buffer.next();
        OrderCommand cmd2 = buffer.next();
        OrderCommand cmd3 = buffer.next();
        cmd.reset(1L, OrderCommand.ADD, OrderCommand.SIDE_BUY, 100L, 2L);
        cmd2.reset(2L, OrderCommand.ADD, OrderCommand.SIDE_BUY, 100L, 2L);
        cmd3.reset(3L, OrderCommand.ADD, OrderCommand.SIDE_SELL, 100L, 2L);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            buffer.shutdown();
        }

        assertThat(orderBook.getBestBidPrice()).isEqualTo(100L);
        assertThat(orderBook.getBestAskPrice()).isEqualTo(0L);
    }
}
