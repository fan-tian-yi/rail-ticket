# order表缺少乘车人id，只有用户的id

USE rail_ticket;

ALTER TABLE t_order
    ADD COLUMN passenger_id BIGINT NOT NULL COMMENT '乘车人 ID'
    AFTER user_id,
    ADD KEY idx_order_passenger_id (passenger_id);