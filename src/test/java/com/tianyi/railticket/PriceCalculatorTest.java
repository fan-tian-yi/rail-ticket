package com.tianyi.railticket;

import com.tianyi.railticket.common.PriceCalculator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PriceCalculatorTest {
    /** 真实获取的数据，前面是距离km，后面是价格 */
    private static final double[][] SAMPLES = {
            {281.2, 141}, {293.5, 137}, {327.6, 158}, {417.9, 194}, {608.8, 293},
            {621.1, 289}, {711.4, 321}, {902.3, 416}, {1039.0, 463}, {1320.2, 576},
    };

    /** 测试价格计算方法，检测误差是否在5％内 */
    @Test
    public void testBasePrice(){
        for(double[] sample : SAMPLES) {
            BigDecimal actual = PriceCalculator.basePrice(BigDecimal.valueOf(sample[0]));
            //计算5％误差值
            BigDecimal tolerance = BigDecimal.valueOf(sample[1]).multiply(BigDecimal.valueOf(0.05));
            assertThat(actual).as("km="+sample[0]).isCloseTo(BigDecimal.valueOf(sample[1]), within(tolerance));
        }
    }

    /** 确保价格随里程增加 */
    @Test
    public void testBiggerPrice(){
        BigDecimal prev = null;
        for (int km = 100; km <= 1400; km += 100) {
            BigDecimal cur = PriceCalculator.basePrice(BigDecimal.valueOf(km));
            if (prev != null) {
                assertThat(cur).as("km=" + km).isGreaterThan(prev);
            }
            prev = cur;
        }
    }

    /** 确保价格随里程增加逐渐缓慢上涨 */
    @Test
    public void testSlowerPrice(){
        BigDecimal prevRate = null;
        for (int km = 100; km <= 1400; km += 100) {
            BigDecimal cur = PriceCalculator.basePrice(BigDecimal.valueOf(km));
            BigDecimal curRate = cur.divide(BigDecimal.valueOf(km), 6, RoundingMode.HALF_UP);
            if (prevRate != null) {
                assertThat(curRate).as("km=" + km).isLessThan(prevRate);
            }
            prevRate = curRate;
        }
    }

    /** 比较边界的计算正确 */
    @Test
    public void testBoundaries(){
        BigDecimal basePrice = PriceCalculator.basePrice(BigDecimal.valueOf(0));
        assertThat(basePrice).as("km=0").isEqualByComparingTo("0.00");
    }
}
