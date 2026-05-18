package com.dduk.controller.inventory;

import com.dduk.dto.inventory.ItemCreateDto;
import com.dduk.dto.inventory.ItemResponseDto;
import com.dduk.domain.inventory.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/search")
    public List<ItemResponseDto> searchItems(@RequestParam(required = false) String name) {
        return itemService.searchItems(name);
    }

    @PostMapping
    public ItemResponseDto createItem(@RequestBody ItemCreateDto requestDto) {
        return itemService.createItem(requestDto);
    }
}
