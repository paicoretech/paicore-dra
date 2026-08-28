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

package com.paic.esg.configuration.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Peer")
@XmlAccessorType(XmlAccessType.FIELD)
public class Peer {
    @XmlAttribute(name = "name")
    private String name;

    @XmlAttribute(name = "attempt_connect")
    private boolean attemptConnect;

    @XmlAttribute(name = "rating")
    private int rating;

    @XmlAttribute(name = "ip")
    private String ip;

    @XmlAttribute(name = "standby_addresses")
    private String standbyAddresses;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAttemptConnect() {
        return attemptConnect;
    }

    public void setAttemptConnect(boolean attemptConnect) {
        this.attemptConnect = attemptConnect;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getStandbyAddresses() {
        return standbyAddresses;
    }

    public void setStandbyAddresses(String standbyAddresses) {
        this.standbyAddresses = standbyAddresses;
    }

    public String getIp() {
        return ip == null ? "" : ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
