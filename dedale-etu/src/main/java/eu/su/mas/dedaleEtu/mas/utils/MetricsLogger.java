package eu.su.mas.dedaleEtu.mas.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class MetricsLogger {
    private static final String CSV_FILE = "llm_benchmark_results.csv";
    private static boolean isHeaderWritten = false;

    public static synchronized void logTurn(String agentName, int turnNumber, String modelName,
                                            int knownMapSize, long responseTimeMs, String actionType, boolean isHunting) {
        try (FileWriter fw = new FileWriter(CSV_FILE, true);
             PrintWriter bw = new PrintWriter(fw)) {

            // Écriture de l'en-tête si c'est la première ligne
            if (!isHeaderWritten) {
                bw.println("AgentName,Turn,Model,KnownMapSize,ResponseTimeMs,ActionType,IsColdStart, IsHunting");
                isHeaderWritten = true;
            }

            boolean isColdStart = (turnNumber == 1);
            bw.printf("%s,%d,%s,%d,%d,%s,%b, %b\n",
                    agentName, turnNumber, modelName, knownMapSize, responseTimeMs, actionType, isColdStart, isHunting);

        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture des métriques : " + e.getMessage());
        }
    }
}