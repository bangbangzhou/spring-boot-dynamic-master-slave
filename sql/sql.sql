create table tb_tutorial
(
    id          bigint auto_increment comment '主键ID'
        primary key,
    title       varchar(40) null comment '标题',
    description varchar(30) null comment '描述',
    published   tinyint     null comment '1 表示发布 0 表示未发布'
);

