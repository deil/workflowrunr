truncate table executions;

alter table executions
    add status varchar(16) not null after id;

alter table executions
    drop column param1;

alter table executions
    add queued_at timestamp not null after status;

alter table executions
    change wf definition mediumblob not null;

alter table executions
    add params json not null;

alter table executions
    add state json null;

alter table executions
    add started_at timestamp null;

alter table executions
    add completed_at timestamp null;
