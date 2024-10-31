package util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private PrintStream output;
    private static final String DEFAULT_LOG_PATH = "../Database/Logs";

    private static final DateTimeFormatter TIME_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public Logger() {
        this(DEFAULT_LOG_PATH);
    }

    public Logger(String logFilePath) {
        try {
            FileOutputStream fileOutput = new FileOutputStream(logFilePath, true);
            output = new PrintStream(fileOutput);
        } catch (FileNotFoundException e) {
            System.err.println("Log file not found: " + logFilePath);
            e.printStackTrace();
        }
    }

    public static String getTimeDate() {
        return LocalDateTime.now().format(TIME_DATE_FORMATTER);
    }

    public static String getDate() {
        return LocalDateTime.now().format(DATE_FORMATTER);
    }

    // logs entry to the file
    public void log(String editor, String patient, String action) {
        if (output != null) {
            String logEntry = String.format("%s: %s %s for %s", getTimeDate(), editor, action, patient);
            output.println(logEntry);
            output.flush();
        } else {
            System.err.println("Logger output stream is not initialized.");
        }
    }

    /**
     * Closes the Logger's output stream.
     */
    public void close() {
        if (output != null) {
            output.close();
        }
    }
}
