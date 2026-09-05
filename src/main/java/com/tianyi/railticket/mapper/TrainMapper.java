package com.tianyi.railticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianyi.railticket.entity.TrainDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TrainMapper extends BaseMapper<TrainDO> {
}
