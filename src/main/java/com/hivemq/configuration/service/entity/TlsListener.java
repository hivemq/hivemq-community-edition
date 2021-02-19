package com.hivemq.configuration.service.entity;

/**
 * @author Christoph Schäbel
 */
public interface TlsListener extends Listener {
    Tls getTls();
}
