# 计价已改为按区间里程现算（PriceCalculator），price_cum 不再参与任何计算

ALTER TABLE t_train_station DROP COLUMN price_cum;