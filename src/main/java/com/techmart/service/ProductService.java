package com.techmart.service;

import com.techmart.entity.Product;
import com.techmart.interceptor.Monitored;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

/**
 * Stateless session bean for catalogue reads.
 *
 * <p>{@code @Stateless} beans are pooled by WildFly, so a small number of
 * instances serve thousands of concurrent callers - the core scalability win
 * over the legacy monolith. All methods are {@link Monitored}.</p>
 */
@Stateless
@Monitored
public class ProductService {

    @PersistenceContext(unitName = "techmartPU")
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Product> findAll() {
        return em.createNamedQuery("Product.findAll", Product.class).getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Product find(Long id) {
        return em.find(Product.class, id);
    }

    public Product create(Product p) {
        em.persist(p);
        return p;
    }
}
