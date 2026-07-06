package com.techmart.entity;

import jakarta.persistence.*;
import java.io.Serializable;

/**
 * Stock level of one product in one warehouse.
 */
@Entity
@Table(name = "inventory_item",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "warehouse_id"}))
@NamedQueries({
        @NamedQuery(name = "InventoryItem.byProduct",
                query = "SELECT i FROM InventoryItem i WHERE i.product.id = :productId ORDER BY i.quantity DESC"),
        @NamedQuery(name = "InventoryItem.all",
                query = "SELECT i FROM InventoryItem i")
})
public class InventoryItem implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(nullable = false)
    private int quantity;

    /** Units held for incomming orders but not yet shipped. */
    @Column(nullable = false)
    private int reserved;

    @Version
    private long version;

    public InventoryItem() {
    }

    public InventoryItem(Product product, Warehouse warehouse, int quantity) {
        this.product = product;
        this.warehouse = warehouse;
        this.quantity = quantity;
    }

    /** Stock that can still be sold. */
    @Transient
    public int getAvailable() {
        return quantity - reserved;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getReserved() { return reserved; }
    public void setReserved(int reserved) { this.reserved = reserved; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
