
/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.services;

import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.exchanges.GetRestaurantsRequest;
import com.crio.qeats.exchanges.GetRestaurantsResponse;
import com.crio.qeats.models.MenuEntity;
import com.crio.qeats.repositoryservices.RestaurantRepositoryService;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class RestaurantServiceImpl implements RestaurantService {

  private final Double peakHoursServingRadiusInKms = 3.0;
  private final Double normalHoursServingRadiusInKms = 5.0;
  @Autowired
  private RestaurantRepositoryService restaurantRepositoryService;


  // TODO: CRIO_TASK_MODULE_RESTAURANTSAPI - Implement findAllRestaurantsCloseby.
  // Check RestaurantService.java file for the interface contract.
  @Override
  public GetRestaurantsResponse findAllRestaurantsCloseBy(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {
        
    List<Restaurant> restaurant;
    int h = currentTime.getHour();
    int m = currentTime.getMinute();
    if ((h >= 8 && h <= 9) || (h == 10 && m == 0) || (h == 13) || (h == 14 && m == 0) 
          || (h >= 19 && h <= 20) || (h == 21 && m == 0)) {
      restaurant = restaurantRepositoryService.findAllRestaurantsCloseBy(
          getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude(), 
          currentTime, peakHoursServingRadiusInKms);
    } else {
      restaurant = restaurantRepositoryService.findAllRestaurantsCloseBy(
        getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude(), 
        currentTime, normalHoursServingRadiusInKms);
    }
    GetRestaurantsResponse response = new GetRestaurantsResponse(restaurant);
  
    return response;
  }

  // @Override
  // public GetRestaurantsResponse findAllRestaurantsCloseBy(
  //     GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {

  //     return null;
  // }


  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Implement findRestaurantsBySearchQuery. The request object has the search string.
  // We have to combine results from multiple sources:
  // 1. Restaurants by name (exact and inexact)
  // 2. Restaurants by cuisines (also called attributes)
  // 3. Restaurants by food items it serves
  // 4. Restaurants by food item attributes (spicy, sweet, etc)
  // Remember, a restaurant must be present only once in the resulting list.
  // Check RestaurantService.java file for the interface contract.
  @Override
  public GetRestaurantsResponse findRestaurantsBySearchQuery(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {

    List<Restaurant> restaurant;
    int h = currentTime.getHour();
    int m = currentTime.getMinute();
    if(getRestaurantsRequest.getSearchFor().equals("")) {
      restaurant = new ArrayList<>();
    } else if ((h >= 8 && h <= 9) || (h == 10 && m == 0) || (h == 13) || (h == 14 && m == 0) 
        || (h >= 19 && h <= 20) || (h == 21 && m == 0)) {
      restaurant = getMatchingRestaurantsBySearchQuery(getRestaurantsRequest, currentTime, peakHoursServingRadiusInKms);
    } else {
      restaurant = getMatchingRestaurantsBySearchQuery(getRestaurantsRequest, currentTime, normalHoursServingRadiusInKms);
    }

    GetRestaurantsResponse response = new GetRestaurantsResponse(restaurant);
    return response;
  }

  private List<Restaurant> getMatchingRestaurantsBySearchQuery(GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime, Double servingRadiusInKms) {
    List<Restaurant> restaurants = new ArrayList<>();
    restaurants.addAll(restaurantRepositoryService.findRestaurantsByName(
                          getRestaurantsRequest.getLatitude(), 
                          getRestaurantsRequest.getLongitude(), 
                          getRestaurantsRequest.getSearchFor(), 
                          currentTime, 
                          servingRadiusInKms
                        )
                      );
    
    restaurants.addAll(restaurantRepositoryService.findRestaurantsByAttributes(
                        getRestaurantsRequest.getLatitude(), 
                        getRestaurantsRequest.getLongitude(), 
                        getRestaurantsRequest.getSearchFor(), 
                        currentTime, 
                        servingRadiusInKms
                      )
                    );
  
    restaurants.addAll(restaurantRepositoryService.findRestaurantsByItemName(
                      getRestaurantsRequest.getLatitude(), 
                      getRestaurantsRequest.getLongitude(), 
                      getRestaurantsRequest.getSearchFor(), 
                      currentTime, 
                      servingRadiusInKms
                    )
                  );
    restaurants.addAll(restaurantRepositoryService.findRestaurantsByItemAttributes(
                    getRestaurantsRequest.getLatitude(), 
                    getRestaurantsRequest.getLongitude(), 
                    getRestaurantsRequest.getSearchFor(), 
                    currentTime, 
                    servingRadiusInKms
                  )
                );
    
    return restaurants.stream().distinct().collect(Collectors.toList());
  }

  

}

