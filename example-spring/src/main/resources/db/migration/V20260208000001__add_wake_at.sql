alter table executions
    add column wake_at timestamp null after state;
