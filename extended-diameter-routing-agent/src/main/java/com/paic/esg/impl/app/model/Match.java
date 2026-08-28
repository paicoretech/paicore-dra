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

package com.paic.esg.impl.app.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import java.util.regex.Pattern;

@XmlAccessorType(XmlAccessType.FIELD)
public class Match {

    private Pattern regexOriginHost;
    private Pattern regexOriginRealm;
    private Pattern regexDestinationHost;
    private Pattern regexDestinationRealm;
    private Pattern regexImsi;
    @XmlAttribute(name = "use-msisdn-second-filter")
    private Boolean useMsisdnSecondFilter;
    private Pattern regexMsisdn;
    private Pattern regexApplicationId;
    @XmlAttribute(name = "subscription-id")
    private Boolean subscriptionId;

    public Match() {
    }

    @XmlAttribute(name = "origin-host")
    public void setOriginHost(String originHost) {
        this.regexOriginHost = Pattern.compile(originHost);
    }

    @XmlAttribute(name = "origin-realm")
    public void setOriginRealm(String originRealm) {
        this.regexOriginRealm = Pattern.compile(originRealm);
    }

    @XmlAttribute(name = "imsi")
    public void setImsi(String imsi) {
        this.regexImsi = Pattern.compile(imsi);
    }

    @XmlAttribute(name = "destination-realm")
    public void setDestinationRealm(String destinationRealm){
        this.regexDestinationRealm = Pattern.compile(destinationRealm);
    }

    @XmlAttribute(name = "destination-host")
    public void setDestinationHost(String destinationHost){
        this.regexDestinationHost= Pattern.compile(destinationHost);
    }

    @XmlAttribute(name = "application-id")
    public void setApplicationId(String applicationId) {
        this.regexApplicationId = Pattern.compile(applicationId);
    }

    @XmlAttribute(name = "msisdn")
    public void setMsisdn(String msisdn) {
        this.regexMsisdn = Pattern.compile(msisdn);
    }

    public Pattern originHost() {
        return regexOriginHost == null ? Pattern.compile(".*") : regexOriginHost;
    }

    public Pattern originRealm() {
        return regexOriginRealm == null ? Pattern.compile(".*") : regexOriginRealm;
    }

    public Pattern imsi() {
        return regexImsi;
    }

    public Boolean isSubscriptionId() {
        return subscriptionId;
    }

    public Pattern destinationRealm(){
        return regexDestinationRealm == null ? Pattern.compile(".*") : regexDestinationRealm;
    }

    public Pattern destinationHost(){
        return regexDestinationHost == null ? Pattern.compile(".*") : regexDestinationHost;
    }

    public Pattern applicationId() { return regexApplicationId == null ? Pattern.compile(".*") : regexApplicationId; }

    public Boolean isMsisdnSecondFilter() {
        return useMsisdnSecondFilter;
    }

    public Pattern msisdn() {
        return regexMsisdn;
    }
}
