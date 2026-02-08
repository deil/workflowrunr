create table executions
(
    id     bigint auto_increment
        primary key,
    wf     mediumblob  not null,
    param1 varchar(64) not null
);
