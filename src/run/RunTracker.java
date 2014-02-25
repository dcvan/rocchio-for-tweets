package run;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Set;

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

	private IndexWriter writer;
	private File recDir;
	private Document runRec;
	private FieldType genericType;
	private Date latest, latestTmp;
	
	public RunTracker(String recPath) 
			throws IOException{
		recDir = new File(recPath);
		if(!recDir.exists())
			recDir.mkdirs();
		IndexWriterConfig writerCfg = new IndexWriterConfig(Version.LUCENE_46, null);
		writer = new IndexWriter(FSDirectory.open(recDir), writerCfg);
		genericType = new FieldType();
		setFieldType(genericType);
		runRec = new Document();
	}
	
	public void writeName(String name){
		runRec.add(new Field(NAME, name, genericType));
	}
	
	public void writeTimestamp(Date date){
		latestTmp = date;
		runRec.add(new LongField(TIMESTAMP, latestTmp.getTime(), LongField.TYPE_STORED));
	}
	
	public void writeResultFile(String path){
		runRec.add(new Field(RESULT, path, genericType));
	}
	
	public void writeAnalyzer(String analyzer){
		runRec.add(new Field(ANALYZER, analyzer, genericType));
	}
	
	public void writeQuery(int topno, String query){
		runRec.add(new Field(String.valueOf(topno), QUERY + '-' + query, genericType));
	}
	
	public void writeQueryTerms(int topno, Set<String> terms){
		for(String t : terms)
			runRec.add(new Field(String.valueOf(topno), Q_TERM + '-' + t, genericType));
	}
	
	public void writeFeedbackTerms(int topno, Map<String, Float> terms){
		for(Map.Entry<String, Float> entry : terms.entrySet())
			runRec.add(new Field(String.valueOf(topno), F_TERM + '-' + entry.toString(), genericType));
	}
	
	public void writeMetrics(Map<String, Double> metrics){
		for(Map.Entry<String, Double> entry : metrics.entrySet())
			runRec.add(new Field(METRIC, entry.toString(), genericType));
	}
	
	
	public ArrayList<Statistics> getStatByName(String id) 
			throws IOException{
		Query query = new TermQuery(new Term(NAME, id));
		return getStat(query);
	}	
	public Statistics getStatbyTimestamp(Date datetime) 
			throws IOException{
		long time = datetime.getTime();
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
	
	public void commit() 
			throws IOException{
		writer.addDocument(runRec);
		latest = latestTmp;
		runRec = new Document();
		writer.commit();
	}
	
	public void close() 
			throws IOException{
		writer.close();
	}
	
	public Statistics getLatestStat() 
			throws IOException{
		return getStatbyTimestamp(latest);
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
