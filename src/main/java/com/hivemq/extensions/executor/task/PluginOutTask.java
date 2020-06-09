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
package com.hivemq.extensions.executor.task;

import java.util.function.Function;

/**
 * A wrapper for an extension task that does not provide information for the extension developer, but can affect HiveMQ.
 * <p>
 * It is assumed that only the returned {@link PluginTaskOutput} object of the {@link Function#apply(Object)} call is
 * relevant.
 *
 * @author Georg Held
 */
public interface PluginOutTask<O extends PluginTaskOutput> extends Function<O, O>, PluginTask {
}
