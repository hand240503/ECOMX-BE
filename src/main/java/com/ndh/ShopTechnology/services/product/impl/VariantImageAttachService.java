package com.ndh.ShopTechnology.services.product.impl;

import com.ndh.ShopTechnology.constants.DocumentEntityType;
import com.ndh.ShopTechnology.constants.DocumentKind;
import com.ndh.ShopTechnology.dto.response.product.ProductDocumentSummary;
import com.ndh.ShopTechnology.dto.response.product.ProductFullResponse;
import com.ndh.ShopTechnology.dto.response.product.ProductVariantResponse;
import com.ndh.ShopTechnology.entities.doc.DocumentEntity;
import com.ndh.ShopTechnology.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class VariantImageAttachService {

    private final DocumentRepository documentRepository;

    private static boolean isTreatableAsImage(DocumentEntity d) {
        if (d == null) {
            return false;
        }
        int t = d.getType();
        return t == DocumentKind.IMAGE || t == 0;
    }

    public void attach(List<ProductFullResponse> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        List<Long> variantIds = new ArrayList<>();
        for (ProductFullResponse p : products) {
            if (p == null || p.getVariants() == null) {
                continue;
            }
            for (ProductVariantResponse v : p.getVariants()) {
                if (v != null && v.getId() != null) {
                    variantIds.add(v.getId());
                }
            }
        }
        if (variantIds.isEmpty()) {
            return;
        }
        List<Integer> entityTypes = List.of(DocumentEntityType.ID_DOCUMENT_ENTITY_PRODUCT_VARIANT);
        List<DocumentEntity> rows = documentRepository.findForProductDocuments(variantIds, entityTypes);
        Map<Long, List<DocumentEntity>> grouped = new LinkedHashMap<>();
        for (DocumentEntity d : rows) {
            Long eid = d.getEntityId();
            if (eid == null) {
                continue;
            }
            grouped.computeIfAbsent(eid, ignored -> new ArrayList<>()).add(d);
        }
        Map<Long, Resolved> resolved = new LinkedHashMap<>();
        for (Map.Entry<Long, List<DocumentEntity>> e : grouped.entrySet()) {
            List<DocumentEntity> docsSorted =
                    new ArrayList<>(e.getValue().stream().filter(Objects::nonNull).toList());
            docsSorted.sort((a, b) -> compareDocIdNullsLast(a.getId(), b.getId()));
            List<ProductDocumentSummary> summaries = docsSorted.stream()
                    .map(ProductDocumentSummary::fromEntity)
                    .filter(Objects::nonNull)
                    .toList();
            if (summaries.isEmpty()) {
                continue;
            }
            List<DocumentEntity> imageDocs = new ArrayList<>();
            for (DocumentEntity doc : docsSorted) {
                if (!isTreatableAsImage(doc)) {
                    continue;
                }
                String u = doc.getFilePath();
                if (u == null) {
                    continue;
                }
                u = u.trim();
                if (u.isEmpty()) {
                    continue;
                }
                imageDocs.add(doc);
            }
            imageDocs.sort((a, b) -> {
                boolean ma = a.isMain();
                boolean mb = b.isMain();
                if (ma != mb) {
                    return ma ? -1 : 1;
                }
                return compareDocIdNullsLast(a.getId(), b.getId());
            });
            LinkedHashSet<String> seen = new LinkedHashSet<>();
            List<String> urls = new ArrayList<>();
            String primary = null;
            for (DocumentEntity doc : imageDocs) {
                String u = doc.getFilePath().trim();
                if (!seen.add(u)) {
                    continue;
                }
                urls.add(u);
                if (primary == null) {
                    primary = u;
                }
            }
            resolved.put(e.getKey(), new Resolved(summaries, primary, urls));
        }

        for (ProductFullResponse p : products) {
            if (p == null || p.getVariants() == null) {
                continue;
            }
            for (ProductVariantResponse v : p.getVariants()) {
                if (v == null || v.getId() == null) {
                    continue;
                }
                Resolved r = resolved.get(v.getId());
                if (r == null) {
                    continue;
                }
                v.setDocuments(List.copyOf(r.documents()));
                String primary = r.primaryUrl();
                if (primary != null && !primary.isEmpty()) {
                    v.setThumbnailUrl(primary);
                    v.setMainImageUrl(primary);
                }
                List<String> imgUrls = r.imageUrlsOrdered();
                if (!imgUrls.isEmpty()) {
                    v.setImageUrls(List.copyOf(imgUrls));
                }
            }
        }
    }

    private static int compareDocIdNullsLast(Long a, Long b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        return Long.compare(a, b);
    }

    private record Resolved(
            List<ProductDocumentSummary> documents, String primaryUrl, List<String> imageUrlsOrdered) {}
}
