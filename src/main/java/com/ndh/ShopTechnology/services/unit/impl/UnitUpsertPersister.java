package com.ndh.ShopTechnology.services.unit.impl;

import com.ndh.ShopTechnology.constants.SystemConstant;
import com.ndh.ShopTechnology.entities.product.UnitEntity;
import com.ndh.ShopTechnology.repository.UnitRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Objects;

/**
 * Upsert một đơn vị tính trong transaction RIÊNG (REQUIRES_NEW) để mỗi dòng import
 * độc lập — dòng lỗi không ảnh hưởng các dòng khác.
 *
 * <p>Khóa định danh: id (ưu tiên) → code → name_unit (fallback cho dữ liệu cũ chưa có code).
 */
@Component
public class UnitUpsertPersister {

    private final UnitRepository unitRepository;

    public UnitUpsertPersister(UnitRepository unitRepository) {
        this.unitRepository = unitRepository;
    }

    public static class Outcome {
        public final String action; // CREATED | UPDATED | SKIPPED
        public final Long id;
        Outcome(String action, Long id) { this.action = action; this.id = id; }
    }

    static class UnitRowException extends RuntimeException {
        UnitRowException(String m) { super(m); }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Outcome upsert(Long id, String code, String nameUnit, Integer ratio, Integer status) {
        if (isBlank(nameUnit)) throw new UnitRowException("Thiếu tên đơn vị (name_unit)");

        UnitEntity existing = null;
        if (id != null) existing = unitRepository.findById(id).orElse(null);
        if (existing == null && !isBlank(code)) {
            existing = unitRepository.findFirstByCodeIgnoreCase(code.trim()).orElse(null);
        }
        if (existing == null && isBlank(code)) {
            // fallback cho dữ liệu cũ chưa có code: dò theo tên.
            existing = unitRepository.findFirstByNameUnitIgnoreCase(nameUnit.trim()).orElse(null);
        }

        if (existing != null) {
            String newCode = isBlank(code)
                    ? (isBlank(existing.getCode()) ? uniqueCode(nameUnit) : existing.getCode())
                    : code.trim().toUpperCase();
            String newName = nameUnit.trim();
            Integer newRatio = ratio != null ? ratio : existing.getRatio();
            Integer newStatus = status != null ? status : existing.getStatus();

            boolean unchanged = Objects.equals(trimToNull(existing.getCode()), trimToNull(newCode))
                    && Objects.equals(trimToNull(existing.getNameUnit()), trimToNull(newName))
                    && Objects.equals(existing.getRatio(), newRatio)
                    && Objects.equals(existing.getStatus(), newStatus);
            if (unchanged) {
                return new Outcome("SKIPPED", existing.getId());
            }

            if (unitRepository.existsByCodeIgnoreCaseAndIdNot(newCode, existing.getId())) {
                throw new UnitRowException("Trùng code với đơn vị khác: " + newCode);
            }
            existing.setCode(newCode);
            existing.setNameUnit(newName);
            if (ratio != null) existing.setRatio(ratio);
            if (status != null) existing.setStatus(status);
            UnitEntity saved = unitRepository.save(existing);
            return new Outcome("UPDATED", saved.getId());
        }

        String c = isBlank(code) ? uniqueCode(nameUnit) : code.trim().toUpperCase();
        if (unitRepository.existsByCodeIgnoreCase(c)) {
            throw new UnitRowException("Code đã tồn tại: " + c);
        }
        UnitEntity created = unitRepository.save(UnitEntity.builder()
                .code(c)
                .nameUnit(nameUnit.trim())
                .ratio(ratio != null ? ratio : 1)
                .status(status != null ? status : SystemConstant.ACTIVE_STATUS)
                .build());
        return new Outcome("CREATED", created.getId());
    }

    private String uniqueCode(String name) {
        String base = slug(name), c = base;
        int i = 1;
        while (unitRepository.existsByCodeIgnoreCase(c)) c = base + "_" + (++i);
        return c;
    }

    static String slug(String raw) {
        String noAccent = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').replace('Đ', 'D');
        String code = noAccent.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (code.isEmpty()) code = "U" + Math.abs(raw.hashCode());
        return code.length() > 60 ? code.substring(0, 60) : code;
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private static String trimToNull(String s) { return isBlank(s) ? null : s.trim(); }
}
