package com.windward.att;

import com.realops.foundation.adapterframework.configuration.AdapterConfigurationException;
import com.realops.foundation.adapterframework.configuration.BaseAdapterConfiguration;

import java.util.Hashtable;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: aglover
 * Date: 10/22/13
 * Time: 1:39 PM
 */
public class CustomSshActorConfiguration extends BaseAdapterConfiguration {
    public CustomSshActorConfiguration(String adapterId) {
        super(adapterId);
    }

    public CustomSshActorConfiguration(String id, Hashtable defaults) {
        super(id, defaults);
    }

    public CustomSshActorConfiguration(String id, Hashtable defaults, Set validKeys, Set requiredKeys) throws AdapterConfigurationException {
        super(id, defaults, validKeys, requiredKeys);
    }
}
