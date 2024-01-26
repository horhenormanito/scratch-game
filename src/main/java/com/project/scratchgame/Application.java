package com.project.scratchgame;

import com.project.scratchgame.controller.ScratchGameController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Application {
    public static void main(String[] args) {
        try {
            String configFileName= null;
            int bettingAmount = 0;

            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("--config") && i + 1 < args.length) {
                    configFileName = args[i + 1];
                } else if (args[i].equals("--betting-amount") && i + 1 < args.length) {
                    bettingAmount = Integer.parseInt(args[i + 1]);
                }
            }
            // Assuming config.json is in the same directory as the project
            Path configFilePath = Paths.get(configFileName);

            byte[] fileBytes = Files.readAllBytes(configFilePath);
            String configJson = new String(fileBytes);

            ScratchGameController controller = new ScratchGameController();
            controller.processNewGame(configJson, bettingAmount);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
