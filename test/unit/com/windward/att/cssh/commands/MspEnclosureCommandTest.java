package com.windward.att.cssh.commands;

import com.realops.common.enumeration.Status;
import com.realops.common.xml.XML;
import com.realops.foundation.adapterframework.AdapterRequest;
import com.realops.foundation.adapterframework.AdapterResponse;
import com.windward.att.CustomSshActor;
import com.windward.att.CustomSshActorConfiguration;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: cs7866
 * Date: 10/25/13
 * Time: 9:48 AM
 * To change this template use File | Settings | File Templates.
 */
public class MspEnclosureCommandTest  {
//            @Test
    public void testMSPConnection() throws Exception{
        CustomSshActor adaptor = new CustomSshActor();
        AdapterResponse adapterResponse = adaptor.performAction(new AdapterRequest(XML.read("etc/test-msp-connect.xml")));
        Assert.assertEquals(adapterResponse.getExecutionStatus(), Status.SUCCESS);

//        System.out.println(adapterResponse.getMessage());
        System.out.println(adapterResponse.getExecutionStatus());
        System.out.println(adapterResponse.getData().toPrettyString());
    }

    @Test
    public void testMSPShowEnclsoure() throws Exception{
        CustomSshActor adaptor = new CustomSshActor();
        AdapterResponse adapterResponse = adaptor.performAction(new AdapterRequest(XML.read("etc/test-msp-show-enclosure.xml")));
        Assert.assertEquals(adapterResponse.getExecutionStatus(), Status.SUCCESS);

        System.out.println(adapterResponse.getMessage());
        System.out.println(adapterResponse.getExecutionStatus());
        System.out.println(adapterResponse.getData().toPrettyString());
    }
}
