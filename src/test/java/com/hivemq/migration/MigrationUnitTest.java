package com.hivemq.migration;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Florian Limpöck
 */
public class MigrationUnitTest {

    @Test
    public void test_payloads_first() {
        assertEquals(MigrationUnit.values()[0], MigrationUnit.FILE_PERSISTENCE_PUBLISH_PAYLOAD);
    }

}