/* Objects */

create cached table if not exists Objects
(
  pkey  uuid not null,
  type  varchar (255) default '',
  owner uuid,
  ts    timestamp not null,
  text  varchar (1024),
  json  varbinary (524288),
  file  blob,

  constraint pk_objects primary key (pkey, type)
);

create index if not exists ix_objects_ts
  on Objects (ts);

create index if not exists ix_objects_owner
  on Objects (owner);

create index if not exists ix_objects_type_ts
  on Objects (type, ts);
