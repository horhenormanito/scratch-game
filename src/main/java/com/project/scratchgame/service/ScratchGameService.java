package com.project.scratchgame.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.project.scratchgame.utils.ScratchGameUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScratchGameService {

    private static final Logger LOGGER = Logger.getLogger(ScratchGameService.class.getName());

    private JsonObject configJsonObject;
    public ScratchGameService(String configJson){
//        LOGGER.info("starting game service..");
        // Using Gson to convert JSON string to JsonObject
        final Gson gson = new Gson();
        this.configJsonObject = gson.fromJson(configJson, JsonObject.class);
    }

    public String[][] createNewMatrix(){
        return this.generateCardMatrix();
    }

    private String[][] generateCardMatrix(){

        final int columns = configJsonObject.get("columns").getAsInt();
        final int rows = configJsonObject.get("rows").getAsInt();
        final JsonObject probabilities = configJsonObject.get("probabilities").getAsJsonObject();

        // create empty card matrix
        String[][] matrix = new String[rows][columns];
        // Print the 2D array
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                matrix[i][j] = "*";
            }
        }

        // set standard symbol for each cell
        JsonArray probabilityArray = probabilities.getAsJsonArray("standard_symbols");
        probabilityArray.forEach( arr -> {
            JsonObject cell = arr.getAsJsonObject();

            // cell property
            int row = cell.get("row").getAsInt();
            int column = cell.get("column").getAsInt();

            JsonObject symbols = cell.get("symbols").getAsJsonObject();
            double totalProbability = symbols.entrySet().stream().mapToDouble( s -> s.getValue().getAsDouble()).sum();
            double randNum = Math.random() * totalProbability;

            double cumulativeProb = 0;
            String chosenSymbol = null;

            for (Map.Entry<String, JsonElement> entry : symbols.entrySet()) {
                cumulativeProb += entry.getValue().getAsDouble();
                //  this process ensures that symbols with higher probabilities are more likely to be selected,
                //  as the range they cover is proportionally larger.
                if (randNum <= cumulativeProb) {
                    chosenSymbol = entry.getKey();
                    break;
                }
            }
            matrix[row][column] = chosenSymbol;
        });

        // set bonus symbol
        JsonObject bonusSymbols = probabilities.get("bonus_symbols").getAsJsonObject();
        if (Objects.nonNull(bonusSymbols)) {
            JsonObject symbolsObject = bonusSymbols.get("symbols").getAsJsonObject();
            double totalBonusProbability = symbolsObject.entrySet().stream().mapToDouble( s -> s.getValue().getAsDouble()).sum();
            double randBonumNum = Math.random() * totalBonusProbability;
            double cumulativeBonusProb = 0;
            String chosenBonusSymbol = null;

            for (Map.Entry<String, JsonElement> entry : symbolsObject.entrySet()) {
                cumulativeBonusProb += entry.getValue().getAsDouble();
                //  this process ensures that symbols with higher probabilities are more likely to be selected,
                //  as the range they cover is proportionally larger.
                if (randBonumNum <= cumulativeBonusProb) {
                    chosenBonusSymbol = entry.getKey();
                    break;
                }
            }

            // Assign bonus symbol to a random empty cell
            int randomRow = new Random().nextInt(rows);
            int randomCol = new Random().nextInt(columns);

            // if there is/are empty space/s, assign bonus symbol to the empty cell
            if (probabilityArray.size() < (rows*columns)){
                // Assign bonus symbol to a random empty cell
                while (true) {
                    randomRow = new Random().nextInt(rows);
                    randomCol = new Random().nextInt(columns);
                    if (matrix[randomRow][randomCol] == "") {
                        break;
                    }
                }
            }
            matrix[randomRow][randomCol] = chosenBonusSymbol;
        }

//        ScratchGameUtils.printMatrix(matrix);
        return matrix;
    }

    public Map<String, Set> generateWinCombination(String[][] matrix){

        Map<String, Integer> uniqueElements = ScratchGameUtils.getSymbolMapCount(matrix);
        JsonObject winCombinations = this.configJsonObject.get("win_combinations").getAsJsonObject();

        Map<String, Set> resultCombination = new LinkedHashMap<>();
        uniqueElements.entrySet().stream().forEach(element -> {
//            LOGGER.info("Element: " + element.getKey() + " : " + element.getValue());
            Set<String> winningCombinationSet = new LinkedHashSet<>();
            winCombinations.entrySet().stream().forEach(winProp -> {
                JsonObject prop = winProp.getValue().getAsJsonObject();

                Pattern pattern = Pattern.compile("same_symbol_(\\d+)_times");
                Matcher matcher = pattern.matcher(winProp.getKey());

                int sameSymbolTimes = 0;
                // Check if there is a match
                if (matcher.matches()) {
                    // Extract and parse the matched numeric part
                    sameSymbolTimes = Integer.parseInt(matcher.group(1));
                }

                if (sameSymbolTimes == element.getValue()){
                    winningCombinationSet.add(winProp.getKey());
                }

                // linear symbol check
                if ("same_symbols_horizontally".equals(winProp.getKey()) ||
                        "same_symbols_vertically".equals(winProp.getKey()) ||
                        "same_symbols_diagonally_left_to_right".equals(winProp.getKey()) ||
                        "same_symbols_diagonally_right_to_left".equals(winProp.getKey())){
                    this.checkLinearSymbols(matrix, winningCombinationSet, winProp, element);
                }
                resultCombination.put(element.getKey(), winningCombinationSet);
            });
        });

//        LOGGER.info("Combination result: " + resultCombination);
        return resultCombination;
    }

    private void checkLinearSymbols(String[][] matrix, Set winningCombinationList,
                                    Map.Entry<String, JsonElement> winProp, Map.Entry<String, Integer> element){

        JsonObject linearSymbol = winProp.getValue().getAsJsonObject();
        JsonArray coveredArea = linearSymbol.get("covered_areas").getAsJsonArray();
        coveredArea.forEach(row -> {
            AtomicBoolean isSameLinearValue = new AtomicBoolean(true);
            row.getAsJsonArray().forEach(cell -> {
                String[] rowCol = cell.getAsString().split(":");
                int cellRow = Integer.parseInt(rowCol[0]);
                int cellCol = Integer.parseInt(rowCol[1]);

                if (!matrix[cellRow][cellCol].equals(element.getKey())){
                    isSameLinearValue.set(false);
                }
            });
            if (isSameLinearValue.get()){
                winningCombinationList.add(winProp.getKey());
            }
        });
    }

    public AtomicReference<Double> computeReward(int betValue, Map<String, Set> winCombinationMap){

        AtomicReference<Double> totalRewards = new AtomicReference<>(0.0);

        JsonObject symbols = configJsonObject.get("symbols").getAsJsonObject();
        JsonObject winCombinations = this.configJsonObject.get("win_combinations").getAsJsonObject();

        Map<String, Set> appliedWinningCombinationMap = new LinkedHashMap<>();

        winCombinationMap.entrySet().stream().forEach(winMap -> {
            AtomicReference<Double> symbolReward = new AtomicReference<>(Double.valueOf(betValue));
            String symbol = winMap.getKey();
            Set<String> winSet = winMap.getValue();

            JsonObject symbolProperty = symbols.get(symbol).getAsJsonObject();
            String type = symbolProperty.get("type").getAsString();
            if ("standard".equals(type)){
                if (winSet.size() > 0){
                    double multiplier = symbolProperty.get("reward_multiplier").getAsDouble();
                    symbolReward.set(symbolReward.get()*multiplier);

                    winSet.forEach(win -> {
                        JsonObject winObject = winCombinations.get(win).getAsJsonObject();
                        double winMultiplier = winObject.get("reward_multiplier").getAsDouble();
                        symbolReward.set(symbolReward.get() * winMultiplier);
                    });
                    appliedWinningCombinationMap.put(symbol, winSet);
                } else {
                    // skip, no totalRewards
                    symbolReward.set(0.0);
                }
//                LOGGER.info("Symbol: " + symbol + " - totalRewards: " + symbolReward.get());
                totalRewards.set(totalRewards.get() + symbolReward.get());
            }
        });

        final Set<String> bonusSet = new LinkedHashSet<>();
        // for bonus, when total rewards is betting value, reset total rewards to 0
        if (totalRewards.get() == betValue) {
            totalRewards.set(0.0);
        }

        winCombinationMap.entrySet().stream().forEach(winMap -> {
            String symbol = winMap.getKey();
            JsonObject symbolProperty = symbols.get(symbol).getAsJsonObject();
            String type = symbolProperty.get("type").getAsString();
            if ("bonus".equals(type)){
                double rewardBeforeBonus = totalRewards.get();
                String impact = symbolProperty.get("impact").getAsString();
                if ("multiply_reward".equals(impact)){
                    int bonusMultiplier = symbolProperty.get("reward_multiplier").getAsInt();
                    totalRewards.set(totalRewards.get() * bonusMultiplier);
                    bonusSet.add(symbol);
                }else if ("extra_bonus".equals(impact) && totalRewards.get() > betValue){
                    int bonusExtra = symbolProperty.get("extra").getAsInt();
                    totalRewards.set(totalRewards.get() + Double.valueOf(bonusExtra));
                    bonusSet.add(symbol);
                }else {
                    // miss or no totalRewards
                }
//                LOGGER.info("Symbol: " + symbol + " - +/x:totalRewards: " + rewardBeforeBonus + " -> " + totalRewards.get());
            }
        });


        if (bonusSet.size() > 0 && totalRewards.get() > 0){
            appliedWinningCombinationMap.put("bonus", bonusSet);
        } else {
            appliedWinningCombinationMap.put("bonus", null);
        }

        // clear win combination map and replace with applied win combination map for output
        winCombinationMap.clear();
        winCombinationMap.putAll(appliedWinningCombinationMap);

//       LOGGER.info("Total totalRewards: " + totalRewards.get());

        return totalRewards;
    }
}
