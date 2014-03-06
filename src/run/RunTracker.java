package run;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LongField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class RunTracker {
	
	private final static int UNLIMITED = Integer.MAX_VALUE;
	private final static String NAME = "name";
	private final static String TIMESTAMP = "timestamp";
	private final static String RESULT = "result file";
	private final static String ANALYZER = "analyzer";
	private final static String QUERY = "query";
	private final static String Q_TERM = "query terms";
	private final static String F_TERM = "feedback terms";
	private final static String METRIC = "metric";

	private final IndexWriter writer;
	private final File recDir;
	private final FieldType genericType;
	
	public RunTracker(String recPath) 
			throws IOException{
		recDir = new File(recPath);
		if(!recDir.exists())
			recDir.mkdirs();
		IndexWriterConfig writerCfg = new IndexWriterConfig(Version.LUCENE_46, null);
		writer = new IndexWriter(FSDirectory.open(recDir), writerCfg);
		genericType = new FieldType();
		setFieldType(genericType);
	}
	
	public void writeStat(Statistics stat) 
			throws IOException{
		Document record = new Document();
		
		record.add(new Field(NAME, stat.getName(), genericType));
		record.add(new LongField(TIMESTAMP, stat.getTimestamp(), LongField.TYPE_STORED));
		record.add(new Field(RESULT, stat.getResult(), genericType));
		record.add(new Field(ANALYZER, stat.getAnalyzer(), genericType));
		
		Map<Integer, Feedback> feedbacks = stat.getFeedbacks();
		for(Integer topno : feedbacks.keySet()){
			Feedback f = feedbacks.get(topno);
			record.add(new Field(String.valueOf(topno), QUERY + '-' + f.getQuery(), genericType));
			for(String t : f.getQueryTerms())
				record.add(new Field(String.valueOf(topno), Q_TERM + '-' + t, genericType));
			for(Map.Entry<String, Float> entry : f.getTermScores().entrySet())
				record.add(new Field(String.valueOf(topno), F_TERM + '-' + entry.toString(), genericType));
		}
		
		for(Map.Entry<String, Double> entry : stat.getMetrics().entrySet())
			record.add(new Field(METRIC, entry.toString(), genericType));
		
		writer.addDocument(record);
		writer.commit();
	}
	
	
	public ArrayList<Statistics> getStatByName(String id) 
			throws IOException{
		Query query = new TermQuery(new Term(NAME, id));
		return getStat(query);
	}	
	public Statistics getStatByTimestamp(Date datetime) 
			throws IOException{
		long time = datetime.getTime();
		return getStatByTimestamp(time);
	}
	
	public Statistics getStatByTimestamp(long time) 
			throws IOException{
		return getStat(NumericRangeQuery.newLongRange(TIMESTAMP, time, time, true, true)).get(0);
	}
	
	public ArrayList<Statistics> getStatByTimeRange(Date start, Date end) 
			throws IOException{
		return getStat(NumericRangeQuery.newLongRange(TIMESTAMP, start.getTime(), end.getTime(), true, true));	
	}
	
	public ArrayList<Statistics> getAllStat() 
			throws IOException{
		return getStat(new MatchAllDocsQuery());
	}
	
	public Map<String, Double> getMetrics(long timestamp, String... metrics) 
			throws IOException{
		Map<String, Double> metricMap = getStatByTimestamp(timestamp).getMetrics();
		Map<String, Double> res = new TreeMap<String, Double>();
		for(String m : metrics)
			res.put(m, metricMap.get(m));
		return res;
	}
	
	public void close() 
			throws IOException{
		writer.close();
	}
	
	private void setFieldType(FieldType ft){
		ft.setStored(true);
		ft.setIndexed(true);
		ft.setTokenized(false);
		ft.setStoreTermVectorOffsets(false);
		ft.setOmitNorms(true);
	}
	
	private ArrayList<Statistics> getStat(Query q) 
			throws IOException{
		IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(
				FSDirectory.open(recDir)));
		TopDocs hits = searcher.search(q, UNLIMITED);
		ArrayList<Statistics> statList = new ArrayList<Statistics>();
		for(ScoreDoc sd : hits.scoreDocs){
			Document doc = searcher.doc(sd.doc);
			Statistics stat = new Statistics();
			for(IndexableField f : doc.getFields()){
				String k = f.name();
				String v = f.stringValue();
				if(k.equals(NAME)){
					stat.setName(v);
				}else if(k.equals(TIMESTAMP)){
					stat.setTimestamp(Long.parseLong(v));
				}else if(k.equals(RESULT)){
					stat.setResult(v);
				}else if(k.equals(ANALYZER)){
					stat.setAnalyzer(v);
				}else if(k.equals(METRIC)){
					String[] fs = v.split("=");
					stat.addMetric(fs[0], Double.parseDouble(fs[1]));
				}else{
					String[] fs = v.split("-");
					Feedback feedback = stat.getFeedback(Integer.parseInt(k));
					if(feedback == null){
						feedback = new Feedback();
						stat.addFeedback(Integer.parseInt(k), feedback);
					}
					
					if(fs[0].equals(QUERY)){
						feedback.setQuery(fs[1]);
					}else if(fs[0].equals(Q_TERM)){
						feedback.addQueryTerms(fs[1]);
					}else if(fs[0].equals(F_TERM)){
						String[] ts = fs[1].split("=");
						feedback.addTerm(ts[0], Float.parseFloat(ts[1]));
					}
				}
			}
			
			statList.add(stat);
		}
		
		return statList;
	}
}
