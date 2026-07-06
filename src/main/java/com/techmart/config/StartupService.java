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
        // Verify the @Resource-injected datasource is present (Jakarta EE 10 / WildFly 27).
        LOG.info("JNDI OK: datasource injected via @Resource (present: " + (dataSource != null) + ")");
    }

    private boolean isAlreadySeeded() {
        Long count = em.createQuery("SELECT COUNT(p) FROM Product p", Long.class).getSingleResult();
        return count != null && count > 0;
    }

    private void seedDemoData() {
        Warehouse wCol = persist(new Warehouse("WH-CMB", "Colombo Main DC", "Colombo, Western Province"));
        Warehouse wKur = persist(new Warehouse("WH-KUR", "Kurunegala Regional DC", "Kurunegala, North Western Province"));
        Warehouse wKan = persist(new Warehouse("WH-KAN", "Kandy Regional DC", "Kandy, Central Province"));

        seedProduct("SKU-1001", "TechMart 27\" 4K Monitor", "Displays", "98500.00", wCol, 120, wKur, 80, wKan, 60);
        seedProduct("SKU-1002", "Mechanical Keyboard Pro", "Peripherals", "26500.00", wCol, 300, wKur, 150, wKan, 90);
        seedProduct("SKU-1003", "Wireless Mouse Ergo", "Peripherals", "11500.00", wCol, 500, wKur, 400, wKan, 250);
        seedProduct("SKU-1004", "USB-C Docking Station", "Accessories", "44900.00", wCol, 60, wKur, 40, wKan, 20);
        seedProduct("SKU-1005", "Noise-Cancelling Headset", "Audio", "59900.00", wCol, 90, wKur, 70, wKan, 45);
        seedProduct("SKU-1006", "1TB NVMe SSD", "Storage", "35900.00", wCol, 220, wKur, 180, wKan, 100);
        seedProduct("SKU-1007", "Webcam 1080p", "Peripherals", "17500.00", wCol, 140, wKur, 110, wKan, 70);
        seedProduct("SKU-1008", "Laptop Stand Aluminium", "Accessories", "13500.00", wCol, 160, wKur, 130, wKan, 80);

        persist(new Customer("Demo Shopper", "user@techmart.lk"));
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