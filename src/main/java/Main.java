import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    static {
        Logger.getLogger("org.hibernate").setLevel(Level.WARNING);
        Logger.getLogger("org.jboss").setLevel(Level.WARNING);
        Logger.getLogger("SQL").setLevel(Level.WARNING);
    }

    static GameState state;
    static BotStrategy bot = new BotStrategy();
    static ConsoleView view = new ConsoleView();
    static ConsoleInput input;
    static int[] scores = new int[10];

    public static void main(String[] args) {
        int bots = 3;
        int games = 1;
        boolean human = false;
        long seed = System.currentTimeMillis();
        int showRecentN = 0;
        String showWinsName = null;
        int showTopN = 0;

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
            } else if (args[i].equals("--show-recent") && i + 1 < args.length) {
                showRecentN = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--show-wins") && i + 1 < args.length) {
                showWinsName = args[++i];
            } else if (args[i].equals("--show-top") && i + 1 < args.length) {
                showTopN = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--self-test")) {
                selfTest();
                return;
            } else if (args[i].equals("--help")) {
                System.out.println("Usage: java -jar uno-cli.jar [--bots N] [--games N] [--human] [--quiet] [--seed N]");
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

        state = new GameState(players, new Random(seed));
        final Scanner scanner = new Scanner(System.in);
        input = new ConsoleInput(new InputSource() {
            public String nextLine() {
                return scanner.nextLine();
            }
        }, view);

        GameStatsRepository statsRepo = new GameStatsRepository();
        try {
            for (int g = 1; g <= games; g++) {
                view.showGameHeader(g);
                GameResult result = playGame();
                if (result.winnerName != null) {
                    statsRepo.saveGame(result.startedAt, result.endedAt, result.roundsPlayed,
                            result.winnerName, result.scores);
                }
            }
        } finally {
            statsRepo.close();
        }

        ArrayList<String> names = new ArrayList<String>();
        for (GamePlayer p : state.players) {
            names.add(p.name());
        }
        view.showFinalScores(names, scores);
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

    static GameResult playGame() {
        Instant startedAt = Instant.now();
        state.setupRound();
        LOG.info("Game started with " + state.playerCount() + " players; first up card " + state.upCard);

        int safetyCounter = 0;
        while (safetyCounter < 3000) {
            safetyCounter++;
            GamePlayer player = state.current();
            String name = player.name();
            ArrayList<String> hand = player.hand();
            LOG.info("Turn " + safetyCounter + ": " + name);

            view.showUpCard(state.upCard, state.calledColor);
            view.showHand(name, hand);

            int chosen = -1;
            if (player.isHuman()) {
                chosen = input.askHuman(hand, state.upCard, state.calledColor);
            } else {
                chosen = chooseBotCard(hand);
            }

            if (chosen == -1) {
                if (!state.deckHasCards()) {
                    LOG.info("Deck exhausted on " + name + "'s turn; resolving round by fewest cards");
                    return resolveStalemate(startedAt, safetyCounter);
                }
                String drawn = state.draw();
                hand.add(drawn);
                LOG.info(name + " drew " + drawn);
                view.announceDraw(name, drawn);
                if (isLegal(drawn, state.upCard, state.calledColor)) {
                    if (!player.isHuman()) {
                        chosen = hand.size() - 1;
                    } else if (input.askPlayDrawn(drawn)) {
                        chosen = hand.size() - 1;
                    }
                }
            }

            if (chosen >= 0) {
                if (chosen >= hand.size()) {
                    LOG.warning(name + " invalid input: index " + chosen + " out of range");
                    view.announceInvalidIndex(name);
                    String penalty = state.draw();
                    hand.add(penalty);
                    LOG.info(name + " drew penalty " + penalty);
                    state.advanceTurn();
                    continue;
                }

                String card = hand.get(chosen);
                boolean ok = isLegal(card, state.upCard, state.calledColor);

                if (!ok) {
                    LOG.warning(name + " invalid input: illegal card " + card);
                    view.announceIllegalCard(name, card);
                    String penalty = state.draw();
                    hand.add(penalty);
                    LOG.info(name + " drew penalty " + penalty);
                    state.advanceTurn();
                    continue;
                }

                hand.remove(chosen);
                state.deck.discard(state.upCard);
                state.upCard = card;
                state.calledColor = "";
                LOG.info(name + " played " + card);
                view.announcePlay(name, card);

                if (card.equals("W") || card.equals("W4")) {
                    if (player.isHuman()) {
                        state.calledColor = input.askColor();
                    } else {
                        state.calledColor = chooseBotColor(hand);
                    }
                    view.announceColorCall(name, state.calledColor);
                }

                if (hand.size() == 1) {
                    view.announceUno(name);
                }

                if (hand.size() == 0) {
                    int points = state.opponentsHandPoints(state.currentPlayer);
                    scores[state.currentPlayer] += points;
                    LOG.info("Game ended: " + name + " won with " + points + " points");
                    view.announceWin(name, points);
                    return buildResult(startedAt, safetyCounter, state.currentPlayer, points);
                }

                GameState.ForcedDraw forced = state.applyEffect(card);
                if (forced != null) {
                    if (forced.count == 2) {
                        LOG.info(forced.playerName + " drew 2 cards (DRAW_TWO effect)");
                        view.announceDrawTwo(forced.playerName);
                    } else if (forced.count == 4) {
                        LOG.info(forced.playerName + " drew 4 cards (WILD_DRAW_FOUR effect)");
                        view.announceDrawFour(forced.playerName);
                    }
                }
            } else {
                state.advanceTurn();
            }
        }
        LOG.warning("Game reached safety limit (3000 turns); resolving round by fewest cards");
        view.announceSafetyLimit();
        return resolveStalemate(startedAt, safetyCounter);
    }

    // Resolves a round that ended without a normal win (deck exhausted or turn cap):
    // the player holding the fewest hand points wins and scores the sum of all
    // other players' remaining card points, mirroring the normal win scoring.
    static GameResult resolveStalemate(Instant startedAt, int rounds) {
        int winner = state.fewestCardsWinner();
        int points = state.opponentsHandPoints(winner);
        scores[winner] += points;

        String name = state.players.get(winner).name();
        LOG.info("Round resolved without a normal win: " + name + " wins by fewest cards with " + points + " points");
        view.announceWin(name, points);
        return buildResult(startedAt, rounds, winner, points);
    }

    // Builds the persistence DTO for a finished round: winner plus a per-player score map.
    static GameResult buildResult(Instant startedAt, int rounds, int winnerIndex, int points) {
        Map<String, Integer> perGameScores = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < state.players.size(); i++) {
            perGameScores.put(state.players.get(i).name(), i == winnerIndex ? points : 0);
        }
        return new GameResult(startedAt, Instant.now(), rounds, state.players.get(winnerIndex).name(), perGameScores);
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

    static int chooseBotCard(ArrayList<String> hand) {
        return bot.chooseCard(hand, state.upCard, state.calledColor);
    }

    static String chooseBotColor(ArrayList<String> hand) {
        return bot.chooseColor(hand);
    }

    static boolean isLegal(String card, String up, String call) {
        return Rules.isLegal(card, up, call);
    }

    static String color(String card) {
        return Card.color(card);
    }

    static String rank(String card) {
        return Card.rank(card);
    }

    static int number(String card) {
        return Card.number(card);
    }

    static int points(String card) {
        return Card.points(card);
    }

    static int testPassed = 0;
    static int testFailed = 0;

    static void check(String name, boolean cond) {
        if (cond) {
            testPassed++;
        } else {
            testFailed++;
            System.out.println("FAIL: " + name);
        }
    }

    static void selfTest() {
        testPassed = 0;
        testFailed = 0;
        Random random = new Random(42);
        view.setQuiet(true);

        //Card helpers
        check("color_R5", color("R5").equals("R"));
        check("color_wild_empty", color("W").equals(""));
        check("rank_number", rank("R5").equals("NUMBER"));
        check("rank_skip", rank("BS").equals("SKIP"));
        check("rank_wild_draw_four", rank("W4").equals("WILD_DRAW_FOUR"));
        check("number_extracted", number("R7") == 7);
        check("number_minus_one_for_action_card", number("RS") == -1);

        //Legality by color, number, action
        check("legal_same_color", isLegal("R2", "R9", ""));
        check("legal_same_number_diff_color", isLegal("G9", "R9", ""));
        check("legal_skip_on_skip_diff_color", isLegal("BS", "RS", ""));
        check("legal_reverse_on_reverse_diff_color", isLegal("BR", "GR", ""));
        check("legal_draw_two_on_draw_two_diff_color", isLegal("B+2", "G+2", ""));
        check("illegal_action_on_number", !isLegal("BS", "R5", ""));
        check("illegal_color_and_number_mismatch", !isLegal("B3", "R9", ""));

        //Wild always legal
        check("wild_always_legal_on_number", isLegal("W", "R9", ""));
        check("wild_draw_four_always_legal_on_action", isLegal("W4", "G+2", ""));

        //Called color
        check("legal_after_wild_called_color", isLegal("B3", "W", "B"));

        //Points / scoring
        check("points_number_face_value", points("R5") == 5);
        check("points_skip", points("BS") == 20);
        check("points_reverse", points("GR") == 20);
        check("points_draw_two", points("R+2") == 20);
        check("points_wild", points("W") == 50);
        check("points_wild_draw_four", points("W4") == 50);

        //Bot card priority: DRAW_TWO > SKIP > NUMBER > WILD
        ArrayList<String> h = new ArrayList<String>();
        h.add("B3"); h.add("R4"); h.add("W");
        check("bot_picks_number_before_wild", bot.chooseCard(h, "R9", "") == 1);

        ArrayList<String> hPriority = new ArrayList<String>();
        hPriority.add("R3"); hPriority.add("RS"); hPriority.add("R+2"); hPriority.add("W");
        check("bot_prefers_draw_two_over_skip_and_number", bot.chooseCard(hPriority, "R9", "") == 2);

        ArrayList<String> hSkipOverNum = new ArrayList<String>();
        hSkipOverNum.add("R3"); hSkipOverNum.add("RS"); hSkipOverNum.add("W");
        check("bot_prefers_skip_over_number", bot.chooseCard(hSkipOverNum, "R9", "") == 1);

        ArrayList<String> hNumOverWild = new ArrayList<String>();
        hNumOverWild.add("R3"); hNumOverWild.add("W");
        check("bot_prefers_number_over_wild_when_legal", bot.chooseCard(hNumOverWild, "R9", "") == 0);

        ArrayList<String> hWildOnly = new ArrayList<String>();
        hWildOnly.add("B3"); hWildOnly.add("W");
        check("bot_falls_back_to_wild_when_nothing_legal", bot.chooseCard(hWildOnly, "R9", "") == 1);

        ArrayList<String> hNoPlay = new ArrayList<String>();
        hNoPlay.add("B3"); hNoPlay.add("Y5");
        check("bot_returns_minus_one_when_no_legal_and_no_wild", bot.chooseCard(hNoPlay, "R9", "") == -1);

        //Bot color tie-breaking: R > Y > G > B
        ArrayList<String> h2 = new ArrayList<String>();
        h2.add("B1"); h2.add("B2"); h2.add("R3");
        check("bot_color_blue_majority", bot.chooseColor(h2).equals("B"));

        ArrayList<String> tieAll = new ArrayList<String>();
        tieAll.add("R1"); tieAll.add("Y1"); tieAll.add("G1"); tieAll.add("B1");
        check("bot_color_four_way_tie_prefers_red", bot.chooseColor(tieAll).equals("R"));

        ArrayList<String> tieYG = new ArrayList<String>();
        tieYG.add("Y1"); tieYG.add("G1");
        check("bot_color_tie_prefers_yellow_over_green", bot.chooseColor(tieYG).equals("Y"));

        //Draw quirks: reshuffle when deck empty; "W" fallback when both empty
        Deck testDeck = new Deck(random);
        testDeck.discard("R5");
        check("draw_reshuffles_discard_when_deck_empty", testDeck.draw().equals("R5"));
        check("draw_returns_W_when_deck_and_discard_both_empty", testDeck.draw().equals("W"));

        //Card effects: skip, reverse (2p quirk), reverse (3p), draw two, wild draw four
        GameState g3 = newTestState(3, random);
        g3.currentPlayer = 0;
        g3.direction = 1;
        g3.applyEffect("BS");
        check("skip_advances_two_players_in_3p", g3.currentPlayer == 2);

        GameState g2 = newTestState(2, random);
        g2.currentPlayer = 0;
        g2.direction = 1;
        g2.applyEffect("BR");
        check("reverse_in_2p_returns_to_same_player", g2.currentPlayer == 0);
        check("reverse_in_2p_still_flips_direction", g2.direction == -1);

        GameState g3b = newTestState(3, random);
        g3b.currentPlayer = 0;
        g3b.direction = 1;
        g3b.applyEffect("BR");
        check("reverse_flips_direction_in_3p", g3b.direction == -1);
        check("reverse_in_3p_advances_one_step_backward", g3b.currentPlayer == 2);

        GameState g3c = newTestState(3, random);
        g3c.currentPlayer = 0;
        g3c.direction = 1;
        g3c.applyEffect("R+2");
        check("draw_two_target_gains_two_cards", g3c.players.get(1).hand().size() == 2);
        check("draw_two_advances_past_target", g3c.currentPlayer == 2);

        GameState g3d = newTestState(3, random);
        g3d.currentPlayer = 0;
        g3d.direction = 1;
        g3d.applyEffect("W4");
        check("wild_draw_four_target_gains_four_cards", g3d.players.get(1).hand().size() == 4);
        check("wild_draw_four_advances_past_target", g3d.currentPlayer == 2);

        //Human input quirks via InputSource seam
        ArrayList<String> handForInputTest = new ArrayList<String>();
        handForInputTest.add("R5");
        ConsoleInput testInput1 = new ConsoleInput(queueInput("draw"), view);
        check("human_can_type_draw_with_legal_play", testInput1.askHuman(handForInputTest, "R9", "") == -1);

        ConsoleInput testInput2 = new ConsoleInput(queueInput("R5"), view);
        check("human_typing_card_code_returns_index_when_legal", testInput2.askHuman(handForInputTest, "R9", "") == 0);

        ConsoleInput testInput3 = new ConsoleInput(queueInput("BAD", "R5"), view);
        check("human_illegal_code_reprompts_then_returns_legal", testInput3.askHuman(handForInputTest, "R9", "") == 0);

        ConsoleInput testInput4 = new ConsoleInput(queueInput("G"), view);
        check("askColor_returns_chosen_color", testInput4.askColor().equals("G"));

        System.out.println("Passed " + testPassed + " of " + (testPassed + testFailed) + " characterization checks.");
        if (testFailed > 0) {
            System.exit(1);
        }
    }

    // A bare GameState with an empty-handed roster and a freshly built deck, for
    // exercising action-card effects without dealing a full round.
    static GameState newTestState(int bots, Random random) {
        GameState gs = new GameState(buildPlayers(bots, false), random);
        gs.deck.buildFresh();
        return gs;
    }

    static InputSource queueInput(final String... lines) {
        return new InputSource() {
            int idx = 0;
            public String nextLine() {
                return lines[idx++];
            }
        };
    }
}
