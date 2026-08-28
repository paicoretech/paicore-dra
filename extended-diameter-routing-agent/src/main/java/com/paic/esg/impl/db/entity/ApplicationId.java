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

package com.paic.esg.impl.db.entity;

import com.paic.esg.impl.db.persistence.Column;
import com.paic.esg.impl.db.persistence.Table;

@Table(name = "application_id")
public class ApplicationId {

    @Column(name = "appl_id")
    private Long id;
    @Column(name = "vendor_id")
    private Long vendorId;
    @Column(name = "auth_appl_id")
    private Long authApplId;
    @Column(name = "acct_appl_id")
    private Long acctApplId;

    public ApplicationId() {
    }

    public ApplicationId(Long id, Long vendorId, Long authApplId, Long acctApplId) {
        this.id = id;
        this.vendorId = vendorId;
        this.authApplId = authApplId;
        this.acctApplId = acctApplId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public Long getAuthApplId() {
        return authApplId;
    }

    public void setAuthApplId(Long authApplId) {
        this.authApplId = authApplId;
    }

    public Long getAcctApplId() {
        return acctApplId;
    }

    public void setAcctApplId(Long acctApplId) {
        this.acctApplId = acctApplId;
    }

    @Override
    public String toString() {
        return "ApplicationId{" +
                "id=" + id +
                ", vendorId=" + vendorId +
                ", authApplId=" + authApplId +
                ", acctApplId=" + acctApplId +
                '}';
    }
}
