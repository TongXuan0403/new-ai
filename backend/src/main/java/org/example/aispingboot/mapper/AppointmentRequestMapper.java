package org.example.aispingboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.aispingboot.entity.AppointmentRequest;

@Mapper
public interface AppointmentRequestMapper extends BaseMapper<AppointmentRequest> {
}
