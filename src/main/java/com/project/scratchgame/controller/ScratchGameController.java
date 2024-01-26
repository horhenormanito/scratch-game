package com.project.scratchgame.controller;

import com.project.scratchgame.service.ScratchGameService;
import com.project.scratchgame.utils.ScratchGameUtils;

import java.util.Map;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class ScratchGameController {

    private static final Logger LOGGER = Logger.getLogger(ScratchGameController.class.getName());

    public void processNewGame(String configJson, int betValue) {

        ScratchGameService service = new ScratchGameService(configJson);

        // Generate game matrix based on config.json data
        String[][] matrix = service.createNewMatrix();

        // Generate win combination for each unique symbol in the matrix
        Map<String, Set> winCombinationMap = service.generateWinCombination(matrix);

        // compute reward based on the generated win combination of each symbol
        AtomicReference<Double> totalRewards = service.computeReward(betValue, winCombinationMap);

        // Generate result in Json
        String result = ScratchGameUtils.generateResultJson(matrix, totalRewards.get(), winCombinationMap);

        System.out.print(result);
    }
}
