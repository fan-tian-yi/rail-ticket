package com.tianyi.railticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianyi.railticket.entity.PassengerDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PassengerMapper extends BaseMapper<PassengerDO> {
}
