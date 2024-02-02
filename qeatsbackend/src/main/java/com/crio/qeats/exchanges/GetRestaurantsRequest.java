/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.exchanges;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

// TODO: CRIO_TASK_MODULE_RESTAURANTSAPI
//  Implement GetRestaurantsRequest.
//  Complete the class such that it is able to deserialize the incoming query params from
//  REST API clients.
//  For instance, if a REST client calls API
//  /qeats/v1/restaurants?latitude=28.4900591&longitude=77.536386&searchFor=tamil,
//  this class should be able to deserialize lat/long and optional searchFor from that.
// @Data
// @NoArgsConstructor
// @RequiredArgsConstructor
// @AllArgsConstructor
public class GetRestaurantsRequest {
    @NotNull
    @Min(value = -90)
    @Max(value = 90)
    private Double latitude;

    @NotNull
    @Min(value = -180)
    @Max(value = 180)
    private Double longitude;

    private String searchFor = "";

    public GetRestaurantsRequest() {
        
    }

    public GetRestaurantsRequest(@NotNull double latitude, @NotNull double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public GetRestaurantsRequest(@NotNull double latitude, @NotNull double longitude, @NotNull String searchFor) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.searchFor = searchFor;
    }

    public String getSearchFor() {
        return searchFor;
    }

    public void setSearchFor(String searchFor) {
        this.searchFor = searchFor;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
}

