package com.mozantech.mozantech_test.service;

import com.mozantech.mozantech_test.domain.Item;
import com.mozantech.mozantech_test.dto.ItemDto;
import com.mozantech.mozantech_test.exception.ConflictException;
import com.mozantech.mozantech_test.exception.NotFoundException;
import com.mozantech.mozantech_test.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ItemService {
    private final ItemRepository itemRepository;

    public ItemDto create(ItemDto data) {
        if (itemRepository.existsByName(data.getName()))
            throw new ConflictException("Item ja existe: " + data.getName());
        Item i = new Item();
        i.setName(data.getName());
        return toDto(itemRepository.save(i));
    }

    @Transactional(readOnly = true)
    public List<ItemDto> findAll() { return itemRepository.findAll().stream().map(this::toDto).collect(Collectors.toList()); }

    @Transactional(readOnly = true)
    public ItemDto findById(Long id) { return toDto(get(id)); }

    public ItemDto update(Long id, ItemDto data) {
        Item i = get(id);
        if (itemRepository.existsByNameAndIdNot(data.getName(), id))
            throw new ConflictException("Item já existe " + data.getName());
        i.setName(data.getName());
        return toDto(i);
    }

    public void delete(Long id) { itemRepository.delete(get(id)); itemRepository.flush(); }

    private Item get(Long id) { return itemRepository.findById(id).orElseThrow(() -> new NotFoundException("Item " + id + " not found")); }

    private ItemDto toDto(Item i) {
        ItemDto d = new ItemDto();
        d.setId(i.getId()); d.setName(i.getName());
        return d;
    }
}
