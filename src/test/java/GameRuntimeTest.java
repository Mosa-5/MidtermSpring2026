import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end runtime checks: a seeded bot-only game must always finish with a
 * winner and a nonzero score. Before the stalemate-resolution fix, roughly half
 * of two-bot games ran to the 3000-turn safety limit and returned no winner,
 * which meant nothing was persisted for those games.
 */
class GameRuntimeTest {

    @BeforeAll
    static void quietOutput() {
        // Silence per-turn java.util.logging output and the console view so the
        // surefire report stays readable across many simulated games.
        Logger.getLogger(GameEngine.class.getName()).setLevel(Level.SEVERE);
        Main.view.setQuiet(true);
    }

    @Test
    void everySeededBotGameFinishesWithAWinnerAndNonzeroScore() {
        for (int bots = 2; bots <= 4; bots++) {
            for (long seed = 1; seed <= 50; seed++) {
                GameState state = new GameState(Main.buildPlayers(bots, false), new Random(seed));
                GameEngine engine = new GameEngine(state, Main.view, null, Main.bot);

                GameResult result = engine.playRound();

                String ctx = "bots=" + bots + " seed=" + seed;
                assertNotNull(result.winnerName, ctx + " finished with no winner");

                Integer winnerScore = result.scores.get(result.winnerName);
                assertNotNull(winnerScore, ctx + " has no score entry for the winner");
                assertTrue(winnerScore > 0, ctx + " winner scored " + winnerScore + " (expected > 0)");
            }
        }
    }
}
