package com.adaptive.loadmanagement.driver.service;

import com.adaptive.loadmanagement.driver.domain.model.DriverLocation;
import com.adaptive.loadmanagement.driver.dto.request.UpdateLocationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DriverService {

    private static final Logger log = LoggerFactory.getLogger(DriverService.class);

    public DriverLocation updateLocation(UpdateLocationRequest request, String priorityHeader) {
        String driverId = (request.driverId() != null) ? request.driverId() : "DRV-9941";
        double lat = (request.latitude() != 0) ? request.latitude() : 37.7749;
        double lng = (request.longitude() != 0) ? request.longitude() : -122.4194;

        log.info("Driver Location Updated for [{}], Priority [{}]", driverId, priorityHeader);

        return new DriverLocation(driverId, lat, lng, "ONLINE", System.currentTimeMillis());
    }
}
