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
package util;

import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

/**
 * @author Christoph Schäbel
 */
@SuppressWarnings("deprecation")
public class TestChannelAttribute<T> implements Attribute<T> {

    private T value;

    public TestChannelAttribute(final T value) {
        this.value = value;
    }

    @Override
    public AttributeKey<T> key() {
        return null;
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public void set(final T value) {
        this.value = value;
    }

    @Override
    public T getAndSet(final T value) {
        final T value1 = this.value;
        this.value = value;
        return value1;
    }

    @Override
    public T setIfAbsent(final T value) {
        if (this.value == null) {
            this.value = value;
            return null;
        }
        return value;
    }

    @Override
    public T getAndRemove() {
        final T value1 = this.value;
        this.value = null;
        return value1;
    }

    @Override
    public boolean compareAndSet(final T oldValue, final T newValue) {
        if (this.value.equals(oldValue)) {
            this.value = newValue;
            return true;
        }
        return false;
    }

    @Override
    public void remove() {
        this.value = null;
    }
}
