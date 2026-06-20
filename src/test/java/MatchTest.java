import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Multi-round matches: cumulative scoring, target detection, and champion selection.
class MatchTest {

    @BeforeAll
    static void quietLogging() {
        Logger.getLogger(GameEngine.class.getName()).setLevel(Level.SEVERE);
    }

    private GameEngine engine(GameState gs) {
        return new GameEngine(gs, TestSupport.quietView(), null, new BotStrategy());
    }

    @Test
    void targetReachedReflectsCumulativeScore() {
        GameState gs = TestSupport.builtDeckState(2, 1);
        GameEngine engine = engine(gs);

        assertFalse(engine.targetReached(100));
        gs.players.get(1).addScore(120);
        assertTrue(engine.targetReached(100));
    }

    @Test
    void championIsTheHighestCumulativeScorer() {
        GameState gs = TestSupport.builtDeckState(3, 1);
        gs.players.get(0).addScore(50);
        gs.players.get(1).addScore(90);
        gs.players.get(2).addScore(30);

        assertEquals(1, engine(gs).championIndex());
    }

    @Test
    void scoresAccumulateAcrossRounds() {
        GameState gs = new GameState(TestSupport.bots(3), new Random(42));
        GameEngine engine = engine(gs);

        engine.playRound();
        int afterOne = totalScore(gs);
        assertTrue(afterOne > 0, "the first round's winner should have scored");

        engine.playRound();
        int afterTwo = totalScore(gs);
        assertTrue(afterTwo > afterOne, "the second round should add more points");
    }

    private int totalScore(GameState gs) {
        int total = 0;
        for (GamePlayer p : gs.players) {
            total += p.totalScore();
        }
        return total;
    }
}
