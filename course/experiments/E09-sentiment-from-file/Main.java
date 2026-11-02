import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String inputSentence = sc.nextLine();

        String[] posKeywords = {"happy", "good", "excellent", "positive"};
        String[] negKeywords = {"sad", "bad", "terrible", "negative"};

        try {
            // Step 1: Write input sentence to input.txt
            FileWriter inWriter = new FileWriter("input.txt");
            inWriter.write(inputSentence);
            inWriter.close();

            // Step 2: Read sentence from input.txt and convert to lowercase
            Scanner fileReader = new Scanner(new File("input.txt"));
            String sentence = fileReader.hasNextLine()
                    ? fileReader.nextLine().toLowerCase() : "";
            fileReader.close();

            // Step 3: Check for positive or negative keywords
            String sentiment = "Neutral";
            for (String word : posKeywords) {
                if (sentence.contains(word)) {
                    sentiment = "Positive";
                    break;
                }
            }
            if (sentiment.equals("Neutral")) {
                for (String word : negKeywords) {
                    if (sentence.contains(word)) {
                        sentiment = "Negative";
                        break;
                    }
                }
            }

            // Step 4: Write classified sentiment to output.txt and display
            FileWriter outWriter = new FileWriter("output.txt");
            outWriter.write(sentiment);
            outWriter.close();

            System.out.println(sentiment);
        } catch (IOException e) {
            System.out.println("File Error: " + e.getMessage());
        }

        sc.close();
    }
}
