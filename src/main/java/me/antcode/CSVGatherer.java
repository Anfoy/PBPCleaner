package me.antcode;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class CSVGatherer {


    private final List<Player> totalPlayers;

    public CSVGatherer(){
        totalPlayers = extractAllPlayers();
        String inputCsvFile = "C:/Users/antho/IdeaProjects/PBPCleaner/PlayByPlay1Month.csv";
        String halfProcessedCsv = "halfProcessed.csv";
        String finalCsvFile = "normalizedPBP.csv";
        markupCSV(inputCsvFile, halfProcessedCsv);// Adjust the output path if needed
        establishPossession(halfProcessedCsv, finalCsvFile);
    }


    private List<Player> extractAllPlayers() {
        List<Player> players = new ArrayList<>();
        try (Reader reader = new FileReader("C:/Users/antho/IdeaProjects/PBPCleaner/Matchup1Month.csv");
             //Opens the parser and starts at second row since first row is the headers/column names
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {

            //Creates all the matchups
            for (CSVRecord csvRecord : csvParser) {
                List<Player> homeStarters = extractPlayersFromMatchupData(csvRecord, "home_starter_", 5);
                List<Player> awayStarters = extractPlayersFromMatchupData(csvRecord, "away_starter_", 5);
                List<Player> homeBench = extractPlayersFromMatchupData(csvRecord, "home_bench_", 10);
                List<Player> awayBench = extractPlayersFromMatchupData(csvRecord, "away_bench_", 10);
                players.addAll(homeStarters);
                players.addAll(awayStarters);
                players.addAll(homeBench);
                players.addAll(awayBench);
            }
        } catch (IOException e) {
            System.out.println("failed to read file.");
            e.printStackTrace();
        }
        return players;
    }

    /**
     * Attempts to find player object based on ID parameter
     * @param ID Player ID to look for
     * @return Player object with matching id; otherwise null
     */
    private Player findPlayer(int ID){
        for (Player player : totalPlayers){
            if (player.getPlayerID() == ID){
                return player;
            }
        }
        return null;
    }

    /**
     * Gets all the player's ids under a specific prefix.
     * @param record row to look at
     * @param prefix prefix for looking at column
     * @param count how many duplicate columns are there? ex: athlete_id_1, athlete_id_2
     * @return List of all player objects created from searching the csv.
     */
    private  List<Player> extractPlayersFromMatchupData(CSVRecord record, String prefix, int count) {
        List<Player> players = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            String idKey = prefix + i + "_id";
            String nameKey = prefix.substring(0, 5) + "display_name";
            if (rowHasValue(record, idKey) && rowHasValue(record, nameKey)) {
                int playerId = parseInt(record.get(idKey));
                String playerTeam = record.get(nameKey);
                players.add(new Player(playerId, playerTeam));
            }
        }
        return players;
    }

    /**
     * Changes a String value to an integer value.
     * @param value String to get integer from.
     * @return value of String as integer; Otherwise returns 0;
     */
    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0; // or some default value or throw an exception
        }
    }

    private boolean rowHasValue(CSVRecord record, String key) {
        return record.isMapped(key) && !record.get(key).equals("NA") && !record.get(key).isEmpty();
    }

    /**
     * Reads the data from the inputCsvFile, gets the original headers, appends the new headers, and then creates a new csv with these values added:
     * true/false: rebound, assist, foul, offensive, defensive, turnover, violation, ignore, wasTeam, timeout, flagrant.
     * plays ignored are: Challenges, End Game, End Period, Not Available, Ref reviews
     * plays also considered turnovers: traveling
     * @param inputCsvFile File to read
     * @param outputCsvFile file to write too.
     */
    private void markupCSV(String inputCsvFile, String outputCsvFile){
        try (
                Reader reader = new FileReader(inputCsvFile);
                FileWriter writer = new FileWriter(outputCsvFile)
        ) {
            // Read the input CSV file
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build();
            Iterable<CSVRecord> records = csvFormat.parse(reader);

            // Get the original header
            List<String> originalHeader = new ArrayList<>(records.iterator().next().toMap().keySet());

            // Define the new headers to be added
            List<String> newHeaders = List.of("rebound", "assist", "foul", "offensive", "defensive", "turnover", "violation", "ignore", "wasTeam", "timeout", "flagrant");

            // Combine the original header with the new headers
            List<String> combinedHeader = new ArrayList<>(originalHeader);
            combinedHeader.addAll(newHeaders);

            // Write the header to the output CSV file
            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(combinedHeader.toArray(new String[0])).build());

            // Reset the reader to re-read the file including the first data row
            reader.close();
            try (Reader reader2 = new FileReader(inputCsvFile)) {
                records = csvFormat.parse(reader2);

                // Iterate through records and write each record with the new columns
                for (CSVRecord record : records) {
                    List<String> newRecord = new ArrayList<>();
                    record.forEach(newRecord::add);

                    // Extract the relevant data from the 'text' column (assuming it is named "text")
                    String textColumnValue = record.get("text").toLowerCase();
                    String typeColumnValue = record.get("type_text").toLowerCase(
                    );

                    // Add new columns with data based on the content of the 'text' column
                    newRecord.add(textColumnValue.contains("rebound") ? "true" : "false");
                    newRecord.add(textColumnValue.contains("assist") ? "true" : "false");
                    newRecord.add(textColumnValue.contains("foul") ? "true" : "false");
                    newRecord.add(record.get("shooting_play").equals("TRUE") || textColumnValue.contains("offensive") ? "true" : "false");
                    newRecord.add(textColumnValue.contains("defensive") ? "true" : "false");
                    newRecord.add(typeColumnValue.contains("turnover")
                            || typeColumnValue.contains("traveling")? "true" : "false");
                    newRecord.add(textColumnValue.contains("violation") ? "true" : "false");
                    if(textColumnValue.contains("coach's challenge") || typeColumnValue.contains("end game") || typeColumnValue.contains("end period")
                            || textColumnValue.contains("ref") || typeColumnValue.contains("not available") || typeColumnValue.contains("challenge")) {
                        newRecord.add("true");
                    }else{
                        newRecord.add("false");
                    }
                    newRecord.add(textColumnValue.contains("team") ? "true" : "false");
                    newRecord.add(typeColumnValue.contains("timeout") ? "true" : "false");
                    newRecord.add(textColumnValue.contains("flagrant") ? "true" : "false");
                    csvPrinter.printRecord(newRecord);
                }

            }

            csvPrinter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads the data from the inputCsvFile, gets the original headers, appends the new headers, and then creates a new csv with the possession column appended.
     * Possession is considered none if play is: ignored, foul, violation, Timeout, Jumpball, Free Throw, Substitution.
     * Possession goes to athlete one if play is: team play, offensive or defensive.
     * @param inputCsvFile File to read.
     * @param outputCsvFile File to write too.
     */
    private void establishPossession(String inputCsvFile, String outputCsvFile){
        try (
                Reader reader = new FileReader(inputCsvFile);
                FileWriter writer = new FileWriter(outputCsvFile)
        ) {
            // Read the input CSV file
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build();
            Iterable<CSVRecord> records = csvFormat.parse(reader);

            // Get the original header
            List<String> originalHeader = new ArrayList<>(records.iterator().next().toMap().keySet());

            // Define the new headers to be added
            List<String> newHeaders = List.of( "possession");

            // Combine the original header with the new headers
            List<String> combinedHeader = new ArrayList<>(originalHeader);
            combinedHeader.addAll(newHeaders);

            // Write the header to the output CSV file
            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(combinedHeader.toArray(new String[0])).build());

            // Reset the reader to re-read the file including the first data row
            reader.close();
            try (Reader reader2 = new FileReader(inputCsvFile)) {
                records = csvFormat.parse(reader2);

                // Iterate through records and write each record with the new columns
                for (CSVRecord record : records) {
                    List<String> newRecord = new ArrayList<>();
                    record.forEach(newRecord::add);

                    // Extract the relevant data from the 'text' column (assuming it is named "text")
                    String textColumnValue = record.get("text").toLowerCase();
                    String typeColumnValue = record.get("type_text").toLowerCase();

                    // Add new columns with data based on the content of the 'text' column
                    if (record.get("ignore").equals("true") || record.get("foul").equals("true") ||
                            record.get("turnover").equals("true") || typeColumnValue.contains("substitution") ||
                            typeColumnValue.contains("timeout") || typeColumnValue.contains("jumpball") || record.get("violation").equals("true") || parseInt(record.get("free_throw")) > 0){
                        newRecord.add("none");
                    }else if (record.get("wasTeam").equals("true")){
                        for (String val : textColumnValue.split(" ")){
                            if (record.get("home_display_name").contains(val)){
                                newRecord.add(record.get("home_display_name"));
                            }else{
                                newRecord.add(record.get("away_display_name"));
                            }
                            break;
                        }
                    }else if (record.get("offensive").equals("true") || record.get("defensive").equals("true")) {
                        Player player = findPlayer(parseInt(record.get("athlete_id_1")));
                        if (player == null) {
                            newRecord.add("none");
                        } else {
                            String team = player.getTeam();
                            newRecord.add(team);
                        }
                    }
                    csvPrinter.printRecord(newRecord);
                }

            }

            csvPrinter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

