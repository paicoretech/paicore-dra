/*
 * Extended Diameter Routing Agent
 * Copyright (C) 2019-2026 PAiCore Technology
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.paic.esg.configuration.controller;

import com.paic.esg.configuration.dto.Configuration;
import com.paic.esg.configuration.dto.Peer;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import java.io.File;
import java.io.IOException;

public class ConfigurationController {

    public static final String RESOURCE_FILE = "../conf/diameter-server.xml";
    private Configuration mainConfig;

    public Object getDeserializedResource() throws IOException {
        try {
            File fileResource = new File(RESOURCE_FILE);
            JAXBContext jaxbContext = JAXBContext.newInstance(Configuration.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            return jaxbUnmarshaller.unmarshal(fileResource);
        } catch (JAXBException jex) {
            System.out.println("Could not parse XML file due to: " + jex.getMessage());
            return null;
        }
    }

    public Configuration getConfiguration() {
        try {
            Configuration configuration = (Configuration) getDeserializedResource();
            this.mainConfig = configuration;
            return configuration;

        } catch (IOException e) {
            System.out.println("An error has occurred while attempting to perform I/O operations: " + e.getMessage());
            return null;
        } catch (Exception ex) {
            System.out.println("An exception was thrown while attempting to get the destination number, exception is: " + ex.getMessage());
            return null;
        }
    }

    public boolean addNewPeer(Peer peer) {
        try {
            getConfiguration();
            this.mainConfig.getNetwork().getPeers().getPeerList().add(peer);

            JAXBContext jc = JAXBContext.newInstance(Configuration.class);

            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            File fileResource = new File(RESOURCE_FILE);

            marshaller.marshal(this.mainConfig, fileResource);
            return true;
        } catch (JAXBException jax) {
            return false;
        }
    }


    public boolean modifyPeer(int peerIndex, Peer peer) {
        try {
            getConfiguration();
            Peer selectedPeer = this.mainConfig.getNetwork().getPeers().getPeerList().get(peerIndex);

            selectedPeer.setName(peer.getName());
            selectedPeer.setRating(peer.getRating());
            selectedPeer.setAttemptConnect(peer.isAttemptConnect());
            selectedPeer.setIp(peer.getIp());
            selectedPeer.setStandbyAddresses(peer.getStandbyAddresses());


            JAXBContext jc = JAXBContext.newInstance(Configuration.class);

            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            File fileResource = new File(RESOURCE_FILE);

            marshaller.marshal(this.mainConfig, fileResource);
            return true;
        } catch (JAXBException jax) {
            return false;
        }
    }

    public boolean remove(int peerIndex) {
        try {
            getConfiguration();
            this.mainConfig.getNetwork().getPeers().getPeerList().remove(peerIndex - 1);
            JAXBContext jc = JAXBContext.newInstance(Configuration.class);
            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            File fileResource = new File(RESOURCE_FILE);
            marshaller.marshal(this.mainConfig, fileResource);

            return true;

        } catch (Exception ex) {
            return false;
        }
    }

    public String getLocalRealm() {
        getConfiguration();
        return this.mainConfig.getLocalPeer().getRealm().getValue();
    }
}