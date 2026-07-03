package com.techmart.unit;

import com.techmart.cart.ShoppingCart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShoppingCartTest {

    private ShoppingCart cart;

    @BeforeEach
    void setUp() {
        cart = new ShoppingCart();
    }

    @Test
    void add_accumulatesQuantity() {
        cart.add(1L, 2);
        cart.add(1L, 3);
        assertEquals(5, cart.getLines().get(1L));
    }

    @Test
    void setQuantity_zeroRemovesItem() {
        cart.add(1L, 5);
        cart.setQuantity(1L, 0);
        assertFalse(cart.getLines().containsKey(1L));
    }

    @Test
    void remove_deletesItem() {
        cart.add(2L, 1);
        cart.remove(2L);
        assertTrue(cart.getLines().isEmpty());
    }

    @Test
    void getItemCount_sumsAllQuantities() {
        cart.add(1L, 3);
        cart.add(2L, 2);
        assertEquals(5, cart.getItemCount());
    }

    @Test
    void clear_emptiesCart() {
        cart.add(1L, 1);
        cart.add(2L, 2);
        cart.clear();
        assertEquals(0, cart.getItemCount());
    }

    @Test
    void getLines_returnsDefensiveCopy() {
        cart.add(1L, 1);
        var lines = cart.getLines();
        lines.put(99L, 99);
        assertFalse(cart.getLines().containsKey(99L));
    }
}
