-- ==========================================================
-- 鲜购到家 group_buy 数据库脚本
-- MySQL 8.0+
-- 执行方式：mysql -uroot -p < group_buy.sql
-- ==========================================================
CREATE DATABASE IF NOT EXISTS group_buy DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE group_buy;

SET NAMES utf8mb4;

-- ---------------------- 用户域 ----------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL COMMENT '登录账号',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密密码',
    nickname    VARCHAR(50) COMMENT '昵称',
    phone       VARCHAR(20) COMMENT '手机号',
    avatar      VARCHAR(255) COMMENT '头像',
    role_code   VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色：USER/MERCHANT/ADMIN',
    shop_id     BIGINT COMMENT '所属店铺（商家角色）',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB COMMENT '用户表';

-- ---------------------- 店铺与商品 ----------------------
DROP TABLE IF EXISTS shop;
CREATE TABLE shop
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(100) NOT NULL COMMENT '店铺名称',
    logo        VARCHAR(255) COMMENT '店铺 logo',
    address     VARCHAR(255) COMMENT '店铺地址',
    phone       VARCHAR(20) COMMENT '联系电话',
    description VARCHAR(500) COMMENT '店铺简介',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '1营业中 0休息',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT     NOT NULL DEFAULT 0
) ENGINE = InnoDB COMMENT '店铺表';

DROP TABLE IF EXISTS category;
CREATE TABLE category
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    shop_id     BIGINT COMMENT '所属店铺，为空表示公共分类',
    name        VARCHAR(50) NOT NULL COMMENT '分类名称',
    sort        INT     NOT NULL DEFAULT 0 COMMENT '排序',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT     NOT NULL DEFAULT 0,
    KEY idx_shop (shop_id)
) ENGINE = InnoDB COMMENT '商品分类表';

DROP TABLE IF EXISTS goods;
CREATE TABLE goods
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    shop_id     BIGINT        NOT NULL COMMENT '店铺ID',
    category_id BIGINT COMMENT '分类ID',
    name        VARCHAR(100)  NOT NULL COMMENT '商品名称',
    image       VARCHAR(255) COMMENT '商品图片',
    description VARCHAR(1000) COMMENT '商品描述',
    price       DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '单价',
    stock       INT     NOT NULL DEFAULT 0 COMMENT '库存',
    sales       INT     NOT NULL DEFAULT 0 COMMENT '销量',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
    version     INT     NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    KEY idx_shop (shop_id),
    KEY idx_category (category_id)
) ENGINE = InnoDB COMMENT '商品表';

-- ---------------------- 优惠券 ----------------------
DROP TABLE IF EXISTS coupon;
CREATE TABLE coupon
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(100)   NOT NULL COMMENT '优惠券名称',
    type        TINYINT       NOT NULL DEFAULT 1 COMMENT '1满减 2折扣',
    threshold   DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '使用门槛',
    discount    DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '满减金额 / 折扣率(0.85=85折)',
    total       INT     NOT NULL DEFAULT 0 COMMENT '发行总量',
    remain      INT     NOT NULL DEFAULT 0 COMMENT '剩余数量',
    start_time  DATETIME COMMENT '生效时间',
    end_time    DATETIME COMMENT '失效时间',
    status      TINYINT       NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0
) ENGINE = InnoDB COMMENT '优惠券表';

DROP TABLE IF EXISTS user_coupon;
CREATE TABLE user_coupon
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id      BIGINT NOT NULL COMMENT '用户ID',
    coupon_id    BIGINT NOT NULL COMMENT '优惠券ID',
    status       TINYINT NOT NULL DEFAULT 0 COMMENT '0未使用 1已使用 2已过期',
    obtain_time  DATETIME COMMENT '领取时间',
    use_time     DATETIME COMMENT '使用时间',
    order_id     BIGINT COMMENT '使用的订单ID',
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT NOT NULL DEFAULT 0,
    KEY idx_user (user_id)
) ENGINE = InnoDB COMMENT '用户优惠券表';

-- ---------------------- 订单 ----------------------
DROP TABLE IF EXISTS orders;
CREATE TABLE orders
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    order_no        VARCHAR(64)    NOT NULL COMMENT '订单号',
    user_id         BIGINT         NOT NULL COMMENT '下单用户',
    shop_id         BIGINT         NOT NULL COMMENT '店铺ID',
    goods_amount    DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '商品总额',
    discount_amount DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '优惠金额',
    pay_amount      DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '实付金额',
    user_coupon_id  BIGINT COMMENT '使用的用户优惠券ID',
    status          TINYINT        NOT NULL DEFAULT 0 COMMENT '0待支付 1已支付 2已接单 3配送中 4已完成 5已取消',
    pay_type        TINYINT COMMENT '支付方式：1余额 2微信 3支付宝',
    pay_time        DATETIME COMMENT '支付时间',
    receiver        VARCHAR(50) COMMENT '收货人',
    phone           VARCHAR(20) COMMENT '联系电话',
    address         VARCHAR(255) COMMENT '收货地址',
    remark          VARCHAR(500) COMMENT '备注',
    create_time     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT        NOT NULL DEFAULT 0,
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user (user_id),
    KEY idx_shop (shop_id),
    KEY idx_status (status)
) ENGINE = InnoDB COMMENT '订单表';

DROP TABLE IF EXISTS order_item;
CREATE TABLE order_item
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    order_id    BIGINT         NOT NULL COMMENT '订单ID',
    goods_id    BIGINT         NOT NULL COMMENT '商品ID',
    goods_name  VARCHAR(100) COMMENT '商品名称快照',
    goods_image VARCHAR(255) COMMENT '商品图片快照',
    price       DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '下单时单价',
    quantity    INT     NOT NULL DEFAULT 1 COMMENT '购买数量',
    amount      DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '小计',
    create_time DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT       NOT NULL DEFAULT 0,
    KEY idx_order (order_id)
) ENGINE = InnoDB COMMENT '订单明细表';

-- ==========================================================
-- 初始化数据
-- 密码统一为 123456（BCrypt 加密）
-- ==========================================================
INSERT INTO shop (id, name, address, phone, description, status)
VALUES (1, '鲜购社区店（望京店）', '北京市朝阳区望京 SOHO T1', '13800000001', '每日 6:00-22:00 配送，30 分钟送达', 1),
       (2, '老碗面馆', '北京市朝阳区阜通东大街 6 号', '13800000002', '手工面食，现点现做', 1);

INSERT INTO category (id, shop_id, name, sort)
VALUES (1, 1, '时令蔬菜', 1),
       (2, 1, '肉禽蛋品', 2),
       (3, 1, '奶制品', 3),
       (4, 2, '招牌面食', 1),
       (5, 2, '小菜凉菜', 2);

INSERT INTO goods (id, shop_id, category_id, name, description, price, stock, sales, status)
VALUES (1, 1, 1, '有机小油菜 500g', '当日采摘，冷链配送', 6.80, 200, 128, 1),
       (2, 1, 1, '山东黄瓜 1kg', '脆嫩多汁', 8.50, 150, 96, 1),
       (3, 1, 2, '冷鲜猪里脊 500g', '排酸冷鲜肉', 28.00, 60, 210, 1),
       (4, 1, 2, '土鸡蛋 10 枚', '散养土鸡', 19.90, 100, 175, 1),
       (5, 1, 3, '巴氏鲜牛奶 950ml', '72 小时保质', 15.80, 80, 143, 1),
       (6, 2, 4, '油泼扯面', '秦镇手工扯面', 18.00, 999, 320, 1),
       (7, 2, 4, '番茄鸡蛋面', '现炒番茄', 20.00, 999, 268, 1),
       (8, 2, 5, '凉拌黄瓜', '爽口解腻', 8.00, 500, 154, 1);

INSERT INTO coupon (id, name, type, threshold, discount, total, remain, start_time, end_time, status)
VALUES (1, '满 50 减 8', 1, 50.00, 8.00, 1000, 1000, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1),
       (2, '全场 9 折', 2, 0.00, 0.90, 500, 500, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), 1),
       (3, '新客满 30 减 5', 1, 30.00, 5.00, 300, 300, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1);

-- 账号：admin / merchant / user，密码均为 123456
INSERT INTO sys_user (id, username, password, nickname, phone, role_code, shop_id, status)
VALUES (1, 'admin', '$2a$10$rgAof4/crWra4Xk0nHSM0OP.y2/gZ1VORlr7o8I3QUDzMig6vsGt.', '平台管理员', '13900000001', 'ADMIN', NULL, 1),
       (2, 'merchant', '$2a$10$rgAof4/crWra4Xk0nHSM0OP.y2/gZ1VORlr7o8I3QUDzMig6vsGt.', '望京店店主', '13900000002', 'MERCHANT', 1, 1),
       (3, 'user', '$2a$10$rgAof4/crWra4Xk0nHSM0OP.y2/gZ1VORlr7o8I3QUDzMig6vsGt.', '王小明', '13900000003', 'USER', NULL, 1);
