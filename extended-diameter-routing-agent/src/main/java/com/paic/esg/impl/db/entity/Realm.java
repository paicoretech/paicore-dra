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
import org.jdiameter.api.LocalAction;

import java.util.Arrays;

@Table(name = "realm")
public class Realm {

    @Column(name = "realm_id")
    private Long id;
    private String name;
    private String[] peers;
    @Column(name = "local_action")
    private String localAction;
    private Boolean dynamic;
    @Column(name = "exp_time")
    private Long expTime;
    private ApplicationId applicationId;

    public Realm() {
    }

    public Realm(Long id, String name, String[] peers, String localAction, Boolean dynamic, Long expTime, ApplicationId applicationId) {
        this.id = id;
        this.name = name;
        this.peers = peers;
        this.localAction = localAction;
        this.dynamic = dynamic;
        this.expTime = expTime;
        this.applicationId = applicationId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getPeers() {
        return peers;
    }

    public void setPeers(String[] peers) {
        this.peers = peers;
    }

    public LocalAction getLocalAction() {
        return LocalAction.valueOf(localAction);
    }

    public void setLocalAction(String localAction) {
        this.localAction = localAction;
    }

    public Boolean getDynamic() {
        return dynamic;
    }

    public void setDynamic(Boolean dynamic) {
        this.dynamic = dynamic;
    }

    public Long getExpTime() {
        return expTime;
    }

    public void setExpTime(Long expTime) {
        this.expTime = expTime;
    }

    public ApplicationId getApplicationId(){
        return applicationId;
    }

    public org.jdiameter.api.ApplicationId getDiameterApplicationId() {
        if(applicationId.getVendorId() !=0 && applicationId.getAuthApplId() != 0)
            return org.jdiameter.api.ApplicationId.createByAuthAppId(applicationId.getVendorId(), applicationId.getAuthApplId());
        else
            return org.jdiameter.api.ApplicationId.createByAccAppId(applicationId.getVendorId(), applicationId.getAcctApplId());
    }

    public void setApplicationId(ApplicationId applicationId) {
        this.applicationId = applicationId;
    }

    @Override
    public String toString() {
        return "Realm{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", peers='" + Arrays.toString(peers) + '\'' +
                ", localAction='" + localAction + '\'' +
                ", dynamic=" + dynamic +
                ", expTime=" + expTime +
                ", applicationId=" + applicationId +
                '}';
    }
}
