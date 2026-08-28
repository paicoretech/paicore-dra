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

@XmlRootElement(name = "LocalPeer")
@XmlAccessorType(XmlAccessType.FIELD)
public class LocalPeer {

    @XmlElement(name = "URI")
    private Uri uri;

    @XmlElement(name = "IPAddresses")
    private IpAddresses ipAddresses;

    @XmlElement(name = "Realm")
    private Realm realm;

    @XmlElement(name = "VendorID")
    private VendorID vendorID;

    @XmlElement(name = "ProductName")
    private ProductName productName;

    @XmlElement(name = "FirmwareRevision")
    private FirmwareRevision firmwareRevision;

    @XmlElement(name = "Applications")
    private Applications applications;

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public IpAddresses getIpAddresses() {
        return ipAddresses;
    }

    public void setIpAddresses(IpAddresses ipAddresses) {
        this.ipAddresses = ipAddresses;
    }

    public Realm getRealm() {
        return realm;
    }

    public void setRealm(Realm realm) {
        this.realm = realm;
    }

    public VendorID getVendorID() {
        return vendorID;
    }

    public void setVendorID(VendorID vendorID) {
        this.vendorID = vendorID;
    }

    public ProductName getProductName() {
        return productName;
    }

    public void setProductName(ProductName productName) {
        this.productName = productName;
    }

    public FirmwareRevision getFirmwareRevision() {
        return firmwareRevision;
    }

    public void setFirmwareRevision(FirmwareRevision firmwareRevision) {
        this.firmwareRevision = firmwareRevision;
    }

    public Applications getApplications() {
        return applications;
    }

    public void setApplications(Applications applications) {
        this.applications = applications;
    }
}