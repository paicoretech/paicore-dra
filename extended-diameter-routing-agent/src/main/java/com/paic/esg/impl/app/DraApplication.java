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

package com.paic.esg.impl.app;

import com.paic.esg.impl.app.handler.RequestHandler;
import com.paic.esg.impl.settings.ApplicationSettings;
import com.paic.esg.impl.app.util.GettingRules;
import com.paic.esg.api.chn.ChannelMessage;
import com.paic.esg.impl.app.model.Host;
import com.paic.esg.impl.app.model.Rule;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.ResultCode;
import org.jdiameter.api.Request;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.Answer;
import org.jdiameter.api.Avp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.paic.esg.impl.chn.DraChannel.FAILED_RESPONSE;
import static com.paic.esg.impl.chn.DraChannel.NETWORK_ERROR;
import static com.paic.esg.impl.chn.DraChannel.SUCCESSFUL_RESPONSE;

public class DraApplication extends Application {

    private static final Logger logger = LoggerFactory.getLogger(DraApplication.class);


    public DraApplication(ApplicationSettings applicationSettings) {
        super(applicationSettings);
    }

    @Override
    public void processMessage(ChannelMessage channelMessage) {
        String originId = channelMessage.getOriginId();
        if ("DMR".equals(originId)) {
            RequestHandler.addRequest((Request) channelMessage.getParameter("REQUEST"));
            processRequestMessage(channelMessage);
        } else if ("DMA".equals(originId)) {
            Request dma = (Request) channelMessage.getParameter("REQUEST");
            channelMessage.setParameter("REQUEST", RequestHandler.getRequest(dma.getSessionId(), dma.getEndToEndIdentifier()));
            processAnswerMessage(channelMessage);
        }
    }

    private void processRequestMessage(ChannelMessage channelMessage) {
        List<String> previousHost = new ArrayList<>();
        try {
            Request request = (Request) channelMessage.getParameter("REQUEST");
            long applicationId = request.getApplicationId();
            logger.info(String.format("Processing request with sessionId '%s' using transactionId '%s' from originId '%s'",
                    request.getSessionId(), channelMessage.getTransactionId(), channelMessage.getOriginId()));

            // Create request and configure avps
            //Request msg = new MessageParser().createEmptyMessage(request.getCommandCode(), request.getApplicationId());
            Answer answer = request.createAnswer();
            answer.setRequest(true);
            answer.setProxiable(true);

            AvpSet avps = request.getAvps();
            avps.removeAvp(Avp.AUTH_APPLICATION_ID);
            avps.removeAvp(Avp.VENDOR_SPECIFIC_APPLICATION_ID);
            answer.getAvps().removeAvp(Avp.SESSION_ID);
            answer.getAvps().addAvp(avps);
            Rule rule = getRule(avps, applicationId);
            if (rule != null) {
                boolean useRealmToRoute = rule.getFallbackPolicy() == null || rule.getFallbackPolicy().isEmpty();
                while (true) {
                    Host host = getRoutingHost(rule, previousHost);
                    if (host == null || previousHost.contains(host.getName())) {
                        // verify if exist default rule.
                        Rule defaultRule = getDefaultRule();
                        if (defaultRule != null) {
                            previousHost.clear();
                            rule = defaultRule;
                            continue;
                        }
                        String imsi = getImsi(rule, avps);
                        logger.info(String.format("No routing host with the following data: rule '%s' imsi '%s'",
                                rule.getName(), imsi));

                        // drop-policy  // no-routing
                        processDropPolicy(channelMessage, request, "no-routing");
                        break;
                    }
                    host.incrementSentMessages();

                    if (host.getRouteRecord())
                        channelMessage.setParameter("ROUTE_RECORD", true);

                    // Realm Replacement; Host Replacement
                    if (host.getRealm() != null && !"".equals(host.getRealm().trim())) {
                        answer.getAvps().removeAvp(Avp.DESTINATION_REALM);
                        answer.getAvps().addAvp(Avp.DESTINATION_REALM, host.getRealm().getBytes());
                    }

                    if (host.getAddress() != null && !"".equals(host.getAddress().trim())) {
                        if (host.isReplaceHost()) {
                            answer.getAvps().removeAvp(Avp.DESTINATION_HOST);
                            answer.getAvps().addAvp(Avp.DESTINATION_HOST, host.getAddress().getBytes());
                        } else {
                            answer.getAvps().addAvp(999, host.getAddress().getBytes());
                        }
                    }
                    if (host.getOriginHost() != null && !"".equals(host.getOriginHost().trim())) {
                        answer.getAvps().removeAvp(Avp.ORIGIN_HOST);
                        answer.getAvps().addAvp(Avp.ORIGIN_HOST, host.getOriginHost().getBytes());
                    }
                    if (host.getOriginRealm() != null && !"".equals(host.getOriginRealm().trim())) {
                        answer.getAvps().removeAvp(Avp.ORIGIN_REALM);
                        answer.getAvps().addAvp(Avp.ORIGIN_REALM, host.getOriginRealm().getBytes());
                    }
                    if (host.getDestinationHost() != null && !"".equals(host.getDestinationHost().trim())) {
                        answer.getAvps().removeAvp(Avp.DESTINATION_HOST);
                        answer.getAvps().addAvp(Avp.DESTINATION_HOST, host.getDestinationHost().getBytes());
                    }
                    if (host.getDestinationRealm() != null && !"".equals(host.getDestinationRealm().trim())) {
                        answer.getAvps().removeAvp(Avp.DESTINATION_REALM);
                        answer.getAvps().addAvp(Avp.DESTINATION_REALM, host.getDestinationRealm().getBytes());
                    }
                    channelMessage.setParameter("FORWARDING", answer);
                    channelMessage.setParameter("END_SESSION", false);
                    //This param is use to route using realm in case that the peer isn't available and isn't has a FallbackPolicy
                    channelMessage.setParameter("USE_REALM", useRealmToRoute);
                    int response = channelHandler.sendMessageResponse(channelMessage);
                    logger.info(String.format("Send message response result '%d' for sessionId '%s'", response, answer.getSessionId()));
                    if (response == SUCCESSFUL_RESPONSE) {
                        break;
                    } else if (rule.getFallbackPolicy().contains((response == NETWORK_ERROR ? "network" : response == FAILED_RESPONSE ? "error" : "unknown"))) {
                        // fallback-policy
                        previousHost.add(host.getName());
                    } else {
                        //no-fallback-policy
                        rule.getFallbackPolicy().forEach(value -> {
                            // TODO this should be checked on loading just once
                            if (!Arrays.asList("error", "network").contains(value)) {
                                logger.warn(String.format("The value '%s' of the fallback-policy attribute is not valid", value));
                            }
                        });
                        break;
                    }
                }
            } else {
                // drop-policy
                processDropPolicy(channelMessage, request, "no-match");
            }

        } catch (AvpDataException ex) {
            // TODO consider all the potential exception thrown or propagater over protected code - error is too generic to find something out of it
            logger.error("Error in getting AVP", ex);
        }
    }

    private void processDropPolicy(ChannelMessage channelMessage, Request request, String policy) {
        if (!GettingRules.rules().dropPolicyEnabled(policy)) {
            Answer answerPolicy = request.createAnswer(ResultCode.UNABLE_TO_COMPLY);
            channelMessage.setParameter("FORWARDING", answerPolicy);
            channelMessage.setParameter("END_SESSION", true);
            channelHandler.sendMessageResponse(channelMessage);
        } else {
            logger.warn(String.format("Omitting request message '%s' for transactionId '%s' from origin '%s' due to no matching rule found",
                    request.getSessionId(), channelMessage.getTransactionId(), channelMessage.getOriginId()));
        }
    }

    private void processAnswerMessage(ChannelMessage channelMessage) {
        Request request = null;
        Answer answer = null;

        try {
            request = (Request) channelMessage.getParameter("REQUEST");
            answer = (Answer) channelMessage.getParameter("ANSWER");
            logger.info(String.format("Processing answer with sessionId '%s' corresponding to request '%s' using transactionId '%s' from originId '%s'",
                    answer.getSessionId(), request.getSessionId(), channelMessage.getTransactionId(), channelMessage.getOriginId()));
            Answer forwardAnswer = request.createAnswer();
            if (forwardAnswer != null) {
                request.getAvps().removeAvp(Avp.AUTH_APPLICATION_ID);
                request.getAvps().removeAvp(Avp.VENDOR_SPECIFIC_APPLICATION_ID);
                forwardAnswer.getAvps().removeAvp(Avp.SESSION_ID);
                forwardAnswer.getAvps().addAvp(answer.getAvps());
                channelMessage.setParameter("ROUTE_RECORD", false);
                channelMessage.setParameter("FORWARDING", forwardAnswer);
                channelMessage.setParameter("END_SESSION", true);
                channelHandler.sendMessageResponse(channelMessage);
            } else {
                logger.error(String.format("Unable to create answer for transactionId '%s'", channelMessage.getTransactionId()));
            }
        } catch (Exception e) {
            logger.error(String.format("Exception '%s' caught while processing answer for transactionId '%s' from origin '%s', request '%s', answer '%s'",
                    e.getMessage(), channelMessage.getTransactionId(), channelMessage.getOriginId(),
                    ((request == null) ? "null" : request.getSessionId()), ((answer == null) ? "null" : answer.getSessionId())));
        }
    }

    public Rule getRule(AvpSet avps, long applicationId) throws AvpDataException {
        String originRealm = avps.getAvp(Avp.ORIGIN_REALM).getUTF8String();
        String originHost = avps.getAvp(Avp.ORIGIN_HOST).getUTF8String();
        String destinationRealm = avps.getAvp(Avp.DESTINATION_REALM).getUTF8String();
        Avp avpDestHost = avps.getAvp(Avp.DESTINATION_HOST);
        String destinationHost = avpDestHost != null ? avpDestHost.getUTF8String() : null;
        AtomicReference<String> currentImsi = new AtomicReference<>("");
        AtomicReference<String> currentMsisdn = new AtomicReference<>("");

        Optional<Rule> ruleOptional = GettingRules
                .rules().list().stream().filter(rule -> rule.isEnabled() && !rule.getDefault() && rule.match() != null).filter(rule -> {
                    boolean isMsisdnSecondFilter = false;
                    if (rule.match().isMsisdnSecondFilter() != null) {
                        isMsisdnSecondFilter = rule.match().isMsisdnSecondFilter();
                    }

                    String imsi_avp = getImsi(rule, avps);
                    currentImsi.set(imsi_avp);
                    boolean isImsiAttribute = false;
                    if (rule.match().imsi() != null) {
                        isImsiAttribute = !(rule.match().imsi().toString().isEmpty());
                    }

                    boolean isImsiPresent = false;
                    if (imsi_avp != null) {
                        isImsiPresent = !(imsi_avp.isEmpty());
                    }

                    String msisdn_avp = getMsisdn(rule, avps);
                    currentMsisdn.set(msisdn_avp);

                    boolean isMsisdnAttribute = false;
                    if (rule.match().msisdn() != null) {
                        isMsisdnAttribute = !(rule.match().msisdn().toString().isEmpty());
                    }

                    boolean isMsisdnPresent = false;
                    if (msisdn_avp != null) {
                        isMsisdnPresent = !(msisdn_avp.isEmpty());
                    }

                    boolean match = (rule.match().originHost().matcher(originHost).matches() && rule.match().originRealm().matcher(originRealm).matches()
                            && rule.match().destinationRealm().matcher(destinationRealm).matches()
                            && (destinationHost == null || rule.match().destinationHost().matcher(destinationHost).matches())
                            && rule.match().applicationId().matcher(Long.toString(applicationId)).matches());


                    boolean usingImsi = false;
                    boolean usingMsisdn = false;
                    boolean usingSecondFilter = false;
                    //check if the IMSI attribute exist
                    if (isImsiAttribute) {
                        if (isImsiPresent) {
                            match = match && rule.match().imsi().matcher((imsi_avp)).matches();
                            usingImsi = true;
                            if (!isMsisdnSecondFilter) {
                                if (isMsisdnAttribute) {
                                    if (isMsisdnPresent) {
                                        match = match && rule.match().msisdn().matcher((msisdn_avp)).matches();
                                        usingMsisdn = true;
                                    }

                                    else {
                                        match = false;
                                    }
                                }
                            }
                        } else {
                            if (isMsisdnSecondFilter) {
                                if (isMsisdnAttribute) {
                                    if (isMsisdnPresent) {
                                        usingSecondFilter = true;
                                        match = match && rule.match().msisdn().matcher((msisdn_avp)).matches();
                                    }
                                    else {
                                        match = false;
                                    }
                                }


                            } else {
                                match = false;
                            }
                        }
                    } else {
                        if (isMsisdnAttribute) {
                            if (isMsisdnPresent) {
                                usingMsisdn = true;
                                match = match && rule.match().msisdn().matcher((msisdn_avp)).matches();
                            } else {
                                match = false;
                            }

                        }
                    }

                    if (logger.isDebugEnabled()) {
                        if (match) {
                            if (usingSecondFilter) {
                                logger.debug(String.format("Match in rule [%s] with the following data: " +
                                                "using second filter " +
                                                "msisdn[%s] match msisdn[%s] " +
                                                "originHost[%s] match host[%s] originRealm[%s] match realm[%s] " +
                                                "destinationHost[%s] match host[%s] destinationRealm[%s] match realm[%s] " +
                                                "applicationId[%s] match applicationId[%s]",
                                        rule.getName(),
                                        msisdn_avp, (rule.match().msisdn() != null) ? rule.match().msisdn().toString() : "",
                                        originHost, rule.match().originHost().pattern(), originRealm, rule.match().originRealm().pattern(),
                                        destinationHost == null ? "" : destinationHost, rule.match().destinationHost().pattern(), destinationRealm, rule.match().destinationRealm().pattern(),
                                        applicationId, rule.match().applicationId().pattern()));
                            } else {
                                if (usingImsi && usingMsisdn) {
                                    logger.debug(String.format("Match in rule [%s] with the following data: " +
                                                    "imsi[%s] match imsi[%s] " +
                                                    "msisdn[%s] match msisdn[%s] " +
                                                    "originHost[%s] match host[%s] originRealm[%s] match realm[%s] " +
                                                    "destinationHost[%s] match host[%s] destinationRealm[%s] match realm[%s] " +
                                                    "applicationId[%s] match applicationId[%s]",
                                            rule.getName(),
                                            imsi_avp, (rule.match().imsi() != null) ? rule.match().imsi().toString() : "",
                                            msisdn_avp, (rule.match().msisdn() != null) ? rule.match().msisdn().toString() : "",
                                            originHost, rule.match().originHost().pattern(), originRealm, rule.match().originRealm().pattern(),
                                            destinationHost == null ? "" : destinationHost, rule.match().destinationHost().pattern(), destinationRealm, rule.match().destinationRealm().pattern(),
                                            applicationId, rule.match().applicationId().pattern()));
                                } else {
                                    if (usingImsi) {
                                        logger.debug(String.format("Match in rule [%s] with the following data: " +
                                                        "imsi[%s] match imsi[%s] " +
                                                        "originHost[%s] match host[%s] originRealm[%s] match realm[%s] " +
                                                        "destinationHost[%s] match host[%s] destinationRealm[%s] match realm[%s] " +
                                                        "applicationId[%s] match applicationId[%s]",
                                                rule.getName(),
                                                imsi_avp, (rule.match().imsi() != null) ? rule.match().imsi().toString() : "",
                                                originHost, rule.match().originHost().pattern(), originRealm, rule.match().originRealm().pattern(),
                                                destinationHost == null ? "" : destinationHost, rule.match().destinationHost().pattern(), destinationRealm, rule.match().destinationRealm().pattern(),
                                                applicationId, rule.match().applicationId().pattern()));
                                    } else {
                                        if (usingMsisdn) {
                                            logger.debug(String.format("Match in rule [%s] with the following data: " +
                                                            "msisdn[%s] match msisdn[%s] " +
                                                            "originHost[%s] match host[%s] originRealm[%s] match realm[%s] " +
                                                            "destinationHost[%s] match host[%s] destinationRealm[%s] match realm[%s] " +
                                                            "applicationId[%s] match applicationId[%s]",
                                                    rule.getName(),
                                                    msisdn_avp, (rule.match().msisdn() != null) ? rule.match().msisdn().toString() : "",
                                                    originHost, rule.match().originHost().pattern(), originRealm, rule.match().originRealm().pattern(),
                                                    destinationHost == null ? "" : destinationHost, rule.match().destinationHost().pattern(), destinationRealm, rule.match().destinationRealm().pattern(),
                                                    applicationId, rule.match().applicationId().pattern()));
                                        } else {
                                            logger.debug(String.format("Match in rule [%s] with the following data: " +
                                                            "originHost[%s] match host[%s] originRealm[%s] match realm[%s] " +
                                                            "destinationHost[%s] match host[%s] destinationRealm[%s] match realm[%s] " +
                                                            "applicationId[%s] match applicationId[%s]",
                                                    rule.getName(),
                                                    originHost, rule.match().originHost().pattern(), originRealm, rule.match().originRealm().pattern(),
                                                    destinationHost == null ? "" : destinationHost, rule.match().destinationHost().pattern(), destinationRealm, rule.match().destinationRealm().pattern(),
                                                    applicationId, rule.match().applicationId().pattern()));
                                        }
                                    }
                                }
                            }
                        } else {
                            logger.debug(String.format("Rule [%s] does not match", rule.getName()));
                        }
                    }
                    return match;
                })
                .findFirst();
        if (ruleOptional.isPresent()) {
            return ruleOptional.get();
        } else {
            // verify if exist default rule
            Rule defaultRule = getDefaultRule();
            if (defaultRule != null) return defaultRule;
        }

        if (!currentImsi.get().isEmpty()) {
            if (!currentMsisdn.get().isEmpty()) {
                logger.info(String.format("No matching rule for msisdn '%s', imsi '%s', originHost '%s' , originRealm '%s'", currentMsisdn.get(), currentImsi.get() ,originHost, originRealm));
            } else {
                logger.info(String.format("No matching rule for imsi '%s', originHost '%s' , originRealm '%s'", currentImsi.get() ,originHost, originRealm));
            }
        } else {
            if (!currentMsisdn.get().isEmpty()) {
                logger.info(String.format("No matching rule for msisdn '%s', imsi '%s', originHost '%s' , originRealm '%s'", currentMsisdn.get(), currentImsi.get() ,originHost, originRealm));
            } else {
                logger.info(String.format("No matching rule for originHost '%s' , originRealm '%s'", originHost, originRealm));
            }
        }
        return null;
    }

    private Rule getDefaultRule() {
        Optional<Rule> defaultRule = GettingRules.rules().list().stream().filter(rule -> {
            boolean match = rule.isEnabled() && rule.getDefault();
            if (match)
                logger.debug(String.format("Match in default rule [%s]", rule.getName()));
            return match;
        }).findFirst();
        if (defaultRule.isPresent()) {
            return defaultRule.get();
        }
        return null;
    }

    private String getImsi(Rule rule, AvpSet avps) {
        Avp imsi = null;
        try {
            if (!rule.match().isSubscriptionId()) {
                imsi = avps.getAvp(Avp.TGPP_IMSI);
                if (imsi == null) {
                    logger.debug(String.format("No IMSI found in [Avp.TGPP_IMSI], rule name=[%s]", rule.getName()));
                    return "";
                }
            } else {
                if (avps.getAvps(Avp.SUBSCRIPTION_ID) != null) {
                    for (Avp avp : avps.getAvps(Avp.SUBSCRIPTION_ID).asArray()) {
                        AvpSet grouped = avp.getGrouped();
                        if (grouped.getAvp(Avp.SUBSCRIPTION_ID_TYPE).getInteger32() == 1) {
                            imsi = grouped.getAvp(Avp.SUBSCRIPTION_ID_DATA);
                            break;
                        }
                    }
                }
                if (imsi == null) {
                    logger.debug(String.format("No IMSI found in [Avp.SUBSCRIPTION_ID_DATA], rule name=[%s]", rule.getName()));
                    return "";
                }
            }
            return imsi.getUTF8String();
        } catch (Exception e) {
            logger.error("Exception caught while get Imsi in the {} rule", rule.getName(), e);
        }
        return "";
    }

    private String getMsisdn(Rule rule, AvpSet avps) {
        Avp msisdn = null;
        try {
            // first check if the AVP 601
            msisdn = avps.getAvp(Avp.PUBLIC_IDENTITY);
            if (msisdn == null) {
                // check the AVP 701
                logger.debug(String.format("No MSISDN found in [Avp.PUBLIC_IDENTITY] checking [Avp.MSISDN], rule name=[%s]", rule.getName()));
                msisdn = avps.getAvp(Avp.MSISDN);
                if (msisdn == null) {
                    //Check the AVP list 700
                    logger.debug(String.format("No MSISDN found in [Avp.MSISDN] checking [Avp.USER_IDENTITY], rule name=[%s]", rule.getName()));
                    for (Avp avp : avps.getAvps(Avp.USER_IDENTITY).asArray()) {
                        AvpSet grouped = avp.getGrouped();
                        msisdn = grouped.getAvp(Avp.PUBLIC_IDENTITY);
                        if (msisdn == null) {
                            logger.debug(String.format("No MSISDN found in [Avp.USER_IDENTITY; Avp.PUBLIC_IDENTITY] checking [Avp.MSISDN], rule name=[%s]", rule.getName()));
                            msisdn = grouped.getAvp(Avp.MSISDN);
                        }
                    }
                }

            }
            if (msisdn != null) {
                return msisdn.getUTF8String();
            } else {
                logger.debug(String.format("No MSISDN found for rule name=[%s]", rule.getName()));
                return "";
            }

        } catch (Exception e) {
            logger.error("Exception caught while get Imsi in the {} rule", rule.getName(), e);
        }
        return "";
    }

    public Host getRoutingHost(Rule rule, List<String> previousHost) {
        Host host = rule.getHostByPriority(previousHost); // get first;
        if (host == null) { // find load balance
            host = rule.getHostByLoadBalance();
        }
        return host;
    }

}