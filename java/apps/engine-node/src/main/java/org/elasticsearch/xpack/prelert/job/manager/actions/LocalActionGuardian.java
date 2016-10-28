
package org.elasticsearch.xpack.prelert.job.manager.actions;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Prevents concurrent actions on a job based on the contents of a local
 * map. If a job currently partaking in an action {@linkplain #tryAcquiringAction(String, Enum)}
 * will throw otherwise an ActionTicket is returned
 */
public class LocalActionGuardian<T extends Enum<T> & ActionState<T>>
                            extends ActionGuardian<T>
{
    private static final Logger LOGGER = Loggers.getLogger(LocalActionGuardian.class);

    private final Map<String, T> actionsByJob = new HashMap<>();

    public LocalActionGuardian(T defaultAction)
    {
        super(defaultAction);
    }

    public LocalActionGuardian(T defaultAction, ActionGuardian<T> guardian)
    {
        super(defaultAction, guardian);
    }

    /**
     * Get the action the job is currently processing
     * or NoneAction if the job is not active
     */
    public T currentAction(String jobId)
    {
        synchronized (this)
        {
            return actionsByJob.getOrDefault(jobId, noneAction);
        }
    }

    /**
     * The returned ActionTicket MUST be closed in a try-with-resource block
     *
     * Returns an {@code ActionTicket} if requested action is available for the given job.
     * @param jobId the job id
     * @param action the requested action
     * @return the {@code ActionTicket} granting permission to execute the action
     */
    @Override
    public ActionTicket tryAcquiringAction(String jobId, T action) {
        synchronized (this)
        {
            T currentAction = actionsByJob.getOrDefault(jobId, noneAction);

            if (currentAction.isValidTransition(action))
            {
                if (nextGuardian.isPresent())
                {
                    nextGuardian.get().tryAcquiringAction(jobId, action);
                }

                actionsByJob.put(jobId, action);

                return newActionTicket(jobId, action.nextState(currentAction));
            }
            else
            {
                String msg = action.getBusyActionError(jobId, currentAction);
                LOGGER.warn(msg);
                throw ExceptionsHelper.invalidRequestException(msg, action.getErrorCode());
            }
        }
    }


    @Override
    public void releaseAction(String jobId, T nextState)
    {
        synchronized (this)
        {
            actionsByJob.put(jobId, nextState);

            if (nextGuardian.isPresent())
            {
                nextGuardian.get().releaseAction(jobId, nextState);
            }
        }
    }

}
