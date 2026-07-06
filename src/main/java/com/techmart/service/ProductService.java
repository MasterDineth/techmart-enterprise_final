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
