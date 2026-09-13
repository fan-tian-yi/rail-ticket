package com.tianyi.railticket.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 票价 = A × 里程^B（幂函数；平均单价随里程递减）
 * 系数由对数线性回归拟合 12306 真实二等座票价得出：ln(票价) = ln A + B·ln(里程)
 * 解得 A = 0.684236、B = 0.939232，对数空间 R² = 0.9978，残差标准误 2.54%
 * 等价拆解：票价 = 0.6842(基准牌价) × 里程 × 里程^(-0.0608)
 */
public final class PriceCalculator {

    private PriceCalculator() {
    }

    /** 拟合系数（基准牌价，不是"每公里单价"） */
    private static final double A = 0.6842;

    /** 拟合系数（幂指数，<1 即递远递减） */
    private static final double B = 0.9392;

    /** 二等座基准票价 = A × 里程^B，保留 2 位四舍五入 */
    public static BigDecimal basePrice(BigDecimal km) {
        // Math.pow 只吃 double，故中间走 double，立即转回 BigDecimal 定标（valueOf 走 toString，避免二进制误差）
        double raw = A * Math.pow(km.doubleValue(), B);
        return BigDecimal.valueOf(raw).setScale(2, RoundingMode.HALF_UP);
    }
}
