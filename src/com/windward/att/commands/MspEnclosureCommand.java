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
    private XML lines=null;
    @Override
    public XML executeCommand(XML requestXML) throws Exception {
        SSHClient client = sshClientInstance();
        client.addHostKeyVerifier(new HostKeyVerifier() {
            @Override
            public boolean verify(String s, int i, PublicKey publicKey) {
                return true;  //To change body of implemented methods use File | Settings | File Templates.
            }
        });

        client.connect("10.174.223.254",22);
        client.authPassword("cs7866", "1Changeme");
        System.out.println("Staring session");
        final Session session = client.startSession();
        System.out.println("Started");
        lines = new XML("lines");
        try {
            session.allocateDefaultPTY();
            Session.Shell shell = session.startShell();

            OutputStream ops = shell.getOutputStream();
            PrintStream ps = new PrintStream(ops, true);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(shell.getInputStream()));

            ps.println("telnet psloats02 2271");
            if (readUntilPromptFound(bufferedReader, "Escape character is")){
                ps.println();
                if (readUntilPromptFound(bufferedReader, "PSLNZ01MSP2oa01 login:")) {
                    ps.println("Administrator");
                    if (readUntilPromptFound(bufferedReader, "Password:")) {
                        ps.println("changeme");
                        if (readUntilPromptFound(bufferedReader, "PSLNZ01MSP2oa01>")) {
                            readUntilPromptFound(bufferedReader, "PSLNZ01MSP2oa01>");
                            ps.println("show enclosure info");
                            readUntilPromptFound(bufferedReader, "PSLNZ01MSP2oa01>", true);
                        } else {
                            System.out.println("Couldn't find enclosure prompt");
                        }
                    } else {
                        System.out.println("Couldn't find password prompt");
                    }
                } else {
                    System.out.println("Couldn't find login prompt");
                }
            } else {
                System.out.println("Couldn't find tty prompt");
            }

            System.out.println("Done!!");
            ps.println(new byte[]{29}); //what is CTRL-]?
            ps.println("quit");
            return lines;
        } finally {
            session.close();
            client.disconnect();
        }
    }
    private boolean readUntilPromptFound(BufferedReader bufferedReader, String prompt){
        ArrayList<String> prompts = new ArrayList<String>(1);
        prompts.add(prompt);
        return  0==readUntilPromptFound(bufferedReader, prompts, 100000, false);
    }
    private boolean readUntilPromptFound(BufferedReader bufferedReader, String prompt, boolean captureOutput){
        ArrayList<String> prompts = new ArrayList<String>(1);
        prompts.add(prompt);
        return  0==readUntilPromptFound(bufferedReader, prompts, 100000, captureOutput);
    }

   private int readUntilPromptFound( BufferedReader bufferedReader, ArrayList<String> prompts, long timeout, boolean captureOutput){
        int found = -1;
        long finishTime = System.currentTimeMillis()+timeout;
       StringBuilder buf = new StringBuilder();

        try {
            String line = null;
            while ( (found==-1) && (System.currentTimeMillis() <= finishTime)){
                if (bufferedReader.ready()){
                    int character = bufferedReader.read();
                    switch (character){
                        case 10:
                        case 13:
                            if (captureOutput && line!=null && line.length()>0)
                                lines.addChild(new XML("line").setText(line));
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
