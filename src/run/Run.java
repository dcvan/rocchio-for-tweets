package run;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import common.exception.FileExistsException;
import common.exception.InstanceExistsException;
import common.exception.InvalidParameterException;
import common.exception.ResetException;
import common.exception.TweetSearchEvaluatorException;
import common.exception.WrongFileTypeException;

public class Run {

	public final static String TIMESTAMP = "timestamp";
	public final static String DOC_NUM = "document number";
	public final static String TERM_NUM = "term number";
	public final static String ANALYZER = "analyzer";
	public final static String STEP = "step";
	public final static String WITH_TERMS = "with selected terms";
	public final static String WITH_HASHTAGS = "with hashtags";
	public final static String METRICS = "metrics";
	public final static String IMPROVE = "improvement";
	public final static String RUN_TYPE = "run type";
	
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
		if(iterNum <= 0){
			//TODO throw exception
			return;
		}
		Document doc = new Document();
		if(iterNum == 1){
			doc.add(new Field(RUN_TYPE, "baseline run", genericType));
		}else if(iterNum == 2){
			doc.add(new Field(RUN_TYPE, "single iteration", genericType));
		}else if(iterNum > 2){
			doc.add(new Field(RUN_TYPE, "multiple iterations", genericType));
		}
			
		doc.add(new IntField(DOC_NUM, config.getDocNum(), IntField.TYPE_STORED));
		doc.add(new IntField(TERM_NUM, config.getTermNum(), IntField.TYPE_STORED));
		doc.add(new Field(ANALYZER, config.getAnalyzer().getClass().getSimpleName(), genericType));
		doc.add(new DoubleField(STEP, config.getStep(), DoubleField.TYPE_STORED));
		doc.add(new Field(WITH_TERMS, String.valueOf(config.isWithTerms()), genericType));
		doc.add(new Field(WITH_HASHTAGS, String.valueOf(config.isWithHashtags()), genericType));
		
		Search search = new Search(config.getAnalyzer(), config.getDocNum(), config.getTermNum());
		boolean isWithTerms = config.isWithTerms(), isWithHashtags = config.isWithHashtags();
		String[] metrics = config.getMetrics();
		Map<String, Double> baseMetrics = new TreeMap<String, Double>();
		for(String m : metrics)
			baseMetrics.put(m, 0.0);
		
		for(int i = 0; i < iterNum; i ++){
			search.start(config.getNote());
			long t = search.getTimestamp();
			if(i == 0)
				doc.add(new LongField(TIMESTAMP, t, LongField.TYPE_STORED));
			Map<String, Double> curMetrics = search.getTracker().getMetrics(t, metrics);
			doc.add(new Field(METRICS, curMetrics.toString(), genericType));

			if(isWithTerms && isWithHashtags)
				search.expandQueriesWithAllTerms(config.getStep());
			else if(isWithTerms)
				search.expandQueriesWithTopTerms(config.getStep());
			else if(isWithHashtags)
				search.expandQueriesWithHashtags(config.getStep());
			
			if(i > 0 && i == iterNum - 1){
				Map<String, Double> impr = new TreeMap<String, Double>();
				for(String m : metrics){
					double incr = (curMetrics.get(m) - baseMetrics.get(m)) / baseMetrics.get(m);
					impr.put(m, new BigDecimal(incr).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue());
				}
				doc.add(new Field(IMPROVE, impr.toString(), genericType));
			}
			
			baseMetrics.putAll(curMetrics);
		}
		writer.addDocument(doc);
		writer.commit();
		search.close();
	}
	
	public void close() 
			throws IOException{
		writer.close();
	}
	
}
