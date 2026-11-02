import java.util.Scanner;

class InvalidHourException extends Exception {
    public InvalidHourException(String msg) { super(msg); }
}

class InvalidMinuteException extends Exception {
    public InvalidMinuteException(String msg) { super(msg); }
}

class InvalidSecondException extends Exception {
    public InvalidSecondException(String msg) { super(msg); }
}

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        int hours = sc.nextInt();
        int minutes = sc.nextInt();
        int seconds = sc.nextInt();

        try {
            if (hours < 0 || hours > 23) {
                throw new InvalidHourException("InvalidHourException: Invalid Hour");
            }
            if (minutes < 0 || minutes > 59) {
                throw new InvalidMinuteException("InvalidMinuteException: Invalid Minute");
            }
            if (seconds < 0 || seconds > 59) {
                throw new InvalidSecondException("InvalidSecondException: Invalid Second");
            }
            System.out.println("Correct Time - " + hours + ":" + minutes + ":" + seconds);
        } catch (InvalidHourException | InvalidMinuteException | InvalidSecondException e) {
            System.out.println(e.getMessage());
        }

        sc.close();
    }
}
