/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extensions.iteration;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Christoph Schäbel
 */
@SuppressWarnings("NullabilityAnnotations")
public class ResultBufferTest {

    @Test
    public void test_fetch_once() {

        final ResultBuffer<String> resultBuffer = prepareBuffer();

        final List<String> values = new ArrayList<>();
        Collection<String> chunk = resultBuffer.getNextChunk();
        while (chunk != null) {
            values.addAll(chunk);
            chunk = resultBuffer.getNextChunk();
        }

        assertEquals(4, values.size());
    }

    @Test
    public void test_clean() {
        final ResultBuffer<String> resultBuffer = prepareBuffer();

        resultBuffer.clean();

        final List<String> values = new ArrayList<>();
        Collection<String> chunk = resultBuffer.getNextChunk();
        while (chunk != null) {
            values.addAll(chunk);
            chunk = resultBuffer.getNextChunk();
        }

        assertEquals(0, values.size());
    }

    @SuppressWarnings("ConstantConditions")
    private ResultBuffer<String> prepareBuffer() {
        final Queue<ChunkResult<String>> items =
                new ArrayDeque<>(List.of(new ChunkResult<>(List.of("1", "2"), new ChunkCursor(), false),
                        new ChunkResult<>(List.of("3", "4"), new ChunkCursor(), true)));

        final ResultBuffer<String> resultBuffer = new ResultBuffer<>((cursor, resultBuffer1) -> {
            resultBuffer1.addChunk(items.poll());
        });
        resultBuffer.addChunk(items.poll());

        return resultBuffer;
    }

}
