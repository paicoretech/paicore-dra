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

import java.util.List;

@XmlRootElement(name = "Realm")
@XmlAccessorType(XmlAccessType.FIELD)
public class RealmRlm {

    @XmlElement(name = "ApplicationID")
    private List<RealmApplicationID> applicationIDList;

    @XmlAttribute(name = "name")
    private String name;

    @XmlAttribute(name = "peers")
    private String peers;

    @XmlAttribute(name = "local_action")
    private String localAction;

    @XmlAttribute(name = "dynamic")
    private String dynamic;

    @XmlAttribute(name = "exp_time")
    private String expTime;

    public List<RealmApplicationID> getApplicationIDList() {
        return applicationIDList;
    }

    public void setApplicationIDList(List<RealmApplicationID> applicationIDList) {
        this.applicationIDList = applicationIDList;
    }
}
