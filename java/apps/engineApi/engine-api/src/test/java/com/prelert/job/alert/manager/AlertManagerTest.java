package com.prelert.job.alert.manager;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.UriBuilder;

import org.junit.Test;
import org.mockito.*;

import static org.mockito.Mockito.*;

import com.prelert.job.alert.Alert;
import com.prelert.job.exceptions.UnknownJobException;
import com.prelert.job.manager.JobManager;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.process.exceptions.ClosedJobException;

public class AlertManagerTest {

    @Test
    public void testRegisterRequest()
    throws UnknownJobException, ClosedJobException
    {
        JobManager jobManager = mock(JobManager.class);
        JobProvider jobProvider = mock(JobProvider.class);

        AlertManager am = new AlertManager(jobProvider, jobManager);

        AsyncResponse response = mock(AsyncResponse.class);
        URI uri = UriBuilder.fromUri("http://testing").build();

        am.registerRequest(response, "foo", uri, 20l, 60.0, 70.0);

        verify(response, times(1)).setTimeout(20l, TimeUnit.SECONDS);
        verify(response, times(1)).setTimeoutHandler(am);
        verify(jobManager, times(1)).addAlertObserver(eq("foo"), any());
    }

    @Test
    public void testRegisterRequest_ClosedJobExceptionThrown()
    throws UnknownJobException, ClosedJobException
    {
        JobManager jobManager = mock(JobManager.class);
        JobProvider jobProvider = mock(JobProvider.class);

        AlertManager am = new AlertManager(jobProvider, jobManager);

        AsyncResponse response = mock(AsyncResponse.class);
        URI uri = UriBuilder.fromUri("http://testing").build();

        ClosedJobException e = new ClosedJobException("foo", "bar");
        doThrow(e).when(jobManager).addAlertObserver(eq("foo"), any());

        am.registerRequest(response, "foo", uri, 20l, 60.0, 70.0);
        verify(response, times(1)).resume(e);
    }

    @Test
    public void testHandleTimeout()
    {
        JobManager jobManager = mock(JobManager.class);
        JobProvider jobProvider = mock(JobProvider.class);

        AlertManager am = new AlertManager(jobProvider, jobManager);
        AsyncResponse response = mock(AsyncResponse.class);

        am.handleTimeout(response);

        ArgumentCaptor<Alert> argument = ArgumentCaptor.forClass(Alert.class);
        verify(response).resume(argument.capture());
        assertTrue(argument.getValue().isTimeout());
    }

}
