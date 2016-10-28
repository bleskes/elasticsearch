
package org.elasticsearch.xpack.prelert.job.manager.actions;

import java.util.Optional;

/**
 * Guards and manages permissions for performing actions on jobs.
 *
 * The {@linkplain ActionTicket} returned by {@linkplain #tryAcquiringAction(String, Enum)}
 * must be closed and should be used only in a try-with-resource block.
 *
 * Guardians can be chained together by passing another to he
 * constructor. Locks are only granted when all locks are acquired.
 *
 * Implementing classes must acquire and release the next guardian.
 */
public abstract class ActionGuardian< T extends Enum<T> & ActionState<T>>
{
    protected Optional<ActionGuardian<T>> nextGuardian;

    protected final T noneAction;

    /**
     * noneAction is the enum value representing the state where
     * no action is taking place.
     * e.g. @
     */
    public ActionGuardian(T noneAction)
    {
        this.noneAction = noneAction;
        nextGuardian = Optional.empty();
    }

    /**
     * noneAction is the enum value representing the state where
     * no action is taking place.
     * guardian is the next guard to check if this guard succeeds in
     * acquiring an action.
     */
    public ActionGuardian(T noneAction, ActionGuardian<T> guardian)
    {
        this.noneAction = noneAction;
        nextGuardian = Optional.of(guardian);
    }

    /**
     * Set the next guardian in the chain
     */
    public void setNextGuardian(ActionGuardian<T> guardian)
    {
        nextGuardian = Optional.of(guardian);
    }

    /**
     * The returned ActionTicket MUST be closed in a try-with-resource block
     *
     * Returns an {@code ActionTicket} if requested action is available for the given job.
     * @param jobId the job id
     * @param action the requested action
     * @return the {@code ActionTicket} granting permission to execute the action
     */
    public abstract ActionTicket tryAcquiringAction(String jobId, T action);

    /**
     * Releases the action for the given job
     * @param jobId the job id
     * @param nextState Put the guardian into this state after releasing the action
     */
    public abstract void releaseAction(String jobId, T nextState);

    /**
     * A token signifying that its owner has permission to execute an action for a job.
     * Designed to be used with try-with-resources to ensure the action is released.
     */
    public class ActionTicket implements AutoCloseable
    {
        private final String jobId;
        private final T nextState;

        private ActionTicket(String jobId, T nextState)
        {
            this.jobId = jobId;
            this.nextState = nextState;
        }

        @Override
        public void close()
        {
            ActionGuardian.this.releaseAction(jobId, nextState);
        }
    }

    protected ActionTicket newActionTicket(String jobId, T nextState)
    {
        return new ActionTicket(jobId, nextState);
    }
}
