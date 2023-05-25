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
package com.hivemq.persistence.local.xodus;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import jetbrains.exodus.env.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Makes it simpler to run multiple actions on transaction commit
 * while preventing us from forgetting to define the commit hook accordingly.
 */
public class TransactionCommitActions implements Runnable {

    private final @NotNull List<Runnable> actions;

    private TransactionCommitActions() {
        actions = new ArrayList<>();
    }

    public static @NotNull TransactionCommitActions asCommitHookFor(final @NotNull Transaction transaction) {
        final TransactionCommitActions transactionCommitActions = new TransactionCommitActions();
        transaction.setCommitHook(transactionCommitActions);
        return transactionCommitActions;
    }

    public void add(final @NotNull Runnable action) {
        actions.add(action);
    }

    @Override
    public void run() {
        actions.forEach(Runnable::run);
    }
}
