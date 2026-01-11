package com.ndh.ShopTechnology.services.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CommonService {

    public static Set<Integer> convertToHashSet(String rs){
        ObjectMapper objectMapper = new ObjectMapper();
        Set<Integer> hashSet = new HashSet<>();
        if(rs == null) return hashSet;
        try {
            return new HashSet<>(Arrays.asList(objectMapper.readValue(rs, Integer[].class)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
