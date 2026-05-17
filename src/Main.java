import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Main {
    static ArrayList<String> playerNames = new ArrayList<String>();
    static ArrayList<Boolean> humanPlayers = new ArrayList<Boolean>();
    static ArrayList<ArrayList<String>> hands = new ArrayList<ArrayList<String>>();
    static ArrayList<String> deck = new ArrayList<String>();
    static ArrayList<String> discard = new ArrayList<String>();
    static int[] scores = new int[10];
    static int currentPlayer = 0;
    static int direction = 1;
    static String upCard = "";
    static String calledColor = "";
    static boolean quiet = false;
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
                quiet = true;
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
        setupPlayers(bots, human);

        if (playerNames.size() < 2 || playerNames.size() > 4) {
            System.out.println("UNO needs 2 to 4 players.");
            return;
        }

        for (int g = 1; g <= games; g++) {
            if (!quiet) {
                System.out.println("\n=== Game " + g + " ===");
            }
            playGame();
        }

        System.out.println("\nFinal scores:");
        for (int i = 0; i < playerNames.size(); i++) {
            System.out.println(playerNames.get(i) + ": " + scores[i]);
        }
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
        deck.clear();
        String[] colors = {"R", "Y", "G", "B"};
        for (int c = 0; c < colors.length; c++) {
            deck.add(colors[c] + "0");
            for (int n = 1; n <= 9; n++) {
                deck.add(colors[c] + n);
                deck.add(colors[c] + n);
            }
            deck.add(colors[c] + "S");
            deck.add(colors[c] + "S");
            deck.add(colors[c] + "R");
            deck.add(colors[c] + "R");
            deck.add(colors[c] + "+2");
            deck.add(colors[c] + "+2");
        }
        for (int i = 0; i < 4; i++) {
            deck.add("W");
            deck.add("W4");
        }
        Collections.shuffle(deck, random);
        discard.clear();
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
            discard.add(upCard);
            upCard = draw();
        }
        calledColor = "";
        direction = 1;
        currentPlayer = random.nextInt(playerNames.size());

        int guard = 0;
        while (guard < 3000) {
            guard++;
            String name = playerNames.get(currentPlayer);
            ArrayList<String> hand = hands.get(currentPlayer);

            if (!quiet) {
                System.out.println("\nUp card: " + upCard + (calledColor.equals("") ? "" : " called " + calledColor));
                System.out.println(name + " hand: " + join(hand));
            }

            int chosen = -1;
            if (humanPlayers.get(currentPlayer).booleanValue()) {
                chosen = askHuman(hand);
            } else {
                chosen = chooseBotCard(hand);
            }

            if (chosen == -1) {
                String drawn = draw();
                hand.add(drawn);
                if (!quiet) {
                    System.out.println(name + " draws " + drawn);
                }
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
                    if (!quiet) {
                        System.out.println(name + " selected an invalid index and draws a penalty card.");
                    }
                    hand.add(draw());
                    next();
                    continue;
                }

                String card = hand.get(chosen);
                boolean ok = false;
                String cardColor = color(card);
                String upColor = color(upCard);
                String cardRank = rank(card);
                String upRank = rank(upCard);

                if (card.startsWith("W")) {
                    ok = true;
                } else if (cardColor.equals(upColor)) {
                    ok = true;
                } else if (!calledColor.equals("") && cardColor.equals(calledColor)) {
                    ok = true;
                } else if (cardRank.equals(upRank) && !cardRank.equals("NUMBER")) {
                    ok = true;
                } else if (cardRank.equals("NUMBER") && upRank.equals("NUMBER") && number(card) == number(upCard)) {
                    ok = true;
                }

                if (!ok) {
                    if (!quiet) {
                        System.out.println(name + " tried illegal card " + card + " and draws a penalty card.");
                    }
                    hand.add(draw());
                    next();
                    continue;
                }

                hand.remove(chosen);
                discard.add(upCard);
                upCard = card;
                calledColor = "";
                if (!quiet) {
                    System.out.println(name + " plays " + card);
                }

                if (card.equals("W") || card.equals("W4")) {
                    if (humanPlayers.get(currentPlayer).booleanValue()) {
                        calledColor = askColor();
                    } else {
                        calledColor = chooseBotColor(hand);
                    }
                    if (!quiet) {
                        System.out.println(name + " calls " + calledColor);
                    }
                }

                if (hand.size() == 1 && !quiet) {
                    System.out.println(name + " says UNO!");
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
                    if (!quiet) {
                        System.out.println(name + " wins and scores " + points);
                    }
                    return;
                }

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
                    if (!quiet) {
                        System.out.println(playerNames.get(currentPlayer) + " draws two.");
                    }
                    next();
                } else if (rank(card).equals("WILD_DRAW_FOUR")) {
                    next();
                    for (int i = 0; i < 4; i++) {
                        hands.get(currentPlayer).add(draw());
                    }
                    if (!quiet) {
                        System.out.println(playerNames.get(currentPlayer) + " draws four.");
                    }
                    next();
                } else {
                    next();
                }
            } else {
                next();
            }
        }
        if (!quiet) {
            System.out.println("Game stopped at safety limit.");
        }
    }

    static String draw() {
        if (deck.size() == 0) {
            deck.addAll(discard);
            discard.clear();
            Collections.shuffle(deck, random);
        }
        if (deck.size() == 0) {
            return "W";
        }
        return deck.remove(0);
    }

    static int chooseBotCard(ArrayList<String> hand) {
        for (int i = 0; i < hand.size(); i++) {
            String card = hand.get(i);
            boolean ok = false;
            if (card.startsWith("W")) ok = true;
            else if (color(card).equals(color(upCard))) ok = true;
            else if (!calledColor.equals("") && color(card).equals(calledColor)) ok = true;
            else if (rank(card).equals(rank(upCard)) && !rank(card).equals("NUMBER")) ok = true;
            else if (rank(card).equals("NUMBER") && rank(upCard).equals("NUMBER") && number(card) == number(upCard)) ok = true;
            if (rank(card).equals("DRAW_TWO") && ok) {
                return i;
            }
        }
        for (int i = 0; i < hand.size(); i++) {
            String card = hand.get(i);
            boolean ok = false;
            if (card.startsWith("W")) ok = true;
            else if (color(card).equals(color(upCard))) ok = true;
            else if (!calledColor.equals("") && color(card).equals(calledColor)) ok = true;
            else if (rank(card).equals(rank(upCard)) && !rank(card).equals("NUMBER")) ok = true;
            else if (rank(card).equals("NUMBER") && rank(upCard).equals("NUMBER") && number(card) == number(upCard)) ok = true;
            if (rank(card).equals("SKIP") && ok) {
                return i;
            }
        }
        for (int i = 0; i < hand.size(); i++) {
            String card = hand.get(i);
            boolean ok = false;
            if (card.startsWith("W")) ok = true;
            else if (color(card).equals(color(upCard))) ok = true;
            else if (!calledColor.equals("") && color(card).equals(calledColor)) ok = true;
            else if (rank(card).equals(rank(upCard)) && !rank(card).equals("NUMBER")) ok = true;
            else if (rank(card).equals("NUMBER") && rank(upCard).equals("NUMBER") && number(card) == number(upCard)) ok = true;
            if (rank(card).equals("NUMBER") && ok) {
                return i;
            }
        }
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).startsWith("W")) {
                return i;
            }
        }
        return -1;
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
        int r = 0;
        int y = 0;
        int g = 0;
        int b = 0;
        for (int i = 0; i < hand.size(); i++) {
            String c = color(hand.get(i));
            if (c.equals("R")) {
                r++;
            } else if (c.equals("Y")) {
                y++;
            } else if (c.equals("G")) {
                g++;
            } else if (c.equals("B")) {
                b++;
            }
        }
        if (r >= y && r >= g && r >= b) {
            return "R";
        } else if (y >= r && y >= g && y >= b) {
            return "Y";
        } else if (g >= r && g >= y && g >= b) {
            return "G";
        } else {
            return "B";
        }
    }

    static boolean isLegal(String card, String up, String call) {
        if (card.startsWith("W")) {
            return true;
        }
        if (color(card).equals(color(up))) {
            return true;
        }
        if (!call.equals("") && color(card).equals(call)) {
            return true;
        }
        if (rank(card).equals(rank(up)) && !rank(card).equals("NUMBER")) {
            return true;
        }
        if (rank(card).equals("NUMBER") && rank(up).equals("NUMBER") && number(card) == number(up)) {
            return true;
        }
        return false;
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

    static String join(ArrayList<String> cards) {
        String out = "";
        for (int i = 0; i < cards.size(); i++) {
            out += i + ":" + cards.get(i);
            if (i < cards.size() - 1) {
                out += " ";
            }
        }
        return out;
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
        deck.clear();
        discard.clear();
        discard.add("R5");
        String drawn1 = draw();
        check("draw_reshuffles_discard_when_deck_empty", drawn1.equals("R5"));
        check("draw_reshuffle_clears_discard", discard.size() == 0);

        deck.clear();
        discard.clear();
        String drawn2 = draw();
        check("draw_returns_W_when_deck_and_discard_both_empty", drawn2.equals("W"));

        System.out.println("Passed " + testPassed + " of " + (testPassed + testFailed) + " characterization checks.");
        if (testFailed > 0) {
            System.exit(1);
        }
    }

    static void fail(String name) {
        throw new RuntimeException("Failed: " + name);
    }
}
