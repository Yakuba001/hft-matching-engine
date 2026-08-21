package com.hft.matching.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OrderIdMapperTest {

    OrderIdMapper orderIdMapper;

    @BeforeEach
    void setUp() {
        orderIdMapper = new OrderIdMapper(4);
    }

    @Test
    void shouldGetByOrderIdCorrectlyAfterAdding() {
        orderIdMapper.put(1, 1);
        orderIdMapper.put(2, 2);

        int res1 = orderIdMapper.get(1);
        int res2 = orderIdMapper.get(2);

        assertEquals(1, res1);
        assertEquals(2, res2);
    }

    @Test
    void shouldReturnMinusOneWhenOrderIdNotFound() {
        int res = orderIdMapper.get(1);
        assertEquals(-1, res);
    }

    @Test
    void shouldReturnTrueIfDeletedExistKeyAndTheSecondTimeReturnFalse() {
        long orderId = 1;
        orderIdMapper.put(orderId, 1);

        boolean firstTry = orderIdMapper.remove(orderId);
        boolean secondTry = orderIdMapper.remove(orderId);

        assertTrue(firstTry);
        assertFalse(secondTry);
    }

    @Test
    void shouldRewriteExistingKey() {
        long orderId = 1;
        orderIdMapper.put(orderId, 1);
        orderIdMapper.put(orderId, 2);

        int res = orderIdMapper.get(orderId);
        assertEquals(2, res);
    }

    @Test
    void shouldKeepCollisionChainIntactAfterRemove() {
        int firstId = 1;
        int secondId = 5;
        orderIdMapper.put(firstId, 1);
        orderIdMapper.put(secondId, 2);

        orderIdMapper.remove(firstId);

        int res = orderIdMapper.get(secondId);
        assertEquals(2, res);
    }

    @Test
    void shouldHandleRingWrapCorrectly() {
        long firstId = 3L;
        long secondId = 7L;

        orderIdMapper.put(firstId, 10);
        orderIdMapper.put(secondId, 20);

        orderIdMapper.remove(firstId);

        assertEquals(20, orderIdMapper.get(secondId));
    }
}
