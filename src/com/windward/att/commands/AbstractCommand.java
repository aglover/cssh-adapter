package com.windward.att.commands;

import com.realops.common.enumeration.Status;
import com.realops.common.xml.XML;
import com.realops.foundation.adapterframework.AdapterRequest;
import com.realops.foundation.adapterframework.AdapterResponse;
import com.realops.foundation.adapterframework.configuration.BaseAdapterConfiguration;
import com.windward.att.CustomSshActorConfiguration;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;

import java.io.BufferedReader;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: aglover
 * Date: 10/23/13
 * Time: 2:20 PM
 */
public abstract class AbstractCommand {
    private SSHClient sshClient;
    private CustomSshActorConfiguration config;

    public void setConfig(BaseAdapterConfiguration aConfig) {
        if (aConfig instanceof CustomSshActorConfiguration) {
            this.config = (CustomSshActorConfiguration) aConfig;
        }
    }

    public void setSshClient(SSHClient client) {
        this.sshClient = client;
    }

    protected SSHClient sshClientInstance() {
        return (this.sshClient == null) ? new SSHClient() : this.sshClient;
    }

    /**
     * Used by the factory to execute the command. This keeps track of execution time and
     * handles all exceptions thrown by the commands. Appropriately wrapping the response
     * in an adaptor response object.
     *
     * @param adapterRequest - the request made by the workflow
     * @return AdapterResponse - the response handed back to the adapter manager.
     */
    public AdapterResponse execute(AdapterRequest adapterRequest) {
        long startTime = new Date().getTime();
        try {
            XML data = this.executeCommand(adapterRequest.getData());
            long duration = new Date().getTime() - startTime;
            XML response = new XML("response");
            response.addChild("status").setText(Status.SUCCESS.toString());
            response.addChild("message").setText("Command successful");
            response.addChild(data);
            return new AdapterResponse(duration, Status.SUCCESS.toString(), response, Status.SUCCESS);
        } catch (Exception e) {
            long duration = new Date().getTime() - startTime;
            XML response = new XML("response");
            response.addChild("status").setText(Status.ERROR.toString());
            response.addChild("message").setText(e.getLocalizedMessage());
            // If we return Status.ERROR here, the workflow engine will throw a compensation, which stops
            // the workflow. So we need to return an adapter response of success, and a mesage and let
            // the workflow deal with the response.
            return new AdapterResponse(duration, Status.ERROR.toString(), response, Status.SUCCESS);
        }
    }

    /**
     * Abstract method for the concrete class to implement the command.
     *
     * @param XML the XML provided by the adapter cal in workflow
     * @return XML Object to return in the data portion of the adatper response.
     * @throws Exception
     */
    public abstract XML executeCommand(XML requestXML) throws Exception;


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

    protected boolean readUntilPromptFound(BufferedReader bufferedReader, String prompt){
        ArrayList<String> prompts = new ArrayList<String>(1);
        prompts.add(prompt);
        return  0==readUntilPromptFound(bufferedReader, prompts, 10000, null);
    }
    protected boolean readUntilPromptFound(BufferedReader bufferedReader, String prompt, XML output){
        ArrayList<String> prompts = new ArrayList<String>(1);
        prompts.add(prompt);
        return  0==readUntilPromptFound(bufferedReader, prompts, 10000, output);
    }
    protected int readUntilPromptFound( BufferedReader bufferedReader, ArrayList<String> prompts){
        return readUntilPromptFound(bufferedReader, prompts, 10000, null);
    }
    protected int readUntilPromptFound( BufferedReader bufferedReader, ArrayList<String> prompts, long timeout, XML output){
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
