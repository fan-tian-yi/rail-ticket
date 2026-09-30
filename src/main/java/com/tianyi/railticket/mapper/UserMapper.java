package com.tianyi.railticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianyi.railticket.entity.UserDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserDO> {
}
