package com.tjoeun.goldenstep.ai.client;

import com.tjoeun.goldenstep.ai.dto.request.AnalysisPredictionRequest;
import com.tjoeun.goldenstep.ai.dto.response.AnalysisPredictionResponse;

public interface AnalysisClient {

	AnalysisPredictionResponse analyze(AnalysisPredictionRequest request);
}