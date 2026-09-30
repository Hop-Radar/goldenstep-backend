package com.tjoeun.goldenstep.analysis.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tjoeun.goldenstep.analysis.entity.PlaceCheck;
import com.tjoeun.goldenstep.analysis.entity.PriorityPlace;
import com.tjoeun.goldenstep.analysis.repository.PlaceCheckRepository;
import com.tjoeun.goldenstep.analysis.repository.PriorityPlaceRepository;
import com.tjoeun.goldenstep.global.exception.ErrorCode;
import com.tjoeun.goldenstep.global.exception.RestException;
import com.tjoeun.goldenstep.search.entity.SearchSession;
import com.tjoeun.goldenstep.search.service.SearchSessionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlaceCheckService {

    private final SearchSessionService searchSessionService;
    private final PriorityPlaceRepository priorityPlaceRepository;
    private final PlaceCheckRepository placeCheckRepository;

    @Transactional
	public PlaceCheck check(Long placeId, String recoveryToken) {
        if (placeId == null || placeId <= 0) {
            throw new RestException(ErrorCode.INVALID_PLACE_ID);
        }

		SearchSession searchSession = searchSessionService.restore(recoveryToken);

        PriorityPlace priorityPlace = priorityPlaceRepository
                .findOwnedPlaceForUpdate(placeId, searchSession)
                .orElseThrow(() ->
                        new RestException(ErrorCode.PRIORITY_PLACE_NOT_FOUND)
                );

        return placeCheckRepository
                .findByPriorityPlace(priorityPlace)
                .orElseGet(() ->
                        placeCheckRepository.saveAndFlush(
                                new PlaceCheck(priorityPlace)
                        )
                );
    }
}