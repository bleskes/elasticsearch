package com.prelert.rs.resources;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.Application;

import org.junit.Test;

import com.prelert.job.manager.JobManager;
import com.prelert.server.info.ServerInfo;
import com.prelert.server.info.ServerInfoFactory;

public class ApiBaseTest {

    @Test
    public void testVersion()
    {
        ApiBase api = new ApiBase();
        JobManager manager = mock(JobManager.class);
        Application application = mock(Application.class);
        ServerInfoFactory sif = mock(ServerInfoFactory.class);
        Set<Object> objects = new HashSet<>();
        objects.add(manager);
        objects.add(sif);
        when(application.getSingletons()).thenReturn(objects);
        when(manager.apiVersion()).thenReturn("999999");
        when(manager.getAnalyticsVersion()).thenReturn("8888");
        ServerInfo si = new ServerInfo();
        si.setAvailableDiskMb(12345L);
        si.setHostname("Johnny V");
        si.setOsName("bob");
        si.setOsVersion("Prancing Beast");
        si.setTotalDiskMb(54321L);
        si.setTotalMemoryMb(44L);
        when(sif.serverInfo()).thenReturn(si);

        api.setApplication(application);

        String ver = api.version();
        System.out.println(ver);

        Pattern p = Pattern.compile(".*\\b999999\\b.*", Pattern.DOTALL);
        Matcher m = p.matcher(ver);
        assertTrue(m.matches());

        p = Pattern.compile(".*\\b8888\\b.*", Pattern.DOTALL);
        m = p.matcher(ver);
        assertTrue(m.matches());

        p = Pattern.compile(".*\\b12345\\b.*", Pattern.DOTALL);
        m = p.matcher(ver);
        assertTrue(m.matches());

        p = Pattern.compile(".*\\bJohnny V\\b.*", Pattern.DOTALL);
        m = p.matcher(ver);
        assertTrue(m.matches());

        p = Pattern.compile(".*\\bbob\\b.*", Pattern.DOTALL);
        m = p.matcher(ver);
        assertTrue(m.matches());

        p = Pattern.compile(".*\\bPrancing Beast\\b.*", Pattern.DOTALL);
        m = p.matcher(ver);
        assertTrue(m.matches());

        p = Pattern.compile(".*\\b54321\\b.*", Pattern.DOTALL);
        m = p.matcher(ver);
        assertTrue(m.matches());

        p = Pattern.compile(".*\\b44\\b.*", Pattern.DOTALL);
        m = p.matcher(ver);
        assertTrue(m.matches());
    }

}
