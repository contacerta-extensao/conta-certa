create table audit_events (
    id uuid primary key,
    actor_user_id uuid not null references users(id),
    action varchar(48) not null,
    target_type varchar(32) not null,
    target_id uuid not null,
    occurred_at timestamptz not null
);

create index idx_audit_events_actor_occurred_at
    on audit_events (actor_user_id, occurred_at desc);

create index idx_audit_events_target_occurred_at
    on audit_events (target_type, target_id, occurred_at desc);
