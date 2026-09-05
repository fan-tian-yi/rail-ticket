# 🚄 rail-ticket

> 高并发火车票购票系统 · 简历项目

**Spring Boot 3 + MyBatis-Plus + Redis 高并发 + Flyway 版本化 + 区段共享余票模型**

模拟 12306 的核心业务：车次查询、区间余票查询、下单扣减、退票对账。技术深度优先于功能覆盖，重点解决"区间共享余票"与"高并发扣库存"两大难题。

---

## ✨ 技术亮点

| 维度 | 实现 |
|---|---|
| **区间共享余票** | 座位在多个区间上共享（A→B、A→C 不能同时卖），区段容量模型等价区间图着色，保证不超卖 |
| **Redis 原子扣库存** | 单线程 + Lua 脚本完成"检查 + 扣减 + 记流水"原子操作，扛秒杀级并发 |
| **订单快照** | t_order 冗余存出发到达站与票价，外部表变更不污染历史订单 |
| **乐观锁** | 支付用 `UPDATE WHERE status='CREATED'` + 影响行数做 CAS，防止并发重复支付 |
| **MySQL 主从** | Phase 4 引入，主写从读 + 哨兵自动故障转移 |
| **对账** | t_stock_deduction_log 流水 + 定时任务比对 Redis 库存 vs MySQL 订单，漂移自动修正 |

---

## 🛠 技术栈

**后端**：Spring Boot 3.5.14 / JDK 21 / MyBatis-Plus 3.5.7 / Flyway / Redisson / Sa-Token / Sentinel / Caffeine

**存储**：MySQL 8 / Redis 7（主从+哨兵）

**前端**（Phase 2+）：Vue 3 + Element Plus + ECharts

---

## 🚀 快速开始

### 环境要求

- JDK 21
- MySQL 8.0+
- Maven 3.8+
- Redis 7.0+（Phase 3 引入）

### 启动步骤

```bash
# 1. 克隆仓库
git clone https://github.com/<your-account>/rail-ticket.git
cd rail-ticket

# 2. 配置数据库（拷贝示例并修改密码）
cp src/main/resources/application.yml.example src/main/resources/application.yml
# 编辑 application.yml，把 <your-mysql-password> 改成你的 MySQL 密码

# 3. 启动（Flyway 自动建库建表，零手动操作）
mvn spring-boot:run
```

访问 `http://localhost:8080`（接口文档见 Phase 2）

---

## 📦 数据库设计

7 张表 + 完整索引 + Flyway 版本化迁移：

| 表 | 作用 |
|---|---|
| `t_station` | 站点（含电报码、经纬度） |
| `t_train` | 车次模板（无发车日期，每日复用时刻表） |
| `TrainStationDO` | 车次经停（时刻表 + 区间票价基准）★核心表 |
| `t_user` | 用户（手机号登录） |
| `t_passenger` | 乘客（一人多张身份证） |
| `t_order` | 订单（订单快照冻结业务事实） |
| `t_stock_deduction_log` | 库存流水（对账依据，追加型） |

详细见 `src/main/resources/db/migration/V1__init_schema.sql`，注释完整。

---

## 🗺 路线图

- **Phase 0** 文档 ✅
- **Phase 1** 工程骨架 + 建表 + 种子数据（进行中）
- **Phase 2** 核心闭环 ★：区段库存模型 + Lua 扣减 + 下单/退票 + 登录
- **Phase 3** 高并发武器 ★：库存分段 + Sentinel 限流 + Caffeine 降级 + 压测报告
- **Phase 4** 容灾运维：主从+哨兵 + MySQL 主从 + 对账 + 云服务器部署
- **Phase 5** 加分项：中转寻路、模拟地图、随机车次、支付、分库分表、RocketMQ、AI

详见 [`docs/00-项目规划与开发路线.md`](docs/00-项目规划与开发路线.md)（规划阶段文档同步仓库）

---

## 📝 设计决策（6 个核心点，详见 `docs/design-decisions.md`）

1. **主键 BIGINT 雪花 ID**（`id-type: assign_id`）— 为分库分表铺路 + 防枚举越权
2. **席别配置用 JSON 列** — 高频读、低频写，无需按席别做行级统计
3. **经停时刻用 TIME** — 时刻表是"每日模板"，具体实例由 `order.train_date` 拼接
4. **票价存累计价**（`price_cum`）— A→B = price_cum(B) − price_cum(A)，一次减法
5. **order_no 雪花生成** — 全局唯一 + 无规律防枚举越权
6. **train_date 业务键** — 与 `create_time`（技术审计）必须分开，否则跨天买票的时间语义错乱

---

## 📄 License

MIT（学习项目，欢迎参考借鉴）