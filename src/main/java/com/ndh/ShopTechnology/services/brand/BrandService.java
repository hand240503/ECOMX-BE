package com.ndh.ShopTechnology.services.brand;

import com.ndh.ShopTechnology.dto.request.brand.CreateBrandRequest;
import com.ndh.ShopTechnology.dto.request.brand.UpdateBrandRequest;
import com.ndh.ShopTechnology.dto.response.brand.BrandResponse;

import java.util.List;

public interface BrandService {

    List<BrandResponse> listAll();

    BrandResponse getById(long id);

    BrandResponse create(CreateBrandRequest request);

    BrandResponse update(long id, UpdateBrandRequest request);

    void delete(long id);
}
