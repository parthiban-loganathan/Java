import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();

        try {
            // Step 1: Create prices.txt and write the N item prices
            FileWriter priceWriter = new FileWriter("prices.txt");
            for (int i = 0; i < n; i++) {
                double price = sc.nextDouble();
                priceWriter.write(price + " ");
            }
            priceWriter.close();

            // Step 2: Read prices from prices.txt and apply 10% tax
            Scanner fileReader = new Scanner(new File("prices.txt"));
            FileWriter taxWriter = new FileWriter("tax.txt");
            StringBuilder output = new StringBuilder();

            while (fileReader.hasNextDouble()) {
                double rawPrice = fileReader.nextDouble();
                double taxedPrice = rawPrice * 1.10;
                output.append(String.format("%.2f ", taxedPrice));
            }
            fileReader.close();

            // Step 3: Write taxed amounts to tax.txt and print to screen
            String finalOutput = output.toString().trim();
            taxWriter.write(finalOutput);
            taxWriter.close();

            System.out.println(finalOutput);
        } catch (IOException e) {
            System.out.println("File Error: " + e.getMessage());
        }

        sc.close();
    }
}
