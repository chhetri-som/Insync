package com.insync.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

import java.util.List;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record PageLayout(
        int pageNumber,
        List<SlotLayout> slots
) {
    public static PageLayout singleSlot(int pageNumber, SlotLayout slot) {
        return new PageLayout(pageNumber, List.of(slot));
    }

    public static PageLayout grid(int pageNumber, List<SlotLayout> slots) {
        return new PageLayout(pageNumber, List.copyOf(slots));
    }
}
