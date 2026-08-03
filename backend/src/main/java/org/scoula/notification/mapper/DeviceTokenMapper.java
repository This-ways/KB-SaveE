package org.scoula.notification.mapper;

import org.apache.ibatis.annotations.Param;
import org.scoula.notification.domain.DeviceTokenVO;

import java.util.List;

public interface DeviceTokenMapper {
    void insertOrUpdate(DeviceTokenVO deviceTokenVO);
    List<String> selectTokensByUserId(@Param("userId") Long userId);
    void deleteByToken(@Param("fcmToken") String fcmToken);
}
