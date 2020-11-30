package com.hivemq.migration;

/**
 * @author Florian Limpöck
 * @since 4.3.0
 */
@FunctionalInterface
public interface ValueMigration {

    void migrateToValue();
}
