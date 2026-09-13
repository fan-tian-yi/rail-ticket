# 🚄 rail-ticket

> 高并发火车票购票系统 · 简历项目

**Spring Boot 3 + MyBatis-Plus + Redis 高并发 + Flyway 版本化 + 区段共享余票模型**

模拟 12306 的核心业务：车次查询、区间余票查询、下单扣减、退票对账。技术深度优先于功能覆盖，重点解决"区间共享余票"与"高并发扣库存"两大难题。

---

## ✨ 技术亮点

| 维度 | 实现 | 状态 |
|---|---|---|
| **区间共享余票** | 座位在多个区间上共享（A→B、A→C 不能同时卖）。按「相邻两站」建独立计数器，某区间可售数 = 覆盖它的所有计数器的最小值（等价区间图着色），保证不超卖 | ✅ |
| **Redis 原子扣库存** | Lua 两段式脚本：先全量检查、再逐段扣减，失败不会部分扣减。扣减成功后落 MySQL 订单与流水，落库失败手动回补 Redis | ✅ |
| **里程计价模型** | `票价 = 0.6842 × 里程^0.9392`，指数 < 1 即递远递减。里程由站点经纬度折线累加求得；两个系数用 12306 真实票价做对数线性回归拟合，样本内平均偏差 1.91% | ✅ |
| **订单快照** | `t_order` 冗余存出发/到达站与票价，外部表变更不污染历史订单 | ✅ |
| **参数校验分层** | 格式取值（DTO 注解）→ 类型（强类型 + 枚举）→ 业务（Service 手写 + 静态校验类），信任边界收敛在 Controller 入口 | ✅ |
| **乐观锁** | 支付用 `UPDATE ... WHERE status = WAIT_PAY` + 影响行数做 CAS，防并发重复支付 | ⏳ |
| **对账** | `t_stock_deduction_log` 流水 + 定时任务比对 Redis 库存 vs MySQL 订单，漂移自动修正 | ⏳ |
| **MySQL 主从 / Redis 哨兵** | 主写从读 + 自动故障转移 | ⏳ |

---

## 🛠 技术栈

**后端**：Spring Boot 3.5.14 / JDK 21 / MyBatis-Plus 3.5.7 / Flyway / springdoc-openapi

**存储**：MySQL 8 / Redis 7

**规划中**：Sa-Token（认证）、Sentinel（限流）、Caffeine（本地缓存）、Redisson（分布式锁）

**前端**：Vue 3 + Element Plus + ECharts（未开始）

---

## 🚀 快速开始

### 环境要求

- JDK 21
- MySQL 8.0+
- Redis 7.0+
- Maven 3.8+

### 启动步骤

```bash
# 1. 克隆仓库
git clone https://github.com/fan-tian-yi/rail-ticket.git
cd rail-ticket

# 2. 配置（拷贝示例，填写 MySQL 与 Redis 密码）
cp src/main/resources/application.yml.example src/main/resources/application.yml

# 3. 先启动 MySQL 与 Redis，再启动应用
#    Flyway 会自动建库、建表并灌入种子数据，零手动操作
mvn spring-boot:run
```

启动后：

- **Swagger UI**：`http://localhost:8080/swagger-ui.html` —— 可直接调接口
- 库存预热在启动时自动执行，覆盖未来 7 天（窗口由 `Const.PRESALE_DAYS` 控制）

---

## 📦 数据库设计

7 张表 + 完整索引 + Flyway 版本化迁移：

| 表 | 作用 |
|---|---|
| `t_station` | 站点（含电报码、经纬度） |
| `t_train` | 车次模板（无发车日期，每日复用时刻表） |
| `t_train_station` | 车次经停（时刻表 + 累计里程 `distance_cum`）★核心表 |
| `t_user` | 用户（手机号登录） |
| `t_passenger` | 乘客（一人多张身份证） |
| `t_order` | 订单（订单快照冻结业务事实） |
| `t_stock_deduction_log` | 库存流水（对账依据，追加型） |

详细见 `src/main/resources/db/migration/V1__init_schema.sql`，注释完整。

---

## 🗺 路线图

- **Phase 0** 文档 ✅
- **Phase 1** 工程骨架 + 建表 + 种子数据 ✅
- **Phase 2** 核心闭环 ★
  - ✅ 区段库存模型（per-seg 计数器，可售数 = 覆盖区间的最小值）
  - ✅ Lua 原子扣减 + 下单落库 + 失败回补
  - ✅ 车次查询 / 余票查询（三档票价按里程现算）
  - ⏳ 退票（库存回补 + 状态流转 + 重复退票防护）
  - ⏳ 登录（当前为固定测试用户，待接 Sa-Token）
- **Phase 3** 高并发武器 ★：Sentinel 限流 + Caffeine 降级 + 压测报告
- **Phase 4** 容灾运维：Redis 哨兵 + MySQL 主从 + 对账 + 云服务器部署
- **Phase 5** 加分项：中转寻路、模拟地图、支付、分库分表、RocketMQ、AI

---

## 📝 设计决策

1. **主键 BIGINT 雪花 ID**（`id-type: assign_id`）— 为分库分表铺路 + 防枚举越权
2. **席别配置用 JSON 列** — 高频读、低频写，无需按席别做行级统计
3. **经停时刻用 TIME** — 时刻表是"每日模板"，具体实例由 `order.train_date` 拼接
4. **票价不落库，按里程现算** — `票价 = PriceCalculator.basePrice(里程) × 席别倍率`，服务端永远重算金额、不信前端。里程存 `distance_cum`（相邻站经纬度折线累加 × 1.11 绕行系数）
   - 早期版本曾用「累计票价相减」（`price_cum` 字段），因递远递减的参照点错误（把长途优惠错误地送给了中途上车的乘客，实测系统性低估 4~13%）而被废弃，V6 迁移已删除该字段
5. **order_no 雪花生成** — 全局唯一 + 无规律防枚举越权
6. **train_date 业务键** — 与 `create_time`（技术审计）必须分开，否则跨天买票的时间语义错乱
7. **库存以 Redis 为准** — MySQL 只存订单与扣减流水；两者之间的漂移由流水表 + 定时对账修正

---

## 📄 License

MIT（学习项目，欢迎参考借鉴）