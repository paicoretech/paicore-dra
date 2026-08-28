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
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "ApplicationID")
@XmlAccessorType(XmlAccessType.FIELD)
public class RealmApplicationID {
    @XmlElement(name = "VendorId")
    private RealmVendorID vendorID;

    @XmlElement(name = "AuthApplId")
    private RealmAuthApplId authApplId;

    @XmlElement(name = "AcctApplId")
    private RealmAcctApplId acctApplId;

    public RealmVendorID getVendorID() {
        return vendorID;
    }

    public void setVendorID(RealmVendorID vendorID) {
        this.vendorID = vendorID;
    }

    public RealmAuthApplId getAuthApplId() {
        return authApplId;
    }

    public void setAuthApplId(RealmAuthApplId authApplId) {
        this.authApplId = authApplId;
    }

    public RealmAcctApplId getAcctApplId() {
        return acctApplId;
    }

    public void setAcctApplId(RealmAcctApplId acctApplId) {
        this.acctApplId = acctApplId;
    }
}
