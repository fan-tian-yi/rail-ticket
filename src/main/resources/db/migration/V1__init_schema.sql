# 初始建表（7 张表）

CREATE DATABASE IF NOT EXISTS rail_ticket DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;
USE rail_ticket;

-- 站点表
CREATE TABLE t_station (
    id          BIGINT        NOT NULL COMMENT '主键（雪花 ID）',
    name        VARCHAR(32)   NOT NULL COMMENT '站名（如 北京南）',
    code        VARCHAR(8)             COMMENT '电报码（可空但唯一，真实规则=站名拼音首字母+铁路局码，如 BJP/SHH）',
    city        VARCHAR(32)   NOT NULL COMMENT '所属城市（如 北京）',
    lng         DECIMAL(10,6)          COMMENT '经度（地图用）',
    lat         DECIMAL(10,6)          COMMENT '纬度（地图用）',
    deleted     TINYINT       NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_station_name (name) COMMENT '站名唯一，防重名',
    UNIQUE KEY uk_station_code (code) COMMENT '电报码唯一（NULL 不参与比较，可空）',
    KEY idx_station_city (city) COMMENT '按城市查站点列表'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站点表';

-- 车次表（车次模板，无发车日期——时刻表存模板，具体实例由 order.train_date 拼接）
CREATE TABLE t_train (
    id           BIGINT      NOT NULL COMMENT '主键（雪花 ID）',
    train_no     VARCHAR(8)  NOT NULL COMMENT '车次号（如 G101）',
    train_type   TINYINT     NOT NULL COMMENT '车次类型 1高铁 2动车 3普快',
    status       TINYINT     NOT NULL DEFAULT 0 COMMENT '状态 0草稿 1上架 2停运（上架触发缓存预热）',
    seat_config  JSON                 COMMENT '席别配置，如 {"business":50,"first":100,"second":850}',
    deleted      TINYINT     NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（上下架/调席别时刷新）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_train_train_no (train_no) COMMENT '车次号唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车次表';

-- 车次经停表（★ 核心表，不加 deleted——热路径查询，跟车次级联硬删）
CREATE TABLE t_train_station (
    id          BIGINT        NOT NULL COMMENT '主键',
    train_id    BIGINT        NOT NULL COMMENT '车次 ID',
    station_id  BIGINT        NOT NULL COMMENT '站点 ID',
    seq         INT           NOT NULL COMMENT '站序，从 1 开始递增',
    arrive_time TIME                   COMMENT '到站时刻（时刻表模板，每日复用，与 train_date 拼具体时刻）',
    depart_time TIME                   COMMENT '离站时刻',
    price_cum   DECIMAL(10,2)          COMMENT '从起点到本站的累计票价（A→B 票价 = price_cum(B) - price_cum(A)）',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（调图改时刻时刷新）',
    PRIMARY KEY (id),
    KEY idx_train_station_train_seq (train_id, seq) COMMENT '查车次经停列表（按 seq 升序）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车次经停表（时刻表模板 + 区间票价基准）';

-- 用户表
CREATE TABLE t_user (
    id          BIGINT       NOT NULL COMMENT '主键（雪花 ID）',
    phone       VARCHAR(16)  NOT NULL COMMENT '手机号（登录账号）',
    password    VARCHAR(72)           COMMENT 'BCrypt 密文（输出 60 字符，留余量）',
    nickname    VARCHAR(32)           COMMENT '昵称',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除标记（注销不真删，历史订单要挂）',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_phone (phone) COMMENT '手机号唯一，登录键'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 乘客表
CREATE TABLE t_passenger (
    id          BIGINT       NOT NULL COMMENT '主键',
    user_id     BIGINT       NOT NULL COMMENT '归属用户 ID',
    name        VARCHAR(32)           COMMENT '乘客姓名',
    id_card     VARCHAR(18)            COMMENT '身份证号（MVP 明文，安全阶段做脱敏/加密改造）',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除标记',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_passenger_user_id (user_id) COMMENT '查某用户的乘客列表'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='乘客表';

-- 订单表（不加 deleted——status 已管取消/退票，订单永不删；删了对账崩）
CREATE TABLE t_order (
    id              BIGINT        NOT NULL COMMENT '主键（雪花 ID）',
    order_no        VARCHAR(32)   NOT NULL COMMENT '订单号（雪花生成，全局唯一 + 防枚举越权）',
    user_id         BIGINT        NOT NULL COMMENT '下单用户 ID',
    train_id        BIGINT        NOT NULL COMMENT '车次 ID',
    train_date      DATE          NOT NULL COMMENT '乘车日期（业务键，≠ create_time）',
    from_station_id BIGINT        NOT NULL COMMENT '出发站 ID（订单快照，外部调图不污染历史）',
    to_station_id   BIGINT        NOT NULL COMMENT '到达站 ID（订单快照）',
    seat_type       TINYINT       NOT NULL COMMENT '席别 1二等座 2一等座 3商务座',
    amount          DECIMAL(10,2) NOT NULL COMMENT '票价（订单快照，票价变动不影响已支付订单）',
    status          TINYINT       NOT NULL DEFAULT 0 COMMENT '状态 0待支付 1已支付 2已完成 3超时取消 4用户取消 5已退票',
    expire_time     DATETIME               COMMENT '支付截止时间（下单 +15min）',
    pay_time        DATETIME               COMMENT '支付时间（关键业务时刻，独立字段防被 update_time 覆盖）',
    cancel_time     DATETIME               COMMENT '取消/退票时间（同上）',
    create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（状态机流转审计）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_order_no (order_no) COMMENT '订单号唯一 + 幂等兜底',
    KEY idx_order_user_create (user_id, create_time) COMMENT '我的订单（按用户查 + 时间倒序，避免 filesort）',
    KEY idx_order_status_expire (status, expire_time) COMMENT '扫超时订单（WHERE status=0 AND expire_time<NOW()）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表（订单快照冻结业务事实）';

-- 库存流水表（追加型，只 INSERT 永不 UPDATE，故不加 deleted/update_time——加了语义错误）
CREATE TABLE t_stock_deduction_log (
    id          BIGINT       NOT NULL COMMENT '主键',
    order_no    VARCHAR(32)  NOT NULL COMMENT '关联订单号',
    train_id    BIGINT       NOT NULL COMMENT '车次 ID（有意冗余，流水自包含防 JOIN）',
    train_date  DATE         NOT NULL COMMENT '乘车日期（冗余）',
    seat_type   TINYINT      NOT NULL COMMENT '席别（冗余）',
    segments    JSON                  COMMENT '本次扣/补覆盖的区段列表',
    delta       INT          NOT NULL COMMENT '数量变化 -1扣减 / +1回补',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
    PRIMARY KEY (id),
    KEY idx_stock_log_order_no (order_no) COMMENT '按订单查流水',
    KEY idx_stock_log_train_date_seat (train_id, train_date, seat_type) COMMENT '对账聚合（某天某车某席别净扣减）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存扣减/回补流水表（对账依据）';
