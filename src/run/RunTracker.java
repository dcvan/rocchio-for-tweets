package run;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

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
	private final static String ANALYZER = "analyzer";
	private final static String QUERY = "query";
	private final static String TERM = "term";
	private final static String METRIC = "metric";

	private IndexWriter writer;
	private File recDir;
	private Document runRec;
	private FieldType genericType;
	
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
		runRec.add(new LongField(TIMESTAMP, date.getTime(), LongField.TYPE_STORED));
	}
	
	public void writeAnalyzer(String analyzer){
		runRec.add(new Field(ANALYZER, analyzer, genericType));
	}
	
	public void writeQuery(int topno, String query){
		runRec.add(new Field(String.valueOf(topno), QUERY + ':' + query, genericType));
	}
	
	public void writeTerms(int topno, Map<String, Float> terms){
		for(Map.Entry<String, Float> entry : terms.entrySet())
			runRec.add(new Field(String.valueOf(topno), TERM + ':' + entry.toString(), genericType));
	}
	
	public void writeMetrics(Map<String, String> metrics){
		for(Map.Entry<String, String> entry : metrics.entrySet())
			runRec.add(new Field(METRIC, entry.toString(), genericType));
	}
	
	
	public String getStatByName(String id) 
			throws IOException{
		Query query = new TermQuery(new Term(NAME, id));
		return getStat(query);
	}	
	
	public String getStatByTimeRange(Date start, Date end) 
			throws IOException{
		return getStat(NumericRangeQuery.newLongRange(TIMESTAMP, start.getTime(), end.getTime(), true, true));	
	}
	
	public String getAllStat() 
			throws IOException{
		return getStat(new MatchAllDocsQuery());
	}
	
	public void commit() 
			throws IOException{
		writer.addDocument(runRec);
		writer.close();
	}
	
	private void setFieldType(FieldType ft){
		ft.setStored(true);
		ft.setIndexed(true);
		ft.setTokenized(false);
		ft.setStoreTermVectorOffsets(false);
		ft.setOmitNorms(true);
	}
	
	private String getStat(Query q) 
			throws IOException{
		IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(
				FSDirectory.open(recDir)));
		TopDocs hits = searcher.search(q, UNLIMITED);
		if(hits.totalHits == 0)
			return "No record found";
		StringBuilder res = new StringBuilder();
		for(ScoreDoc sd : hits.scoreDocs){
			Document doc = searcher.doc(sd.doc);
			for(IndexableField f : doc.getFields())
				res.append(f.name()).append(":").append(f.stringValue()).append('\n');
		}

		return res.toString();
	}
}
