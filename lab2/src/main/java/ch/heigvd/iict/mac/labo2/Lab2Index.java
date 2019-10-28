package ch.heigvd.iict.mac.labo2;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Lab2Index {
    private static final String AUTHOR_SEPARATOR = ";";
    private static final String DOC_FIELD_SEPARATOR = "\t";
    private static final String FIELD_ID = "id";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_SUMMARY = "summary";
    private static final String FIELD_CONTENT = "content";
    private final Analyzer analyzer;
    private final Similarity similarity;
    private static final FieldType TYPE_STORED = new FieldType();

    static {
        TYPE_STORED.setIndexOptions(
                IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS
        );
        TYPE_STORED.setTokenized(true);
        TYPE_STORED.setStored(true);
        TYPE_STORED.setStoreTermVectors(true);
        TYPE_STORED.setStoreTermVectorPositions(true);
        TYPE_STORED.setStoreTermVectorOffsets(true);
        TYPE_STORED.freeze();
    }

    public Lab2Index(Analyzer analyzer) {
        this.analyzer = analyzer;
        this.similarity = new ClassicSimilarity();
    }

    public void index(String filename) {
        IndexWriter indexWriter;
        try {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(filename),
                            StandardCharsets.UTF_8
                    )
            );

            Path path = FileSystems.getDefault().getPath("index");
            Directory dir = FSDirectory.open(path);
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(OpenMode.CREATE);
            iwc.setUseCompoundFile(false);
            iwc.setSimilarity(similarity);

            indexWriter = new IndexWriter(dir, iwc);
            String line = br.readLine();
            int docCounter = 0;
            while (line != null) {
                String[] items = line.split(DOC_FIELD_SEPARATOR);
                Document doc = new Document();

                doc.add(new StringField(FIELD_ID, items[0], Field.Store.YES));

                String[] authors = items[1].split(AUTHOR_SEPARATOR);
                for (String author : authors) {
                    if (!author.isEmpty()) {
                        doc.add(new StringField("author", author, Field.Store.YES));
                    }
                }

                doc.add(new Field(FIELD_TITLE, items[2], TYPE_STORED));

                if (items.length == 4) {
                    doc.add(new Field(FIELD_SUMMARY, items[3], TYPE_STORED));
                    doc.add(new Field(FIELD_CONTENT,
                            items[2] + " " + items[3], TYPE_STORED
                    ));
                } else {
                    doc.add(new Field(FIELD_CONTENT, items[2], TYPE_STORED));
                }

                indexWriter.addDocument(doc);
                docCounter++;
                line = br.readLine();
            }
            System.out.println("Number of indexed documents: " + docCounter);
            indexWriter.close();
            dir.close();
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception in Indexing.\n");
        }
    }

    public List<Integer> search(String queryString) {
        List<Integer> queryResults = new ArrayList<>();
        try {
            Path path = FileSystems.getDefault().getPath("index");
            Directory dir = FSDirectory.open(path);
            DirectoryReader reader = DirectoryReader.open(dir);

            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(similarity);
            QueryParser parser = new QueryParser(FIELD_CONTENT, analyzer);
            Query query = parser.parse(QueryParser.escape(queryString));

            TopDocs results = searcher.search(query, 10000);

            ScoreDoc[] hits = results.scoreDocs;

            for (ScoreDoc hit : hits) {
                Document doc = searcher.doc(hit.doc);
                queryResults.add(Integer.parseInt(doc.get(FIELD_ID)));
            }
            reader.close();
            dir.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception in Search.\n");
        }
        return queryResults;
    }
}