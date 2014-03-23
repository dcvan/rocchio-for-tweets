package run;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LongField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import common.exception.FileExistsException;
import common.exception.InstanceExistsException;
import common.exception.InvalidParameterException;
import common.exception.ResetException;
import common.exception.TweetSearchEvaluatorException;
import common.exception.WrongFileTypeException;

public class Run {

	
	public static void main(String[] args) 
			throws IOException, ParseException, org.apache.lucene.queryparser.classic.ParseException, InvalidParameterException, TweetSearchEvaluatorException, FileExistsException, WrongFileTypeException, InstanceExistsException, ResetException{
		RunConfig config = new RunConfig(10, 10, 
				new EnglishAnalyzer(Version.LUCENE_46), 0.1, true, false, 
				new String[]{"P_30", "map", "ndcg"}, "test");
		
		Run run = new Run();
		run.go(config, 4);
		run.close();
		
		IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(new File(REC_BASE))));
		TopDocs hits = searcher.search(new MatchAllDocsQuery(), Integer.MAX_VALUE);
		for(ScoreDoc sd : hits.scoreDocs){
			Document d = searcher.doc(sd.doc);
			for(IndexableField f : d.getFields())
				System.out.println(f.name() + ": " + f.stringValue());
		}
	}

	public final static String TIMESTAMP = "timestamp";
	public final static String DOC_NUM = "document number";
	public final static String TERM_NUM = "term number";
	public final static String ANALYZER = "analyzer";
	public final static String STEP = "step";
	public final static String WITH_TERMS = "with selected terms";
	public final static String WITH_HASHTAGS = "with hashtags";
	public final static String METRICS = "metrics";
	public final static String IMPROVE = "improvement";
	
	private final static String REC_BASE = System.getProperty("user.home") + "/Documents/run";

	private final IndexWriter writer;
	private final FieldType genericType;
	
	public Run() 
			throws IOException{
		File recDir = new File(REC_BASE);
		if(!recDir.exists())
			recDir.mkdirs();
		writer = new IndexWriter(FSDirectory.open(recDir),
				new IndexWriterConfig(Version.LUCENE_46, null));
		genericType = new FieldType();
		genericType.setStored(true);
		genericType.setIndexed(true);
		genericType.setTokenized(false);
		genericType.setStoreTermVectors(false);
		genericType.setOmitNorms(true);
	}
	
	public void go(RunConfig config, int iterNum) 
			throws IOException, ParseException, org.apache.lucene.queryparser.classic.ParseException, InvalidParameterException, TweetSearchEvaluatorException, FileExistsException, WrongFileTypeException, InstanceExistsException, ResetException{
		Document doc = new Document();
		doc.add(new Field(DOC_NUM, String.valueOf(config.getDocNum()), genericType));
		doc.add(new Field(TERM_NUM, String.valueOf(config.getTermNum()), genericType));
		doc.add(new Field(ANALYZER, config.getAnalyzer().getClass().getSimpleName(), genericType));
		doc.add(new Field(STEP, String.valueOf(config.getStep()), genericType));
		doc.add(new Field(WITH_TERMS, String.valueOf(config.isWithTerms()), genericType));
		doc.add(new Field(WITH_HASHTAGS, String.valueOf(config.isWithHashtags()), genericType));
		
		Search search = new Search(config.getAnalyzer(), config.getDocNum(), config.getTermNum());
		search.start(config.getNote());
		long t = search.getTimestamp();
		doc.add(new LongField(TIMESTAMP, t, LongField.TYPE_STORED));
		Map<String, Double> baseline = search.getTracker().getMetrics(t, config.getMetrics());
		doc.add(new Field(METRICS, baseline.toString(), genericType));

		for(int i = 1; i < iterNum; i ++){
			if(config.isWithTerms() && config.isWithHashtags())
				search.expandQueriesWithAllTerms(config.getStep());
			else if(config.isWithTerms())
				search.expandQueriesWithTopTerms(config.getStep());
			else if(config.isWithHashtags())
				search.expandQueriesWithHashtags(config.getStep());
			search.start(config.getNote());
			t = search.getTimestamp();
			Map<String, Double> metrics = search.getTracker().getMetrics(t, config.getMetrics());
			doc.add(new Field(METRICS, metrics.toString(), genericType));

			if(i == iterNum - 1){
				Map<String, Double> impr = new TreeMap<String, Double>();
				for(String m : config.getMetrics()){
					double incr = (metrics.get(m) - baseline.get(m)) / baseline.get(m);
					impr.put(m, new BigDecimal(incr).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue());
				}
				doc.add(new Field(IMPROVE, impr.toString(), genericType));
			}
		}
		writer.addDocument(doc);
		search.close();
	}
	
	public void close() 
			throws IOException{
		writer.close();
	}
	
}
