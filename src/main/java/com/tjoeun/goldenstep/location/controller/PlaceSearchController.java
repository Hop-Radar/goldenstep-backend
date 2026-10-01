package com.tjoeun.goldenstep.location.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tjoeun.goldenstep.location.dto.response.PlaceSearchResponse;
import com.tjoeun.goldenstep.location.service.PlaceSearchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "장소명 검색", description = "장소명 기준 후보 검색 API")
@RestController
@RequestMapping("/api/maps")
@RequiredArgsConstructor
public class PlaceSearchController {

    private final PlaceSearchService placeSearchService;

    @Operation(
            summary = "장소명 검색",
            description = "입력 중인 장소명으로 최대 5개의 장소 후보를 조회합니다."
    )
    @GetMapping("/places")
    public List<PlaceSearchResponse> search(
            @RequestParam String query
    ) {
        return placeSearchService.search(query);
    }
}