package dev.dskarpets;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class TabooGame extends JFrame {
    private final List<Card> deck = new ArrayList<>();
    private final List<Card> centerRow = new ArrayList<>();
    private final Stack<Card> discardPile = new Stack<>();
    private final Player humanPlayer;
    private final Player computerPlayer;
    private Player currentPlayer;
    private final Random random = new Random();

    private final JPanel centerRowPanel = new JPanel();
    private final JPanel playerHandPanel = new JPanel();
    private final JPanel gameInfoPanel = new JPanel();
    private final JTextArea gameLog = new JTextArea();
    private final JLabel stockCountLabel = new JLabel();
    private final JLabel opponentCardCountLabel = new JLabel();
    private final JButton drawCardButton = new JButton("Draw Card");
    private final JButton placeCardButton = new JButton("Place Card in Center");


    public TabooGame() {
        this.humanPlayer = new Player("Human", false);
        this.computerPlayer = new Player("Computer", true);
        this.currentPlayer = humanPlayer;

        setupUI();
        initializeGame();
    }

    private void setupUI() {
        setTitle("Taboo Card Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLayout(new BorderLayout());

        gameLog.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(gameLog);
        add(logScrollPane, BorderLayout.EAST);

        centerRowPanel.setLayout(new FlowLayout());
        add(centerRowPanel, BorderLayout.CENTER);

        playerHandPanel.setLayout(new FlowLayout());
        add(playerHandPanel, BorderLayout.SOUTH);

        gameInfoPanel.setLayout(new FlowLayout());
        gameInfoPanel.add(stockCountLabel);
        gameInfoPanel.add(opponentCardCountLabel);
        gameInfoPanel.add(drawCardButton);
        add(gameInfoPanel, BorderLayout.NORTH);

        drawCardButton.addActionListener(e -> handleDrawCard());

        gameInfoPanel.add(placeCardButton);
        placeCardButton.addActionListener(e -> handlePlaceCard());
    }

    private void initializeGame() {
        initializeDeck();
        shuffleDeck();
        dealInitialCards();
        setupCenterRow();
        updateUI();
        log("Game started! It's your turn.");
    }

    private void initializeDeck() {
        String[] suits = {"Hearts", "Diamonds", "Clubs", "Spades"};
        String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "Jack", "Queen", "King", "Ace"};

        for (String suit : suits) {
            for (String rank : ranks) {
                deck.add(new Card(rank, suit));
            }
        }

        deck.add(new Card("Joker", ""));
        deck.add(new Card("Joker", ""));
    }

    private void shuffleDeck() {
        Collections.shuffle(deck);
    }

    private void dealInitialCards() {
        for (int i = 0; i < 5; i++) {
            humanPlayer.addCardToHand(deck.remove(deck.size() - 1));
            computerPlayer.addCardToHand(deck.remove(deck.size() - 1));
        }
    }

    private void setupCenterRow() {
        while (centerRow.size() < 4) {
            Card card = deck.remove(deck.size() - 1);
            if (!card.getRank().equals("Joker") && !card.getRank().equals("Jack")) {
                centerRow.add(card);
            }
        }
    }

    private void playTurn() {

        hasDrawnCard = false;
        placeCardButton.setEnabled(false);

        if (currentPlayer == humanPlayer) {
            log("It's your turn!");
        } else {
            playComputerTurn();
        }

        checkGameEnd();
        currentPlayer = (currentPlayer == humanPlayer) ? computerPlayer : humanPlayer;

        if (currentPlayer.isComputer()) {
            playTurn();
        }

        updateUI();
    }

    private void playComputerTurn() {
        log(computerPlayer.getName() + "'s turn.");
        if (!computerPlayer.playAutomatedMove(centerRow, deck, discardPile, random)) {
            drawCard(computerPlayer);
        }
    }

    private void handlePlayerMove(Card selectedCard) {
        boolean validMove = false;

        while (!validMove) {
            if (selectedCard.getRank().equals("Jack")) {
                log("You played a Jack! Clearing all face-up cards.");
                discardPile.addAll(centerRow);
                discardPile.add(selectedCard);
                centerRow.clear();
                humanPlayer.removeCardFromHand(selectedCard);

                while (centerRow.size() < 4 && !deck.isEmpty()) {
                    centerRow.add(deck.remove(deck.size() - 1));
                }

                drawCard(computerPlayer);
                validMove = true;
            }
             else if (selectedCard.getRank().equals("Joker")) {
                log("You played a Joker! Opponent draws a card.");
                discardPile.add(selectedCard);
                humanPlayer.removeCardFromHand(selectedCard);
                drawCard(computerPlayer);
                validMove = true;
            } else if (humanPlayer.playMove(centerRow, selectedCard, discardPile)) {
                log("You played " + selectedCard + ".");
                validMove = true;
            } else {
                log("Invalid move! Try again.");
                return;
            }
        }

        playTurn();
    }

    private boolean hasDrawnCard = false;

    private void handleDrawCard() {
        if (hasDrawnCard) {
            log("You can only draw one card per turn!");
            return;
        }

        if (!deck.isEmpty()) {
            Card drawnCard = deck.remove(deck.size() - 1);
            humanPlayer.addCardToHand(drawnCard);
            log("You drew " + drawnCard + ".");
            hasDrawnCard = true;
            placeCardButton.setEnabled(true); // Enable the Place Card button
            JOptionPane.showMessageDialog(this, "You must now use this card or place one card in the center row.");
        } else {
            log("Stock pile is empty. Reshuffling discard pile.");
            reshuffleDiscardPile();
        }
        updateUI();
    }

    private void handlePlaceCard() {
        if (!hasDrawnCard) {
            log("You must draw a card before placing one in the center.");
            return;
        }

        String[] cardOptions = humanPlayer.getHand()
                .stream()
                .map(Card::toString)
                .toArray(String[]::new);

        String selectedCardString = (String) JOptionPane.showInputDialog(
                this,
                "Choose a card to place in the center:",
                "Place Card",
                JOptionPane.PLAIN_MESSAGE,
                null,
                cardOptions,
                cardOptions[0]
        );

        if (selectedCardString != null) {
            Card selectedCard = humanPlayer.getHand().stream()
                    .filter(card -> card.toString().equals(selectedCardString))
                    .findFirst()
                    .orElse(null);

            if (selectedCard != null) {
                humanPlayer.removeCardFromHand(selectedCard);
                centerRow.add(selectedCard);
                log("You placed " + selectedCard + " in the center.");
                hasDrawnCard = false;
                placeCardButton.setEnabled(false);
                playTurn();
            }
        }
    }

    private void drawCard(Player player) {
        if (!deck.isEmpty()) {
            Card card = deck.remove(deck.size() - 1);
            player.addCardToHand(card);
            if (player == humanPlayer) {
                log(player.getName() + " drew " + card + ".");
            } else {
                log(player.getName() + " drew a card.");
            }
        } else {
            log("Deck is empty, reshuffling discard pile...");
            reshuffleDiscardPile();
        }
    }


    private void reshuffleDiscardPile() {
        while (!discardPile.isEmpty()) {
            deck.add(discardPile.pop());
        }
        shuffleDeck();
    }

    private void checkGameEnd() {
        if (humanPlayer.getHand().isEmpty() || computerPlayer.getHand().isEmpty()) {
            log((humanPlayer.getHand().isEmpty() ? "You" : "Computer") + " have finished all cards!");
            calculateScores();
            JOptionPane.showMessageDialog(this, "Game finished! Final Scores:\n" +
                    "You: " + humanPlayer.getScore() + "\n" +
                    "Computer: " + ((computerPlayer.getScore() < 0) ? "0" : computerPlayer.getScore()) ) ;
            System.exit(0);
        }
    }

    private void calculateScores() {
        boolean humanFinishedFirst = humanPlayer.getHand().isEmpty();

        humanPlayer.calculateScore(humanFinishedFirst);
        computerPlayer.calculateScore(!humanFinishedFirst);

        log("Final Scores:\nYou: " + humanPlayer.getScore() + "\nComputer: " + computerPlayer.getScore());
        log(humanPlayer.getScore() > computerPlayer.getScore() ? "You win!" : "Computer wins!");
    }



    private void updateUI() {
        centerRowPanel.removeAll();
        for (Card card : centerRow) {
            JLabel cardLabel = new JLabel(card.toString());
            cardLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            centerRowPanel.add(cardLabel);
        }

        centerRowPanel.revalidate();
        centerRowPanel.repaint();

        playerHandPanel.removeAll();
        for (Card card : humanPlayer.getHand()) {
            JButton cardButton = new JButton(card.toString());
            cardButton.addActionListener(e -> handlePlayerMove(card));
            playerHandPanel.add(cardButton);
        }

        playerHandPanel.revalidate();
        playerHandPanel.repaint();

        stockCountLabel.setText("Stock Pile: " + deck.size());
        opponentCardCountLabel.setText("Opponent's Cards: " + computerPlayer.getHand().size());
    }

    private void log(String message) {
        gameLog.append(message + "\n");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TabooGame game = new TabooGame();
            game.setVisible(true);
        });
    }
}

class Card {
    private final String rank;
    private final String suit;

    public Card(String rank, String suit) {
        this.rank = rank;
        this.suit = suit;
    }

    public String getRank() {
        return rank;
    }

    public String getSuit() {
        return suit;
    }

    @Override
    public String toString() {
        return rank + (suit.isEmpty() ? "" : " of " + suit);
    }
}

class Player {
    private final String name;
    private final boolean isComputer;
    private final ArrayList<Card> hand = new ArrayList<>();
    private final ArrayList<Card> scorePile = new ArrayList<>();
    private int score = 0;

    public Player(String name, boolean isComputer) {
        this.name = name;
        this.isComputer = isComputer;
    }

    public String getName() {
        return name;
    }

    public boolean isComputer() {
        return isComputer;
    }

    public ArrayList<Card> getHand() {
        return hand;
    }

    public void addCardToHand(Card card) {
        hand.add(card);
    }

    public void addCardToScore(Card card) {
        scorePile.add(card);
    }

    public void removeCardFromHand(Card card) {
        hand.remove(card);
    }

    public int getScore() {
        return score;
    }

    public boolean playMove(List<Card> centerRow, Card selectedCard, Stack<Card> discardPile) {
        List<Card> matchingCards = new ArrayList<>();
        int selectedValue = getCardValue(selectedCard);

        for (Card card : centerRow) {
            if (getCardValue(card) == selectedValue) {
                matchingCards.add(card);
            }
        }

        if (!matchingCards.isEmpty()) {
            centerRow.removeAll(matchingCards);
            removeCardFromHand(selectedCard);
            discardPile.add(selectedCard);

            // Handle scoring or discarding logic
            if (centerRow.isEmpty()) {
                scorePile.addAll(matchingCards);
                scorePile.add(selectedCard);
                logMove("Cleared center row! Gaining points.");
            } else {
                discardPile.addAll(matchingCards);
            }

            return true;
        }

        return false;
    }

    public boolean playAutomatedMove(List<Card> centerRow, List<Card> deck, Stack<Card> discardPile, Random random) {
        for (Card card : hand) {
            if (playMove(centerRow, card, discardPile)) {
                return true;
            }
        }

        if (!deck.isEmpty()) {
            Card drawnCard = deck.remove(deck.size() - 1);
            addCardToHand(drawnCard);
            logMove("Drew " + drawnCard);

            if (playMove(centerRow, drawnCard, discardPile)) {
                return true;
            }

            hand.remove(drawnCard);
            centerRow.add(drawnCard);
            logMove("Placed " + drawnCard + " in the center row.");
        }

        return false;
    }

    public int calculateScore(boolean isFinalScorer) {

        // Add score for matched cards in the score pile
        for (Card card : scorePile) {
            score += getCardValue(card);
        }

        // Subtract score for remaining cards in hand if not the final scorer
        if (!isFinalScorer) {
            for (Card card : hand) {
                score -= getCardValue(card);
            }
        }

        return Math.max(score, 0); // Ensure score is not negative
    }

    private int getCardValue(Card card) {
        switch (card.getRank()) {
            case "Ace":
                return 1;
            case "Queen":
                return 11;
            case "King":
                return 12;
            case "Joker":
            case "Jack":
                return 0;
            default:
                try {
                    return Integer.parseInt(card.getRank());
                } catch (NumberFormatException e) {
                    return 0;
                }
        }
    }

    private void logMove(String message) {
        System.out.println(name + ": " + message);
    }
}


