package org.example;

import com.google.common.io.Resources;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Utils {


    static void calculateData(List<BigDecimal> last100, BigDecimal cashOutLvL) {
        if (last100.isEmpty()) {
            return;
        }

        List<BigDecimal> last30 = last100.subList(0, 29);
        Messages last100Messages = resultMessages(last100, cashOutLvL);
        Messages last30Messages = resultMessages(last30, cashOutLvL);

        // Format to exactly 80 characters, left-aligned with trailing spaces, truncated if > 80
        String printable100 = String.format("%-50s", last100Messages.output + last100Messages.winRatio);
        String printable30 = last30Messages.output + " " + last30Messages.winRatio;

        System.out.print("===Time: " + getTime() + " ===");
        System.out.println(printable100 + "   " + printable30);


        storeData(cashOutLvL, "===Time: " + getTime() + " ===");
        storeData(cashOutLvL, printable100 + "   " + printable30 + System.lineSeparator());
    }

    private static Messages resultMessages(List<BigDecimal> lastXrounds, BigDecimal cashOutLvL) {
        long losses = lastXrounds.stream().filter(e -> e.compareTo(cashOutLvL) < 0).count();
        long wins = lastXrounds.stream().filter(e -> e.compareTo(cashOutLvL) >= 0).count();


        double winRatio = (double) wins / losses;
        double output = wins * ((cashOutLvL.intValue() - 100) / 100.0) - losses;

        DecimalFormat df = new DecimalFormat("#.##");
        Messages messages = new Messages(
                "G" + lastXrounds.size() + "output: " + df.format(output) + "x",
                " (W ratio " + df.format(winRatio) + "  W: " + wins + "/L:" + losses + ")");
        return messages;
    }

    private record Messages(String output, String winRatio) {
    }

    static void storeData(BigDecimal cashOutLvL, String textToAppend) {
        Path filePath = Paths.get("outputs", cashOutLvL.intValue() + "historicalCrashes"); // Create a Path object for the file
        try {
            // Create or confirm the output parent directory exists
            Files.createDirectories(filePath.getParent());
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
                System.out.println("File created: " + filePath.toAbsolutePath());
            }
            // Append text to the file
            Files.writeString(filePath, textToAppend, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getTime() {
        LocalDateTime now = LocalDateTime.now();  // Gets the local time from the system clock
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        return now.format(formatter);
    }

    static String getFeedOld() throws IOException {
        String content = Resources.toString(Resources.getResource("crashfeed44"), StandardCharsets.UTF_8);
        List<String> res = new ArrayList();
        StringBuilder sb = null;
        boolean building = false;

        for (char c : content.toCharArray()) {
            if (c == '>') {
                building = true;
                sb = new StringBuilder(); //reset
            } else if (building) {
                if (c == '%') {
                    if (!sb.toString().equals("0")) {
                        res.add(sb.toString());  //close it
                    }
                    building = false;
                } else {
                    sb.append(c);
                }
            }
        }
        BigDecimal cashOutLvL = new BigDecimal("160");

        long losses = res.stream().filter(e -> new BigDecimal(e).compareTo(cashOutLvL) < 0).count();
        long wins = res.stream().filter(e -> new BigDecimal(e).compareTo(cashOutLvL) >= 0).count();

        System.out.println("Win ratio " + (double) wins / losses + "      W:" + wins + " / L:" + losses);
        System.out.println("Our output is " + (wins * ((cashOutLvL.intValue() - 100) / 100.0) - losses));

        return null;
    }


    static String getFeed1() throws IOException {
        String content = Resources.toString(Resources.getResource("crashfeed"), StandardCharsets.UTF_8);

        Document doc = Jsoup.parse(content);
        Elements rows = doc.select("tr.r_item");

        List<Double> percentages = new ArrayList<>();

        for (Element row : rows) {
            Elements cells = row.select("div.td");
            for (Element cell : cells) {
                String text = cell.text().trim();
                if (text.endsWith("%")) {
                    // Remove the trailing '%' and parse the double
                    String numberPart = text.substring(0, text.length() - 1);
                    try {
                        double value = Double.parseDouble(numberPart);
                        percentages.add(value);
                    } catch (NumberFormatException e) {
                        // Not a valid number, ignore or handle as needed
                    }
                }
            }
        }

        // Print the extracted percentages
        for (double pct : percentages) {
            System.out.println(pct);
        }


        return null;
    }


}