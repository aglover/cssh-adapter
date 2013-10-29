package com.windward.att.commands;

import com.realops.common.xml.XML;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;

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

    protected SSHClient clientFromXML(XML clientXML) throws Exception{
        SSHClient client = sshClientInstance();
        client.addHostKeyVerifier(new HostKeyVerifier() {
            @Override
            public boolean verify(String s, int i, PublicKey publicKey) {
                return true;  // Allow any host, don't verify the key.
            }
        });
        String num = clientXML.getChild("port").getText();
        int port = 22;
        if (num!=null)
            port = Integer.parseInt(num);


        client.connect(clientXML.getChild("host").getText(), port);
        client.authPassword(clientXML.getChild("username").getText(), clientXML.getChild("password").getText());
        return client;
    }
    @Override
    public XML executeCommand(XML requestXML) throws Exception {
        SSHClient client = this.clientFromXML(requestXML.getChild("jump-host"));
        final Session session = client.startSession();
        XML commandsOutput = new XML("commands-output");
        try {
            session.allocateDefaultPTY();
            Session.Shell shell = session.startShell();

            OutputStream ops = shell.getOutputStream();
            PrintStream ps = new PrintStream(ops, true);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(shell.getInputStream()));
            ArrayList<String> prompts = new ArrayList<String>(6);
            XML enclosure = requestXML.getChild("enclosure");
            String enclosureName = enclosure.getChild("name").getText();
            prompts.add("Escape character is");      // 0
            prompts.add(enclosureName + " login:");   // 1
            prompts.add("Password:");                // 2
            String enclosurePrompt = enclosureName + ">";
            prompts.add(enclosurePrompt);         // 3

            XML terminalServer = requestXML.getChild("terminal-server");
            ps.println("telnet " + terminalServer.getChild("host").getText() + " " + terminalServer.getChild("port").getText());
            int promptFound = readUntilPromptFound(bufferedReader, prompts);;
            boolean loggedIn = false;
            do {
                promptFound = readUntilPromptFound(bufferedReader, prompts);
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
                                readUntilPromptFound(bufferedReader, enclosurePrompt);
                                ps.println(command.getText());
                                XML output = new XML("output");
                                long startTime = System.currentTimeMillis();
                                boolean found = readUntilPromptFound(bufferedReader, enclosurePrompt, output);
                                long duration = System.currentTimeMillis() - startTime;

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
            } while (promptFound>=0);
            ps.println("exit"); // Exit out of the enclousre prompt
            ps.println(new byte[]{29}); //what is CTRL-]?
            ps.println("quit"); // Quit out of the terminal session.
            return commandsOutput;
        } finally {
            session.close();
            client.disconnect();
        }
    }
    private boolean readUntilPromptFound(BufferedReader bufferedReader, String prompt){
        ArrayList<String> prompts = new ArrayList<String>(1);
        prompts.add(prompt);
        return  0==readUntilPromptFound(bufferedReader, prompts, 10000, null);
    }
    private boolean readUntilPromptFound(BufferedReader bufferedReader, String prompt, XML output){
        ArrayList<String> prompts = new ArrayList<String>(1);
        prompts.add(prompt);
        return  0==readUntilPromptFound(bufferedReader, prompts, 10000, output);
    }
   private int readUntilPromptFound( BufferedReader bufferedReader, ArrayList<String> prompts){
       return readUntilPromptFound(bufferedReader, prompts, 10000, null);
   }
   private int readUntilPromptFound( BufferedReader bufferedReader, ArrayList<String> prompts, long timeout, XML output){
        int found = -1;
        long finishTime = System.currentTimeMillis()+timeout;
       StringBuilder buf = new StringBuilder();
       int lineIndex = 0;
        try {
            String line = null;
            while ( (found==-1) && (System.currentTimeMillis() <= finishTime)){
                if (bufferedReader.ready()){
                    int character = bufferedReader.read();
                    switch (character){
                        case 10:
                        case 13:
                            if (output!=null && line!=null && line.length()>0)   {
                                XML lineXML = new XML("line");
                                lineXML.setAttribute("index",lineIndex);
                                lineXML.setText(line);
                                output.addChild(lineXML);
                                lineIndex++;
                            }
                            line = null;
                            buf.delete(0, buf.length());
                            // clear out buffer array
                            break;
                        default:
                            buf.append((char)character);
                            line = buf.toString();
                            for (int i=0;i<prompts.size();i++){
                                if (line.startsWith(prompts.get(i))){
                                    found = i;
                                }
                            }

                    }
                }
            }
        } catch (Exception e){
            e.printStackTrace();
            System.out.println(e.getLocalizedMessage());
        }
        return found;
    }
}
