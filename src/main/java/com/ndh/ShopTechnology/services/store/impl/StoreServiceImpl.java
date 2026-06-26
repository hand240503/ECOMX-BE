package com.ndh.ShopTechnology.services.store.impl;

import com.ndh.ShopTechnology.dto.request.store.StoreCreateRequest;
import com.ndh.ShopTechnology.dto.request.store.StoreUpdateRequest;
import com.ndh.ShopTechnology.dto.response.store.StoreResponse;
import com.ndh.ShopTechnology.entities.store.StoreEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.OrderRepository;
import com.ndh.ShopTechnology.repository.StoreRepository;
import com.ndh.ShopTechnology.repository.StoreStockRepository;
import com.ndh.ShopTechnology.services.store.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;
    private final StoreStockRepository storeStockRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public StoreResponse create(StoreCreateRequest req) {
        String code = req.getCode().trim();
        if (storeRepository.existsByCodeIgnoreCase(code)) {
            throw new CustomApiException(HttpStatus.CONFLICT, "Mã kho đã tồn tại: " + code);
        }
        boolean makeDefault = Boolean.TRUE.equals(req.getIsDefault())
                || storeRepository.count() == 0; // kho đầu tiên mặc định là default
        StoreEntity store = StoreEntity.builder()
                .code(code)
                .name(req.getName().trim())
                .phone(trimToNull(req.getPhone()))
                .addressLine(trimToNull(req.getAddressLine()))
                .city(trimToNull(req.getCity()))
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .active(req.getActive() == null ? Boolean.TRUE : req.getActive())
                .isDefault(makeDefault)
                .note(trimToNull(req.getNote()))
                .build();
        store = storeRepository.save(store);
        if (makeDefault) {
            clearOtherDefaults(store.getId());
        }
        return toResponse(store);
    }

    @Override
    @Transactional
    public StoreResponse update(Long id, StoreUpdateRequest req) {
        StoreEntity store = getEntityOrThrow(id);
        if (req.getCode() != null) {
            String code = req.getCode().trim();
            if (storeRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
                throw new CustomApiException(HttpStatus.CONFLICT, "Mã kho đã tồn tại: " + code);
            }
            store.setCode(code);
        }
        if (req.getName() != null) store.setName(req.getName().trim());
        if (req.getPhone() != null) store.setPhone(trimToNull(req.getPhone()));
        if (req.getAddressLine() != null) store.setAddressLine(trimToNull(req.getAddressLine()));
        if (req.getCity() != null) store.setCity(trimToNull(req.getCity()));
        if (req.getLatitude() != null) store.setLatitude(req.getLatitude());
        if (req.getLongitude() != null) store.setLongitude(req.getLongitude());
        if (req.getActive() != null) store.setActive(req.getActive());
        if (req.getNote() != null) store.setNote(trimToNull(req.getNote()));
        if (Boolean.TRUE.equals(req.getIsDefault())) {
            store.setActive(Boolean.TRUE);
            store.setIsDefault(true);
            clearOtherDefaults(store.getId());
        }
        store = storeRepository.save(store);
        return toResponse(store);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        StoreEntity store = getEntityOrThrow(id);
        if (Boolean.TRUE.equals(store.getIsDefault())) {
            throw new CustomApiException(HttpStatus.CONFLICT,
                    "Không thể xoá kho mặc định. Hãy đặt kho khác làm mặc định trước.");
        }
        if (orderRepository.existsByStore_Id(id)) {
            // Có đơn tham chiếu → không xoá cứng, chỉ vô hiệu hoá để giữ lịch sử.
            store.setActive(false);
            storeRepository.save(store);
            throw new CustomApiException(HttpStatus.CONFLICT,
                    "Kho đã gắn với đơn hàng nên không thể xoá. Đã chuyển sang trạng thái ngừng hoạt động.");
        }
        if (storeStockRepository.existsByStore_IdAndOnHandGreaterThan(id, 0)) {
            throw new CustomApiException(HttpStatus.CONFLICT,
                    "Kho vẫn còn tồn hàng. Hãy chuyển hết hàng sang kho khác trước khi xoá.");
        }
        storeStockRepository.deleteByStore_Id(id);
        storeRepository.delete(store);
    }

    @Override
    @Transactional(readOnly = true)
    public StoreResponse get(Long id) {
        return toResponse(getEntityOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoreResponse> list(String q) {
        String key = (q != null && !q.isBlank()) ? q.trim() : null;
        return storeRepository.search(key).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoreResponse> listActive() {
        return storeRepository.findByActiveTrueOrderByIdAsc().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StoreEntity getEntityOrThrow(Long id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Không tìm thấy kho id=" + id));
    }

    @Override
    @Transactional(readOnly = true)
    public StoreEntity getDefaultStoreOrNull() {
        return storeRepository.findFirstByIsDefaultTrueOrderByIdAsc()
                .orElseGet(() -> storeRepository.findByActiveTrueOrderByIdAsc()
                        .stream().findFirst().orElse(null));
    }

    private void clearOtherDefaults(Long keepId) {
        storeRepository.findAll().forEach(s -> {
            if (!s.getId().equals(keepId) && Boolean.TRUE.equals(s.getIsDefault())) {
                s.setIsDefault(false);
                storeRepository.save(s);
            }
        });
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private StoreResponse toResponse(StoreEntity s) {
        return StoreResponse.builder()
                .id(s.getId())
                .code(s.getCode())
                .name(s.getName())
                .phone(s.getPhone())
                .addressLine(s.getAddressLine())
                .city(s.getCity())
                .latitude(s.getLatitude())
                .longitude(s.getLongitude())
                .active(s.getActive())
                .isDefault(s.getIsDefault())
                .note(s.getNote())
                .createdDate(s.getCreatedDate())
                .modifiedDate(s.getModifiedDate())
                .build();
    }
}
