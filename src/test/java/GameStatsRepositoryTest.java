import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GameStatsRepositoryTest {

    private GameStatsRepository repo;

    @BeforeEach
    void setUp() {
        Map<String, String> overrides = new HashMap<>();
        overrides.put("jakarta.persistence.jdbc.url",
                "jdbc:h2:mem:test-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        overrides.put("hibernate.hbm2ddl.auto", "create-drop");
        repo = new GameStatsRepository("uno", overrides);
    }

    @AfterEach
    void tearDown() {
        if (repo != null) {
            repo.close();
        }
    }

    @Test
    void saveGamePersistsTimestampsRoundsAndWinner() {
        Instant start = Instant.parse("2024-01-01T10:00:00Z");
        Instant end = Instant.parse("2024-01-01T10:05:00Z");

        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put("Alice", 30);
        scores.put("Bob", 0);
        repo.saveGame(start, end, 42, "Alice", scores);

        List<Game> games = repo.findRecentGames(10);
        assertEquals(1, games.size());

        Game g = games.get(0);
        assertEquals(start, g.getStartedAt());
        assertEquals(end, g.getEndedAt());
        assertEquals(42, g.getRoundsPlayed());
        assertNotNull(g.getWinner());
        assertEquals("Alice", g.getWinner().getName());
    }

    @Test
    void findRecentGamesReturnsMostRecentFirst() {
        Map<String, Integer> s = new LinkedHashMap<>();
        s.put("Alice", 30);
        s.put("Bob", 0);
        repo.saveGame(Instant.parse("2024-01-01T10:00:00Z"),
                Instant.parse("2024-01-01T10:05:00Z"), 10, "Alice", s);

        Map<String, Integer> s2 = new LinkedHashMap<>();
        s2.put("Alice", 0);
        s2.put("Bob", 50);
        repo.saveGame(Instant.parse("2024-01-02T10:00:00Z"),
                Instant.parse("2024-01-02T10:05:00Z"), 20, "Bob", s2);

        List<Game> recent = repo.findRecentGames(10);
        assertEquals(2, recent.size());
        assertEquals("Bob", recent.get(0).getWinner().getName());
        assertEquals("Alice", recent.get(1).getWinner().getName());
    }

    @Test
    void findRecentGamesRespectsLimit() {
        for (int i = 0; i < 5; i++) {
            Map<String, Integer> s = new LinkedHashMap<>();
            s.put("Alice", 10 * i);
            s.put("Bob", 0);
            repo.saveGame(Instant.now().minusSeconds(60 - i),
                    Instant.now().minusSeconds(50 - i), i + 1, "Alice", s);
        }

        assertEquals(3, repo.findRecentGames(3).size());
        assertEquals(5, repo.findRecentGames(100).size());
    }

    @Test
    void findPlayerWinCountCountsOnlyThatPlayer() {
        Map<String, Integer> s = new LinkedHashMap<>();
        s.put("Alice", 50);
        s.put("Bob", 0);

        repo.saveGame(Instant.now(), Instant.now(), 10, "Alice", s);
        repo.saveGame(Instant.now(), Instant.now(), 10, "Alice", s);

        Map<String, Integer> s2 = new LinkedHashMap<>();
        s2.put("Alice", 0);
        s2.put("Bob", 60);
        repo.saveGame(Instant.now(), Instant.now(), 10, "Bob", s2);

        assertEquals(2L, repo.findPlayerWinCount("Alice"));
        assertEquals(1L, repo.findPlayerWinCount("Bob"));
        assertEquals(0L, repo.findPlayerWinCount("Cara"));
    }

    @Test
    void findTopScoresOrdersDescendingAndRespectsLimit() {
        Map<String, Integer> s = new LinkedHashMap<>();
        s.put("Alice", 50);
        s.put("Bob", 80);
        s.put("Cara", 30);
        repo.saveGame(Instant.now(), Instant.now(), 10, "Bob", s);

        List<Score> all = repo.findTopScores(10);
        assertEquals(3, all.size());
        assertEquals(80, all.get(0).getScoreValue());
        assertEquals(50, all.get(1).getScoreValue());
        assertEquals(30, all.get(2).getScoreValue());

        List<Score> topTwo = repo.findTopScores(2);
        assertEquals(2, topTwo.size());
        assertEquals(80, topTwo.get(0).getScoreValue());
        assertEquals(50, topTwo.get(1).getScoreValue());
    }
}
