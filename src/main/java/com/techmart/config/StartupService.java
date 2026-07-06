package com.techmart.config;

import com.techmart.entity.*;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import jakarta.annotation.Resource;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Logger;


@Singleton
@Startup
public class StartupService {

    private static final Logger LOG = Logger.getLogger(StartupService.class.getName());

    @PersistenceContext(unitName = "techmartPU")
    private EntityManager em;

    // Dependency injection of the container-managed datasource by JNDI name.
    @Resource(lookup = "java:jboss/datasources/TechMartDS")
    private DataSource dataSource;

    @PostConstruct
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void bootstrap() {
        verifyJndiResources();
        if (isAlreadySeeded()) {
            LOG.info("Catalogue already seeded - skipping demo data.");
            return;
        }
        seedDemoData();
        LOG.info("TechMart demo data seeded successfully.");
    }

    private void verifyJndiResources() {
        // Verify the datasource is present
        LOG.info("JNDI OK: datasource injected via @Resource (present: " + (dataSource != null) + ")");
    }

    private boolean isAlreadySeeded() {
        Long count = em.createQuery("SELECT COUNT(p) FROM Product p", Long.class).getSingleResult();
        return count != null && count > 0;
    }

    private void seedDemoData() {
        Warehouse wEast = persist(new Warehouse("WH-EAST", "East Coast DC", "Newark, NJ"));
        Warehouse wWest = persist(new Warehouse("WH-WEST", "West Coast DC", "Fremont, CA"));
        Warehouse wCent = persist(new Warehouse("WH-CENT", "Central DC", "Dallas, TX"));

        seedProduct("SKU-1001", "TechMart 27\" 4K Monitor", "Displays", "329.00", wEast, 120, wWest, 80, wCent, 60);
        seedProduct("SKU-1002", "Mechanical Keyboard Pro", "Peripherals", "89.00", wEast, 300, wWest, 150, wCent, 90);
        seedProduct("SKU-1003", "Wireless Mouse Ergo", "Peripherals", "39.00", wEast, 500, wWest, 400, wCent, 250);
        seedProduct("SKU-1004", "USB-C Docking Station", "Accessories", "149.00", wEast, 60, wWest, 40, wCent, 20);
        seedProduct("SKU-1005", "Noise-Cancelling Headset", "Audio", "199.00", wEast, 90, wWest, 70, wCent, 45);
        seedProduct("SKU-1006", "1TB NVMe SSD", "Storage", "119.00", wEast, 220, wWest, 180, wCent, 100);
        seedProduct("SKU-1007", "Webcam 1080p", "Peripherals", "59.00", wEast, 140, wWest, 110, wCent, 70);
        seedProduct("SKU-1008", "Laptop Stand Aluminium", "Accessories", "45.00", wEast, 160, wWest, 130, wCent, 80);

        persist(new Customer("Demo Shopper", "demo@techmart.example"));
    }

    private void seedProduct(String sku, String name, String category, String price,
                             Warehouse w1, int q1, Warehouse w2, int q2, Warehouse w3, int q3) {
        Product p = persist(new Product(sku, name, category, new BigDecimal(price),
                name + " - enterprise-grade quality from TechMart."));
        persist(new InventoryItem(p, w1, q1));
        persist(new InventoryItem(p, w2, q2));
        persist(new InventoryItem(p, w3, q3));
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }

    /** Exposed for health checks / metrics. */
    public List<String> boundDatasourceInfo() {
        return List.of("java:jboss/datasources/TechMartDS", dataSource != null ? "INJECTED" : "MISSING");
    }
}
