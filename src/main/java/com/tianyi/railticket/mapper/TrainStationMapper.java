package com.tianyi.railticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianyi.railticket.entity.model.TrainRoute;
import com.tianyi.railticket.entity.TrainStationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TrainStationMapper extends BaseMapper<TrainStationDO> {
    @Select(
            """
            SELECT A.train_id,
            A.depart_time              AS depart,
            B.arrive_time              AS arrive,
            B.price_cum - A.price_cum  AS price
            FROM t_train_station A
            JOIN t_train_station B
            ON A.train_id = B.train_id
            AND A.seq < B.seq
            WHERE A.station_id = #{fromStationId}
            AND B.station_id = #{toStationId}
           """)
    List<TrainRoute> selectRoutes(@Param("fromStationId") Long fromStationId,
                                  @Param("toStationId") Long toStationId);
}
