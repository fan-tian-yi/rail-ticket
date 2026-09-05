package com.tianyi.railticket.service;

import com.tianyi.railticket.dto.TrainQueryDTO;
import com.tianyi.railticket.entity.model.TrainRoute;
import com.tianyi.railticket.entity.StationDO;
import com.tianyi.railticket.entity.TrainDO;
import com.tianyi.railticket.mapper.StationMapper;
import com.tianyi.railticket.mapper.TrainMapper;
import com.tianyi.railticket.mapper.TrainStationMapper;
import com.tianyi.railticket.vo.TrainItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainQueryService {
    private final TrainStationMapper trainStationMapper;
    private final TrainMapper trainMapper;
    private final StationMapper stationMapper;

    public List<TrainItemVO> query(TrainQueryDTO trainQueryDTO){
        List<TrainRoute> routes  = trainStationMapper.selectRoutes(
                trainQueryDTO.getFromStationId(), trainQueryDTO.getToStationId());
        if(routes.isEmpty()){
            return List.of();
        }
        LocalDate date = LocalDate.parse(trainQueryDTO.getTrainDate());

        // 站名只查一次，提循环外（两个固定值）
        StationDO from = stationMapper.selectById(trainQueryDTO.getFromStationId());
        StationDO to = stationMapper.selectById(trainQueryDTO.getToStationId());

        List<TrainItemVO> result = new ArrayList<>();
        for (TrainRoute r : routes) {
            TrainDO train = trainMapper.selectById(r.getTrainId());

            TrainItemVO vo = new TrainItemVO();
            vo.setTrainId(train.getId());
            vo.setTrainNo(train.getTrainNo());
            vo.setTrainType(train.getTrainType());
            vo.setFromStationName(from.getName());
            vo.setToStationName(to.getName());
            vo.setDepartTime(date.atTime(r.getDepart()));   // TODO: 跨日车次到达日期 +1
            vo.setArriveTime(date.atTime(r.getArrive()));
            vo.setPrice(r.getPrice());
            vo.setSeatConfig(train.getSeatConfig());         // 原样透传
            vo.setAvailable(null);                           // Step3 Redis 库存填充
            result.add(vo);
        }
        return result;
    }
}
