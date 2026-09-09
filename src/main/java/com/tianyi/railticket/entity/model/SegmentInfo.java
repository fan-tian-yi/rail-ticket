package com.tianyi.railticket.entity.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalTime;

@Data
public class SegmentInfo {
    /** 出发站序；对象非 null 时该值一定有值，故用基本类型 */
    private int fromSeq;
    /** 到达站序 */
    private int toSeq;
    private LocalTime depart;
    private LocalTime arrive;
    private BigDecimal price;
}
