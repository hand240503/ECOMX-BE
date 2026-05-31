package com.ndh.ShopTechnology.dto.request.task;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class AddChecklistRequest {
    @NotBlank
    private String title;
    private List<String> items;
}
