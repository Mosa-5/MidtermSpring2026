import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    static {
        Logger.getLogger("org.hibernate").setLevel(Level.WARNING);
        Logger.getLogger("org.jboss").setLevel(Level.WARNING);
        Logger.getLogger("SQL").setLevel(Level.WARNING);
    }

    static BotStrategy bot = new BotStrategy();
    static ConsoleView view = new ConsoleView();

    public static void main(String[] args) {
        int bots = 3;
        int games = 1;
        boolean human = false;
        long seed = System.currentTimeMillis();
        int showRecentN = 0;
        String showWinsName = null;
        int showTopN = 0;
        int target = 0;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--bots") && i + 1 < args.length) {
                bots = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--games") && i + 1 < args.length) {
                games = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--human")) {
                human = true;
            } else if (args[i].equals("--quiet")) {
                view.setQuiet(true);
            } else if (args[i].equals("--seed") && i + 1 < args.length) {
                seed = Long.parseLong(args[++i]);
            } else if (args[i].equals("--target") && i + 1 < args.length) {
                target = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--show-recent") && i + 1 < args.length) {
                showRecentN = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--show-wins") && i + 1 < args.length) {
                showWinsName = args[++i];
            } else if (args[i].equals("--show-top") && i + 1 < args.length) {
                showTopN = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--help")) {
                System.out.println("Usage: java -jar uno-cli.jar [--bots N] [--games N] [--target N] [--human] [--quiet] [--seed N]");
                System.out.println("                            [--show-recent N] [--show-wins NAME] [--show-top N]");
                return;
            }
        }

        if (showRecentN > 0 || showWinsName != null || showTopN > 0) {
            GameStatsRepository queryRepo = new GameStatsRepository();
            try {
                if (showRecentN > 0) {
                    printRecentGames(queryRepo, showRecentN);
                }
                if (showWinsName != null) {
                    printPlayerWins(queryRepo, showWinsName);
                }
                if (showTopN > 0) {
                    printTopScores(queryRepo, showTopN);
                }
            } finally {
                queryRepo.close();
            }
            return;
        }

        List<GamePlayer> players = buildPlayers(bots, human);
        if (players.size() < 2 || players.size() > 4) {
            System.out.println("UNO needs 2 to 4 players.");
            return;
        }

        GameState state = new GameState(players, new Random(seed));
        final Scanner scanner = new Scanner(System.in);
        ConsoleInput input = new ConsoleInput(new InputSource() {
            public String nextLine() {
                return scanner.nextLine();
            }
        }, view);
        GameEngine engine = new GameEngine(state, view, input, bot);

        GameStatsRepository statsRepo = new GameStatsRepository();
        try {
            if (target > 0) {
                // Match mode: play rounds until a player reaches the target score.
                int round = 0;
                do {
                    round++;
                    view.showGameHeader(round);
                    persistRound(statsRepo, engine.playRound());
                } while (!engine.targetReached(target) && round < 1000);
            } else {
                for (int g = 1; g <= games; g++) {
                    view.showGameHeader(g);
                    persistRound(statsRepo, engine.playRound());
                }
            }
        } finally {
            statsRepo.close();
        }

        engine.showFinalScores();
        if (target > 0) {
            engine.announceChampion();
        }
    }

    static void persistRound(GameStatsRepository repo, GameResult result) {
        if (result.winnerName != null) {
            repo.saveGame(result.startedAt, result.endedAt, result.roundsPlayed,
                    result.winnerName, result.scores);
        }
    }

    static List<GamePlayer> buildPlayers(int bots, boolean human) {
        List<GamePlayer> players = new ArrayList<GamePlayer>();
        if (human) {
            players.add(new GamePlayer("You", true));
        }
        for (int i = 1; i <= bots; i++) {
            players.add(new GamePlayer("Bot" + i, false));
        }
        return players;
    }

    static void printRecentGames(GameStatsRepository repo, int n) {
        java.util.List<Game> games = repo.findRecentGames(n);
        System.out.println("Recent games (" + games.size() + "):");
        if (games.isEmpty()) {
            System.out.println("  no games persisted yet");
            return;
        }
        for (Game g : games) {
            System.out.println("  " + g.getEndedAt() + "  winner=" + g.getWinner().getName()
                    + "  rounds=" + g.getRoundsPlayed());
        }
    }

    static void printPlayerWins(GameStatsRepository repo, String name) {
        long wins = repo.findPlayerWinCount(name);
        System.out.println(name + " wins: " + wins);
    }

    static void printTopScores(GameStatsRepository repo, int n) {
        java.util.List<Score> top = repo.findTopScores(n);
        System.out.println("Top scores (" + top.size() + "):");
        if (top.isEmpty()) {
            System.out.println("  no scores persisted yet");
            return;
        }
        for (Score s : top) {
            System.out.println("  " + s.getPlayer().getName() + ": " + s.getScoreValue());
        }
    }
}
