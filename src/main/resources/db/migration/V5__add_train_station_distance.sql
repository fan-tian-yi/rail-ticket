# V5__add_train_station_distance.sql
# 给车次经停表加「累计里程」，为按里程计价（递远递减）做准备

ALTER TABLE t_train_station
    ADD COLUMN distance_cum DECIMAL(8,1) NULL
        COMMENT '从起点到本站的累计里程(km)，经纬度折线累加×1.11 近似' AFTER price_cum;

-- ===== 2001 G101 北京南→上海虹桥（经天津南/济南西/徐州东/南京南）=====
UPDATE t_train_station SET distance_cum =    0.0 WHERE id = 5001;   -- 北京南
UPDATE t_train_station SET distance_cum =  114.8 WHERE id = 5002;   -- 天津南
UPDATE t_train_station SET distance_cum =  417.9 WHERE id = 5003;   -- 济南西
UPDATE t_train_station SET distance_cum =  711.4 WHERE id = 5004;   -- 徐州东
UPDATE t_train_station SET distance_cum = 1039.0 WHERE id = 5005;   -- 南京南
UPDATE t_train_station SET distance_cum = 1320.2 WHERE id = 5006;   -- 上海虹桥

-- ===== 2002 G103 北京南→上海虹桥（经苏州北）=====
UPDATE t_train_station SET distance_cum =    0.0 WHERE id = 5010;   -- 北京南
UPDATE t_train_station SET distance_cum =  114.8 WHERE id = 5011;   -- 天津南
UPDATE t_train_station SET distance_cum =  417.9 WHERE id = 5012;   -- 济南西
UPDATE t_train_station SET distance_cum =  711.4 WHERE id = 5013;   -- 徐州东
UPDATE t_train_station SET distance_cum = 1039.0 WHERE id = 5014;   -- 南京南
UPDATE t_train_station SET distance_cum = 1251.4 WHERE id = 5015;   -- 苏州北
UPDATE t_train_station SET distance_cum = 1320.2 WHERE id = 5016;   -- 上海虹桥

-- ===== 2003 G105 北京南→上海虹桥（越站：只停济南西/南京南）=====
-- 对比 2001：同为北京南→上海虹桥，因少停两站、折线更直，里程更短（1293.1 vs 1320.2）
UPDATE t_train_station SET distance_cum =    0.0 WHERE id = 5020;   -- 北京南
UPDATE t_train_station SET distance_cum =  403.5 WHERE id = 5021;   -- 济南西
UPDATE t_train_station SET distance_cum = 1011.9 WHERE id = 5022;   -- 南京南
UPDATE t_train_station SET distance_cum = 1293.1 WHERE id = 5023;   -- 上海虹桥

-- ===== 2004 G201 西安北→上海虹桥 =====
UPDATE t_train_station SET distance_cum =    0.0 WHERE id = 5030;   -- 西安北
UPDATE t_train_station SET distance_cum =  493.5 WHERE id = 5031;   -- 郑州东
UPDATE t_train_station SET distance_cum =  850.0 WHERE id = 5032;   -- 徐州东
UPDATE t_train_station SET distance_cum = 1177.6 WHERE id = 5033;   -- 南京南
UPDATE t_train_station SET distance_cum = 1458.8 WHERE id = 5034;   -- 上海虹桥

-- ===== 2005 G301 武汉→上海虹桥 =====
UPDATE t_train_station SET distance_cum =    0.0 WHERE id = 5040;   -- 武汉
UPDATE t_train_station SET distance_cum =  343.7 WHERE id = 5041;   -- 合肥南
UPDATE t_train_station SET distance_cum =  498.9 WHERE id = 5042;   -- 南京南
UPDATE t_train_station SET distance_cum =  753.0 WHERE id = 5043;   -- 杭州东
UPDATE t_train_station SET distance_cum =  915.0 WHERE id = 5044;   -- 上海虹桥

-- ===== 2006 G401 北京南→广州南（经上海/杭州/长沙，绕行大）=====
UPDATE t_train_station SET distance_cum =    0.0 WHERE id = 5050;   -- 北京南
UPDATE t_train_station SET distance_cum = 1006.3 WHERE id = 5051;   -- 南京南
UPDATE t_train_station SET distance_cum = 1260.4 WHERE id = 5052;   -- 杭州东
UPDATE t_train_station SET distance_cum = 1422.4 WHERE id = 5053;   -- 上海虹桥
UPDATE t_train_station SET distance_cum = 2383.3 WHERE id = 5054;   -- 长沙南
UPDATE t_train_station SET distance_cum = 3021.9 WHERE id = 5055;   -- 广州南

-- ===== 2007 G501 成都东→广州南 =====
UPDATE t_train_station SET distance_cum =    0.0 WHERE id = 5060;   -- 成都东
UPDATE t_train_station SET distance_cum = 1089.3 WHERE id = 5061;   -- 武汉
UPDATE t_train_station SET distance_cum = 1422.9 WHERE id = 5062;   -- 长沙南
UPDATE t_train_station SET distance_cum = 2061.5 WHERE id = 5063;   -- 广州南
