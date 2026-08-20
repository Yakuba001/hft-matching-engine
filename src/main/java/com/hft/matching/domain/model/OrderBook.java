package com.hft.matching.domain.model;

public class OrderBook {

    private final OrderPool orderPool;
    private final PriceLevelPool priceLevelPool;
    private int headBidLevel = -1; // Highest bid (buy)
    private int headAskLevel = -1; // Lowest ask (sell)

    public OrderBook(int orderPoolSize, int priceLevelPoolSize) {
        this.orderPool = new OrderPool(orderPoolSize);
        this.priceLevelPool = new PriceLevelPool(priceLevelPoolSize);
    }

    public int addOrder(long orderId, long price, long quantity, byte side, byte type) {
        int qty = (int) quantity;
        boolean isBuy = side == Order.SIDE_BUY;
        int currLevelIdx = isBuy ? headAskLevel : headBidLevel;
        byte targetSide = isBuy ? Order.SIDE_SELL : Order.SIDE_BUY;
        while (currLevelIdx != -1 && qty > 0) {
            PriceLevel currLevel = priceLevelPool.get(currLevelIdx);
            boolean canMatch = isBuy ? currLevel.getPrice() <= price : currLevel.getPrice() >= price;
            if (!canMatch) {
                break;
            }
            int nextLevelIdx = currLevel.getNextLevelIndex();
            int orderIdx = currLevel.getHeadIndex();
            while (orderIdx != -1 && qty > 0) {
                Order order = orderPool.get(orderIdx);
                int orderNextIdx = order.getNextIndex();
                long orderQty = order.getQuantity();
                int matchQty = Math.min(qty, (int) orderQty);
                qty -= matchQty;
                order.setQuantity(orderQty - matchQty);
                currLevel.setTotalQuantity(currLevel.getTotalQuantity() - matchQty);
                if (order.getQuantity() == 0) {
                    currLevel.removeOrder(orderIdx, orderPool);
                    order.setPriceLevelIndex(-1);
                    orderPool.release(orderIdx);
                }
                orderIdx = orderNextIdx;
            }
            if (currLevel.getHeadIndex() == -1) {
                removeLevel(currLevelIdx, targetSide);
            }
            currLevelIdx = nextLevelIdx;
        }
        if (qty > 0) {
            int orderIndex = orderPool.acquire();
            if (orderIndex == -1) {
                return -1;
            }
            Order order = orderPool.get(orderIndex);
            order.reset(orderId, price, qty, side, type);
            int levelIndex = findOrInsertLevel(price, side);
            if (levelIndex == -1) {
                orderPool.release(orderIndex);
                return -1;
            }
            order.setPriceLevelIndex(levelIndex);
            PriceLevel level = priceLevelPool.get(levelIndex);
            level.addOrder(orderIndex, orderPool);
            return orderIndex;
        }
        return -2;
    }

    public boolean cancelOrder(int orderIndex) {
        if (orderIndex < 0 || orderIndex >= orderPool.getCapacity()) return false;
        Order order = orderPool.get(orderIndex);
        int levelIndex = order.getPriceLevelIndex();
        if (levelIndex == -1) return false;
        PriceLevel level = priceLevelPool.get(levelIndex);
        byte side = order.getSide();
        level.removeOrder(orderIndex, orderPool);
        if (level.getHeadIndex() == -1) removeLevel(levelIndex, side);
        order.setPriceLevelIndex(-1);
        orderPool.release(orderIndex);
        return true;
    }

    public int matchMarketOrder(byte side, int qty) {
        boolean isBid = side == Order.SIDE_BUY;
        int currLevelIdx = isBid ? headAskLevel : headBidLevel;
        byte targetSide = isBid ? Order.SIDE_SELL : Order.SIDE_BUY;
        int nextLevelIdx;
        int initialQty = qty;
        while (currLevelIdx != -1 && qty > 0) {
            PriceLevel currLevel = priceLevelPool.get(currLevelIdx);
            nextLevelIdx = currLevel.getNextLevelIndex();
            int orderIdx = currLevel.getHeadIndex();
            while (orderIdx != -1 && qty > 0) {
                Order order = orderPool.get(orderIdx);
                int orderNextIdx = order.getNextIndex();
                long orderQty = order.getQuantity();
                int matchQty = Math.min(qty, (int) orderQty);
                qty -= matchQty;
                order.setQuantity(orderQty - matchQty);
                currLevel.setTotalQuantity(currLevel.getTotalQuantity() - matchQty);
                if (order.getQuantity() == 0) {
                    currLevel.removeOrder(orderIdx, orderPool);
                    order.setPriceLevelIndex(-1);
                    orderPool.release(orderIdx);
                }
                orderIdx = orderNextIdx;
            }
            if (currLevel.getHeadIndex() == -1) {
                removeLevel(currLevelIdx, targetSide);
            }
            currLevelIdx = nextLevelIdx;
        }
        return initialQty - qty;
    }

    public long getBestBidPrice() {
        if (headBidLevel == -1) {
            return 0;
        }
        return priceLevelPool.get(headBidLevel).getPrice();
    }

    public long getBestAskPrice() {
        if (headAskLevel == -1) {
            return 0;
        }
        return priceLevelPool.get(headAskLevel).getPrice();
    }

    public Order getOrder(int index) {
        return orderPool.get(index);
    }

    private void removeLevel(int levelIndex, byte side) {
        boolean isBid = side == Order.SIDE_BUY;
        PriceLevel targetLevel = priceLevelPool.get(levelIndex);
        int prevIdx = targetLevel.getPrevLevelIndex();
        int nextIdx = targetLevel.getNextLevelIndex();
        boolean isHead = isBid ? levelIndex == headBidLevel : levelIndex == headAskLevel;
        if (isHead && nextIdx == -1) {
            if (isBid) headBidLevel = -1;
            else headAskLevel = -1;
        } else if (isHead) {
            if (isBid) headBidLevel = nextIdx;
            else headAskLevel = nextIdx;
            priceLevelPool.get(nextIdx).setPrevLevelIndex(-1);
        } else if (nextIdx == -1) {
            priceLevelPool.get(prevIdx).setNextLevelIndex(-1);
        } else {
            priceLevelPool.get(prevIdx).setNextLevelIndex(nextIdx);
            priceLevelPool.get(nextIdx).setPrevLevelIndex(prevIdx);
        }
        priceLevelPool.release(levelIndex);
    }

    private int findOrInsertLevel(long price, byte side) {
        boolean isBid = side == Order.SIDE_BUY;
        int head = isBid ? headBidLevel : headAskLevel;
        PriceLevel newLevel, headLevel, prevLevel, currLevel;

        int newIdx;
        if (head == -1) {
            newIdx = priceLevelPool.acquire();
            if (newIdx == -1) return -1;
            newLevel = priceLevelPool.get(newIdx);
            newLevel.reset(price);
            if (isBid) headBidLevel = newIdx;
            else headAskLevel = newIdx;
            return newIdx;
        }
        long headPrice = priceLevelPool.get(head).getPrice();
        boolean isNewHead = isBid ? price > headPrice : price < headPrice;
        if (isNewHead) {
            newIdx = priceLevelPool.acquire();
            if (newIdx == -1) return -1;
            newLevel = priceLevelPool.get(newIdx);
            headLevel = priceLevelPool.get(head);
            newLevel.reset(price);
            newLevel.setNextLevelIndex(head);
            headLevel.setPrevLevelIndex(newIdx);
            if (isBid) headBidLevel = newIdx;
            else headAskLevel = newIdx;
            return newIdx;
        }
        int curr = head;
        int prev = -1;
        while (curr != -1) {
            currLevel = priceLevelPool.get(curr);
            long currPrice = currLevel.getPrice();
            if (currPrice == price) {
                return curr;
            }
            boolean insertBefore = isBid ? price > currPrice : price < currPrice;
            if (insertBefore) {
                newIdx = priceLevelPool.acquire();
                if (newIdx == -1) return -1;
                newLevel = priceLevelPool.get(newIdx);
                prevLevel = priceLevelPool.get(prev);
                currLevel = priceLevelPool.get(curr);
                newLevel.reset(price);
                newLevel.setNextLevelIndex(curr);
                newLevel.setPrevLevelIndex(prev);
                prevLevel.setNextLevelIndex(newIdx);
                currLevel.setPrevLevelIndex(newIdx);
                return newIdx;
            }
            prev = curr;
            curr = currLevel.getNextLevelIndex();
        }
        newIdx = priceLevelPool.acquire();
        if (newIdx == -1) return -1;
        newLevel = priceLevelPool.get(newIdx);
        newLevel.reset(price);
        prevLevel = priceLevelPool.get(prev);
        prevLevel.setNextLevelIndex(newIdx);
        newLevel.setPrevLevelIndex(prev);
        return newIdx;
    }
}
