import java.time.Instant;
import java.util.Map;

public class GameResult {

    public final Instant startedAt;
    public final Instant endedAt;
    public final int roundsPlayed;
    public final String winnerName;
    public final Map<String, Integer> scores;

    public GameResult(Instant startedAt, Instant endedAt, int roundsPlayed,
                      String winnerName, Map<String, Integer> scores) {
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.roundsPlayed = roundsPlayed;
        this.winnerName = winnerName;
        this.scores = scores;
    }
}
