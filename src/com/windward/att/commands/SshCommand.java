package com.windward.att.commands;

import com.realops.common.xml.XML;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: cs7866
 * Date: 10/29/13
 * Time: 4:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class SshCommand  extends AbstractCommand {
    @Override
    public XML executeCommand(XML requestXML) throws Exception {
        SSHClient client = this.clientFromXML(requestXML.getChild("target"));
        final Session session = client.startSession();
        XML commandsOutput = new XML("commands-output");
        try {
            session.allocateDefaultPTY();
            Session.Shell shell = session.startShell();

            OutputStream ops = shell.getOutputStream();
            PrintStream ps = new PrintStream(ops, true);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(shell.getInputStream()));


            XML commands = requestXML.getChild("commands");
            for (XML command: commands.getChildren()){
                XML commandOutput = new XML("command-output");
                XML output = new XML("output");
                String prompt = command.getAttribute("prompt");

                long pause = command.getLongAttribute("pause-before-milliseconds",0);
                if (pause>0)
                    Thread.sleep(pause);
                long startTime = System.currentTimeMillis();
                if (command.getBooleanAttribute("ctrl"))
                   this.sendControlCharacter(ps, command.getText());
                else
                    ps.println(command.getText());
                boolean found = true;
                if (prompt!=null && prompt.length()>0)
                     found = readUntilPromptFound(bufferedReader, prompt, output);
                long duration = System.currentTimeMillis() - startTime;

                pause = command.getLongAttribute("pause-after-milliseconds",0);
                if (pause>0)
                    Thread.sleep(pause);
                XML metadata = new XML("metadata");
                metadata.addChild("command").setText(command.getText());
                metadata.addChild("line-count").setText(""+output.getChildren().length);
                metadata.addChild("execution-milliseconds").setText(""+duration);
                metadata.addChild("success").setText(found ? "success" : "failure");

                commandOutput.addChild(metadata);
                commandOutput.addChild(output);
                commandsOutput.addChild(commandOutput);

            }
            return commandsOutput;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            session.close();
            client.disconnect();
        }
    }
}
