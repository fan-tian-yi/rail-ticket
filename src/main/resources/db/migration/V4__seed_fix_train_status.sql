-- V4：修正种子数据的车次状态
-- 原因：V2 的 INSERT 未指定 status，落了 DDL 默认值 0（草稿）。
--       而库存预热只扫 status=1（上架）的车次，导致预热结果为 0 个 key。
--       种子车次本意是「可售票」，应为上架状态。

UPDATE t_train SET status = 1 WHERE status = 0;
