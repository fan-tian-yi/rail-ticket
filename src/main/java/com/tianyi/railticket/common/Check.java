package com.tianyi.railticket.common;

import com.tianyi.railticket.common.exception.BizException;
import com.tianyi.railticket.common.exception.ErrorCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** 业务规则校验（L3）：DTO 注解表达不了、依赖"当前状态"的规则集中在这里 */
public final class Check {

    private Check() {
    }

    /** 乘车日期窗口 [今天, 今天+预售期-1]，上界与库存预热窗口对齐（否则最后一天假售罄） */
    public static void trainDate(LocalDate trainDate) {
        LocalDate today = LocalDate.now(Const.ZONE);
        if (trainDate.isBefore(today)) {
            throw new BizException(ErrorCode.INVALID_DATE);
        }
        if (trainDate.isAfter(today.plusDays(Const.PRESALE_DAYS - 1))) {
            throw new BizException(ErrorCode.BEYOND_PRESALE);
        }
    }

    /** 停止售票窗口：距发车不足 STOP_SELL_BEFORE_MINUTES 分钟（含已发车）→ 40012；缺时刻则放行，交给数据校验 */
    public static void trainDepart(LocalDate trainDate, LocalTime departTime) {
        if (departTime == null) {
            return;
        }
        LocalDateTime depart = trainDate.atTime(departTime);
        if (!depart.isAfter(LocalDateTime.now(Const.ZONE).plusMinutes(Const.STOP_SELL_BEFORE_MINUTES))) {
            throw new BizException(ErrorCode.TRAIN_SELL_CLOSED);
        }
    }

    /** 站序区间：未取到 → 40001；方向反（含两站相同）→ 40002 */
    public static void segment(Integer fromSeq, Integer toSeq) {
        if (fromSeq == null || toSeq == null) {
            throw new BizException(ErrorCode.TRAIN_NOT_PASS_STATION);
        }
        if (fromSeq >= toSeq) {
            throw new BizException(ErrorCode.INVALID_DIRECTION);
        }
    }
}
