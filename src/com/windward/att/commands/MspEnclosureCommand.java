package com.windward.att.commands;

import com.realops.common.xml.XML;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.PublicKey;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Chris Schroeder
 * Date: 10/25/13
 * Time: 9:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class MspEnclosureCommand extends AbstractCommand  {
    private static Logger LOGGER = Logger.getLogger(MspEnclosureCommand.class);


    @Override
    public XML executeCommand(XML requestXML) throws Exception {
        SSHClient client = this.clientFromXML(requestXML.getChild("jump-host"));
        final Session session = client.startSession();
        XML commandsOutput = new XML("commands-output");
        try {
            session.allocateDefaultPTY();
            Session.Shell shell = session.startShell();
            LOGGER.error("Started session");
            OutputStream ops = shell.getOutputStream();
            PrintStream ps = new PrintStream(ops, true);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(shell.getInputStream()));
            ArrayList<String> prompts = new ArrayList<String>(4);
            XML enclosure = requestXML.getChild("enclosure");
            String enclosureName = enclosure.getChild("name").getText();
            prompts.add("Escape character is '^]'.");      // 0
            prompts.add(enclosureName + " login:");   // 1
            prompts.add("Password:");                // 2
            String enclosurePrompt = enclosureName + ">";
            prompts.add(enclosurePrompt);         // 3
            LOGGER.error("added prompts");

            XML terminalServer = requestXML.getChild("terminal-server");
            int promptFound = -1;
            int attempts = 0;
            do {
                if (attempts>0)
                  Thread.sleep(2000);
                LOGGER.error("Starting telnet");
                ps.println("telnet " + terminalServer.getChild("host").getText() + " " + terminalServer.getChild("port").getText());
                LOGGER.error("Sent, now waiting for prompt");
                promptFound =  readUntilPromptFound(bufferedReader, prompts);
                LOGGER.error("Got promptFound: "+promptFound);
                attempts++;
            } while (promptFound<0 && attempts<15);

            boolean loggedIn = false;
            do {

                switch (promptFound)    {
                    case 0:
                        ps.println();
                        break;
                    case 1:
                        ps.println(enclosure.getChild("username").getText().trim());
                        break;
                    case 2:
                        ps.println(enclosure.getChild("password").getText().trim());
                        loggedIn = true;
                        break;
                    case 3:
                        if (loggedIn){
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

                                if (command.getBooleanAttribute("read-enclosure-prompt"))
                                    readUntilPromptFound(bufferedReader,enclosurePrompt);

                                boolean found = false;
                                if (prompt!=null && prompt.length()>0)
                                    found = readUntilPromptFound(bufferedReader, prompt, output);
                                else
                                    found = readUntilPromptFound(bufferedReader, enclosurePrompt, output);

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

                            promptFound = -1;           // bail out of the while loop
                        } else {
                            ps.println("exit");
                        }
                        break;
                }
                promptFound = readUntilPromptFound(bufferedReader, prompts);
                LOGGER.error("Got promptFound: "+promptFound);
            } while (promptFound>=0);

            ps.println("exit"); // Exit out of the enclosure prompt
            this.sendControlCharacter(ps, "]");
            ps.println("quit"); // Quit out of the terminal session.
            if (!loggedIn)
                throw new Exception("Unable to log into enclosure.");
            return commandsOutput;
        } catch (Exception e) {
           throw e;
        } finally {
            session.close();
            client.disconnect();
        }
    }

}
