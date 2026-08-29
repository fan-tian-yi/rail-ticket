package com.tianyi.railticket.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class TrainQueryDTO {
    @NotNull(message = "出发站不能为空")
    private Long fromStationId;
    @NotNull(message = "到达站不能为空")
    private Long toStationId;
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "日期格式不正确")
    private String trainDate;
}
