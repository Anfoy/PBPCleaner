package me.antcode;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class CSVGatherer {


    public CSVGatherer() {
        String inputFolder = "C://Users//antho//IdeaProjects//PBPCleaner//NeededPBPS";
        String finalCSV = "processedCSVs";
        String matchupInput = "C://Users//antho//IdeaProjects//PBPCleaner//MATCHUPS (2).csv";
        String matchupOutput = "matchupsProcessed";
       markupCSVs(getAllCSVFiles(inputFolder), finalCSV); // Adjust the output path if needed
        convertMatchupDates(matchupInput, matchupOutput);
    }


    private List<File> getAllCSVFiles(String folderPath) {
        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
        return files != null ? Arrays.asList(files) : new ArrayList<>();
    }


    private void markupCSVs(List<File> inputCsvFiles, String outputFolderPath) {
        // Create the output folder if it doesn't exist
        File outputFolder = new File(outputFolderPath);
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }

        for (File inputFile : inputCsvFiles) {
            // Determine output file path
            File outputFile = new File(outputFolder, inputFile.getName());

            try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
                 FileWriter writer = new FileWriter(outputFile);
                 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                         .setHeader(getCombinedHeader(inputFile).toArray(new String[0]))
                         .build())) {

                // List to hold processed CSV lines
                List<String> processedLines = new ArrayList<>();
                String line;

                // Read and process each line one by one
                while ((line = reader.readLine()) != null) {
                    // Replace any occurrences of "Jones, Jr." in the raw CSV line
                    line = line.replace("Jones, Jr.", "Jones Jr.")
                            .replaceAll("Jones,\"\\s*Jr", "Jones Jr");

                    // Add the processed line to the list
                    processedLines.add(line);
                }

                // Combine the lines into a single block for parsing
                StringBuilder csvData = new StringBuilder();
                for (String processedLine : processedLines) {
                    csvData.append(processedLine).append("\n");
                }

                // Now parse the entire modified CSV data in one go
                try (Reader csvReader = new StringReader(csvData.toString())) {
                    CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build();
                    Iterable<CSVRecord> records = csvFormat.parse(csvReader);

                    // Get the index of the "date" column
                    List<String> headersList = new ArrayList<>(records.iterator().next().toMap().keySet());
                    int dateIndex = headersList.indexOf("date");

                    for (CSVRecord record : records) {
                        List<String> newRecord = new ArrayList<>(record.toList());

                        // Convert the date format and set it at the appropriate index
                        String dateValue = record.get(dateIndex);
                        newRecord.set(dateIndex, convertDateFormat(dateValue));

                        // Existing code to add new columns
                        String textColumnValue = record.get("description").toLowerCase();
                        String typeColumnValue = record.get("type").toLowerCase();
                        String eventColumnValue = record.get("event_type").toLowerCase();

                        newRecord.add(eventColumnValue.contains("rebound") ? "true" : "false");
                        newRecord.add(textColumnValue.contains("ast)") ? "true" : "false");
                        newRecord.add(eventColumnValue.contains("foul") ? "true" : "false");
                        newRecord.add(!record.get("result").isEmpty() || !record.get("shot_distance").isEmpty() || typeColumnValue.contains("offensive") ? "true" : "false");
                        newRecord.add(typeColumnValue.contains("defensive") ? "true" : "false");
                        newRecord.add(eventColumnValue.contains("turnover") ? "true" : "false");
                        newRecord.add(eventColumnValue.contains("violation") ? "true" : "false");
                        newRecord.add(textColumnValue.contains("coach") || eventColumnValue.contains("start of period") || typeColumnValue.contains("challenge") || eventColumnValue.isEmpty() || textColumnValue.isEmpty() ? "true" : "false");
                        newRecord.add(typeColumnValue.contains("team") || record.get("player").isEmpty() ? "true" : "false");
                        newRecord.add(eventColumnValue.contains("timeout") ? "true" : "false");
                        newRecord.add(textColumnValue.contains("flagrant") ? "true" : "false");
                        newRecord.add(textColumnValue.contains("technical") || typeColumnValue.contains("technical") || eventColumnValue.contains("technical") ? "true" : "false");
                        newRecord.add(typeColumnValue.contains("3pt") || textColumnValue.toUpperCase().contains(" 3PT ") ? "true" : "false");
                        newRecord.add(eventColumnValue.contains("free throw") ? "true" : "false");
                        newRecord.add(eventColumnValue.contains("end of period") ? "true" : "false");

                        csvPrinter.printRecord(newRecord);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    private String convertDateFormat(String dateValue) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("M/d/yyyy");
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date date = inputFormat.parse(dateValue);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return dateValue; // Return the original value if parsing fails
        }
    }

    // Helper method to get combined header
    private List<String> getCombinedHeader(File inputFile) {
        try (Reader reader = new FileReader(inputFile)) {
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build();
            Iterable<CSVRecord> records = csvFormat.parse(reader);
            List<String> originalHeader = new ArrayList<>(records.iterator().next().toMap().keySet());
            List<String> newHeaders = List.of("rebound", "assists", "foul", "offensive", "defensive", "turnover", "violation", "ignore", "wasTeam", "timeout", "flagrant", "technical", "3pt", "free_throw", "end_period");
            List<String> combinedHeader = new ArrayList<>(originalHeader);
            combinedHeader.addAll(newHeaders);
            return combinedHeader;
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private void handleMissingHeader(String inputCsvFile) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(inputCsvFile));
            if (!lines.isEmpty()) {
                String[] headers = lines.get(0).split(",");
                for (int i = 0; i < headers.length; i++) {
                    if (!headers[i].matches(".*[a-zA-Z0-9].*")) {
                        headers[i] = "UnNamedColumn";
                    }
                }
                lines.set(0, String.join(",", headers));
                try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(inputCsvFile))) {
                    writer.write(lines.get(0));
                    writer.newLine();
                    for (int i = 1; i < lines.size(); i++) {
                        writer.write(lines.get(i));
                        writer.newLine();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean rowHasValue(CSVRecord record, String key) {
        return record.isMapped(key) && !record.get(key).equals("NA") && !record.get(key).isEmpty();
    }

    private void convertMatchupDates(String matchupPath, String outputFilePath){
        // Define the input and output date formats
        SimpleDateFormat inputFormat = new SimpleDateFormat("MM/dd/yyyy");
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");

        try (BufferedReader br = new BufferedReader(new FileReader(matchupPath));
             BufferedWriter bw = new BufferedWriter(new FileWriter(outputFilePath))) {

            String line;
            // Read the header first
            String header = br.readLine();
            bw.write(header);  // Copy the header to the output file
            bw.newLine();

            // Read each line
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");

                // Assuming the date is in the first column (adjust index as needed)
                String dateStr = values[2];

                try {
                    // Parse the date in MM/dd/yyyy format
                    Date date = inputFormat.parse(dateStr);

                    // Format the date in yyyy-MM-dd format
                    String formattedDate = outputFormat.format(date);

                    // Replace the original date with the formatted one
                    values[2] = formattedDate;

                    // Write the modified line to the output file
                    bw.write(String.join(",", values));
                    bw.newLine();

                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    }


