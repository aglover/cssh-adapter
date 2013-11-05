package com.windward.att.cssh.commands;

import com.realops.common.enumeration.Status;
import com.realops.common.xml.XML;
import com.realops.foundation.adapterframework.AdapterRequest;
import com.realops.foundation.adapterframework.AdapterResponse;
import com.windward.att.CustomSshActor;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: cs7866
 * Date: 11/5/13
 * Time: 9:09 AM
 * To change this template use File | Settings | File Templates.
 */
public class MspEnclosureConnectTest {
    @Test
    public void testMSPConnection() throws Exception{
        CustomSshActor adaptor = new CustomSshActor();
        AdapterResponse adapterResponse = adaptor.performAction(new AdapterRequest(XML.read("etc/test-msp-connect.xml")));
        Assert.assertEquals(adapterResponse.getExecutionStatus(), Status.SUCCESS);

        System.out.println(adapterResponse.getMessage());
        System.out.println(adapterResponse.getExecutionStatus());
        System.out.println(adapterResponse.getData().toPrettyString());
    }
}
