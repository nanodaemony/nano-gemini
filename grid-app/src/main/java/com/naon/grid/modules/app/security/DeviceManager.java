package com.naon.grid.modules.app.security;

import com.naon.grid.modules.app.domain.GridUserToken;
import com.naon.grid.modules.app.repository.GridUserTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceManager {

    private final GridUserTokenRepository gridUserTokenRepository;

    public void registerDevice(Long userId, String deviceId, String deviceName, String refreshToken, String accessToken, Date expireTime) {
        Optional<GridUserToken> existingToken = gridUserTokenRepository.findByUserIdAndDeviceId(userId, deviceId);

        GridUserToken userToken;
        if (existingToken.isPresent()) {
            userToken = existingToken.get();
            userToken.setRefreshToken(refreshToken);
            userToken.setAccessToken(accessToken);
            userToken.setExpireTime(expireTime);
            if (deviceName != null) {
                userToken.setDeviceName(deviceName);
            }
        } else {
            userToken = new GridUserToken();
            userToken.setUserId(userId);
            userToken.setDeviceId(deviceId);
            userToken.setDeviceName(deviceName);
            userToken.setRefreshToken(refreshToken);
            userToken.setAccessToken(accessToken);
            userToken.setExpireTime(expireTime);
        }
        gridUserTokenRepository.save(userToken);
    }

    public void removeDevice(Long userId, String deviceId) {
        gridUserTokenRepository.deleteByUserIdAndDeviceId(userId, deviceId);
    }

    public void removeAllDevices(Long userId) {
        gridUserTokenRepository.deleteByUserId(userId);
    }
}
