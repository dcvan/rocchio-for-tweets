package query;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import analyzer.TweetAnalyzer;
import query.topic.Topic;
import query.topic.TopicReader;
import common.exception.FileExistsException;
import common.exception.InstanceExistsException;

public class TweetQueryLauncher {
	
	private final static int Q_NUM = 1000;
	private final static String DOCNO_FN = "docno";
	
	private static TweetQueryLauncher launcher;
	
	private File indexDir;
	private File resFile;
	private IndexSearcher searcher;
	private PrintWriter writer;
	
	//Tester
	public static void main(String[] args) 
			throws  CorruptIndexException, FileExistsException, IOException, ParseException, org.apache.lucene.queryParser.ParseException, InstanceExistsException{
		if(args.length < 3){
			System.err.println("<Usage> java TweetQueryLauncher <topic file> <index dir> <result file>");
			System.exit(1);
		}
		
		String topIn = args[0];
		String in = args[1];
		String out = args[2];
		
		QueryParser parser = new QueryParser(Version.LUCENE_36, "text", 
				new TweetAnalyzer());
		TopicReader reader = TopicReader.create(topIn);
		TweetQueryLauncher launcher = TweetQueryLauncher.create(in, out);
		
		while(reader.hasNext()){
			Topic top = reader.next();
			Query query = parser.parse(top.getTitle().toLowerCase());
			launcher.query(top.getTopNo(), query);
		}
		
		reader.close();
		launcher.close();
	}
	
	public static TweetQueryLauncher create(String index, String res) 
			throws FileExistsException, CorruptIndexException, IOException, InstanceExistsException{
		if(launcher != null)
			throw new InstanceExistsException(TweetQueryLauncher.class);
		File in = new File(index);
		File out = new File(res);
		
		if(out.exists())
			throw new FileExistsException(res);
		return new TweetQueryLauncher(in, out);
	}
	
	public synchronized void query(int topNum, Query q) 
			throws IOException{
		System.out.println("Currently querying: " + topNum);
		TopDocs hits = searcher.search(q, Q_NUM);
		ScoreDoc[] scoreDocs = hits.scoreDocs;
		for(int i = 0; i < scoreDocs.length; i ++){
			Document d = searcher.doc(scoreDocs[i].doc);
			write(topNum, d.get(DOCNO_FN), i, scoreDocs[i].score);
		}
	}
	
	public void close() 
			throws IOException{
		searcher.close();
		writer.close();
	}
	
	private TweetQueryLauncher(File in, File out) 
			throws CorruptIndexException, IOException{
		indexDir = in;
		resFile = out;
		
		searcher = new IndexSearcher(IndexReader.open(
				FSDirectory.open(indexDir)));
		writer = new PrintWriter(resFile);
	}
	
	private synchronized void write(int topNo, String docNo, int rank, float score){
		StringBuffer sb = new StringBuffer();
		sb.append(topNo).append(' ')
			.append("Q0").append(' ')
			.append(docNo).append(' ')
			.append(rank).append(' ')
			.append(score).append(' ')
			.append("run");
		
		writer.println(sb);
	}
	
}
