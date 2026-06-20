import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

// Runs a single UNO round: the turn loop, console I/O (view + input), bot decisions,
// and round scoring. Owns a GameState (the rule model) but no persistence or argument
// parsing — those stay in Main. This is the only layer that talks to the console, so the
// rule logic in GameState stays headless and unit-testable.
public class GameEngine {
    private static final Logger LOG = Logger.getLogger(GameEngine.class.getName());

    private final GameState state;
    private final ConsoleView view;
    private final ConsoleInput input;
    private final BotStrategy bot;
    private final int[] scores;

    public GameEngine(GameState state, ConsoleView view, ConsoleInput input, BotStrategy bot) {
        this.state = state;
        this.view = view;
        this.input = input;
        this.bot = bot;
        this.scores = new int[state.playerCount()];
    }

    public int[] scores() {
        return scores;
    }

    public void showFinalScores() {
        ArrayList<String> names = new ArrayList<String>();
        for (GamePlayer p : state.players) {
            names.add(p.name());
        }
        view.showFinalScores(names, scores);
    }

    public GameResult playRound() {
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
                if (Rules.isLegal(drawn, state.upCard, state.calledColor)) {
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
                boolean ok = Rules.isLegal(card, state.upCard, state.calledColor);

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
                    boolean called = player.isHuman() ? input.askUno() : true;
                    player.setCalledUno(called);
                    if (called) {
                        LOG.info(name + " called UNO");
                        view.announceUno(name);
                    }
                    enforceUnoPenalty(player);
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
    private GameResult resolveStalemate(Instant startedAt, int rounds) {
        int winner = state.fewestCardsWinner();
        int points = state.opponentsHandPoints(winner);
        scores[winner] += points;

        String name = state.players.get(winner).name();
        LOG.info("Round resolved without a normal win: " + name + " wins by fewest cards with " + points + " points");
        view.announceWin(name, points);
        return buildResult(startedAt, rounds, winner, points);
    }

    // Builds the persistence DTO for a finished round: winner plus a per-player score map.
    private GameResult buildResult(Instant startedAt, int rounds, int winnerIndex, int points) {
        Map<String, Integer> perGameScores = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < state.players.size(); i++) {
            perGameScores.put(state.players.get(i).name(), i == winnerIndex ? points : 0);
        }
        return new GameResult(startedAt, Instant.now(), rounds, state.players.get(winnerIndex).name(), perGameScores);
    }

    // If a player is left holding a single card without having called UNO, they draw two
    // penalty cards (self-enforced missed-UNO rule). Package-private so the rule can be
    // unit-tested directly. Returns true if a penalty was applied.
    boolean enforceUnoPenalty(GamePlayer player) {
        if (player.hand().size() == 1 && !player.hasCalledUno()) {
            player.hand().add(state.draw());
            player.hand().add(state.draw());
            LOG.info(player.name() + " forgot to call UNO and drew two penalty cards");
            view.announceUnoPenalty(player.name());
            return true;
        }
        return false;
    }

    private int chooseBotCard(ArrayList<String> hand) {
        return bot.chooseCard(hand, state.upCard, state.calledColor);
    }

    private String chooseBotColor(ArrayList<String> hand) {
        return bot.chooseColor(hand);
    }
}
