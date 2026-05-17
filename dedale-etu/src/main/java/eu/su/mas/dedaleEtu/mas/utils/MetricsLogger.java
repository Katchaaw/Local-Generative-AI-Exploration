package eu.su.mas.dedaleEtu.mas.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Fichier utilitaire de journalisation des performances des agents.
 * Permet de consigner les métriques des SLM au format CSV
 * pour des analyses statistiques ultérieures.
 */
public class MetricsLogger {
    /** Chemin d'accès vers le fichier de sortie CSV. */
    private static final String CSV_FILE = "csv/llm_benchmark_results.csv";

    private static boolean isHeaderWritten = false;

    /**
     * Consigne les métriques d'exécution d'un tour d'un agent dans le fichier CSV global.
     * Calcule automatiquement le statut de démarrage à froid (Cold Start) au premier tour.
     *
     * @param agentName      Le nom unique de l'agent JADE.
     * @param turnNumber     Le numéro du tour actuel dans la simulation Dédale.
     * @param modelName      Le nom complet du modèle local utilisé.
     * @param knownMapSize   Le nombre d'arêtes ou de nœuds actuellement découverts et mémorisés.
     * @param responseTimeMs Le temps de traitement actif requis pour l'inférence LLM en millisecondes.
     * @param actionType     Le type d'action sémantique sélectionné par le modèle (MOVE, CHAT, etc.).
     * @param isHunting      Indique si l'agent est actuellement en phase de traque active du Golem.
     */
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