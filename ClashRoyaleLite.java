import java.util.*;

abstract class Card {
    String name;
    int elixirCost;
    int hitPoints;
    int damage;
    int range;
    int speed; // 1=slow, 2=medium, 3=fast

    public Card(String name, int elixirCost, int hitPoints, int damage, int range, int speed) {
        this.name = name;
        this.elixirCost = elixirCost;
        this.hitPoints = hitPoints;
        this.damage = damage;
        this.range = range;
        this.speed = speed;
    }

    abstract void performAction(Game game, Player owner, Player opponent);

    public boolean isAlive() {
        return hitPoints > 0;
    }

    public void takeDamage(int dmg) {
        hitPoints -= dmg;
    }
}

class TroopCard extends Card {
    int position; // position on battlefield (0-9)

    public TroopCard(String name, int elixirCost, int hitPoints, int damage, int range, int speed) {
        super(name, elixirCost, hitPoints, damage, range, speed);
        this.position = -1; // not deployed yet
    }

    @Override
    void performAction(Game game, Player owner, Player opponent) {
        if (!isAlive() || position == -1) return;

        // Find closest enemy troop in range
        TroopCard target = null;
        int minDistance = Integer.MAX_VALUE;
        for (TroopCard enemy : opponent.troops) {
            if (!enemy.isAlive()) continue;
            int dist = Math.abs(enemy.position - this.position);
            if (dist <= this.range && dist < minDistance) {
                minDistance = dist;
                target = enemy;
            }
        }

        if (target != null) {
            // Attack target
            target.takeDamage(this.damage);
            if (!target.isAlive()) {
                opponent.troops.remove(target);
            }
        } else {
            // Move forward towards enemy side
            if (owner.isPlayerOne) {
                if (position < 9) position++;
            } else {
                if (position > 0) position--;
            }
        }
    }
}

class SpellCard extends Card {
    int areaDamage;

    public SpellCard(String name, int elixirCost, int areaDamage) {
        super(name, elixirCost, 0, 0, 0, 0);
        this.areaDamage = areaDamage;
    }

    @Override
    void performAction(Game game, Player owner, Player opponent) {
        // Spell effect is instant, handled on play
    }

    public void castSpell(Player opponent) {
        // Damage all enemy troops
        List<TroopCard> toRemove = new ArrayList<>();
        for (TroopCard troop : opponent.troops) {
            troop.takeDamage(areaDamage);
            if (!troop.isAlive()) toRemove.add(troop);
        }
        opponent.troops.removeAll(toRemove);
    }
}

class Player {
    String name;
    boolean isPlayerOne;
    int elixir;
    int maxElixir;
    int towerHealth;
    List<Card> deck;
    List<TroopCard> troops;
    Queue<Card> hand;
    Random random;

    public Player(String name, boolean isPlayerOne, List<Card> deck) {
        this.name = name;
        this.isPlayerOne = isPlayerOne;
        this.elixir = 5;
        this.maxElixir = 10;
        this.towerHealth = 100;
        this.deck = deck;
        this.troops = new ArrayList<>();
        this.hand = new LinkedList<>();
        this.random = new Random();
        drawInitialHand();
    }

    private void drawInitialHand() {
        Collections.shuffle(deck);
        for (int i = 0; i < 4 && i < deck.size(); i++) {
            hand.add(deck.get(i));
        }
    }

    public void regenerateElixir() {
        if (elixir < maxElixir) elixir++;
    }

    public boolean playCard(Game game, Player opponent) {
        // AI or player logic to play a card if possible
        for (Card card : hand) {
            if (card.elixirCost <= elixir) {
                elixir -= card.elixirCost;
                hand.remove(card);
                drawCard();
                if (card instanceof TroopCard) {
                    TroopCard troop = (TroopCard) card;
                    troop.position = isPlayerOne ? 0 : 9;
                    troops.add(troop);
                } else if (card instanceof SpellCard) {
                    SpellCard spell = (SpellCard) card;
                    spell.castSpell(opponent);
                }
                return true;
            }
        }
        return false;
    }

    private void drawCard() {
        if (deck.size() > 0) {
            Card nextCard = deck.remove(0);
            hand.add(nextCard);
        }
    }

    public boolean isDefeated() {
        return towerHealth <= 0;
    }
}

class Game {
    Player player1;
    Player player2;
    int turn;

    public Game(Player p1, Player p2) {
        this.player1 = p1;
        this.player2 = p2;
        this.turn = 0;
    }

    public void start() {
        System.out.println("Game started between " + player1.name + " and " + player2.name);
        while (!player1.isDefeated() && !player2.isDefeated() && turn < 100) {
            turn++;
            System.out.println("Turn " + turn);
            player1.regenerateElixir();
            player2.regenerateElixir();

            player1.playCard(this, player2);
            player2.playCard(this, player1);

            updateTroops(player1, player2);
            updateTroops(player2, player1);

            attackTowers(player1, player2);
            attackTowers(player2, player1);

            printStatus();
        }
        if (player1.isDefeated()) {
            System.out.println(player2.name + " wins!");
        } else if (player2.isDefeated()) {
            System.out.println(player1.name + " wins!");
        } else {
            System.out.println("Game ended in a draw.");
        }
    }

    private void updateTroops(Player owner, Player opponent) {
        List<TroopCard> toRemove = new ArrayList<>();
        for (TroopCard troop : owner.troops) {
            troop.performAction(this, owner, opponent);
            if (!troop.isAlive()) toRemove.add(troop);
        }
        owner.troops.removeAll(toRemove);
    }

    private void attackTowers(Player owner, Player opponent) {
        for (TroopCard troop : owner.troops) {
            if (owner.isPlayerOne && troop.position == 9) {
                opponent.towerHealth -= troop.damage;
            } else if (!owner.isPlayerOne && troop.position == 0) {
                opponent.towerHealth -= troop.damage;
            }
        }
    }

    private void printStatus() {
        System.out.println(player1.name + " Tower HP: " + player1.towerHealth + " Elixir: " + player1.elixir + " Troops: " + player1.troops.size());
        System.out.println(player2.name + " Tower HP: " + player2.towerHealth + " Elixir: " + player2.elixir + " Troops: " + player2.troops.size());
        System.out.println();
    }
}

public class ClashRoyaleLite {
    public static void main(String[] args) {
        List<Card> deck1 = new ArrayList<>();
        deck1.add(new TroopCard("Knight", 3, 30, 5, 1, 2));
        deck1.add(new TroopCard("Archer", 2, 15, 3, 3, 3));
        deck1.add(new TroopCard("Giant", 5, 50, 8, 1, 1));
        deck1.add(new SpellCard("Fireball", 4, 10));

        List<Card> deck2 = new ArrayList<>();
        deck2.add(new TroopCard("Knight", 3, 30, 5, 1, 2));
        deck2.add(new TroopCard("Archer", 2, 15, 3, 3, 3));
        deck2.add(new TroopCard("Giant", 5, 50, 8, 1, 1));
        deck2.add(new SpellCard("Fireball", 4, 10));

        Player player1 = new Player("Player1", true, deck1);
        Player player2 = new Player("AI", false, deck2);

        Game game = new Game(player1, player2);
        game.start();
    }
}
