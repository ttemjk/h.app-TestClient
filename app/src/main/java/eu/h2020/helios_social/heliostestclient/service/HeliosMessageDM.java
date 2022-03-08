package eu.h2020.helios_social.heliostestclient.service;

import eu.h2020.helios_social.core.messaging.HeliosMessage;
import eu.h2020.helios_social.core.messaging.HeliosNetworkAddress;

class HeliosMessageDM extends HeliosMessage {
    private HeliosNetworkAddress networkAddress;

    public HeliosMessageDM(String msg, String mediaFileName, HeliosNetworkAddress networkAddress) {
        super(msg, mediaFileName);
        this.networkAddress = networkAddress;
    }

    public HeliosNetworkAddress getNetworkAddress() {
        return networkAddress;
    }
}
