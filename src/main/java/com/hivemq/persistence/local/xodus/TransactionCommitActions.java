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
