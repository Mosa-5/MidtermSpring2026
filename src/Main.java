import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class Main {
    static ArrayList<String> playerNames = new ArrayList<String>();
    static ArrayList<Boolean> humanPlayers = new ArrayList<Boolean>();
    static ArrayList<ArrayList<String>> hands = new ArrayList<ArrayList<String>>();
    static Deck deck;
    static BotStrategy bot = new BotStrategy();
    static ConsoleView view = new ConsoleView();
    static int[] scores = new int[10];
    static int currentPlayer = 0;
    static int direction = 1;
    static String upCard = "";
    static String calledColor = "";
    static Random random = new Random();
    static Scanner scanner = new Scanner(System.in);

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
        setupPlayers(bots, human);

        if (playerNames.size() < 2 || playerNames.size() > 4) {
            System.out.println("UNO needs 2 to 4 players.");
            return;
        }

        for (int g = 1; g <= games; g++) {
            view.showGameHeader(g);
            playGame();
        }

        view.showFinalScores(playerNames, scores);
    }

    static void setupPlayers(int bots, boolean human) {
        playerNames.clear();
        humanPlayers.clear();
        hands.clear();
        if (human) {
            playerNames.add("You");
            humanPlayers.add(Boolean.TRUE);
            hands.add(new ArrayList<String>());
        }
        for (int i = 1; i <= bots; i++) {
            playerNames.add("Bot" + i);
            humanPlayers.add(Boolean.FALSE);
            hands.add(new ArrayList<String>());
        }
    }

    static void playGame() {
        setupRound();

        int guard = 0;
        while (guard < 3000) {
            guard++;
            String name = playerNames.get(currentPlayer);
            ArrayList<String> hand = hands.get(currentPlayer);

            view.showUpCard(upCard, calledColor);
            view.showHand(name, hand);

            int chosen = -1;
            if (humanPlayers.get(currentPlayer).booleanValue()) {
                chosen = askHuman(hand);
            } else {
                chosen = chooseBotCard(hand);
            }

            if (chosen == -1) {
                String drawn = draw();
                hand.add(drawn);
                view.announceDraw(name, drawn);
                if (isLegal(drawn, upCard, calledColor)) {
                    if (!humanPlayers.get(currentPlayer).booleanValue()) {
                        chosen = hand.size() - 1;
                    } else {
                        System.out.print("Play drawn card " + drawn + "? y/n: ");
                        String answer = scanner.nextLine();
                        if (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes")) {
                            chosen = hand.size() - 1;
                        }
                    }
                }
            }

            if (chosen >= 0) {
                if (chosen >= hand.size()) {
                    view.announceInvalidIndex(name);
                    hand.add(draw());
                    next();
                    continue;
                }

                String card = hand.get(chosen);
                boolean ok = isLegal(card, upCard, calledColor);

                if (!ok) {
                    view.announceIllegalCard(name, card);
                    hand.add(draw());
                    next();
                    continue;
                }

                hand.remove(chosen);
                deck.discard(upCard);
                upCard = card;
                calledColor = "";
                view.announcePlay(name, card);

                if (card.equals("W") || card.equals("W4")) {
                    if (humanPlayers.get(currentPlayer).booleanValue()) {
                        calledColor = askColor();
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
                    view.announceWin(name, points);
                    return;
                }

                applyEffect(card);
            } else {
                next();
            }
        }
        view.announceSafetyLimit();
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
            next();
            next();
        } else if (rank(card).equals("REVERSE")) {
            direction = direction * -1;
            if (playerNames.size() == 2) {
                next();
                next();
            } else {
                next();
            }
        } else if (rank(card).equals("DRAW_TWO")) {
            next();
            hands.get(currentPlayer).add(draw());
            hands.get(currentPlayer).add(draw());
            view.announceDrawTwo(playerNames.get(currentPlayer));
            next();
        } else if (rank(card).equals("WILD_DRAW_FOUR")) {
            next();
            for (int i = 0; i < 4; i++) {
                hands.get(currentPlayer).add(draw());
            }
            view.announceDrawFour(playerNames.get(currentPlayer));
            next();
        } else {
            next();
        }
    }

    static String draw() {
        return deck.draw();
    }

    static int chooseBotCard(ArrayList<String> hand) {
        return bot.chooseCard(hand, upCard, calledColor);
    }

    static int askHuman(ArrayList<String> hand) {
        while (true) {
            System.out.print("Choose card index/code or draw: ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("DRAW")) {
                return -1;
            }
            try {
                int index = Integer.parseInt(input);
                if (index >= 0 && index < hand.size()) {
                    return index;
                }
            } catch (Exception ignored) {
            }
            for (int i = 0; i < hand.size(); i++) {
                if (hand.get(i).equals(input)) {
                    if (isLegal(hand.get(i), upCard, calledColor)) {
                        return i;
                    }
                    System.out.println("That card is not legal.");
                }
            }
            System.out.println("Card not found.");
        }
    }

    static String askColor() {
        while (true) {
            System.out.print("Call color R/Y/G/B: ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("R")) {
                return "R";
            }
            if (input.equals("Y")) {
                return "Y";
            }
            if (input.equals("G")) {
                return "G";
            }
            if (input.equals("B")) {
                return "B";
            }
            System.out.println("Bad color.");
        }
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

    static void next() {
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

        System.out.println("Passed " + testPassed + " of " + (testPassed + testFailed) + " characterization checks.");
        if (testFailed > 0) {
            System.exit(1);
        }
    }

    static void fail(String name) {
        throw new RuntimeException("Failed: " + name);
    }
}
