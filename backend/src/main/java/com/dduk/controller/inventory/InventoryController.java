package com.dduk.controller.inventory;

import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    @GetMapping("/items")
    public List<Map<String, Object>> getItems() {
        return Collections.emptyList();
    }

    @PostMapping("/purchase")
    public Map<String, Object> createPurchaseOrder(@RequestBody Map<String, Object> request) {
        return Collections.singletonMap("message", "Inventory stub - purchase order requested");
    }
}
