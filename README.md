# 🚄 rail-ticket

> 高并发火车票购票系统 · 简历项目

**Spring Boot 3 + MyBatis-Plus + Redis 高并发 + Flyway 版本化 + 区段共享余票模型**

模拟 12306 的核心业务：车次查询、区间余票、下单扣减、退票、超时关单。技术深度优先于功能覆盖，重点解决「区间共享余票」「高并发扣库存」「并发下的状态流转」三类问题。

---

## ✨ 技术亮点

| 维度 | 实现 | 状态 |
|---|---|---|
| **区间共享余票** | 座位在多个区间上共享（A→B、A→C 不能同时卖）。按「相邻两站」建独立计数器，某区间可售数 = 覆盖它的所有计数器的最小值（等价区间图着色），保证不超卖 | ✅ |
| **Redis 原子扣库存** | Lua 两段式脚本：先全量检查、再逐段扣减，失败不会部分扣减。扣减成功后落 MySQL 订单与流水，落库失败手动回补 Redis | ✅ |
| **里程计价模型** | `票价 = 0.6842 × 里程^0.9392`，指数 < 1 即递远递减。里程由站点经纬度折线累加求得；两个系数用 12306 真实票价做对数线性回归拟合，样本内平均偏差 1.91% | ✅ |
| **CAS 状态机** | 「检查 + 写入」压进同一条 `UPDATE ... WHERE status = 期望值`，影响行数即所有权凭据：`rows=1` 才动库存。退票与超时关单共用这套机制，并发重复退票、定时任务与用户操作撞车都不会重复回补 | ✅ |
| **超时未支付自动关单** | 每分钟扫描到期订单，逐条 CAS 抢占后回补库存并记流水。多实例同时扫描也安全（CAS 保证只有一个成功，**不需要分布式锁**） | ✅ |
| **回补区间取自流水** | 退票/关单的 seg 区间不重新解析车站 ID，而是读 `t_stock_deduction_log` 里当初扣减的那份——避免经停表变动导致 `ID→seq` 漂移，把票还到错误的区间 | ✅ |
| **订单快照** | `t_order` 冗余存出发/到达站与票价，外部表变更不污染历史订单 | ✅ |
| **参数校验分层** | 格式取值（DTO 注解）→ 类型（强类型 + 枚举）→ 业务（Service 手写 + 静态校验类），信任边界收敛在 Controller 入口 | ✅ |
| **支付（模拟）** | 一条 CAS 同时卡两个条件：`status=0` 防重复支付、`expire_time > 支付时间` 防超时支付。两者的条件互斥，所以同一张订单不可能既被支付又被关单 —— 扫描任务只是清理工，不是裁判 | ✅ |
| **会话认证** | Sa-Token 的 token 模式：token 只是一串随机串，真正的会话在 Redis（`satoken:login:token:{token}` → `loginId`）。选它而非 JWT，是因为退票/封号/踢人要求**立即失效**，JWT 要撤销就得再维护一份 Redis 黑名单——状态一点没省掉，反而白背一套签名与刷新逻辑 | ✅ |
| **对账** | `t_stock_deduction_log` 流水 + 定时任务比对 Redis 库存 vs MySQL 订单，漂移自动修正 | ⏳ |
| **MySQL 主从 / Redis 哨兵** | 主写从读 + 自动故障转移 | ⏳ |

---

## 🛠 技术栈

**后端**：Spring Boot 3.5.14 / JDK 21 / MyBatis-Plus 3.5.7 / Flyway / springdoc-openapi / Sa-Token 1.44（认证）

**存储**：MySQL 8（订单与流水）/ Redis 7（库存 + 会话）

**规划中**：Sentinel（限流）、Caffeine（本地缓存）、Redisson

**前端**：Vue 3 + Vite（车次查询 / 三步下单页 / 支付 / 我的订单 / 退票，独立仓库 `rail-ticket-ui`）
> 下单走**独立页面分三步**（填写信息 → 确认支付 → 支付完成），带 15 分钟支付倒计时；不引 vue-router，三个页面用 20 行 hash 路由搞定。
> 没有引入 Element Plus —— 这是定制化 C 端界面，手写了一套基于 CSS 变量的设计系统；Element Plus 的强项是表单密集的后台系统。

---

## 🚀 快速开始

### 环境要求

- JDK 21
- MySQL 8.0+
- Redis 7.0+
- **无需安装 Maven** —— 项目自带 Maven Wrapper（`mvnw` / `mvnw.cmd`），首次运行会自动下载锁定的 Maven 版本

### 启动步骤

```bash
# 1. 克隆仓库
git clone https://github.com/fan-tian-yi/rail-ticket.git
cd rail-ticket

# 2. 配置（拷贝示例，填写 MySQL 与 Redis 密码）
cp src/main/resources/application.yml.example src/main/resources/application.yml

# 3. 先启动 MySQL 与 Redis，再启动应用
#    Flyway 会自动建库、建表并灌入种子数据，零手动操作
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

启动后：

- **Swagger UI**：`http://localhost:8080/swagger-ui.html` —— 可直接调接口
- 库存预热在启动时自动执行，覆盖未来 7 天（窗口由 `Const.PRESALE_DAYS` 控制）
- **测试账号**：`13800000001 / 123456`（用户 A，主测试号）、`13800000002 / 123456`（用户 B，用于验证越权拦截）

> ⚠️ Windows 的 **Git Bash（MSYS）下 `./mvnw` 会失败**（路径自动转换导致 `ClassNotFoundException: classworlds.launcher.Launcher`），请改用 PowerShell/cmd 里的 `mvnw.cmd`。

---

## 🔌 接口一览

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/login` | 登录（手机号 + 密码，BCrypt 校验，签发 token） |
| POST | `/api/auth/logout` | 登出（当前 token 立即失效） |
| GET | `/api/auth/me` | 当前登录用户 |
| GET | `/api/trains` | 车次查询（三档票价按里程现算 + 上架过滤） |
| GET | `/api/inventory/available` | 区间余票（取覆盖区间的 seg 计数器最小值） |
| POST | `/api/orders` | 下单（Lua 原子扣减 → 落库 → 失败回补） |
| POST | `/api/orders/pay` | 支付（模拟渠道；CAS 同时卡「仍待支付」+「未过有效期」） |
| POST | `/api/orders/refund` | 退票（待支付→已取消，已支付→已退票） |
| POST | `/api/orders/close-timeout` | 手动触发超时关单（运维用，正常由定时任务执行） |
| POST | `/api/inventory/warm-up` | 手动预热库存（运维用） |

**认证方式**：除 `/api/auth/login`、`/api/trains`、`/api/inventory/available` 外，所有接口都要求登录，请求头带 `Authorization: Bearer <token>`（token 来自登录接口返回值）。未登录或 token 失效返回 `code=40101`，登录失败返回 `code=40100`。

统一响应体 `{ code, message, data }`：HTTP 恒为 200，`code=0` 为成功，非 0 时 `message` 是可直接展示给用户的中文提示。

---

## 📦 数据库设计

7 张表 + 完整索引 + Flyway 版本化迁移：

| 表 | 作用 |
|---|---|
| `t_station` | 站点（含电报码、经纬度） |
| `t_train` | 车次模板（无发车日期，每日复用时刻表） |
| `t_train_station` | 车次经停（时刻表 + 累计里程 `distance_cum`）★核心表 |
| `t_user` | 用户（手机号登录 + BCrypt 密码密文） |
| `t_passenger` | 乘客（一人多张身份证） |
| `t_order` | 订单（订单快照冻结业务事实 + `expire_time` 支撑超时关单） |
| `t_stock_deduction_log` | 库存流水（`delta` 正负成对，退票/关单的回补依据 + 对账依据） |

详细见 `src/main/resources/db/migration/V1__init_schema.sql`，注释完整。**迁移文件一旦被应用过就不可修改**（Flyway 对全文算 checksum），要改结构一律新建 `V{N+1}`。

---

## 🗺 路线图

- **Phase 0** 文档 ✅
- **Phase 1** 工程骨架 + 建表 + 种子数据 ✅
- **Phase 2** 核心闭环 ★
  - ✅ 区段库存模型（per-seg 计数器，可售数 = 覆盖区间的最小值）
  - ✅ Lua 原子扣减 + 下单落库 + 失败回补
  - ✅ 车次查询 / 余票查询（三档票价按里程现算）
  - ✅ 退票（状态流转 + CAS 防重复退票 + 从扣减流水回补）
  - ✅ 支付（模拟渠道，CAS 防重复支付 + 防超时支付）
  - ✅ 超时未支付自动关单（每分钟扫描 + CAS 抢占 + 回补库存）
  - ✅ 登录（Sa-Token，会话存 Redis，token 走 `Authorization` 头；下单/退票/支付的 userId 全部改为从会话取）
  - ✅ 前端（车次查询 / 三步下单页 / 支付 / 我的订单 / 退票）
- **Phase 3** 高并发武器 ★：Sentinel 限流 + Caffeine 降级 + 压测报告
- **Phase 4** 容灾运维：Redis 哨兵 + MySQL 主从 + 对账 + 云服务器部署
- **Phase 5** 加分项：中转寻路、模拟地图、分库分表、RocketMQ、AI

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
8. **状态流转一律用 CAS，不用「先查再改」** — `UPDATE ... WHERE status = 期望值` 把检查和写入压进同一条 SQL。若写成"先 SELECT 判断、再 UPDATE"，两个并发请求都会在对方写入前通过检查，导致库存被回补两次。CAS 还顺带免疫多实例部署
9. **定时任务不承担正确性** — 超时关单靠扫描，但「支付」会自己再判一次 `expire_time`。因为扫描有延迟窗口（15:00 到期、最晚 16:00 才被关），只信 `status` 就会放过已过期的支付。正确性由 SQL 的条件互斥保证，不依赖后台任务的及时性
10. **回补区间从流水读，不从车站 ID 反推** — 订单只存 `from/to_station_id`，而回补要的是 seg 序号。若重新解析，经停表一旦增删站，同一车站 ID 会翻出不同序号，票就还到了错误的区间且**永久错乱**（日志和状态全都正常，只有 Redis 慢慢失真）
11. **认证选 Sa-Token 的 token 模式，不用 JWT、不用 Cookie** — JWT 用无状态换水平扩展，代价是签发后到过期前不可撤销；而退票、封号、强制下线都要求立即生效，要撤销就得引 Redis 黑名单，等于状态没省掉还多背一套签名与刷新逻辑。Sa-Token 的 token 是不透明随机串、会话在 Redis，天然支持踢人与自动续期。凭证走 `Authorization` 头而非 Cookie，避开前后端分离下的跨域配置与 CSRF
12. **会话里只存 loginId，不存用户资料** — 塞进会话的数据会陈旧（改了昵称、封了号都不更新），得在每个改动点记得同步。只放不变的标识 `userId`，其余按需查库

---

## 📄 License

MIT（学习项目，欢迎参考借鉴）
