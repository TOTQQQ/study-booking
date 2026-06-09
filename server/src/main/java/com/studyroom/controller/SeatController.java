package com.studyroom.controller;

import com.studyroom.dto.QuerySeatDTO;
import com.studyroom.service.SeatService;
import com.studyroom.vo.SeatVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/seat")
public class SeatController {
    
    private final SeatService seatService;
    
    // 手动添加构造函数
    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }
    
    @PostMapping("/query")
    public Map<String, Object> querySeatStatus(@Valid @RequestBody QuerySeatDTO dto) {
        List<SeatVO> seats = seatService.querySeatStatus(dto);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", seats);
        return result;
    }
}
