# V4：修正种子数据的车次状态
# 原因：V2 的 INSERT 未指定 status，落了 DDL 默认值 0（草稿）。

UPDATE t_train SET status = 1 WHERE status = 0;
