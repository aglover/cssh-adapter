package com.windward.att;

import com.realops.common.enumeration.StateEnum;
import com.realops.common.enumeration.Status;
import com.realops.common.xml.XML;
import com.realops.foundation.adapterframework.AbstractActorAdapter;
import com.realops.foundation.adapterframework.AdapterException;
import com.realops.foundation.adapterframework.AdapterRequest;
import com.realops.foundation.adapterframework.AdapterResponse;
import com.windward.att.commands.AbstractCommand;
import net.schmizz.sshj.SSHClient;
import org.apache.commons.lang.WordUtils;
import org.apache.log4j.Logger;

import java.util.Date;

import static java.lang.Class.forName;
import static org.apache.commons.lang.WordUtils.capitalizeFully;

/**
 * Created with IntelliJ IDEA.
 * User: aglover
 * Date: 10/22/13
 * Time: 1:38 PM
 */
public class CustomSshActor extends AbstractActorAdapter {

    private SSHClient sshClient;
    private static Logger LOGGER = Logger.getLogger(CustomSshActor.class);

    public void setSshClient(SSHClient client) {
        this.sshClient = client;
    }

    private SSHClient sshClientInstance() {
        return (this.sshClient == null) ? new SSHClient() : this.sshClient;
    }

    @Override
    public AdapterResponse performAction(AdapterRequest adapterRequest) throws AdapterException, InterruptedException {
        long startTime = new Date().getTime();
        try {
            LOGGER.error("Starting command: " + adapterRequest.getAction());
            AbstractCommand cmd = newCommand(adapterRequest);
            cmd.setSshClient(sshClientInstance());
            cmd.setConfig(this.getConfiguration());
            AdapterResponse adapterResponse = cmd.execute(adapterRequest);
            LOGGER.error("Returning with response: " +adapterResponse.getData().toPrettyString());
            return adapterResponse;
        } catch (Exception e) {
            long duration = new Date().getTime()- startTime;
            XML response = new XML("response");
            response.addChild("status").setText(Status.ERROR.toString());
            response.addChild("message").setText(e.getLocalizedMessage());
            return new AdapterResponse(duration, Status.ERROR.toString() ,response, Status.SUCCESS);
        }
    }

    private AbstractCommand newCommand(AdapterRequest adapterRequest) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return (AbstractCommand) forName(getCmdClassName(adapterRequest)).newInstance();
    }

    private String getCmdClassName(AdapterRequest adapterRequest) {
        return "com.windward.att.commands." + capitalizeFully(adapterRequest.getAction(), new char[]{'_'}).replaceAll("_", "") + "Command";
    }

    @Override
    public void shutdown() throws AdapterException {
        setState(StateEnum.STOPPED);
    }
}
