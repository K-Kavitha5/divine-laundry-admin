package com.divinelaundry.api;

import com.divinelaundry.domain.LaundryServiceItem;
import com.divinelaundry.domain.PricingUnit;
import com.divinelaundry.repository.LaundryServiceRepository;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {
    private final LaundryServiceRepository services;

    public CatalogController(LaundryServiceRepository services) {
        this.services = services;
    }

    @GetMapping("/services")
    List<ServiceResponse> listServices() {
        return services.findByActiveTrueOrderByCategoryAscNameAsc().stream().map(ServiceResponse::from).toList();
    }

    public record ServiceResponse(Long id, String code, String name, String category, String group, PricingUnit unit, BigDecimal rate) {
        static ServiceResponse from(LaundryServiceItem item) {
            return new ServiceResponse(item.getId(), item.getCode(), item.getName(), item.getCategory(), item.getCatalogGroup(), item.getPricingUnit(), item.getUnitRate());
        }
    }
}
