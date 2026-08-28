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

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@XmlAccessorType(XmlAccessType.FIELD)
public class Rule {

    // requests per second, on average; min 1
    private final int average = 200;

    @XmlAttribute
    private String name;
    @XmlAttribute(name = "default")
    private Boolean isDefault = false;
    private List<String> fallbackPolicy;
    @XmlElement
    private Match match;
    @XmlElementWrapper(name = "routing")
    @XmlElement(name = "host")
    private List<Host> hosts;
    @XmlAttribute(name = "enabled")
    private boolean enabled;

    public Rule() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getDefault() {
        return isDefault;
    }

    public void setDefault(Boolean aDefault) {
        isDefault = aDefault;
    }

    public Match match() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public List<Host> hosts() {
        return hosts;
    }

    public void setHosts(List<Host> hosts) {
        this.hosts = hosts;
    }

    public List<String> getFallbackPolicy() {
        return fallbackPolicy;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @XmlAttribute(name = "fallback-policy")
    public void setFallback_Policy(String fallbackPolicy) {
        this.fallbackPolicy = Arrays.asList(fallbackPolicy.split(","));
    }

    /**
     * @param type [1|2] if type = 1, returns all hosts with the unique priority
     *             if type = 2, returns all hosts with repeated priority
     * @return Stream<Host>
     */
    public Stream<Host> filterHost(int type) {
        Map<Integer, Long> data = hosts.stream().collect(Collectors.groupingBy(Host::getPriority, Collectors.counting()));
        if (data != null && !data.isEmpty()) {
            Map<Integer, Long> arr = data.entrySet().stream()
                    .filter(f -> (type == 1 ? (f.getValue() == type) : f.getValue() >= type))
                    .collect(Collectors.toMap(entry -> entry.getKey(),
                            entry -> entry.getValue()));

            Stream<Host> result = hosts.stream().filter(f -> arr.keySet().contains(f.getPriority()));
            if (type == 1) return result;
            else {
                Long sum = arr.values().stream().mapToLong(value -> value.longValue()).sum();
                return result
                        .peek(host -> {
                            if (host.getLoadBalance() == null || host.getLoadBalance() == 0)
                                host.setLoadBalance(Math.round(100 / sum));
                        });
            }
        }

        return null;
    }

    /**
     * @return get the host list by omitting the previous host.
     */
    public Host getHostByPriority(List<String> previousHost) {
        Stream<Host> stream = filterHost(1);
        Collection<Host> collection = stream.collect(Collectors.toList());
        if (collection.size() == 1) return collection.iterator().next();
        Optional<Host> optionalHost = collection.stream().filter(
                f -> !previousHost.contains(f.getName()) && (f.getLoadBalance() == null || f.getLoadBalance() <= 0))
                .max(Comparator.comparingInt(Host::getPriority));
        Host result = optionalHost.isPresent() ? optionalHost.get() : null;
        return result;
    }

    public Host getHostByLoadBalance() {
        Stream<Host> hostStream = filterHost(2);
        if (hostStream == null) {
            hostStream = hosts.stream();
        }
        List<Host> data = hostStream.collect(Collectors.toList());
        if (data == null || data.isEmpty()) return null;

        Optional<Host> optionalHost = data.stream().filter(f -> f.getLoadBalance() != null && f.getLoadBalance() > 0
                && ((f.getLoadBalance().doubleValue() / (average > 1 ? 100 : 1)) * average) > f.getSentMessages())
                .sorted(Comparator.comparingInt(Host::getLoadBalance).reversed())
                .min(Comparator.comparingLong(Host::getSentMessages));

        Host result = optionalHost.isPresent() ? optionalHost.get() : null;
        if (result == null) {
            hosts.stream().filter(f -> f.getLoadBalance() != null && f.getLoadBalance() > 0)
                    .collect(Collectors.toList()).forEach(h -> {
                h.resetSentMessages();
            });
            return getHostByLoadBalance();
        }
        return result;
    }
}
