package com.tianyi.railticket.dto;

import com.tianyi.railticket.common.SeatType;
import com.tianyi.railticket.common.validation.InEnum;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AvailableQueryDTO {

    @NotNull(message = "车次ID不能为空")
    @Positive(message = "车次ID不合法")
    private Long trainId;

    @NotNull(message = "乘车日期不能为空")
    private LocalDate trainDate;

    @NotNull(message = "出发站不能为空")
    @Positive(message = "出发站不合法")
    private Long fromStationId;

    @NotNull(message = "到达站不能为空")
    @Positive(message = "到达站不合法")
    private Long toStationId;

    @NotNull(message = "席别不能为空")
    @InEnum(enumClass = SeatType.class, message = "席别不存在")
    private Integer seatType;
}
