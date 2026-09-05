package com.tianyi.railticket.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class OrderCreateDTO {
    @NotNull(message = "车次ID不能为空")
    private Long trainId;
    @NotNull(message = "乘车日期不能为空")
    private LocalDate trainDate;
    @NotNull(message = "出发站不能为空")
    private Long fromStationId;
    @NotNull(message = "到达站不能为空")
    private Long toStationId;
    @NotNull(message = "座位类型不能为空")
    private Integer seatType;
    @NotNull(message = "乘车人不能为空")
    private Long passengerId;
}
