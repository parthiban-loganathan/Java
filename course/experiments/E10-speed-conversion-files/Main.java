import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        double speedKmh = sc.nextDouble();

        try {
            // Step 1: Write speed in km/h to data.txt
            FileWriter dataWriter = new FileWriter("data.txt");
            dataWriter.write(String.valueOf(speedKmh));
            dataWriter.close();

            // Step 2: Read speed from data.txt
            Scanner fileReader = new Scanner(new File("data.txt"));
            double readSpeed = fileReader.nextDouble();
            fileReader.close();

            // Step 3: Convert km/h to m/s (multiply by 5.0 / 18.0)
            double speedMs = readSpeed * (5.0 / 18.0);
            String formatted = String.format("%.2f", speedMs);

            // Step 4: Save to converted.txt and display
            FileWriter convWriter = new FileWriter("converted.txt");
            convWriter.write(formatted);
            convWriter.close();

            System.out.println(formatted);
        } catch (IOException e) {
            System.out.println("File Error: " + e.getMessage());
        }

        sc.close();
    }
}
