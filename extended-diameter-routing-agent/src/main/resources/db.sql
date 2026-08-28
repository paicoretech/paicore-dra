create schema if not exists "signaling-gateway";

-- DROP TABLE "signaling-gateway".realm;

CREATE TABLE IF NOT EXISTS "signaling-gateway".realm
(
    realm_id bigint NOT NULL GENERATED ALWAYS AS IDENTITY ( INCREMENT 1 START 1 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1 ),
    name character varying(255) COLLATE pg_catalog."default" NOT NULL,
    peers character varying[] COLLATE pg_catalog."default",
    local_action character varying(12) COLLATE pg_catalog."default" NOT NULL,
    dynamic boolean,
    exp_time bigint,
    CONSTRAINT realm_pkey PRIMARY KEY (realm_id)
);

CREATE TABLE IF NOT EXISTS "signaling-gateway".application_id
(
    appl_id bigint NOT NULL GENERATED ALWAYS AS IDENTITY ( INCREMENT 1 START 1 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1 ),
    vendor_id bigint,
    auth_appl_id bigint,
    acct_appl_id bigint,
    CONSTRAINT application_id_pkey PRIMARY KEY (appl_id),
    CONSTRAINT application_id_vendor_id_auth_appl_id_acct_appl_id_key UNIQUE (vendor_id, auth_appl_id, acct_appl_id)
);

CREATE TABLE "signaling-gateway".realm_application
(
    realm_id bigint NOT NULL,
    appl_id bigint NOT NULL,
    CONSTRAINT realm_application_pkey PRIMARY KEY (realm_id, appl_id),
    CONSTRAINT realm_application_appl_id_fkey FOREIGN KEY (appl_id)
        REFERENCES "signaling-gateway".application_id (appl_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT realm_application_realm_id_fkey FOREIGN KEY (realm_id)
        REFERENCES "signaling-gateway".realm (realm_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);