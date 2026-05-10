package com.ndh.ShopTechnology.services.unit.impl;

import com.ndh.ShopTechnology.constants.SystemConstant;
import com.ndh.ShopTechnology.dto.request.unit.CreateUnitRequest;
import com.ndh.ShopTechnology.dto.request.unit.UpdateUnitRequest;
import com.ndh.ShopTechnology.dto.response.unit.UnitResponse;
import com.ndh.ShopTechnology.entities.product.UnitEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.PriceRepository;
import com.ndh.ShopTechnology.repository.UnitRepository;
import com.ndh.ShopTechnology.services.unit.UnitService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UnitServiceImpl implements UnitService {

    private final UnitRepository unitRepository;
    private final PriceRepository priceRepository;

    public UnitServiceImpl(UnitRepository unitRepository, PriceRepository priceRepository) {
        this.unitRepository = unitRepository;
        this.priceRepository = priceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnitResponse> listAll() {
        return unitRepository.findAllByOrderByIdAsc().stream()
                .map(UnitResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UnitResponse getById(long id) {
        return UnitResponse.fromEntity(
                unitRepository.findById(id)
                        .orElseThrow(() -> new NotFoundEntityException("Unit not found with id: " + id)));
    }

    @Override
    @Transactional
    public UnitResponse create(CreateUnitRequest request) {
        String name = normalizeName(request.getNameUnit());
        if (unitRepository.existsByNameUnitIgnoreCase(name)) {
            throw new CustomApiException(HttpStatus.CONFLICT, "Unit name already exists: " + name);
        }
        int ratio = request.getRatio() != null ? request.getRatio() : 1;
        int status = request.getStatus() != null ? request.getStatus() : SystemConstant.ACTIVE_STATUS;
        UnitEntity e = UnitEntity.builder()
                .nameUnit(name)
                .ratio(ratio)
                .status(status)
                .build();
        return UnitResponse.fromEntity(unitRepository.save(e));
    }

    @Override
    @Transactional
    public UnitResponse update(long id, UpdateUnitRequest request) {
        UnitEntity e = unitRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Unit not found with id: " + id));
        if (request.getNameUnit() != null) {
            String name = normalizeName(request.getNameUnit());
            if (unitRepository.existsByNameUnitIgnoreCaseAndIdNot(name, id)) {
                throw new CustomApiException(HttpStatus.CONFLICT, "Unit name already exists: " + name);
            }
            e.setNameUnit(name);
        }
        if (request.getRatio() != null) {
            e.setRatio(request.getRatio());
        }
        if (request.getStatus() != null) {
            e.setStatus(request.getStatus());
        }
        return UnitResponse.fromEntity(unitRepository.save(e));
    }

    @Override
    @Transactional
    public void delete(long id) {
        UnitEntity e = unitRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Unit not found with id: " + id));
        if (priceRepository.countByUnit_Id(id) > 0) {
            throw new CustomApiException(HttpStatus.CONFLICT,
                    "Cannot delete unit: still referenced by product prices (unit id " + id + ")");
        }
        unitRepository.delete(e);
    }

    private static String normalizeName(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "name_unit must not be blank");
        }
        return t;
    }
}
