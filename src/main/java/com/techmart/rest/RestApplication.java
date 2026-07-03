package com.techmart.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS activation. All resources live under {@code /api}.
 */
@ApplicationPath("/api")
public class RestApplication extends Application {
}
