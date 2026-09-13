package com.tianyi.railticket;

import com.tianyi.railticket.common.DistanceCalculator;
import com.tianyi.railticket.entity.StationDO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 里程算法护栏：用已落库的 V5 真实值回代，防止绕行系数或累加方式被改坏 */
class DistanceCalculatorTest {

    /** 常规车次：北京南→天津南→济南西→徐州东→南京南→上海虹桥 */
    @Test
    void cumulative_normalRoute() {
        assertCumulative(List.of(
                station("北京南", "116.378803", "39.871933"),
                station("天津南", "117.012808", "39.081408"),
                station("济南西", "116.890619", "36.627258"),
                station("徐州东", "117.236641", "34.266363"),
                station("南京南", "118.799628", "31.957093"),
                station("上海虹桥", "121.319450", "31.193856")),
                "0.0, 114.8, 417.9, 711.4, 1039.0, 1320.2");
    }

    /** 多停一站（苏州北）不改变终点里程，因为苏州北几乎在直线上 */
    @Test
    void cumulative_extraStop() {
        assertCumulative(List.of(
                station("北京南", "116.378803", "39.871933"),
                station("天津南", "117.012808", "39.081408"),
                station("济南西", "116.890619", "36.627258"),
                station("徐州东", "117.236641", "34.266363"),
                station("南京南", "118.799628", "31.957093"),
                station("苏州北", "120.704437", "31.378956"),
                station("上海虹桥", "121.319450", "31.193856")),
                "0.0, 114.8, 417.9, 711.4, 1039.0, 1251.4, 1320.2");
    }

    /** 越站车次：同起终点但折线更直 → 全程比常规车次短（这是"折线累加"而非"直线距离"的实证） */
    @Test
    void cumulative_skipStopShorterThanNormal() {
        List<BigDecimal> skip = DistanceCalculator.cumulative(List.of(
                station("北京南", "116.378803", "39.871933"),
                station("济南西", "116.890619", "36.627258"),
                station("南京南", "118.799628", "31.957093"),
                station("上海虹桥", "121.319450", "31.193856")));

        assertEquals(new BigDecimal("0.0"), skip.get(0));
        assertEquals(new BigDecimal("403.5"), skip.get(1));
        assertEquals(new BigDecimal("1011.9"), skip.get(2));
        assertEquals(new BigDecimal("1293.1"), skip.get(3));
    }

    /** 跨线车次：西安北→郑州东→徐州东→南京南→上海虹桥 */
    @Test
    void cumulative_crossLine() {
        assertCumulative(List.of(
                station("西安北", "108.944948", "34.378179"),
                station("郑州东", "113.780447", "34.745296"),
                station("徐州东", "117.236641", "34.266363"),
                station("南京南", "118.799628", "31.957093"),
                station("上海虹桥", "121.319450", "31.193856")),
                "0.0, 493.5, 850.0, 1177.6, 1458.8");
    }

    /** 里程必须严格递增（用「本站到起点直线距离」会破这条） */
    @Test
    void cumulative_isStrictlyIncreasing() {
        List<BigDecimal> km = DistanceCalculator.cumulative(List.of(
                station("北京南", "116.378803", "39.871933"),
                station("天津南", "117.012808", "39.081408"),
                station("济南西", "116.890619", "36.627258"),
                station("徐州东", "117.236641", "34.266363"),
                station("南京南", "118.799628", "31.957093"),
                station("上海虹桥", "121.319450", "31.193856")));

        for (int i = 1; i < km.size(); i++) {
            assertTrue(km.get(i).compareTo(km.get(i - 1)) > 0,
                    "第 " + i + " 站里程未递增：" + km);
        }
    }

    /** 空列表不抛异常 */
    @Test
    void cumulative_emptyStops() {
        assertEquals(List.of(), DistanceCalculator.cumulative(List.of()));
    }

    /** 期望值取自 V5__add_train_station_distance.sql，逐行核对过 */
    private void assertCumulative(List<StationDO> stops, String expected) {
        List<BigDecimal> want = Arrays.stream(expected.split(","))
                .map(s -> new BigDecimal(s.trim()))
                .toList();
        assertEquals(want, DistanceCalculator.cumulative(stops));
    }

    /** 只用到经纬度，其余字段不参与计算 */
    private StationDO station(String name, String lng, String lat) {
        StationDO s = new StationDO();
        s.setName(name);
        s.setLng(new BigDecimal(lng));
        s.setLat(new BigDecimal(lat));
        return s;
    }
}
