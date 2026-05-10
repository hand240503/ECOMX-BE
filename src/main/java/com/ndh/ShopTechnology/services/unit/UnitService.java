package com.ndh.ShopTechnology.services.unit;

import com.ndh.ShopTechnology.dto.request.unit.CreateUnitRequest;
import com.ndh.ShopTechnology.dto.request.unit.UpdateUnitRequest;
import com.ndh.ShopTechnology.dto.response.unit.UnitResponse;

import java.util.List;

public interface UnitService {

    List<UnitResponse> listAll();

    UnitResponse getById(long id);

    UnitResponse create(CreateUnitRequest request);

    UnitResponse update(long id, UpdateUnitRequest request);

    void delete(long id);
}
