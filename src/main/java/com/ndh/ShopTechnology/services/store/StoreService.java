package com.ndh.ShopTechnology.services.store;

import com.ndh.ShopTechnology.dto.request.store.StoreCreateRequest;
import com.ndh.ShopTechnology.dto.request.store.StoreUpdateRequest;
import com.ndh.ShopTechnology.dto.response.store.StoreResponse;
import com.ndh.ShopTechnology.entities.store.StoreEntity;

import java.util.List;

/** CRUD kho / cửa hàng (store). */
public interface StoreService {

    StoreResponse create(StoreCreateRequest request);

    StoreResponse update(Long id, StoreUpdateRequest request);

    void delete(Long id);

    StoreResponse get(Long id);

    /** Danh sách kho (lọc theo tên/mã/thành phố; q rỗng = tất cả). */
    List<StoreResponse> list(String q);

    /** Danh sách kho đang hoạt động (cho khách chọn). */
    List<StoreResponse> listActive();

    /** Lấy entity kho hoặc ném lỗi nếu không tồn tại. */
    StoreEntity getEntityOrThrow(Long id);

    /** Kho mặc định (tạo fallback khi đơn không chỉ định store). Có thể null. */
    StoreEntity getDefaultStoreOrNull();
}
