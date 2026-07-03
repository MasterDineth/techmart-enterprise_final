package com.techmart.it;

import com.techmart.config.StartupService;
import com.techmart.entity.Product;
import com.techmart.interceptor.Monitored;
import com.techmart.interceptor.PerformanceInterceptor;
import com.techmart.jms.JmsConfig;
import com.techmart.metrics.MethodStats;
import com.techmart.metrics.PerformanceMonitor;
import com.techmart.service.InventoryCache;
import com.techmart.service.InventoryService;
import com.techmart.service.ProductService;
import jakarta.ejb.EJB;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ArquillianExtension.class)
class ProductServiceIT {

    @Deployment
    static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "test.war")
                .addPackages(true, "com.techmart")
                .addAsResource("META-INF/persistence.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @EJB
    private ProductService productService;

    @EJB
    private InventoryService inventoryService;

    @Test
    void findAll_returnsSeededProducts() {
        var products = productService.findAll();
        assertFalse(products.isEmpty(), "Seeded products should be present");
        assertTrue(products.size() >= 8, "Expected at least 8 demo products");
    }

    @Test
    void find_returnsCorrectProduct() {
        var all = productService.findAll();
        Product first = all.get(0);
        Product found = productService.find(first.getId());
        assertNotNull(found);
        assertEquals(first.getSku(), found.getSku());
    }

    @Test
    void availableFor_returnsPositiveStock() {
        var products = productService.findAll();
        int available = inventoryService.availableFor(products.get(0).getId());
        assertTrue(available > 0, "Seeded product should have stock");
    }

    @Test
    void inventoryService_findAll_notEmpty() {
        var items = inventoryService.findAll();
        assertFalse(items.isEmpty());
        // Each item should have non-negative available stock
        items.forEach(i -> assertTrue(i.getAvailable() >= 0));
    }
}
