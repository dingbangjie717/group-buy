# 鲜购到家 · 社区团购与点餐系统

Spring Boot 3.5 + Vue 3 前后端分离单体项目，覆盖「商品浏览 → 购物车 → 下单 → 支付 → 商家接单配送 → 完成」完整交易链路，重点解决**库存超卖**与**订单超时取消**。

## 一、技术栈

| 层次 | 技术 |
| --- | --- |
| 后端 | Spring Boot 3.5.6、Spring Security 6、MyBatis-Plus 3.5.7、MySQL 8 |
| 缓存 | Redis（购物车 Hash、库存预扣、下单幂等锁） |
| 消息 | RabbitMQ（TTL + 死信交换机实现订单超时取消） |
| 实时推送 | WebSocket（订单状态变更推送） |
| 前端 | Vue 3 + Vite 5 + Element Plus + Pinia + Vue Router + Axios |
| 其它 | Lua 脚本、乐观锁设计、JWT、Hutool、Jakarta Validation |

## 二、本地启动

### 前置环境
- JDK 17+、Maven 3.8+
- MySQL 8.0：执行 `sql/group_buy.sql`
- Redis（默认 127.0.0.1:6379，db2）
- RabbitMQ（默认 127.0.0.1:5672，guest/guest）
- Node 18+

> RabbitMQ 的交换机与队列在应用启动时由 `RabbitConfig` 自动声明，无需手动创建。

### 启动后端
```bash
cd group-buy
mvn spring-boot:run
# 服务端口 8082，context-path /api
```

### 启动前端
```bash
cd mall-web
npm install
npm run dev     # http://localhost:5174
```

### 演示账号（密码统一 123456）
| 账号 | 角色 | 可做什么 |
| --- | --- | --- |
| user | 买家 | 浏览商品、加购、领券、下单、支付、查看订单 |
| merchant | 商家 | 商品上下架、库存维护、接单 / 配送 / 完成 |
| admin | 平台管理员 | 商家全部权限 + 全平台数据 |

## 三、目录结构

```
group-buy
├── sql/group_buy.sql
└── src/main/java/com/groupbuy
    ├── common          # 统一响应、全局异常、常量、用户上下文、实体基类
    ├── config          # Security、Redis（含 Lua 脚本）、MyBatis-Plus、RabbitMQ、WebSocket、JWT 与订单配置
    ├── security        # LoginUser、JWT 工具、JWT 过滤器
    ├── websocket       # 按 userId 维护连接的订单推送
    └── module
        ├── auth        # 多角色登录 / 注册
        ├── user        # 用户（一用户一角色轻量模型）
        ├── goods       # 店铺、分类、商品、库存
        ├── cart        # 购物车（Redis Hash）
        ├── coupon      # 优惠券领取与核销
        └── order       # 下单、支付、状态机、超时取消消费者
```

## 四、核心设计（面试常问点）

### 1. 防超卖三板斧
1. **Redis Lua 脚本原子预扣**：`判断库存 → 扣减` 必须是原子操作，用 Lua 保证。返回 `-1` 未预热（回源 DB 后重试）、`-2` 库存不足、>=0 剩余库存。
2. **数据库条件更新**：`UPDATE goods SET stock = stock - #{num} WHERE id = #{id} AND stock >= #{num}`，影响行数为 0 即库存不足。用「条件更新」而不是「先查后改」，避免并发下把 stock 改成负数。
3. **下单幂等**：`SETNX lock:order:create:{userId}` 5 秒锁，防止用户重复点击生成重复订单。

任意一步失败都会把已预扣的 Redis 库存还回去（`rollbackStock`）。

### 2. 为什么购物车放 Redis
购物车是「高频读写 + 允许丢失」的数据。用 Redis Hash `cart:user:{userId}`（field = 商品 ID），
改数量时只改一个 field，不用整条覆盖；30 天未操作自动过期。

### 3. 订单超时未支付怎么取消
用 RabbitMQ 的 **TTL + 死信交换机**：
1. 下单成功后把订单号投递到 `order.delay.queue`（无消费者，设置 `x-message-ttl`）
2. 消息过期后经 `x-dead-letter-exchange` 转发到 `order.exchange`，routingKey = `order.timeout`
3. `order.timeout.queue` 的消费端收到订单号，**回查订单状态**，仍是待支付才取消并回补库存

> 关键点是第 3 步的回查：用户可能在消息过期前刚好完成支付。
> TTL 是队列级固定延迟；若要「每单不同延迟」，改用 `rabbitmq_delayed_message_exchange` 插件的 `x-delayed-message`。

### 4. 库存回补
订单取消 / 超时取消 / 退款时，Redis 与 MySQL 两边都要还回去：
`goodsMapper.restoreStock` + `restoreStockScript`（Lua `incrby`）。

### 5. 订单状态机
```
0 待支付 → 1 已支付 → 2 商家已接单 → 3 配送中 → 4 已完成
   ↓（超时 / 主动）
5 已取消
```
每次状态变更都通过 WebSocket 向该用户推送消息，前端收到后刷新列表并弹提示。

### 6. 金额与快照
`order_item` 保存下单时的商品名与单价快照，后续商家改价不影响历史订单。
金额统一用 `BigDecimal`，四舍五入到 2 位。

### 7. 优惠券防超发
领取时用 `UPDATE coupon SET remain = remain - 1 WHERE id = #{id} AND remain > 0`，
影响行数为 0 即已被抢完。满减券校验门槛，折扣券按折扣率计算优惠额。

## 五、接口一览

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/auth/login` `/auth/register` | 登录 / 注册 |
| GET | `/goods/list` `/goods/{id}/detail` | 商品列表 / 详情（公开） |
| GET | `/shop/list` `/category/list` | 店铺 / 分类（公开） |
| GET/POST/DELETE | `/cart/list` `/cart/add` `/cart/quantity` `/cart/{id}` | 购物车 |
| POST | `/order/preview` `/order/create` | 试算金额 / 下单 |
| POST | `/order/pay/{orderNo}` `/order/cancel/{id}` | 支付 / 取消 |
| GET | `/order/my` `/order/{id}` | 我的订单 / 详情 |
| GET/POST | `/coupon/list` `/coupon/receive/{id}` `/coupon/my` | 领券中心 |
| POST | `/merchant/goods/save` `/merchant/goods/status` | 商品维护（商家） |
| GET | `/order/merchant/list` | 商家订单列表 |
| POST | `/order/merchant/{id}/accept` `/deliver` `/finish` | 商家推进订单状态 |

WebSocket 连接地址：`ws://localhost:8082/api/ws/order?userId={userId}`

## 六、已知边界（可继续扩展）

- 支付为模拟实现（直接改状态），未接入微信 / 支付宝沙箱
- WebSocket 单机用 `ConcurrentHashMap` 维护连接；多实例部署需换成 Redis Pub/Sub 或消息中间件广播
- 未做订单分库分表、未接 Sentinel 限流、未做幂等表
- 商品图片为 URL 直填，未实现上传接口
