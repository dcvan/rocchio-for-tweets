package test.analyzer;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import analyzer.TweetAnalyzer;

public class AnalyzerTester{
	public static void main(String[] args) 
			throws CorruptIndexException, LockObtainFailedException, IOException, ParseException{
//		String text = "'The sun sets on the war, the day breaks and everything is new' KOC - Winning A Battle, Losing The War";
		String text = "War Rounds: Lessons learned from causalities of War (Iraq and Afghanistan) starts in 10  #GAPACONF";
		Analyzer a1 = new StandardAnalyzer(Version.LUCENE_36);
		Analyzer a2 = new TweetAnalyzer();
		
		AnalyzerUtils.displayTokens(a1, text);
		System.out.println();
		AnalyzerUtils.displayTokensWithFullDetails(a1, text);
		System.out.println();
		AnalyzerUtils.displayTokens(a2, text);
		System.out.println();
		AnalyzerUtils.displayTokensWithFullDetails(a2, text);
//		Directory dir = new RAMDirectory();
//		IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(Version.LUCENE_36,
//				new StandardAnalyzer(Version.LUCENE_36)));
//		
//		Document doc1 = new Document();
//		doc1.add(new Field("text",
//				"'The sun sets on the war, the day breaks and everything is new' KOC - Winning A Battle, Losing The War",
//				Field.Store.YES,
//				Field.Index.ANALYZED));
//		writer.addDocument(doc1);
//		
//		Document doc2 = new Document();
//		doc2.add(new Field("text",
//				"War Rounds: Lessons learned from causalities of War (Iraq and Afghanistan) starts in 10  #GAPACONF",
//				Field.Store.YES,
//				Field.Index.ANALYZED));
//		writer.addDocument(doc2);
//		writer.close();
//		
//		QueryParser parser = new QueryParser(Version.LUCENE_36, "text", new TweetAnalyzer());
//		Query q = parser.parse("war");
//		IndexSearcher searcher = new IndexSearcher(IndexReader.open(dir));
//		TopDocs hits = searcher.search(q, 10);
//		
//		for(ScoreDoc sd : hits.scoreDocs){
//			Document d = searcher.doc(sd.doc);
//			System.out.println(d.get("text"));
//		}
//		
//		searcher.close();
		
	}
}
