package query;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import common.exception.FileExistsException;
import common.exception.ResetException;

public class TweetQueryLauncher {
	
	private final static int Q_NUM = 1000;
	private final static String DOCNO_FN = "docno";
	
	private Directory indexDir;
	private IndexSearcher searcher;
	private PrintWriter writer;
	private Map<Integer, Set<String>> queryTerms;
	private Map<Integer, Map<String, Float>> termMap;
	private Map<Integer, Set<String>> hashtags;
	private TermCollector collector;
	
	public TweetQueryLauncher(String index, String res, TermCollector collector) 
			throws CorruptIndexException, IOException, FileExistsException{
	
		File in = new File(index);
		File out = new File(res);
		
		if(out.exists())
			throw new FileExistsException(res);
		
		indexDir = FSDirectory.open(in);
		searcher = new IndexSearcher(DirectoryReader.open(indexDir));
		writer = new PrintWriter(out);

		this.collector = collector;
		
		termMap = new HashMap<Integer, Map<String, Float>>();
		queryTerms = new HashMap<Integer, Set<String>>();
		hashtags = new HashMap<Integer, Set<String>>();
	}
	
	public void query(int topno, Query q) 
			throws IOException, ResetException{
		System.out.println("Currently querying: " + topno);
		
		TopDocs hits = searcher.search(q, Q_NUM);
		ScoreDoc[] scoreDocs = hits.scoreDocs;
		
		collector.reset(q, scoreDocs, searcher.getIndexReader());
		
		termMap.put(topno, collector.getTerms());
		queryTerms.put(topno, collector.getQueryTerms());
		hashtags.put(topno, collector.getHashtags());
		
		for(int i = 0; i < scoreDocs.length; i ++){
			Document d = searcher.doc(scoreDocs[i].doc);
			write(topno, d.get(DOCNO_FN), i, scoreDocs[i].score);
		}
		
		//write top N terms to the output
		Map<String, Float> terms = termMap.get(topno);
		Set<String> htags = hashtags.get(topno);
		System.out.println("---------------------");
		for(Map.Entry<String, Float> e : terms.entrySet())
			System.out.println(e);
		if(htags.size() > 0){
			System.out.println("******HASHTAGS******");
			for(String tag : htags)
				System.out.println(tag);
		}
		System.out.println("---------------------");
	}
	
	public IndexSearcher getSearcher(){
		return searcher;
	}
	
	public void write(int topNo, String docNo, int rank, float score){
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
		searcher.getIndexReader().close();
		indexDir.close();
	}
}
