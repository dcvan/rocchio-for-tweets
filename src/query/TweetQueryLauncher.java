package query;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import analysis.TweetAnalyzer;
import query.expansion.TermCollector;
import query.topic.Topic;
import query.topic.TopicReader;
import common.exception.FileExistsException;
import common.exception.InstanceExistsException;

public class TweetQueryLauncher {
	//Tester
	public static void main(String[] args) 
			throws  CorruptIndexException, FileExistsException, IOException, ParseException, InstanceExistsException, org.apache.lucene.queryparser.classic.ParseException{
		if(args.length < 3){
			System.err.println("<Usage> java TweetQueryLauncher <topic file> <index dir> <result file>");
			System.exit(1);
		}
		
		String topIn = args[0];
		String in = args[1];
		String out = args[2];
		
		QueryParser parser = new QueryParser(Version.LUCENE_46, "text", 
				new TweetAnalyzer(Version.LUCENE_46));
		TopicReader reader = new TopicReader(topIn);
		TweetQueryLauncher launcher = new TweetQueryLauncher(in, out);
		
		while(reader.hasNext()){
			Topic top = reader.next();
			Query query = parser.parse(top.getTitle());
			launcher.query(top.getTopNo(), query);
		}
		
		reader.close();
		launcher.close();
	}
	
	private final static int Q_NUM = 1000;
	private final static String DOCNO_FN = "docno";
	
	private File indexDir;
	private File resFile;
	private IndexSearcher searcher;
	private PrintWriter writer;
	private Map<Integer, Set<String>> queryTerms;
	private Map<Integer,TermCollector> collectorMap;
	
	public TweetQueryLauncher(String index, String res) 
			throws CorruptIndexException, IOException, FileExistsException{
	
		File in = new File(index);
		File out = new File(res);
		
		if(out.exists())
			throw new FileExistsException(res);
		
		indexDir = in;
		resFile = out;
		
		searcher = new IndexSearcher(DirectoryReader.open(
				FSDirectory.open(indexDir)));
		writer = new PrintWriter(resFile);
		
		collectorMap = new HashMap<Integer, TermCollector>();
		queryTerms = new HashMap<Integer, Set<String>>();
	}
	
	public synchronized void query(int topno, Query q) 
			throws IOException{
		System.out.println("Currently querying: " + topno);
		TopDocs hits = searcher.search(q, Q_NUM);
		TermCollector collector = new TermCollector(q, hits.scoreDocs, searcher.getIndexReader());
		collectorMap.put(topno, collector);
		queryTerms.put(topno, collector.getQueryTerms());
		ScoreDoc[] scoreDocs = hits.scoreDocs;
		for(int i = 0; i < scoreDocs.length; i ++){
			Document d = searcher.doc(scoreDocs[i].doc);
			write(topno, d.get(DOCNO_FN), i, scoreDocs[i].score);
		}
		
		//write top 10 terms to the output
		Map<String, Float> termMap = getFeedbackTerms(topno, 10);
		System.out.println("---------------------");
		for(Map.Entry<String, Float> e : termMap.entrySet())
			System.out.println(e);
		System.out.println("---------------------");
	}
	
	public Map<String, Float> getFeedbackTerms(int topno, int termNum) 
			throws IOException{
		return collectorMap.get(topno).getTerms(termNum);
	}
	
	public Set<String> getQueryTerms(int topno){
		return queryTerms.get(topno);
	}
	
	public Map<Integer, TermCollector> getAllCollector(){
		return collectorMap;
	}
	
	public IndexSearcher getSearcher(){
		return searcher;
	}
	
	public synchronized void write(int topNo, String docNo, int rank, float score){
		StringBuffer sb = new StringBuffer();
		sb.append(topNo).append(' ')
			.append("Q0").append(' ')
			.append(docNo).append(' ')
			.append(rank).append(' ')
			.append(score).append(' ')
			.append("run");
		
		writer.println(sb);
		writer.flush();
	}
	
	public void close() 
			throws IOException{
		writer.close();
	}
}
