package mineopoly_three;

import mineopoly_three.competition.CheeseStrategy;
import mineopoly_three.strategy.CompetitionStrategy;
import mineopoly_three.game.GameEngine;
import mineopoly_three.graphics.UserInterface;
import mineopoly_three.replay.Replay;
import mineopoly_three.replay.ReplayIO;
import mineopoly_three.strategy.*;

import javax.swing.*;

public class MineopolyMain {
    private static final int DEFAULT_BOARD_SIZE = 32;
    private static final int PREFERRED_GUI_WIDTH = 750; // Bump this up or down according to your screen size
    private static final boolean TEST_STRATEGY_WIN_PERCENT = true; // Change to true to test your win percent

    // Use this if you want to view a past match replay
    private static final String savedReplayFilePath = null;
//    private static final String savedReplayFilePath = "src/main/java/mineopoly_three/replay/replay";
    // Use this to save a replay of the current match
    private static final String replayOutputFilePath = "src/main/java/mineopoly_three/replay/replay";

    public static void main(String[] args) {
        if (TEST_STRATEGY_WIN_PERCENT) {
            MinePlayerStrategy yourStrategy = new CheeseStrategy();
            int[] assignmentBoardSizes = new int[]{20, 22, 24, 26, 28, 30, 32};
//            int[] assignmentBoardSizes = new int[]{26};

            for (int testBoardSize : assignmentBoardSizes) {
                double strategyWinPercent = getStrategyWinPercent(yourStrategy, testBoardSize);
                System.out.println("(Board size, win percent): (" + testBoardSize + ", " + strategyWinPercent + ")");
            }
        } else {
            // Not testing the win percent, show the game instead
            playGameOrReplay();
        }
    }

    private static void playGameOrReplay() {
        final GameEngine gameEngine;
        if (savedReplayFilePath == null) {
            // Not viewing a replay, play a game with a GUI instead
            MinePlayerStrategy redStrategy = new CheeseStrategy();
            MinePlayerStrategy blueStrategy = new CompetitionStrategy();
            long randomSeed = System.currentTimeMillis();
            gameEngine = new GameEngine(DEFAULT_BOARD_SIZE, redStrategy, blueStrategy, randomSeed);
            gameEngine.setGuiEnabled(true);
        } else {
            // Showing a replay
            gameEngine = ReplayIO.setupEngineForReplay(savedReplayFilePath);
            if (gameEngine == null) {
                return;
            }
        }

        if (gameEngine.isGuiEnabled()) {
            // 500 is around the minimum value that keeps everything on screen
            assert PREFERRED_GUI_WIDTH >= 500;
            // Run the GUI code on a separate Thread (The event dispatch thread)
            SwingUtilities.invokeLater(() -> UserInterface.instantiateGUI(gameEngine, PREFERRED_GUI_WIDTH));
        }

        gameEngine.runGame();

        // Record the replay if the output path isn't null and we aren't already watching a replay
        if (savedReplayFilePath == null && replayOutputFilePath != null) {
            Replay gameReplay = gameEngine.getReplay();
            ReplayIO.writeReplayToFile(gameReplay, replayOutputFilePath);
        }
    }

    private static double getStrategyWinPercent(MinePlayerStrategy yourStrategy, int boardSize) {
        final int numTotalRounds = 30;
        int numRoundsWonByMinScore = 0;
        MinePlayerStrategy randomStrategy = new CustomStrategy();
        GameEngine gameEngine;

        for (int game = 0; game < 30; game++) {
            gameEngine = new GameEngine(boardSize, yourStrategy, randomStrategy);
            gameEngine.runGame();
            if (gameEngine.getRedPlayerScore() > gameEngine.getBluePlayerScore()) {
//                System.out.println("WIN");
                numRoundsWonByMinScore += 1;
            } else {
//                System.out.println("LOSS");
            }
        }

        return ((double) numRoundsWonByMinScore) / numTotalRounds;
    }
}
