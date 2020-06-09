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
package com.hivemq.extensions.executor;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.executor.task.*;

import java.util.function.Supplier;

/**
 * The main interface for the handling of extension tasks (interceptors, i.e.).
 * <p>
 * There are three kind of available tasks:
 * <ul>
 * <li> {@link PluginInTask} needing only a {@link PluginTaskInput} object and containing no callback in the {@link
 * PluginInTaskContext}
 * <li> {@link PluginOutTask} needing only a {@link PluginTaskOutput} object and containing a callback in the {@link
 * PluginOutTaskContext}
 * <li> {@link PluginInOutTask} needing a {@link PluginTaskInput} and a {@link PluginTaskOutput} object and containing
 * a
 * callback in the {@link PluginInOutTaskContext}
 * </ul>
 * <p>
 * The {@link PluginTaskExecutorService} will execute the {@link PluginTask} in the ThreadPool of the extension system.
 * <p>
 * A HiveMQ core developer, who wishes to implement a new interceptor, needs only to think about the creation of the
 * needed objects, not about the threading.
 *
 * @author Georg Held
 * @author Christoph Schäbel
 */
public interface PluginTaskExecutorService {

    /**
     * Handle a {@link PluginTask}, that can not affect the execution of HiveMQ, but provides additional information to
     * the extension developer.
     *
     * @param pluginInTaskContext a {@link PluginTaskContext} containing only the needed information for the scheduler.
     * @param pluginInputSupplier a supplier for the the {@link PluginTaskInput} object.
     * @param pluginTask          a wrapper around the specific interceptor i.e..
     * @param <I>                 a type extending the {@link PluginTaskInput} marker interface.
     * @throws java.util.concurrent.RejectedExecutionException when task executor is shut down.
     */
    <I extends PluginTaskInput> void handlePluginInTaskExecution(@NotNull final PluginInTaskContext pluginInTaskContext,
                                                                    @NotNull final Supplier<I> pluginInputSupplier,
                                                                    @NotNull final PluginInTask<I> pluginTask);

    /**
     * Handle a {@link PluginTask}, that can affect the execution of HiveMQ, but provides no additional information to
     * the extension developer.
     *
     * @param pluginOutTaskContext a {@link PluginTaskContext} containing the needed information for the scheduler and a
     *                             callback for the further processing of the extension call result.
     * @param pluginOutputSupplier a supplier for the the {@link PluginTaskOutput} object.
     * @param pluginTask           a wrapper around the specific interceptor i.e..
     * @param <O>                  a type extending the {@link PluginTaskOutput} marker interface.
     * @throws java.util.concurrent.RejectedExecutionException when task executor is shut down.
     */
    <O extends PluginTaskOutput> void handlePluginOutTaskExecution(@NotNull final PluginOutTaskContext<O> pluginOutTaskContext,
                                                                      @NotNull final Supplier<O> pluginOutputSupplier,
                                                                      @NotNull final PluginOutTask<O> pluginTask);

    /**
     * Handle a {@link PluginTask}, that can affect the execution of HiveMQ and provides additional information to
     * the extension developer.
     *
     * @param pluginInOutContext   a {@link PluginTaskContext} containing the needed information for the scheduler and a
     *                             callback for the further processing of the extension call result.
     * @param pluginInputSupplier  a supplier for the the {@link PluginTaskInput} object.
     * @param pluginOutputSupplier a supplier for the the {@link PluginTaskOutput} object.
     * @param pluginTask           a wrapper around the specific interceptor i.e..
     * @param <I>                  a type extending the {@link PluginTaskInput} marker interface.
     * @param <O>                  a type extending the {@link PluginTaskOutput} marker interface.
     * @throws java.util.concurrent.RejectedExecutionException when task executor is shut down.
     */
    <I extends PluginTaskInput, O extends PluginTaskOutput> void handlePluginInOutTaskExecution(@NotNull final PluginInOutTaskContext<O> pluginInOutContext,
                                                                                                   @NotNull final Supplier<I> pluginInputSupplier,
                                                                                                   @NotNull final Supplier<O> pluginOutputSupplier,
                                                                                                   @NotNull final PluginInOutTask<I, O> pluginTask);



    /* Usage example:

    public class PublishAuthorizerHandler {

        @NotNull
        private final PluginOutPutAsyncer asyncer;
        @NotNull
        private final PluginTaskExecutorService service;
        @NotNull
        private final ClientSessionPersistence clientSessionPersistence;

        @Inject
        public PublishAuthorizerHandler(@NotNull final PluginOutPutAsyncer asyncer,
                          @NotNull final PluginTaskExecutorService service,
                          @NotNull final ClientSessionPersistence clientSessionPersistence) {
            this.asyncer = asyncer;
            this.service = service;
            this.clientSessionPersistence = clientSessionPersistence;
        }

        void handle(@NotNull final String id, @NotNull final PublishAuthorizer authorizer) {
            final PublishAuthorizerContext pluginInOutContext = new PublishAuthorizerContext(authorizer.getClass(), id, clientSessionPersistence);
            service.handlePluginInOutTaskExecution(pluginInOutContext,
                    () -> new PublishAuthorizerInputImpl(),
                    () -> new PublishAuthorizerOutputImpl(asyncer, pluginInOutContext),
                    new PublishAuthorizerTask(authorizer));
        }
    }

   public class PublishAuthorizerContext extends PluginInOutTaskContext<PublishAuthorizerOutputImpl> {
        @NotNull
        private final String clientId;
        @NotNull
        private final ClientSessionPersistence clientSessionPersistence;

        protected PublishAuthorizerContext(@NotNull final Class<?> taskClazz,
                                           @NotNull final String clientId,
                                           @NotNull final ClientSessionPersistence clientSessionPersistence) {
            super(taskClazz, clientId);
            this.clientId = clientId;
            this.clientSessionPersistence = clientSessionPersistence;
        }

        @Override
        public void pluginPost(@NotNull final PublishAuthorizerOutputImpl pluginOutput) {
            // handle extension result
            if (pluginOutput.isTimedOut() {
                if (pluginOutput.getTimeoutFallback() == TimeoutFallback.FAILURE) {
                    clientSessionPersistence.forceDisconnectClient(clientId, true);
                } else {
                    //continue
                }
            }

            if (pluginOutput.isDisconnect()) {
                clientSessionPersistence.forceDisconnectClient(clientId, true);
            }
        }
    }

    public class PublishAuthorizerOutputImpl extends AbstractAsyncOutput<PublishAuthorizeOutput> implements PublishAuthorizeOutput {

        @Nullable
        private final PluginOutPutAsyncer asyncer;

        @Nullable
        private final PublishAuthorizerContext pluginInOutContext;

        public PublishAuthorizerOutputImpl(@Nullable final PluginOutPutAsyncer asyncer, @Nullable final PublishAuthorizerContext pluginInOutContext) {

            this.asyncer = asyncer;
            this.pluginInOutContext = pluginInOutContext;
        }

        // ... implementation of PublishAuthorizeOutput methods

    }

    public class PublishAuthorizerTask implements PluginInOutTask<PublishAuthorizerInputImpl, PublishAuthorizerOutputImpl> {
        @NotNull
        private final PublishAuthorizer authorizer;

        public PublishAuthorizerTask(@NotNull final PublishAuthorizer authorizer) {
            this.authorizer = authorizer;
        }

        @NotNull
        @Override
        public PublishAuthorizerOutputImpl apply(@NotNull final PublishAuthorizerInputImpl publishAuthorizerInput, @NotNull final PublishAuthorizerOutputImpl publishAuthorizerOutput) {
            try {
                authorizer.authorizePublish(publishAuthorizerInput, publishAuthorizerOutput);
                return publishAuthorizerOutput;
            } catch (final Throwable t) {
                final PublishAuthorizerOutputImpl exceptionalOutput = new PublishAuthorizerOutputImpl(null, null);
                exceptionalOutput.disconnectClient();
                return exceptionalOutput;
            }
        }
    }
    */
}
