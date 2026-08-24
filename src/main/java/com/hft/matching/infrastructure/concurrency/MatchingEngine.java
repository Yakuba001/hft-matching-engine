package com.hft.matching.infrastructure.concurrency;

import com.hft.matching.domain.model.OrderBook;
import com.hft.matching.domain.model.OrderCommand;

public class MatchingEngine implements Runnable {

    private final CommandRingBuffer commandRingBuffer;
    private final OrderBook orderBook;

    public MatchingEngine(CommandRingBuffer commandRingBuffer, OrderBook orderBook) {
        this.commandRingBuffer = commandRingBuffer;
        this.orderBook = orderBook;
    }

    @Override
    public void run() {
        while (true) {
            OrderCommand command = commandRingBuffer.poll();
            if (command == null) {
                break;
            }
            byte type = command.getType();
            if (type == OrderCommand.ADD) {
                orderBook.addOrder(
                        command.getOrderId(),
                        command.getPrice(),
                        command.getQuantity(),
                        command.getSide(),
                        type);
            } else if (type == OrderCommand.CANCEL) {
                orderBook.cancelOrder(command.getOrderId());
            }
        }
    }
}
