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

import javax.xml.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@XmlRootElement(name = "ExtendedSignalingGateway")
@XmlAccessorType(XmlAccessType.FIELD)
public class ExtendedSignalingGatewayRules {

    @XmlElementWrapper(name = "Applications")
    @XmlElement(name = "Application")
    private List<Application> applications;


    public ExtendedSignalingGatewayRules() {
    }

    public List<Application> getApplications() {
        List<Application> filterApplications = applications.stream().filter(f -> f.isEnabled()).collect(Collectors.toList());
        return filterApplications;
    }

    public void setApplications(List<Application> applications) {
        this.applications = applications;
    }

    @Override
    public String toString() {
        return "ExtendedSGRule{" +
                "applications=" + applications +
                '}';
    }
}
