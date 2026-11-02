import java.util.Scanner;

// Abstract Parent Class
abstract class GameCharacter {
    abstract void attack();
    abstract void defend();
}

// Subclass 1: Warrior triples strength for attack
class Warrior extends GameCharacter {
    int strength;

    Warrior(int strength) {
        this.strength = strength;
    }

    @Override
    void attack() {
        System.out.println("Warrior Attack: " + (strength * 3));
    }

    @Override
    void defend() {
        System.out.println("Warrior defends with shield!");
    }
}

// Subclass 2: Wizard doubles magic power for attack
class Wizard extends GameCharacter {
    int power;

    Wizard(int power) {
        this.power = power;
    }

    @Override
    void attack() {
        System.out.println("Wizard Attack: " + (power * 2));
    }

    @Override
    void defend() {
        System.out.println("Wizard defends with magic barrier!");
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        int choice = sc.nextInt(); // 1 = Warrior, 2 = Wizard
        int value = sc.nextInt();  // Strength or Magic Power

        GameCharacter player;
        if (choice == 1) {
            player = new Warrior(value);
        } else {
            player = new Wizard(value);
        }

        player.attack();
        player.defend();

        sc.close();
    }
}
