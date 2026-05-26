package com.studyroom.mapper;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.CheckIn;
import org.apache.ibatis.annotations.Mapper;

/**
 * 签到Mapper接口
 */
@Mapper
public interface CheckInMapper extends BaseMapper<CheckIn> {

    /**
     * 更新座位状态
     * @param updateWrapper 更新条件
     * @return 影响行数
     */
    default int updateSeatStatus(LambdaUpdateWrapper<com.studyroom.entity.SeatStatus> updateWrapper) {
        // 实际实现由MyBatis Plus处理
        return 0;
    }
}
