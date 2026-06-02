package com.studyroom.service;

import com.studyroom.dto.QuerySeatDTO;
import com.studyroom.vo.SeatVO;

import java.util.List;

public interface SeatService {
    List<SeatVO> querySeatStatus(QuerySeatDTO dto);
    void updateSeatStatus(Integer seatId, String date, Integer timePeriodId, Integer status);
}