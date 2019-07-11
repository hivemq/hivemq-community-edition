package com.hivemq.extensions.services.general;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Christoph Schäbel
 */
public class IterationContextImplTest {

    @Test
    public void test_abort() {

        final IterationContextImpl iterationContext = new IterationContextImpl();

        assertFalse(iterationContext.isAborted());

        iterationContext.abortIteration();

        assertTrue(iterationContext.isAborted());
    }

}
