package com.techmart.service;

import com.techmart.entity.Customer;
import com.techmart.interceptor.Monitored;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

/**
 * Stateless bean managing customer records. Demonstrates a get-or-create
 * pattern used by guest checkout.
 */
@Stateless
@Monitored
public class CustomerService {

    @PersistenceContext(unitName = "techmartPU")
    private EntityManager em;

    public Customer findOrCreate(String name, String email) {
        try {
            return em.createNamedQuery("Customer.byEmail", Customer.class)
                    .setParameter("email", email)
                    .getSingleResult();
        } catch (NoResultException e) {
            Customer c = new Customer(name, email);
            em.persist(c);
            return c;
        }
    }
}
