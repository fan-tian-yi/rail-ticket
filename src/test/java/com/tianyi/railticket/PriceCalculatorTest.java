package com.tianyi.railticket;

import com.tianyi.railticket.common.PriceCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

/** 票价计算：按里程递远递减（锁定拟合系数，误改即红） */
class PriceCalculatorTest {

    /** 拟合样本回代：G547 单趟车的 10 个 OD，偏差应 &lt;5%（实测最大 3.79%） */
    @Test
    void basePrice_fitsRealSamples() {
        assertFits("281.2", 141.00);   // 南京南→上海虹桥
        assertFits("293.5", 137.00);   // 济南西→徐州东
        assertFits("327.6", 158.00);   // 徐州东→南京南
        assertFits("417.9", 194.00);   // 北京南→济南西
        assertFits("608.8", 293.00);   // 徐州东→上海虹桥
        assertFits("621.1", 289.00);   // 济南西→南京南
        assertFits("711.4", 321.00);   // 北京南→徐州东
        assertFits("902.3", 416.00);   // 济南西→上海虹桥
        assertFits("1039.0", 463.00);  // 北京南→南京南
        assertFits("1320.2", 576.00);  // 北京南→上海虹桥
    }

    /** 递远递减：平均单价必须随里程单调递减（系数改坏的唯一判据） */
    @Test
    void basePrice_unitPriceDecreasesWithDistance() {
        BigDecimal prevUnit = null;
        for (int km = 100; km <= 1400; km += 100) {
            BigDecimal d = BigDecimal.valueOf(km);
            BigDecimal unit = PriceCalculator.basePrice(d).divide(d, 4, RoundingMode.HALF_UP);
            if (prevUnit != null) {
                assertThat(unit).as("里程 %s 的平均单价应低于上一档", km).isLessThan(prevUnit);
            }
            prevUnit = unit;
        }
    }

    /** 断言：按里程算出的票价与真实票价偏差 &lt;5% */
    private void assertFits(String km, double realPrice) {
        BigDecimal actual = PriceCalculator.basePrice(new BigDecimal(km));
        double diff = Math.abs(actual.doubleValue() - realPrice) / realPrice;
        assertThat(diff).as("里程 %s 算出 %s，真实 %s", km, actual, realPrice).isLessThan(0.05);
    }
}
