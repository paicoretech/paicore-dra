# DRA v3.0.0 Release Notes

Continuous innovation is a core value of PAiCore Technology, and the first open source release of
our Extended Diameter Routing Agent (DRA), version 3.0.0, is the latest testament to this.

This release opens to the community a Diameter Routing Agent that has been in continuous production
service for years. It describes the routing, distribution, message manipulation and management capabilities
provided by PAiCore's Diameter node, built on the Diameter Base Protocol as defined in IETF RFC 6733
and on the 3GPP reference points derived from it.

**LATEST VERSION:** 3.0.0

**RELEASE DATE:** August 27, 2026

**LICENSE:** AGPL-3.0-or-later

**SUPPORTED DIAMETER APPLICATIONS:** 17


## ROUTING SERVICES

### Rule Evaluation

- **RULE SELECTION**
  - Criteria applied on every incoming request:
    - *Document order*: rules are evaluated in the order in which they are declared, and the first
      enabled rule whose match block is satisfied is selected. Precedence is therefore explicit in
      the configuration and requires no separate priority attribute at rule level.
    - *enabled*: when set to false, the rule is excluded from evaluation without being removed from
      the configuration. It shall be evaluated before any match criterion, so that disabled rules
      impose no matching cost.
    - *default*: indicates by its presence that the rule is the last-resort destination for requests
      that satisfy no other rule. It shall be evaluated only after every non-default rule has been
      rejected, and it may be declared without a match block.
  - Behaviour when no rule is selected:
    - The request is subject to the drop policy of the enclosing rules element, as described under
      *Failure Handling Services*.

### Match Criteria

- **MATCH ELEMENT**
  - Parameters added to the rule match criteria:
    - *origin-host*: regular expression evaluated against the Origin-Host AVP (264) of the request.
      When absent it defaults to `.*`, so a rule constrains only the identities it names.
    - *origin-realm*: regular expression evaluated against the Origin-Realm AVP (296).
    - *destination-host*: regular expression evaluated against the Destination-Host AVP (293). It
      shall be applied only when the AVP is present in the request, since Destination-Host is
      optional in a request routed by realm.
    - *destination-realm*: regular expression evaluated against the Destination-Realm AVP (283).
    - *application-id*: regular expression evaluated against the Application-Id of the request,
      allowing a single rule set to discriminate between reference points such as S6a (16777251),
      Cx/Dx (16777216) and Sh (16777217) without inspecting the command code.
    - *imsi*: regular expression evaluated against the subscriber identity extracted from the
      request, as described under *Subscriber Identity Extraction*.
    - *msisdn*: regular expression evaluated against the subscriber number extracted from the
      request, as described under *Subscriber Identity Extraction*.

### Subscriber Identity Extraction

- **IMSI RETRIEVAL**
  - Parameters governing the retrieval source:
    - *subscription-id*: when absent or set to false, the IMSI shall be read from the 3GPP-IMSI AVP
      (1). When set to true, the IMSI shall be read from the Subscription-Id grouped AVP (443),
      selecting the Subscription-Id-Data (444) whose Subscription-Id-Type (450) is END_USER_IMSI, as
      defined in IETF RFC 4006. This parameter allows the same rule set to serve both HSS traffic,
      which carries the identity in the 3GPP AVP, and charging traffic, which carries it in the
      Credit-Control grouped AVP.
  - When the configured source contains no identity, the rule is evaluated according to the MSISDN
    second-filter policy described below.

- **MSISDN RETRIEVAL**
  - The subscriber number shall be resolved in cascade, the first source present determining the
    value:
    - Public-Identity AVP (601).
    - MSISDN AVP (701).
    - User-Identity grouped AVP (700), examined for Public-Identity (601) and, failing that, for
      MSISDN (701).
  - The cascade allows a single rule to match Sh, Cx/Dx and S6a traffic, which carry the subscriber
    number in different information elements.

- **SECOND-FILTER POLICY**
  - Parameters added to the match criteria:
    - *use-msisdn-second-filter*: indicates by its presence that MSISDN shall act as a substitute
      discriminator when the request carries no IMSI. When set to true, a request without IMSI is
      matched on MSISDN alone; when absent or set to false, a rule that declares both criteria
      requires both identities to be present and to match. The parameter allows one rule to cover
      both message shapes, rather than requiring a paired rule for each.

### Configuration Reload

- **RULE FILE SUPERVISION**
  - The rule configuration is placed under filesystem supervision at start-up:
    - *Hot reload*: a modification of the rule file causes the complete rule set to be reloaded in
      place. Established Diameter associations shall not be affected, and no request in flight is
      lost. Routing changes, destination changes and weight changes therefore require no restart of
      the node.
    - *Fail-fast on invalid content*: a rule file that cannot be parsed terminates the process rather
      than allowing the node to serve traffic with a partially loaded routing table.


## TRAFFIC DISTRIBUTION SERVICES

### Destination Selection

- **HOST ELEMENT**
  - Parameters governing selection among the destinations of a rule:
    - *priority*: among hosts whose priority value is unique within the rule, the host with the
      highest priority shall be selected. Uniqueness is what distinguishes priority routing from
      weighted routing, and requires no separate mode attribute.
    - *load-balance*: percentage weight applied among hosts that share a priority value. The weight
      is evaluated over a rolling window of 200 messages per rule, after which the per-host counters
      are reset. Hosts that share a priority value without declaring a weight shall receive an even
      share, computed as the reciprocal of the number of hosts in the group.
    - *name*: logical identifier of the destination. It shall be used to record the hosts already
      attempted during fall-back, so that a retry never returns to a host that has just failed.


## MESSAGE MANIPULATION SERVICES

### Destination Replacement

- **HOST ELEMENT**
  - Parameters added for the manipulation of the forwarded request:
    - *address*: when *replace-host* is absent or set to true, the value shall be written into the
      Destination-Host AVP (293) of the forwarded message, and the routing decision is therefore
      taken by the agent rather than by the underlying stack. When *replace-host* is set to false,
      the value shall be written into the proprietary AVP 999 and host selection is left to the
      stack.
    - *replace-host*: indicates whether the agent imposes the destination host or delegates the
      decision. It defaults to true.
    - *dest-host*: explicit replacement of the Destination-Host AVP (293), independent of the
      *address* parameter.
    - *dest-realm*: explicit replacement of the Destination-Realm AVP (283).
    - *realm*: shorthand replacement of the Destination-Realm AVP (283), retained for compatibility
      with rule sets written before *dest-realm* was introduced.

### Origin Replacement

- **HOST ELEMENT**
  - Parameters added for the manipulation of the originating identity:
    - *origin-host*: replaces the Origin-Host AVP (264) on the forwarded message. It shall be used
      when the downstream node is provisioned to accept a specific peer identity and the identity of
      the agent itself must not be exposed.
    - *origin-realm*: replaces the Origin-Realm AVP (296) on the forwarded message, for the same
      purpose.

### Path Recording

- **HOST ELEMENT**
  - Parameters added for loop detection:
    - *route-record*: indicates that a Route-Record AVP (282) shall be appended for this hop, as
      required of a Diameter agent by IETF RFC 6733. It defaults to true, and shall be disabled only
      where the downstream node is known to reject the AVP.
  - On the answer path the Route-Record indication is cleared, since Route-Record applies to requests
    only.


## FAILURE HANDLING SERVICES

### Fall-Back Policy

- **RULE ELEMENT**
  - Parameters added for the treatment of a failed delivery:
    - *fallback-policy*: comma-separated list of the failure classes for which the agent shall retry
      the next eligible destination. The value `network` covers routing and connectivity failures,
      reported by the stack as a routing exception; the value `error` covers the remaining delivery
      failures. Hosts already attempted are excluded from the retry, and values outside the two
      defined classes are reported in the log and otherwise ignored.
    - Behaviour when the parameter is absent: the agent shall route by destination realm and leave
      the selection of the peer to the stack. This is the appropriate configuration where the realm
      is served by a peer group whose membership is managed outside the rule file.
  - Behaviour when the destinations of a rule are exhausted:
    - The default rule, if one is declared and enabled, shall be given the opportunity to provide a
      destination before the request is subjected to the drop policy.

### Drop Policy

- **RULES ELEMENT**
  - Parameters added for the treatment of an undeliverable request:
    - *drop-policy*: comma-separated list of the conditions under which a request shall be discarded
      without an answer. The value `no-match` applies where no rule was satisfied; the value
      `no-routing` applies where a rule was satisfied but no destination could be obtained.
    - Behaviour for conditions not listed: the request shall be answered with the Result-Code
      DIAMETER_UNABLE_TO_COMPLY (5012) and the session terminated, so that the originating node is
      released immediately rather than waiting for its own transaction timer to expire.

### Transaction Supervision

- **REQUEST CORRELATION**
  - Behaviour of the pending-request store:
    - *Correlation key*: pending requests are indexed by the concatenation of Session-Id (263) and
      the End-to-End Identifier of the message header. The composite key guarantees correct
      correlation of the answer even where an originating node reuses a session identifier across
      concurrent transactions.
    - *Ageing*: an entry that has not been correlated within 30 seconds shall be discarded, and the
      corresponding Diameter session released, by a supervision task executed once per minute. The
      release of the session is what prevents the accumulation of abandoned sessions that otherwise
      degrades a long-running Diameter proxy.
    - *Concurrency*: the store admits concurrent insertion and retrieval without a global lock, so
      that rule evaluation and answer correlation do not contend under load.


## PEER MANAGEMENT SERVICES

### CLI Administration

- **MANAGEMENT INTERFACE**
  - Operations provided over the management interface, without restart of the node:
    - *peer -list*: returns the configured peers together with the live state of each association.
    - *peer -details*: returns the name, connection attempt indication, rating, address and stand-by
      address of a peer.
    - *peer -add*: adds a peer to the stack configuration.
    - *peer -edit*: modifies a peer. The peer shall be stopped before modification is permitted.
    - *peer -remove*: removes a peer from the configuration. The peer shall be stopped before removal
      is permitted.
    - *peer -start*: establishes the association.
    - *peer -stop*: releases the association with a Disconnect-Cause of REBOOTING, BUSY or
      DO_NOT_WANT_TO_TALK_TO_YOU, as defined in IETF RFC 6733.
    - *peer -reset*: releases and re-establishes the association in a single operation.
    - *assoc -list*: returns the configured associations.
  - Configuration changes performed through the interface shall be written back to the stack
    configuration file, so that the running state and the persisted configuration remain consistent.


## SUPPORTED DIAMETER APPLICATIONS

The dictionary supplied with this release carries the command and AVP definitions required to route
the following applications. Since routing is driven by generic AVP matching rather than by
per-application logic, any further application may be routed once its definitions are declared.

- **IETF applications**
  - Diameter Base (0), as defined in IETF RFC 6733.
  - NASREQ (1), as defined in IETF RFC 4005.
  - Diameter Base Accounting (3).
  - Diameter Credit-Control (4), as defined in IETF RFC 4006.
- **3GPP reference points**
  - Rf (3) and Ro (4), offline and online charging.
  - Cx/Dx (16777216), as defined in 3GPP TS 29.229.
  - Sh (16777217), as defined in 3GPP TS 29.329.
  - Gq (16777222) and Gx (16777224), policy and charging control.
  - MM10 (16777226), messaging.
  - Rx (16777236), as defined in 3GPP TS 29.214.
  - S6a (16777251) and S13/S13' (16777252), as defined in 3GPP TS 29.272.
  - SWm (16777264) and S6b (16777272), non-3GPP access, as defined in 3GPP TS 29.273.
- **Vendor applications**
  - Ericsson Service Charging Application Protocol (19302).

