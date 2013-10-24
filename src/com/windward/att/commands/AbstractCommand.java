package com.windward.att.commands;

import com.realops.common.enumeration.Status;
import com.realops.common.xml.XML;
import com.realops.foundation.adapterframework.AdapterRequest;
import com.realops.foundation.adapterframework.AdapterResponse;
import com.realops.foundation.adapterframework.configuration.BaseAdapterConfiguration;
import com.windward.att.CustomSshActorConfiguration;
import net.schmizz.sshj.SSHClient;

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
}
