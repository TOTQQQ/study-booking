package com.studyroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.TimePeriod;
import org.apache.ibatis.annotations.Mapper;

/**
 * 时间段Mapper接口
 */
@Mapper
public interface TimePeriodMapper extends BaseMapper<TimePeriod> {
}
