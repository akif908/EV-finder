package com.example.EV_finder_api.service;

import com.example.EV_finder_api.dto.StationResponse;

import java.math.BigDecimal;
import java.util.List;

/** Public station discovery: list/search/nearby/details. Only ACTIVE stations are shown. */
public interface StationQueryService {

    List<StationResponse> search(String query);

    List<StationResponse> nearby(BigDecimal latitude, BigDecimal longitude, double radiusKm);

    StationResponse details(String stationId);

    List<StationResponse> allActive();
}
