package query;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
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

import query.topic.Topic;
import query.topic.TopicReader;
import query.topic.TopicReaderExistsException;
import common.exception.FileExistsException;

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
			throws TopicReaderExistsException, CorruptIndexException, TweetQueryLauncherExistsException, FileExistsException, IOException, ParseException, org.apache.lucene.queryParser.ParseException{
		if(args.length < 3){
			System.err.println("<Usage> java TweetQueryLauncher <topic file> <index dir> <result file>");
			System.exit(1);
		}
		
		String topIn = args[0];
		String in = args[1];
		String out = args[2];
		
		QueryParser parser = new QueryParser(Version.LUCENE_36, "text", 
				new StandardAnalyzer(Version.LUCENE_36));
		System.out.println(parser.getDefaultOperator());
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
			throws TweetQueryLauncherExistsException, FileExistsException, CorruptIndexException, IOException{
		if(launcher != null)
			throw new TweetQueryLauncherExistsException();
		File in = new File(index);
		File out = new File(res);
		
		if(out.exists())
			throw new FileExistsException(res);
		
		return new TweetQueryLauncher(in, out);
	}
	
	public synchronized void query(String topNum, Query q) 
			throws IOException{
		TopDocs hits = searcher.search(q, Q_NUM);
		ScoreDoc[] scoreDocs = hits.scoreDocs;
		for(int i = 0; i < scoreDocs.length; i ++){
			Document d = searcher.doc(scoreDocs[i].doc);
			System.out.println(d.get("text"));
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
	
	private synchronized void write(String topNo, String docNo, int rank, float score){
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
