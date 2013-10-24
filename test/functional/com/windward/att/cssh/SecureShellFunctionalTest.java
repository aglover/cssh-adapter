package com.windward.att.cssh;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.common.StreamCopier;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.PKCS8KeyFile;
import org.junit.Test;

import java.io.*;
import java.security.Security;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: aglover
 * Date: 10/21/13
 * Time: 2:42 PM
 */
public class SecureShellFunctionalTest {
    @Test
    public void testLogIntoVagrantSSHJ() throws Exception {
        SSHClient client = null;
        Session session = null;
        try {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            client = new SSHClient();
            client.addHostKeyVerifier(new PromiscuousVerifier());
            client.connect("127.0.0.1", 2222);

            PKCS8KeyFile keyFile = new PKCS8KeyFile();
            keyFile.init(new File("/Users/aglover/.vagrant.d/insecure_private_key"));
            client.authPublickey("vagrant", keyFile);

            session = client.startSession();
            Session.Command cmd = session.exec("whoami");
            String response = IOUtils.readFully(cmd.getInputStream()).toString();
            cmd.join(10, TimeUnit.SECONDS);

            assertEquals("vagrant", response.trim());
        } finally {
            session.close();
            client.disconnect();
        }
    }

    @Test
    public void testLogIntoVagrantSSHJShell() throws Exception {
        SSHClient client = null;
        Session session = null;
        try {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            client = new SSHClient();
            client.addHostKeyVerifier(new PromiscuousVerifier());
            client.connect("127.0.0.1", 2222);

            PKCS8KeyFile keyFile = new PKCS8KeyFile();
            keyFile.init(new File("/Users/aglover/.vagrant.d/insecure_private_key"));
            client.authPublickey("vagrant", keyFile);

            session = client.startSession();
            session.allocateDefaultPTY();
            Session.Shell shell = session.startShell();


            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream printStream = new PrintStream(baos);


            new StreamCopier(shell.getInputStream(), printStream)
                    .bufSize(shell.getLocalMaxPacketSize())
                    .spawn("ptyout");

            OutputStream ops = shell.getOutputStream();
            PrintStream ps = new PrintStream(ops, true);
            ps.println("touch file");

            Thread.sleep(1000);
            ps.println("rm -i file");

            Thread.sleep(1000);
            assertTrue("PTY output should end with rm: remove regular empty file `file'?",
                    baos.toString().trim().endsWith("rm: remove regular empty file `file'?"));
            ps.println("y");
            ps.flush();
            ps.close();
            Thread.sleep(1000);

            //must "Start" a new one -- tmp file called file should have been deleted
            session = client.startSession();
            Session.Command cmd2 = session.exec("ls");
            String response2 = IOUtils.readFully(cmd2.getInputStream()).toString();
            cmd2.join(10, TimeUnit.SECONDS);

            assertEquals("postinstall.sh", response2.trim());

        } finally {
            session.close();
            client.disconnect();
        }
    }

    @Test
    public void testLogIntoVagrantSSHJSignal() throws Exception {
        SSHClient client = null;
        Session session = null;
        try {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            client = new SSHClient();
            client.addHostKeyVerifier(new PromiscuousVerifier());
            client.connect("127.0.0.1", 2222);

            PKCS8KeyFile keyFile = new PKCS8KeyFile();
            keyFile.init(new File("/Users/aglover/.vagrant.d/insecure_private_key"));
            client.authPublickey("vagrant", keyFile);

            session = client.startSession();
            session.allocateDefaultPTY(); //key LOC!
            Session.Command cmd = session.exec("sleep 10;ls");

            OutputStream out = cmd.getOutputStream();
            out.write(3); // send CTRL-C
            out.flush();

            String response = IOUtils.readFully(cmd.getInputStream()).toString();
            cmd.join(10, TimeUnit.SECONDS);

            assertEquals("^C", response.trim());
        } finally {
            session.close();
            client.disconnect();
        }
    }

    @Test
    public void testLogIntoVagrantSSHJTwoCommands() throws Exception {
        SSHClient client = null;
        Session session = null;
        try {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            client = new SSHClient();
            client.addHostKeyVerifier(new PromiscuousVerifier());
            client.connect("127.0.0.1", 2222);

            PKCS8KeyFile keyFile = new PKCS8KeyFile();
            keyFile.init(new File("/Users/aglover/.vagrant.d/insecure_private_key"));
            client.authPublickey("vagrant", keyFile);

            session = client.startSession();
            Session.Command cmd = session.exec("whoami");
            String response = IOUtils.readFully(cmd.getInputStream()).toString();
            cmd.join(10, TimeUnit.SECONDS);

            assertEquals("vagrant", response.trim());

            //must "Start" a new one
            session = client.startSession();
            Session.Command cmd2 = session.exec("ls");
            String response2 = IOUtils.readFully(cmd2.getInputStream()).toString();
            cmd.join(10, TimeUnit.SECONDS);

            assertEquals("postinstall.sh", response2.trim());

        } finally {
            session.close();
            client.disconnect();
        }
    }

    @Test
    public void testLogIntoVagrantJSCH() throws Exception {
        com.jcraft.jsch.Session session = null;
        com.jcraft.jsch.Channel channel = null;
        try {
            JSch jsch = new JSch();
            jsch.addIdentity("/Users/aglover/.vagrant.d/insecure_private_key");
            jsch.setConfig("StrictHostKeyChecking", "no");
            session = jsch.getSession("vagrant", "127.0.0.1", 2222);
            session.connect();
            String command = "whoami";
            channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);
            channel.connect();

            InputStream input = channel.getInputStream();
            StringBuffer buff = new StringBuffer();
            //start reading the input from the executed commands on the shell
            byte[] tmp = new byte[1024];
            while (true) {
                while (input.available() > 0) {
                    int i = input.read(tmp, 0, 1024);
                    if (i < 0) break;
                    buff.append(new String(tmp, 0, i));
                }
                if (channel.isClosed()) {
                    break;
                }
                Thread.sleep(1000);
            }
            assertEquals("vagrant", buff.toString().trim());
        } finally {

            channel.disconnect();
            session.disconnect();
        }
    }

    @Test
    public void testLogIntoVagrantJSCHSignalKill() throws Exception {
        com.jcraft.jsch.Session session = null;
        com.jcraft.jsch.Channel channel = null;
        try {
            JSch jsch = new JSch();
            jsch.addIdentity("/Users/aglover/.vagrant.d/insecure_private_key");
            jsch.setConfig("StrictHostKeyChecking", "no");
            session = jsch.getSession("vagrant", "127.0.0.1", 2222);
            session.connect();
            channel = session.openChannel("exec");
            ((ChannelExec) channel).setPty(true);
            ((ChannelExec) channel).setCommand("sleep 20;ls");

            OutputStream out = channel.getOutputStream();

            channel.connect();
            out.write(3); // send CTRL-C
            out.flush();
            InputStream input = channel.getInputStream();
            StringBuffer buff = new StringBuffer();
            //start reading the input from the executed commands on the shell
            byte[] tmp = new byte[1024];
            while (true) {
                while (input.available() > 0) {
                    int i = input.read(tmp, 0, 1024);
                    if (i < 0) break;
                    buff.append(new String(tmp, 0, i));
                }
                if (channel.isClosed()) {
                    assertEquals(-1, channel.getExitStatus());
                    break;
                }
                Thread.sleep(1000);
            }
            assertEquals("^C", buff.toString().trim());
        } finally {
            channel.disconnect();
            session.disconnect();
        }
    }
}
