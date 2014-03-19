package query;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;

import query.topic.Topic;
import query.topic.TopicReader;
import run.Feedback;
import run.Statistics;

public class TweetQueryMaker {
	private final static String TEXT_FN = "text";
	private final static String HTAG_FN = "hashtag";
	
	private Map<Integer, String> lastQueries;
	private QueryParser parser;
	private Analyzer analyzer;
	
	public TweetQueryMaker(String topSrc, Analyzer analyzer) 
			throws IOException, ParseException{
		TopicReader topReader = new TopicReader(topSrc);
		lastQueries = new TreeMap<Integer, String>();
		this.analyzer = analyzer;
		parser = new QueryParser(Version.LUCENE_46, TEXT_FN, analyzer);
		while(topReader.hasNext()){
			Topic t = topReader.next();
			lastQueries.put(t.getTopNo(), t.getTitle());
		}
		topReader.close();
	}
	
	public Map<Integer, Query> getQueries() 
			throws org.apache.lucene.queryparser.classic.ParseException{
		Map<Integer, Query> queries = new TreeMap<Integer, Query>();
		for(Map.Entry<Integer, String> entry : lastQueries.entrySet()){
			queries.put(entry.getKey(), parser.parse(entry.getValue()));
		}
		return queries;
	}
	
	public Map<Integer, Query> expandQueries(Statistics stat) 
			throws org.apache.lucene.queryparser.classic.ParseException{
		for(Map.Entry<Integer, Feedback> entry : stat.getFeedbacks().entrySet()){
			int topno = entry.getKey();
			StringBuilder sb = new StringBuilder(lastQueries.get(topno));
			
			//add top M selected terms in top N retrieved tweets into the query
			for(String term : entry.getValue().getTermScores().keySet())
				sb.append(' ' + term);
			
			//add hashtags in top N retrieved tweets into the query
			for(String htag : entry.getValue().getHashtags())
				//query against hashtag field
//				sb.append(' ' + HTAG_FN + ':' + htag);
				//query against text field
				sb.append(' ' + htag);

			lastQueries.put(topno, sb.toString());
		}
		
		return getQueries();
	}
	
	public Analyzer getAnalyzer(){
		return analyzer;
	}
}
