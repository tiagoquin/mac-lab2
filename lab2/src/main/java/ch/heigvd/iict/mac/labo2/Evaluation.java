package ch.heigvd.iict.mac.labo2;

import com.sun.xml.internal.bind.v2.runtime.output.StAXExStreamWriterOutput;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

public class Evaluation {

    private static Analyzer analyzer = null;

    private static void readFile(String filename, Function<String, Void> parseLine)
            throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(filename),
                        StandardCharsets.UTF_8)
        )) {
            String line = br.readLine();
            while (line != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    parseLine.apply(line);
                }
                line = br.readLine();
            }
        }
    }

    /*
     * Reading CACM queries and creating a list of queries.
     */
    private static List<String> readingQueries() throws IOException {
        final String QUERY_SEPARATOR = "\t";

        List<String> queries = new ArrayList<>();

        readFile("evaluation/query.txt", line -> {
            String[] query = line.split(QUERY_SEPARATOR);
            queries.add(query[1]);
            return null;
        });
        return queries;
    }

    /*
     * Reading stopwords
     */
    private static List<String> readingCommonWords() throws IOException {
        List<String> commonWords = new ArrayList<>();

        readFile("common_words.txt", line -> {
            commonWords.add(line);
            return null;
        });
        return commonWords;
    }


    /*
     * Reading CACM qrels and creating a map that contains list of relevant
     * documents per query.
     */
    private static Map<Integer, List<Integer>> readingQrels() throws IOException {
        final String QREL_SEPARATOR = ";";
        final String DOC_SEPARATOR = ",";

        Map<Integer, List<Integer>> qrels = new HashMap<>();

        readFile("evaluation/qrels.txt", line -> {
            String[] qrel = line.split(QREL_SEPARATOR);
            int query = Integer.parseInt(qrel[0]);

            List<Integer> docs = qrels.get(query);
            if (docs == null) {
                docs = new ArrayList<>();
            }

            String[] docsArray = qrel[1].split(DOC_SEPARATOR);
            for (String doc : docsArray) {
                docs.add(Integer.parseInt(doc));
            }

            qrels.put(query, docs);
            return null;
        });
        return qrels;
    }

    public static void main(String[] args) throws IOException {
        ///
        /// Reading queries and queries relations files
        ///
        List<String> queries = readingQueries();
        System.out.println("Number of queries: " + queries.size());

        Map<Integer, List<Integer>> qrels = readingQrels();
        System.out.println("Number of qrels: " + qrels.size());

        double avgQrels = 0.0;
        for (int q : qrels.keySet()) {
            avgQrels += qrels.get(q).size();
        }
        avgQrels /= qrels.size();
        System.out.println("Average number of relevant docs per query: " + avgQrels);

        //use this when doing the english analyzer + common words
        List<String> commonWords = readingCommonWords();

        ///
        ///  Part I - Select an analyzer
        ///
        // the asked analyzers once the metrics have been implemented

        int choice = 4;
        switch (choice){
            case 1:
                analyzer = new WhitespaceAnalyzer();
                break;
            case 2:
                analyzer = new StandardAnalyzer();
                break;
            case 3:
                analyzer = new EnglishAnalyzer();
                break;
            case 4:
                analyzer = new EnglishAnalyzer(new CharArraySet(commonWords,true));
                break;
        }


        ///
        ///  Part I - Create the index
        ///
        Lab2Index lab2Index = new Lab2Index(analyzer);
        lab2Index.index("documents/cacm.txt");

        ///
        ///  Part II and III:
        ///  Execute the queries and assess the performance of the
        ///  selected analyzer using performance metrics like F-measure,
        ///  precision, recall,...
        ///

        // compute the metrics asked in the instructions
        // you may want to call these methods to get:
        // -  The query results returned by Lucene i.e. computed/empirical
        //    documents retrieved
        //        List<Integer> queryResults = lab2Index.search(query);
        //
        // - The true query results from qrels file i.e. genuine documents
        //   returned matching a query
        //        List<Integer> qrelResults = qrels.get(queryNumber);

        int queryNumber = 0;
        int totalRelevantDocs = 0; //c
        int totalRetrievedDocs = 0; //b
        int totalRetrievedRelevantDocs = 0; //d
        double avgPrecision = 0.0; // e
        double avgRPrecision = 0.0;
        double avgRecall = 0.0;
        double meanAveragePrecision = 0.0;
        double fMeasure = 0.0;

        double avgAP = 0.0;
        int beta = 1;

        // average precision at the 11 recall levels (0,0.1,0.2,...,1) over all queries
        double[] avgPrecisionAtRecallLevels = createZeroedRecalls();

        for(String query : queries){
            //Array for recall level precision for each query
            double[] avgPrecisionAtRecallLevelsByQuery = createZeroedRecalls();

            //Get the retrieval results for this query
            List<Integer> queryResults = lab2Index.search(query);

            //total retrieved document for this query
            int totalRetrievedDocsByQuery = queryResults.size();

            queryNumber += 1;

            //Get the relevent result for this query
            List<Integer> qrelResults = qrels.get(queryNumber);

            // Partie 3.3.1.b - Total number retrieved document for all queries
            totalRetrievedDocs += totalRetrievedDocsByQuery;


            //If we don't find the key
            if(qrelResults != null) {

                //Total of relevant document for this query
                int totalRelevantDocsByQuery = qrelResults.size();

                //Counter of retrievel document this query
                int counterRetrieval = 0;

                //Counter of relevant document for this query
                int counterRelevant = 0;

                //Number of relevant document of the first x (x = totalRelevantDocsByQuery)
                int numberRP = 0;

                //AveragePrecision for this query
                double averagePrecisionByQuery = 0.0;

                //Recall by document
                double recall = 0.0;

                //Precision by document
                double precision = 0.0;

                //For each retrieval document of this query
                for(int retrievial : queryResults){
                    counterRetrieval++;

                    //Check if the document is relevant
                    if(qrelResults.contains(retrievial)){
                        //Increment the counter of relevant
                        counterRelevant++;

                        //Calcul the averagePrecision for this query
                        averagePrecisionByQuery += counterRelevant/(double)counterRetrieval;

                        //Calcul the recall of this documents
                        recall = counterRelevant / (double)totalRelevantDocsByQuery;

                        //Calcule the precision of this document
                        precision = counterRelevant / (double)counterRetrieval;

                        if(counterRetrieval <= totalRelevantDocsByQuery){
                            numberRP++;
                        }

                        int counterPrecision = (int)Math.floor(recall*10);
                        for(int i = counterPrecision; i >= 0; --i){
                            if(avgPrecisionAtRecallLevelsByQuery[i] < precision){
                                avgPrecisionAtRecallLevelsByQuery[i] = precision;
                            }
                        }
                    }
                }

                // Partie 3.3.1.c - Total number relevant document for all queries
                totalRelevantDocs += totalRelevantDocsByQuery;

                // Partie 3.3.1.d - Total number of relevant documents retrieved for all queries
                totalRetrievedRelevantDocs += counterRelevant;

                // Partie 3.3.1.e - Average precision for all queries
                avgPrecision += counterRelevant /(double) totalRetrievedDocsByQuery;

                // Partie 3.3.1.f - Average recall for all queries
                avgRecall +=  counterRelevant / (double)totalRelevantDocsByQuery;;


                // Partie 3.3.3 - Mean average pecision
                meanAveragePrecision += averagePrecisionByQuery/(double)totalRelevantDocsByQuery;

                // Partie 3.3.4 - R-Precision
                avgRPrecision += numberRP /(double)totalRelevantDocsByQuery;

                // Partie 3.3.5 - Average precision at standart recall levels
                for(int i = 0; i < avgPrecisionAtRecallLevels.length; ++i){
                    avgPrecisionAtRecallLevels[i] += avgPrecisionAtRecallLevelsByQuery[i];
                }
            }
        }

        // Partie 3.3.1.e - Average precision for all queries
        avgPrecision /= queries.size();

        // Partie 3.3.1.f - Average recall for all queries
        avgRecall /= queries.size();

        // Partie 3.3.1.g F-measure using average precision and average recall
        fMeasure += ((Math.pow(beta,2) + 1)*avgPrecision*avgRecall)/(avgRecall + avgPrecision*(Math.pow(beta,2)));

        // Partie 3.3.3 - Mean average pecision
        meanAveragePrecision /= queries.size();

        // Partie 3.3.4 - R-Precision
        avgRPrecision /= queries.size();

        // Partie 3.3.5 - Average precision at standart recall levels
        avgPrecisionAtRecallLevels = Arrays.stream(avgPrecisionAtRecallLevels).map(x -> x/queries.size()).toArray();



        ///
        ///  Part IV - Display the metrics
        ///
        displayMetrics(totalRetrievedDocs, totalRelevantDocs,
                totalRetrievedRelevantDocs, avgPrecision, avgRecall, fMeasure,
                meanAveragePrecision, avgRPrecision,
                avgPrecisionAtRecallLevels);
    }

    private static void displayMetrics(
            int totalRetrievedDocs,
            int totalRelevantDocs,
            int totalRetrievedRelevantDocs,
            double avgPrecision,
            double avgRecall,
            double fMeasure,
            double meanAveragePrecision,
            double avgRPrecision,
            double[] avgPrecisionAtRecallLevels
    ) {
        String analyzerName = analyzer.getClass().getSimpleName();
        if (analyzer instanceof StopwordAnalyzerBase) {
            analyzerName += " with set size " + ((StopwordAnalyzerBase) analyzer).getStopwordSet().size();
        }
        System.out.println(analyzerName);

        System.out.println("Number of retrieved documents: " + totalRetrievedDocs);
        System.out.println("Number of relevant documents: " + totalRelevantDocs);
        System.out.println("Number of relevant documents retrieved: " + totalRetrievedRelevantDocs);

        System.out.println("Average precision: " + avgPrecision);
        System.out.println("Average recall: " + avgRecall);

        System.out.println("F-measure: " + fMeasure);

        System.out.println("MAP: " + meanAveragePrecision);

        System.out.println("Average R-Precision: " + avgRPrecision);

        System.out.println("Average precision at recall levels: ");
        for (int i = 0; i < avgPrecisionAtRecallLevels.length; i++) {
            System.out.println(String.format("\t%s: %s", i, avgPrecisionAtRecallLevels[i]));
        }
    }

    private static double[] createZeroedRecalls() {
        double[] recalls = new double[11];
        Arrays.fill(recalls, 0.0);
        return recalls;
    }
}