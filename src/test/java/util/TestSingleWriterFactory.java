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

import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.SingleWriterServiceImpl;

/**
 * @author Lukas Brandl
 */
public class TestSingleWriterFactory {

    public static SingleWriterService defaultSingleWriter() {

        InternalConfigurations.PERSISTENCE_BUCKET_COUNT.set(64);
        InternalConfigurations.SINGLE_WRITER_THREAD_POOL_SIZE.set(1);
        InternalConfigurations.SINGLE_WRITER_CREDITS_PER_EXECUTION.set(100);
        InternalConfigurations.PERSISTENCE_SHUTDOWN_GRACE_PERIOD.set(1000);
        InternalConfigurations.SINGLE_WRITER_CHECK_SCHEDULE.set(100);

        final SingleWriterServiceImpl singleWriterServiceImpl = new SingleWriterServiceImpl();
        for (int i = 0; i < singleWriterServiceImpl.callbackExecutors.length; i++) {
            singleWriterServiceImpl.callbackExecutors[i] = MoreExecutors.newDirectExecutorService();
        }

        singleWriterServiceImpl.postConstruct();

        return singleWriterServiceImpl;
    }
}
