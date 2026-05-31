package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.constants.PermissionCode;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.report.TopProductReportDto;
import com.ndh.ShopTechnology.services.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${api.prefix}/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    @GetMapping("/top-selling")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_ORDER + ")")
    public ResponseEntity<APIResponse<List<TopProductReportDto>>> getTopSellingProducts(
            @RequestParam(defaultValue = "all") String timeRange,
            @RequestParam(defaultValue = "10") int limit) {
        
        List<TopProductReportDto> data = reportService.getTopSellingProducts(timeRange, limit);
        return ResponseEntity.ok(APIResponse.of(
                true,
                "Lấy top sản phẩm bán chạy thành công",
                data,
                null,
                Map.of("count", data.size())
        ));
    }

    @GetMapping("/top-interested")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_PRODUCT + ")")
    public ResponseEntity<APIResponse<List<TopProductReportDto>>> getTopInterestedProducts(
            @RequestParam(defaultValue = "week") String timeRange,
            @RequestParam(defaultValue = "10") int limit) {
        
        List<TopProductReportDto> data = reportService.getTopInterestedProducts(timeRange, limit);
        return ResponseEntity.ok(APIResponse.of(
                true,
                "Lấy top sản phẩm được quan tâm thành công",
                data,
                null,
                Map.of("count", data.size())
        ));
    }
}
