import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Persistence;
import jakarta.persistence.TypedQuery;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class GameStatsRepository {

    private final EntityManagerFactory emf;

    public GameStatsRepository() {
        this("uno", null);
    }

    public GameStatsRepository(String persistenceUnitName, Map<String, String> overrides) {
        if (overrides == null) {
            this.emf = Persistence.createEntityManagerFactory(persistenceUnitName);
        } else {
            this.emf = Persistence.createEntityManagerFactory(persistenceUnitName, overrides);
        }
    }

    public void close() {
        if (emf.isOpen()) {
            emf.close();
        }
    }

    public void saveGame(Instant startedAt, Instant endedAt, int roundsPlayed,
                         String winnerName, Map<String, Integer> playerScores) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            Game game = new Game();
            game.setStartedAt(startedAt);
            game.setEndedAt(endedAt);
            game.setRoundsPlayed(roundsPlayed);
            game.setWinner(findOrCreatePlayer(em, winnerName));
            em.persist(game);

            for (Map.Entry<String, Integer> entry : playerScores.entrySet()) {
                Player player = findOrCreatePlayer(em, entry.getKey());
                em.persist(new Score(game, player, entry.getValue()));
            }

            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    public List<Game> findRecentGames(int limit) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT g FROM Game g ORDER BY g.endedAt DESC", Game.class)
                    .setMaxResults(limit)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public long findPlayerWinCount(String playerName) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT COUNT(g) FROM Game g WHERE g.winner.name = :name", Long.class)
                    .setParameter("name", playerName)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public List<Score> findTopScores(int limit) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT s FROM Score s ORDER BY s.scoreValue DESC", Score.class)
                    .setMaxResults(limit)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    private Player findOrCreatePlayer(EntityManager em, String name) {
        TypedQuery<Player> q = em.createQuery("SELECT p FROM Player p WHERE p.name = :name", Player.class);
        q.setParameter("name", name);
        try {
            return q.getSingleResult();
        } catch (NoResultException e) {
            Player p = new Player(name);
            em.persist(p);
            return p;
        }
    }
}
