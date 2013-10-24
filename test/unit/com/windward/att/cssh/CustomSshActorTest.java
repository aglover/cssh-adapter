package com.windward.att.cssh;

import com.realops.common.xml.XML;
import com.realops.foundation.adapterframework.AdapterRequest;
import com.realops.foundation.adapterframework.AdapterResponse;
import com.windward.att.CustomSshActor;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import org.apache.commons.lang.WordUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: aglover
 * Date: 10/23/13
 * Time: 2:01 PM
 */
public class CustomSshActorTest {

    @Test
    public void convertFromSnakeToCamel() throws Exception {
        String var = "start_console";
        assertEquals("StartConsole", WordUtils.capitalizeFully(var, new char[]{'_'}).replaceAll("_", ""));
    }

    @Test
    public void testActor() throws Exception {
        SSHClient client = mock(SSHClient.class);
        Session session = mock(Session.class);
        Session.Shell shell = mock(Session.Shell.class);

        when(client.startSession()).thenReturn(session);
        when(session.startShell()).thenReturn(shell);
        when(shell.getInputStream()).thenReturn(new InputStream() {
            public int read() throws IOException {
                return -1;
            }
        });
        when(shell.getLocalMaxPacketSize()).thenReturn(1000);

        OutputStream outputStream = new ByteArrayOutputStream();
        when(shell.getOutputStream()).thenReturn(outputStream);

        CustomSshActor adaptor = new CustomSshActor();
        adaptor.setSshClient(client);
        AdapterResponse adapterResponse = adaptor.performAction(new AdapterRequest(XML.read("etc/test-command-1.xml")));

        verify(client, times(1)).connect("198.224.30.24");
        verify(client, times(1)).authPassword("root", "abc123");
        verify(session, times(1)).allocateDefaultPTY();
        verify(session, times(1)).close();
        verify(client, times(1)).disconnect();

        String expected = "reset /SYS\n" +
                "y\n" +
                "start /SP/console\n" +
                "y\n" +
                "ESC (\n";
                //"[B@6c69d02b";//"14";

        //System.out.println("output stream is " + outputStream.toString());
        Assert.assertTrue(outputStream.toString().trim().contains(expected));

        assertNotNull("adapterResponse was not null?", adapterResponse);
    }
}
