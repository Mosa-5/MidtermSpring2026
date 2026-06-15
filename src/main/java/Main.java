import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
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

    static ArrayList<String> playerNames = new ArrayList<String>();
    static ArrayList<Boolean> isHuman = new ArrayList<Boolean>();
    static ArrayList<ArrayList<String>> hands = new ArrayList<ArrayList<String>>();
    static Deck deck;
    static BotStrategy bot = new BotStrategy();
    static ConsoleView view = new ConsoleView();
    static ConsoleInput input;
    static int[] scores = new int[10];
    static int currentPlayer = 0;
    static int direction = 1;
    static String upCard = "";
    static String calledColor = "";
    static Random random = new Random();

    public static void main(String[] args) {
        int bots = 3;
        int games = 1;
        boolean human = false;
        long seed = System.currentTimeMillis();

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
            } else if (args[i].equals("--self-test")) {
                selfTest();
                return;
            } else if (args[i].equals("--help")) {
                System.out.println("Usage: scripts/run.sh [--bots N] [--games N] [--human] [--quiet] [--seed N]");
                return;
            }
        }

        random = new Random(seed);
        deck = new Deck(random);
        final Scanner scanner = new Scanner(System.in);
        input = new ConsoleInput(new InputSource() {
            public String nextLine() {
                return scanner.nextLine();
            }
        }, view);
        setupPlayers(bots, human);

        if (playerNames.size() < 2 || playerNames.size() > 4) {
            System.out.println("UNO needs 2 to 4 players.");
            return;
        }

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

        view.showFinalScores(playerNames, scores);
    }

    static void setupPlayers(int bots, boolean human) {
        playerNames.clear();
        isHuman.clear();
        hands.clear();
        if (human) {
            playerNames.add("You");
            isHuman.add(Boolean.TRUE);
            hands.add(new ArrayList<String>());
        }
        for (int i = 1; i <= bots; i++) {
            playerNames.add("Bot" + i);
            isHuman.add(Boolean.FALSE);
            hands.add(new ArrayList<String>());
        }
    }

    static GameResult playGame() {
        Instant startedAt = Instant.now();
        setupRound();
        LOG.info("Game started with " + playerNames.size() + " players; first up card " + upCard);

        int safetyCounter = 0;
        while (safetyCounter < 3000) {
            safetyCounter++;
            String name = playerNames.get(currentPlayer);
            ArrayList<String> hand = hands.get(currentPlayer);
            LOG.info("Turn " + safetyCounter + ": " + name);

            view.showUpCard(upCard, calledColor);
            view.showHand(name, hand);

            int chosen = -1;
            if (isHuman.get(currentPlayer).booleanValue()) {
                chosen = input.askHuman(hand, upCard, calledColor);
            } else {
                chosen = chooseBotCard(hand);
            }

            if (chosen == -1) {
                String drawn = draw();
                hand.add(drawn);
                LOG.info(name + " drew " + drawn);
                view.announceDraw(name, drawn);
                if (isLegal(drawn, upCard, calledColor)) {
                    if (!isHuman.get(currentPlayer).booleanValue()) {
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
                    String penalty = draw();
                    hand.add(penalty);
                    LOG.info(name + " drew penalty " + penalty);
                    advanceTurn();
                    continue;
                }

                String card = hand.get(chosen);
                boolean ok = isLegal(card, upCard, calledColor);

                if (!ok) {
                    LOG.warning(name + " invalid input: illegal card " + card);
                    view.announceIllegalCard(name, card);
                    String penalty = draw();
                    hand.add(penalty);
                    LOG.info(name + " drew penalty " + penalty);
                    advanceTurn();
                    continue;
                }

                hand.remove(chosen);
                deck.discard(upCard);
                upCard = card;
                calledColor = "";
                LOG.info(name + " played " + card);
                view.announcePlay(name, card);

                if (card.equals("W") || card.equals("W4")) {
                    if (isHuman.get(currentPlayer).booleanValue()) {
                        calledColor = input.askColor();
                    } else {
                        calledColor = chooseBotColor(hand);
                    }
                    view.announceColorCall(name, calledColor);
                }

                if (hand.size() == 1) {
                    view.announceUno(name);
                }

                if (hand.size() == 0) {
                    int points = 0;
                    for (int i = 0; i < hands.size(); i++) {
                        if (i != currentPlayer) {
                            for (int j = 0; j < hands.get(i).size(); j++) {
                                points += points(hands.get(i).get(j));
                            }
                        }
                    }
                    scores[currentPlayer] += points;
                    LOG.info("Game ended: " + name + " won with " + points + " points");
                    view.announceWin(name, points);

                    Map<String, Integer> perGameScores = new LinkedHashMap<String, Integer>();
                    for (int i = 0; i < playerNames.size(); i++) {
                        perGameScores.put(playerNames.get(i), i == currentPlayer ? points : 0);
                    }
                    return new GameResult(startedAt, Instant.now(), safetyCounter, name, perGameScores);
                }

                applyEffect(card);
            } else {
                advanceTurn();
            }
        }
        LOG.warning("Game ended at safety limit (3000 turns reached)");
        view.announceSafetyLimit();
        return new GameResult(startedAt, Instant.now(), safetyCounter, null, Collections.<String, Integer>emptyMap());
    }

    static void setupRound() {
        deck.buildFresh();
        for (int i = 0; i < hands.size(); i++) {
            hands.get(i).clear();
        }
        for (int i = 0; i < playerNames.size(); i++) {
            for (int j = 0; j < 7; j++) {
                hands.get(i).add(draw());
            }
        }
        upCard = draw();
        while (upCard.startsWith("W")) {
            deck.discard(upCard);
            upCard = draw();
        }
        calledColor = "";
        direction = 1;
        currentPlayer = random.nextInt(playerNames.size());
    }

    static void applyEffect(String card) {
        if (rank(card).equals("SKIP")) {
            advanceTurn();
            advanceTurn();
        } else if (rank(card).equals("REVERSE")) {
            direction = direction * -1;
            if (playerNames.size() == 2) {
                advanceTurn();
                advanceTurn();
            } else {
                advanceTurn();
            }
        } else if (rank(card).equals("DRAW_TWO")) {
            advanceTurn();
            hands.get(currentPlayer).add(draw());
            hands.get(currentPlayer).add(draw());
            LOG.info(playerNames.get(currentPlayer) + " drew 2 cards (DRAW_TWO effect)");
            view.announceDrawTwo(playerNames.get(currentPlayer));
            advanceTurn();
        } else if (rank(card).equals("WILD_DRAW_FOUR")) {
            advanceTurn();
            for (int i = 0; i < 4; i++) {
                hands.get(currentPlayer).add(draw());
            }
            LOG.info(playerNames.get(currentPlayer) + " drew 4 cards (WILD_DRAW_FOUR effect)");
            view.announceDrawFour(playerNames.get(currentPlayer));
            advanceTurn();
        } else {
            advanceTurn();
        }
    }

    static String draw() {
        return deck.draw();
    }

    static int chooseBotCard(ArrayList<String> hand) {
        return bot.chooseCard(hand, upCard, calledColor);
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

    static void advanceTurn() {
        currentPlayer += direction;
        if (currentPlayer >= playerNames.size()) {
            currentPlayer = 0;
        }
        if (currentPlayer < 0) {
            currentPlayer = playerNames.size() - 1;
        }
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
        upCard = "R9"; calledColor = "";
        check("bot_picks_number_before_wild", chooseBotCard(h) == 1);

        ArrayList<String> hPriority = new ArrayList<String>();
        hPriority.add("R3"); hPriority.add("RS"); hPriority.add("R+2"); hPriority.add("W");
        upCard = "R9"; calledColor = "";
        check("bot_prefers_draw_two_over_skip_and_number", chooseBotCard(hPriority) == 2);

        ArrayList<String> hSkipOverNum = new ArrayList<String>();
        hSkipOverNum.add("R3"); hSkipOverNum.add("RS"); hSkipOverNum.add("W");
        upCard = "R9"; calledColor = "";
        check("bot_prefers_skip_over_number", chooseBotCard(hSkipOverNum) == 1);

        ArrayList<String> hNumOverWild = new ArrayList<String>();
        hNumOverWild.add("R3"); hNumOverWild.add("W");
        upCard = "R9"; calledColor = "";
        check("bot_prefers_number_over_wild_when_legal", chooseBotCard(hNumOverWild) == 0);

        ArrayList<String> hWildOnly = new ArrayList<String>();
        hWildOnly.add("B3"); hWildOnly.add("W");
        upCard = "R9"; calledColor = "";
        check("bot_falls_back_to_wild_when_nothing_legal", chooseBotCard(hWildOnly) == 1);

        ArrayList<String> hNoPlay = new ArrayList<String>();
        hNoPlay.add("B3"); hNoPlay.add("Y5");
        upCard = "R9"; calledColor = "";
        check("bot_returns_minus_one_when_no_legal_and_no_wild", chooseBotCard(hNoPlay) == -1);

        //Bot color tie-breaking: R > Y > G > B
        ArrayList<String> h2 = new ArrayList<String>();
        h2.add("B1"); h2.add("B2"); h2.add("R3");
        check("bot_color_blue_majority", chooseBotColor(h2).equals("B"));

        ArrayList<String> tieAll = new ArrayList<String>();
        tieAll.add("R1"); tieAll.add("Y1"); tieAll.add("G1"); tieAll.add("B1");
        check("bot_color_four_way_tie_prefers_red", chooseBotColor(tieAll).equals("R"));

        ArrayList<String> tieYG = new ArrayList<String>();
        tieYG.add("Y1"); tieYG.add("G1");
        check("bot_color_tie_prefers_yellow_over_green", chooseBotColor(tieYG).equals("Y"));

        //Draw quirks: reshuffle when deck empty; "W" fallback when both empty
        Deck testDeck = new Deck(random);
        testDeck.discard("R5");
        check("draw_reshuffles_discard_when_deck_empty", testDeck.draw().equals("R5"));
        check("draw_returns_W_when_deck_and_discard_both_empty", testDeck.draw().equals("W"));

        //Card effects: skip, reverse (2p quirk), reverse (3p), draw two, wild draw four
        view.setQuiet(true);
        deck = new Deck(random);
        deck.buildFresh();

        setupPlayers(3, false);
        currentPlayer = 0;
        direction = 1;
        applyEffect("BS");
        check("skip_advances_two_players_in_3p", currentPlayer == 2);

        setupPlayers(2, false);
        currentPlayer = 0;
        direction = 1;
        applyEffect("BR");
        check("reverse_in_2p_returns_to_same_player", currentPlayer == 0);
        check("reverse_in_2p_still_flips_direction", direction == -1);

        setupPlayers(3, false);
        currentPlayer = 0;
        direction = 1;
        applyEffect("BR");
        check("reverse_flips_direction_in_3p", direction == -1);
        check("reverse_in_3p_advances_one_step_backward", currentPlayer == 2);

        setupPlayers(3, false);
        currentPlayer = 0;
        direction = 1;
        applyEffect("R+2");
        check("draw_two_target_gains_two_cards", hands.get(1).size() == 2);
        check("draw_two_advances_past_target", currentPlayer == 2);

        setupPlayers(3, false);
        currentPlayer = 0;
        direction = 1;
        applyEffect("W4");
        check("wild_draw_four_target_gains_four_cards", hands.get(1).size() == 4);
        check("wild_draw_four_advances_past_target", currentPlayer == 2);

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

    static void fail(String name) {
        throw new RuntimeException("Failed: " + name);
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
