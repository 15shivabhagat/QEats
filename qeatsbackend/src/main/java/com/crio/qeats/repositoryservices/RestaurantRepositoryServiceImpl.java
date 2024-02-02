/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.repositoryservices;

import ch.hsr.geohash.GeoHash;
import com.crio.qeats.configs.RedisConfiguration;
import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.globals.GlobalConstants;
import com.crio.qeats.models.ItemEntity;
import com.crio.qeats.models.MenuEntity;
import com.crio.qeats.models.RestaurantEntity;
import com.crio.qeats.repositories.ItemRepository;
import com.crio.qeats.repositories.MenuRepository;
import com.crio.qeats.repositories.RestaurantRepository;
import com.crio.qeats.utils.GeoLocation;
import com.crio.qeats.utils.GeoUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;


@Service
public class RestaurantRepositoryServiceImpl implements RestaurantRepositoryService {

  @Autowired
  private RestaurantRepository restaurantRepository;

  @Autowired
  private MenuRepository menuRepository;


  @Autowired
  private RedisConfiguration redisConfiguration;

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private Provider<ModelMapper> modelMapperProvider;

  private boolean isOpenNow(LocalTime time, RestaurantEntity res) {
    LocalTime openingTime = LocalTime.parse(res.getOpensAt());
    LocalTime closingTime = LocalTime.parse(res.getClosesAt());

    return time.isAfter(openingTime) && time.isBefore(closingTime);
  }

  // TODO: CRIO_TASK_MODULE_NOSQL
  // Objectives:
  // 1. Implement findAllRestaurantsCloseby.
  // 2. Remember to keep the precision of GeoHash in mind while using it as a key.
  // Check RestaurantRepositoryService.java file for the interface contract.
  public List<Restaurant> findAllRestaurantsCloseBy(Double latitude,
      Double longitude, LocalTime currentTime, Double servingRadiusInKms) {
    
  
    List<Restaurant> restaurants = null;
    // TODO: CRIO_TASK_MODULE_REDIS
    // We want to use cache to speed things up. Write methods that perform the same functionality,
    // but using the cache if it is present and reachable.
    // Remember, you must ensure that if cache is not present, the queries are directed at the
    // database instead.
    if(redisConfiguration.isCacheAvailable()) {
      restaurants = findAllRestaurantsCloseByFromCache(latitude, longitude, currentTime, servingRadiusInKms);
    } else {
      restaurants = findAllRestaurantsCloseFromDb(latitude, longitude, currentTime, servingRadiusInKms);
    }


      //CHECKSTYLE:OFF
      //CHECKSTYLE:ON


    return restaurants;
  }

  private List<Restaurant> findAllRestaurantsCloseFromDb(Double latitude, Double longitude, LocalTime currentTime, Double servingRadiusInKms) {
    ModelMapper modelMapper = modelMapperProvider.get();
    List<RestaurantEntity> restaurantEntityList = restaurantRepository.findAll();
    
    List<Restaurant> restaurants = new ArrayList<>();
    for(RestaurantEntity restaurantEntity : restaurantEntityList) {
      if(isOpenNow(currentTime, restaurantEntity)) {
        if(isRestaurantCloseByAndOpen(restaurantEntity, currentTime, latitude, longitude, servingRadiusInKms)) {
          restaurants.add(modelMapper.map(restaurantEntity, Restaurant.class));
        }
      }
    }
    return restaurants;
  }

  private List<Restaurant> findAllRestaurantsCloseByFromCache(Double latitude, Double longitude, LocalTime currentTime, Double servingRadiusInKms) {
    List<Restaurant> restaurantList = new ArrayList<>();
    GeoLocation geoLocation = new GeoLocation(latitude, longitude);
    GeoHash geoHash = GeoHash.withCharacterPrecision(geoLocation.getLatitude(), geoLocation.getLongitude(), 7);
    try (Jedis jedis = redisConfiguration.getJedisPool().getResource()) {
      String jsonStringFromCache = jedis.get(geoHash.toBase32());
      if(jsonStringFromCache == null) {
        String createdJsonString = "";
        try {
          restaurantList = findAllRestaurantsCloseFromDb(latitude, longitude, currentTime, servingRadiusInKms);
          createdJsonString = new ObjectMapper().writeValueAsString(restaurantList);
        } catch (Exception e) {
          //TODO: handle exception
          e.printStackTrace();
        }
        jedis.setex(geoHash.toBase32(), GlobalConstants.REDIS_ENTRY_EXPIRY_IN_SECONDS, createdJsonString);
      } else {
        try {
          restaurantList = new ObjectMapper().readValue(jsonStringFromCache, new TypeReference<List<Restaurant>>(){});
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    } catch (Exception e) {
      //TODO: handle exception
      e.printStackTrace();
    }
    return restaurantList;
  }
  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants whose names have an exact or partial match with the search query.
  @Override
  public List<Restaurant> findRestaurantsByName(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    
    // ModelMapper modelMapper = modelMapperProvider.get();
    List<RestaurantEntity> restaurantList = restaurantRepository.findRestaurantsByNameExact(searchString).orElse(null);
    List<Restaurant> restaurants = converRestaurantEntityToRestaurant(restaurantList, currentTime, latitude, longitude, servingRadiusInKms);
    // if(restaurantList != null) {
    //   for(RestaurantEntity restaurantEntity : restaurantList) {
    //     if(isOpenNow(currentTime, restaurantEntity)) {
    //       if(isRestaurantCloseByAndOpen(restaurantEntity, currentTime, latitude, longitude, servingRadiusInKms)) {
    //         restaurants.add(modelMapper.map(restaurantEntity, Restaurant.class));
    //       }
    //     }
    //   }
    // }
    return restaurants;

     
  }


  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants whose attributes (cuisines) intersect with the search query.
  @Override
  public List<Restaurant> findRestaurantsByAttributes(
      Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {

    // ModelMapper modelMapper = modelMapperProvider.get();
    List<RestaurantEntity> restaurantList = restaurantRepository.findByAttributesContainingIgnoreCase(searchString).orElse(null);
    List<Restaurant> restaurants = converRestaurantEntityToRestaurant(restaurantList, currentTime, latitude, longitude, servingRadiusInKms);
    // if(restaurantList != null) {
    //   for(RestaurantEntity restaurantEntity : restaurantList) {
    //     if(isOpenNow(currentTime, restaurantEntity)) {
    //       if(isRestaurantCloseByAndOpen(restaurantEntity, currentTime, latitude, longitude, servingRadiusInKms)) {
    //         restaurants.add(modelMapper.map(restaurantEntity, Restaurant.class));
    //       }
    //     }
    //   }
    // }
    return restaurants;

    
  }



  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants which serve food items whose names form a complete or partial match
  // with the search query.

  @Override
  public List<Restaurant> findRestaurantsByItemName(
      Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {

    List<String> menutList = menuRepository.findRestaurantIdsByItemsName(searchString).orElse(null);

    List<RestaurantEntity> restaurantList = restaurantRepository.findRestaurantsByRestaurantIdIn(menutList).orElse(null);
    
    // ModelMapper modelMapper = modelMapperProvider.get();
    List<Restaurant> restaurants = converRestaurantEntityToRestaurant(restaurantList, currentTime, latitude, longitude, servingRadiusInKms);
    // if(restaurantList != null) {
    //   for(RestaurantEntity restaurantEntity : restaurantList) {
    //     if(isOpenNow(currentTime, restaurantEntity)) {
    //       if(isRestaurantCloseByAndOpen(restaurantEntity, currentTime, latitude, longitude, servingRadiusInKms)) {
    //         restaurants.add(modelMapper.map(restaurantEntity, Restaurant.class));
    //       }
    //     }
    //   }
    // }
    return restaurants;

     
  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants which serve food items whose attributes intersect with the search query.
  @Override
  public List<Restaurant> findRestaurantsByItemAttributes(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {

      List<String> menutList = menuRepository.findRestaurantIdsByItemsAttributesContainingIgnoreCase(searchString).orElse(null);
      List<RestaurantEntity> restaurantList = restaurantRepository.findRestaurantsByRestaurantIdIn(menutList).orElse(null);
      List<Restaurant> restaurants = converRestaurantEntityToRestaurant(restaurantList, currentTime, latitude, longitude, servingRadiusInKms);
  
      return restaurants;
  }





  /**
   * Utility method to check if a restaurant is within the serving radius at a given time.
   * @return boolean True if restaurant falls within serving radius and is open, false otherwise
   */
  private boolean isRestaurantCloseByAndOpen(RestaurantEntity restaurantEntity,
      LocalTime currentTime, Double latitude, Double longitude, Double servingRadiusInKms) {
    if (isOpenNow(currentTime, restaurantEntity)) {
      return GeoUtils.findDistanceInKm(latitude, longitude,
          restaurantEntity.getLatitude(), restaurantEntity.getLongitude())
          < servingRadiusInKms;
    }

    return false;
  }

  private List<Restaurant> converRestaurantEntityToRestaurant(List<RestaurantEntity> restaurantEntityList, LocalTime currentTime, 
      Double latitude, Double longitude, Double servingRadiusInKms) {
    
    ModelMapper modelMapper = modelMapperProvider.get();
    List<Restaurant> restaurants = new ArrayList<>();
    for(RestaurantEntity restaurantEntity : restaurantEntityList) {
      if(isOpenNow(currentTime, restaurantEntity)) {
        if(isRestaurantCloseByAndOpen(restaurantEntity, currentTime, latitude, longitude, servingRadiusInKms)) {
          restaurants.add(modelMapper.map(restaurantEntity, Restaurant.class));
        }
      }
    }
    return restaurants;
  }



}

