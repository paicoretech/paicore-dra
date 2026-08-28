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

@XmlAccessorType(XmlAccessType.FIELD)
public class Host {

    @XmlAttribute
    private String name;
    @XmlAttribute
    private Integer priority;
    @XmlAttribute
    private String address;
    @XmlAttribute(name = "load-balance")
    private Integer loadBalance;
    @XmlAttribute
    private String realm;
    @XmlAttribute(name = "route-record")
    private Boolean routeRecord = true; //default
    @XmlAttribute(name = "replace-host")
    private Boolean replaceHost = true; //default
    @XmlAttribute(name = "origin-host")
    private String originHost;
    @XmlAttribute(name = "origin-realm")
    private String originRealm;

    public String getOriginHost() {
        return originHost;
    }

    public void setOriginHost(String originHost) {
        this.originHost = originHost;
    }

    public String getOriginRealm() {
        return originRealm;
    }

    public void setOriginRealm(String originRealm) {
        this.originRealm = originRealm;
    }

    public String getDestinationHost() {
        return destinationHost;
    }

    public void setDestinationHost(String destinationHost) {
        this.destinationHost = destinationHost;
    }

    public String getDestinationRealm() {
        return destinationRealm;
    }

    public void setDestinationRealm(String destinationRealm) {
        this.destinationRealm = destinationRealm;
    }

    @XmlAttribute(name = "dest-host")
    private String destinationHost;
    @XmlAttribute(name = "dest-realm")
    private String destinationRealm;

    private Long sentMessages = 0l;

    public Host() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public Boolean getRouteRecord() {
        return routeRecord;
    }

    public void setRouteRecord(Boolean routeRecord) {
        this.routeRecord = routeRecord;
    }

    public Integer getLoadBalance() {
        return loadBalance;
    }

    public void setLoadBalance(Integer loadBalance) {
        this.loadBalance = loadBalance;
    }

    public Long getSentMessages() {
        return sentMessages;
    }

    public void incrementSentMessages() {
        this.sentMessages++;
    }

    public void resetSentMessages(){
        this.sentMessages = 0l;
    }

    public Boolean isReplaceHost() {
        return replaceHost;
    }

    @Override
    public String toString() {
        return "Host{" +
                "name='" + name + '\'' +
                ", priority=" + priority +
                ", ip='" + address + '\'' +
                ", loadBalance=" + loadBalance +
                ", realm='" + realm + '\'' +
                ", routeRecord=" + routeRecord +
                ", sentMessages=" + sentMessages +
                '}';
    }
}
