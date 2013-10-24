package com.windward.att.commands;

import com.realops.common.xml.XML;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.StreamCopier;
import net.schmizz.sshj.connection.channel.direct.Session;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Created with IntelliJ IDEA.
 * User: aglover
 * Date: 10/23/13
 * Time: 9:04 PM
 */
public class StartConsoleCommand extends AbstractCommand {

    @Override
    public XML executeCommand(XML requestXML) throws Exception {
        SSHClient client = sshClientInstance();
        client.connect(requestXML.getChild("host").getText());
        client.authPassword(requestXML.getChild("username").getText(), requestXML.getChild("password").getText());
        final Session session = client.startSession();
        try {
            session.allocateDefaultPTY();
            Session.Shell shell = session.startShell();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream printStream = new PrintStream(baos);

            new StreamCopier(shell.getInputStream(), printStream)
                    .bufSize(shell.getLocalMaxPacketSize())
                    .spawn("ptyout");

            OutputStream ops = shell.getOutputStream();
            PrintStream ps = new PrintStream(ops, true);
            ps.println("reset /SYS");
            Thread.sleep(1000);
            ps.println("y");
            Thread.sleep(1000);
            ps.println("start /SP/console");
            Thread.sleep(1000);
            ps.println("y");
            Thread.sleep(1000);
            ps.println("ESC (");
            Thread.sleep(1000);
            ps.println(new byte[]{14}); //what is CTRL-N?

            return null;
        } finally {
            session.close();
            client.disconnect();
        }
    }
}
