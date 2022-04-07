create table users
(
    username    varchar             not null
        constraint user_pkey
            primary key,
    password    varchar             not null,
    money       integer default 20,
    name        varchar,
    bio         varchar,
    image       varchar,
    elo         integer default 100 not null,
    wins        integer default 0   not null,
    draws       integer default 0   not null,
    losses      integer default 0   not null,
    gamesplayed integer default 0   not null
);

alter table users
    owner to postgres;

create table pack
(
    id     varchar(100)   not null,
    name   varchar(50)    not null,
    damage numeric(10, 2) not null,
    number serial
        constraint pack_pk
            primary key
);

alter table pack
    owner to postgres;

create unique index pack_id_uindex
    on pack (id);

create table stack
(
    id      varchar(100)  not null
        constraint stack_pk
            primary key,
    name    varchar(50)   not null,
    damage  numeric(5, 2) not null,
    player  varchar(50),
    deck    boolean default false,
    element varchar,
    type    varchar
);

alter table stack
    owner to postgres;

create table shop
(
    tradeid varchar               not null
        constraint shop_pk
            primary key,
    cardid  varchar               not null,
    token   varchar               not null,
    typ     varchar               not null,
    damage  integer               not null,
    traded  boolean default false not null
);

alter table shop
    owner to postgres;

create table battle
(
    id      integer not null
        constraint battle_pk
            primary key,
    players integer default 0,
    player1 varchar,
    player2 varchar
);

alter table battle
    owner to postgres;


