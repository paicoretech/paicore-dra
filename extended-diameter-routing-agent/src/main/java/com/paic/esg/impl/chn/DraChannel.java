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

package com.paic.esg.impl.chn;

import com.paic.esg.api.chn.ChannelMessage;
import com.paic.esg.api.network.LayerInterface;
import com.paic.esg.impl.app.handler.RequestHandler;
import com.paic.esg.impl.db.DataSource;
import com.paic.esg.impl.db.entity.ApplicationId;
import com.paic.esg.impl.db.entity.Realm;
import com.paic.esg.impl.db.repository.RealmRepository;
import com.paic.esg.impl.settings.ChannelSettings;
import com.paic.esg.network.layers.DiameterLayer;
import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.Message;
import org.jdiameter.api.OverloadException;
import org.jdiameter.api.RouteException;
import org.jdiameter.api.Session;
import org.jdiameter.client.api.controller.IRealm;
import org.jdiameter.client.impl.controller.RealmImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class DraChannel extends ChannelHandler {

    public static final int FAILED_RESPONSE = -2;
    public static final int NETWORK_ERROR = -1;
    public static final int SUCCESSFUL_RESPONSE = 0;

    private final RealmRepository repository;

    private static final Logger logger = LoggerFactory.getLogger(DraChannel.class);

    DiameterLayer diameter = null;

    public DraChannel(ChannelSettings channelSetting) {
        super(channelSetting);
        repository = DataSource.initialize();
    }

    @Override
    public void channelInitialize(LayerInterface[] layerInterfaces) {
        diameter = (DiameterLayer) layerInterfaces[0];
        RequestHandler.initialize(diameter);
        try {
            List<Realm> realms = repository.findAll();

            diameter.addRealms(realms.stream()
                    .map(realm -> new RealmImpl(realm.getName(), realm.getDiameterApplicationId(), realm.getLocalAction(),
                            null, null, realm.getDynamic(), realm.getExpTime(), realm.getPeers()))
                    .collect(Collectors.toList()).toArray(new IRealm[]{}));
        } catch (Exception e) {
            logger.error("Error loading realms", e);
        }
    }

    @Override
    public LayerInterface getLayerInterface(String s) {
        return null;
    }

    @Override
    public LayerInterface getLayerInterface() {
        return diameter;
    }

    @Override
    public void receiveMessageRequest(ChannelMessage channelMessage) {
        // STEP 1: message to be received from reference point (map, cap, diameter, ...)
        logger.info("Sending message '{}' to application.", channelMessage);
        sendMessageRequest(channelMessage);
    }

    @Override
    public int sendMessageResponse(ChannelMessage channelMessage) {
        // STEP 2: message to be replied over reference point (ss7, diameter, ...)
        try {
            Boolean useRealm = true;
            Message message = (Message) channelMessage.getParameter("FORWARDING");
            Boolean routeRecord = (Boolean) channelMessage.getParameter("ROUTE_RECORD");
            if (channelMessage.getParameter("USE_REALM") != null) {
                useRealm = (Boolean) channelMessage.getParameter("USE_REALM");
            }

            Session newSession = diameter.getSession(message.getSessionId());
            if (newSession != null) {
                try {
                    diameter.sendMessage(newSession, message, routeRecord, useRealm);
                } catch (InternalException | IllegalDiameterStateException | RouteException | OverloadException e) {
                    final int result = (e instanceof RouteException) ? NETWORK_ERROR : FAILED_RESPONSE;
                    logger.warn(String.format("Caught exception '%s' while sending response for sessionId [%s], returning %s.",
                            e.getMessage(), message.getSessionId(), result == NETWORK_ERROR ? "NETWORK_ERROR" : "FAILED_RESPONSE"), e);
                    return result;
                }
            }
            logger.info("Message {} sent to diameter.",  message.getSessionId());
        } catch (Exception e) {
            logger.error("Caught exception '{}' while sending response for transactionId [{}], returning FAILED_RESPONSE.", e.getMessage(), channelMessage.getTransactionId());
            return FAILED_RESPONSE;
        }

        return SUCCESSFUL_RESPONSE;
    }

    @Override
    public void onReceiveUnknownRealm(IRealm unknownRealm) {

        try {
            Realm realm = new Realm();
            realm.setName(unknownRealm.getName());
            realm.setPeers(unknownRealm.getPeerNames());
            realm.setLocalAction(unknownRealm.getLocalAction().name());
            realm.setDynamic(unknownRealm.isDynamic());
            realm.setExpTime(unknownRealm.getExpirationTime());
            ApplicationId applicationId = new ApplicationId();
            applicationId.setVendorId(unknownRealm.getApplicationId().getVendorId());
            applicationId.setAuthApplId(unknownRealm.getApplicationId().getAuthAppId());
            applicationId.setAcctApplId(unknownRealm.getApplicationId().getAcctAppId());
            realm.setApplicationId(applicationId);
            repository.saveRealm(realm);
        } catch (Exception e) {
            logger.warn("Exception caught", e);
        }

    }

}