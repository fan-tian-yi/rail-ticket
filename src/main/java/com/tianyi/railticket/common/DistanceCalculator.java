package com.tianyi.railticket.common;

import com.tianyi.railticket.entity.StationDO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 里程 = 相邻站 Haversine 直线距离 × 绕行系数，按经停顺序折线累加（首站固定 0）
 * 绕行系数按京沪全程标定：折线直线 1189.4km ↔ 真实铁路里程 1318km → 1.108，取 1.11（全长误差 0.17%）
 * ⚠️ 必须用「相邻站折线累加」，不能用「本站到起点的直线距离」——中间站绕出去时后者会算小，而里程必须单调递增
 */
public final class DistanceCalculator {

    private DistanceCalculator() {
    }

    /** 地球平均半径（km） */
    private static final double EARTH_RADIUS_KM = 6371.0;

    /** 绕行系数：直线距离 → 铁路里程的近似放大倍数（逐段实际在 1.007~1.267 波动） */
    private static final double DETOUR = 1.11;

    /** 两站球面（大圆）距离，单位 km */
    public static double haversine(double lat1, double lng1, double lat2, double lng2) {
        double p1 = Math.toRadians(lat1);
        double p2 = Math.toRadians(lat2);
        double dp = Math.toRadians(lat2 - lat1);
        double dl = Math.toRadians(lng2 - lng1);
        double h = Math.sin(dp / 2) * Math.sin(dp / 2)
                + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(h));
    }

    /** 按经停顺序折线累加，返回每站累计里程（与入参一一对应，保留 1 位小数） */
    public static List<BigDecimal> cumulative(List<StationDO> stops) {
        List<BigDecimal> result = new ArrayList<>(stops.size());
        if (stops.isEmpty()) {
            return result;
        }
        // cum 全程保持 double，只在写回时定标；若逐站取整再累加会放大误差
        double cum = 0.0;
        result.add(BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP));
        for (int i = 1; i < stops.size(); i++) {
            StationDO prev = stops.get(i - 1);
            StationDO cur = stops.get(i);
            cum += haversine(prev.getLat().doubleValue(), prev.getLng().doubleValue(),
                    cur.getLat().doubleValue(), cur.getLng().doubleValue()) * DETOUR;
            result.add(BigDecimal.valueOf(cum).setScale(1, RoundingMode.HALF_UP));
        }
        return result;
    }
}
