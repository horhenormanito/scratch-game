package com.project.scratchgame.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.project.scratchgame.service.ScratchGameService;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ScratchGameUtils {

    private static final Logger LOGGER = Logger.getLogger(ScratchGameUtils.class.getName());
    public static void printMatrix(String[][] matrix){
        // Print the 2D array
        System.out.println("Matrix Card: ");
        for (String[] row :  matrix){
            for (String col : row){
                System.out.print(col + " ");
            }
            System.out.println();
        }
    }

    public static Map<String, Integer> getSymbolMapCount (String[][] matrix){
        Map<String, Integer> elementCountMap = new HashMap<>();
        for (String[] row : matrix) {
            for (String element : row) {
                elementCountMap.put(element, elementCountMap.getOrDefault(element, 0) + 1);
            }
        }

//        LOGGER.info("Unique element count map: " + elementCountMap);
        return elementCountMap;
    }

    public static String generateResultJson(String[][] matrix, Double totalRewards, Map<String, Set> winCombinationMap){

        Map<String, Object> resultMap = new LinkedHashMap<>();

        final Gson gson = new Gson();
        resultMap.put("matrix", matrix);
        resultMap.put("reward", String.valueOf(totalRewards));

        Set<String> bonus = winCombinationMap.get("bonus");
        winCombinationMap.remove("bonus");
        resultMap.put("applied_winning_combinations", winCombinationMap);

        String bonusString = "";
        if (bonus != null && !bonus.isEmpty()){
            bonusString = bonus.stream().collect(Collectors.joining(","));
        }

        resultMap.put("applied_bonus_symbol", bonusString);

        return beautifyJson(gson.toJson(resultMap));

    }

    public static String beautifyJson(String jsonString) {
        // Use Gson to parse the JSON string
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jsonParser = new JsonParser();
        JsonElement jsonElement = jsonParser.parse(jsonString);

        // Convert the parsed JSON element back to a beautified string
        return gson.toJson(jsonElement);
    }
}
