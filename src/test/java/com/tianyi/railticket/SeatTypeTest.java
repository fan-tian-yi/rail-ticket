package com.tianyi.railticket;
import com.tianyi.railticket.common.SeatType;
import com.tianyi.railticket.common.exception.BizException;
import com.tianyi.railticket.entity.model.SeatConfig;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class SeatTypeTest {
    /** 测试of方法，失败返回null */
    @Test
    public void testOf(){
        Map<Integer, SeatType> map = Map.of(
                1, SeatType.SECOND,
                2, SeatType.FIRST,
                3, SeatType.BUSINESS
        );
        for(Map.Entry<Integer, SeatType> entry : map.entrySet()){
            SeatType actual = SeatType.of(entry.getKey());
            assertEquals(entry.getValue(), actual, "code=" + entry.getKey());
        }
        assertNull(SeatType.of(99), "code=99");
        assertNull(SeatType.of(null), "code=null");
    }

    /** 测试ofOrThrow方法，失败抛出BizException异常 */
    @Test
    public void testOfOrThrow(){
        Map<Integer, SeatType> map = Map.of(
                1, SeatType.SECOND,
                2, SeatType.FIRST,
                3, SeatType.BUSINESS
        );
        for(Map.Entry<Integer, SeatType> entry : map.entrySet()){
            SeatType actual = SeatType.ofOrThrow(entry.getKey());
            assertEquals(entry.getValue(), actual, "code=" + entry.getKey());
        }
        BizException thrown = assertThrows(BizException.class, () -> SeatType.ofOrThrow(99), "code=99, BizException");
        assertEquals(40007, thrown.getCode() ,"code=99");
        thrown = assertThrows(BizException.class, () -> SeatType.ofOrThrow(null), "code=null, BizException");
        assertEquals(40007, thrown.getCode(), "code=null");
    }

    /** 测试calc方法，确保倍率计算正确且无修改 */
    @Test
    public void testCalc(){
        assertEquals(new BigDecimal("100.00"), SeatType.SECOND.calc(new BigDecimal("100")), "SECOND price=100");
        assertEquals(new BigDecimal("168.00"), SeatType.FIRST.calc(new BigDecimal("100")), "FIRST price=100");
        assertEquals(new BigDecimal("375.00"), SeatType.BUSINESS.calc(new BigDecimal("100")), "BUSINESS price=100");
        //测试四舍五入
        assertEquals(new BigDecimal("100.01"), SeatType.SECOND.calc(new BigDecimal("100.005")), "FIRST price=100.005");
    }

    /** 测试capacityOf方法 */
    @Test
    public void testCapacityOf(){
        SeatConfig seatConfig = new SeatConfig();
        seatConfig.setBusiness(30);
        seatConfig.setFirst(50);
        seatConfig.setSecond(100);

        assertEquals(30, SeatType.BUSINESS.capacityOf(seatConfig), "seatType=BUSINESS");
        assertEquals(50, SeatType.FIRST.capacityOf(seatConfig), "seatType=FIRST");
        assertEquals(100, SeatType.SECOND.capacityOf(seatConfig), "seatType=SECOND");
    }
}
