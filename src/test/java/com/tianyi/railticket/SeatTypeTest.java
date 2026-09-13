package com.tianyi.railticket;

import com.tianyi.railticket.common.PriceCalculator;
import com.tianyi.railticket.common.SeatType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** 席别倍率：锁定 12306 实测区间，手误改错即红 */
class SeatTypeTest {

    /** 一等座倍率应落在实测区间内（10 个 OD：1.6782~1.6835） */
    @Test
    void firstRate_matchesRealData() {
        assertThat(SeatType.FIRST.getRate().doubleValue())
                .as("一等/二等 实测均值 1.680")
                .isBetween(1.67, 1.69);
    }

    /** 商务座倍率应落在实测区间内（10 个 OD：3.7372~3.7532） */
    @Test
    void businessRate_matchesRealData() {
        assertThat(SeatType.BUSINESS.getRate().doubleValue())
                .as("商务/二等 实测均值 3.746")
                .isBetween(3.73, 3.76);
    }

    /** 倍率必须严格递增：二等 &lt; 一等 &lt; 商务 */
    @Test
    void rates_increaseBySeatClass() {
        assertThat(SeatType.FIRST.getRate()).isGreaterThan(SeatType.SECOND.getRate());
        assertThat(SeatType.BUSINESS.getRate()).isGreaterThan(SeatType.FIRST.getRate());
    }

    /** 全程（1320.2km）三档票价应落在 12306 实测值附近（一等 967、商务 2156） */
    @Test
    void calc_appliesRateToBasePrice() {
        BigDecimal base = PriceCalculator.basePrice(new BigDecimal("1320.2"));
        assertThat(SeatType.FIRST.calc(base).doubleValue()).as("北京南→上海虹桥 一等座").isBetween(960.0, 1000.0);
        assertThat(SeatType.BUSINESS.calc(base).doubleValue()).as("北京南→上海虹桥 商务座").isBetween(2140.0, 2210.0);
    }
}
