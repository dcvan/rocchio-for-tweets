package query;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;
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
	
	public TweetQueryMaker(String topSrc, Analyzer analyzer, double step){
		
	}
	
	public Map<Integer, Query> getQueries() 
			throws org.apache.lucene.queryparser.classic.ParseException{
		Map<Integer, Query> queries = new TreeMap<Integer, Query>();
		for(Map.Entry<Integer, String> entry : lastQueries.entrySet()){
			queries.put(entry.getKey(), parser.parse(entry.getValue()));
		}
		return queries;
	}
	
	public Map<Integer, Query> expandQueries(Statistics stat, double step, boolean withTopTerms, boolean withHashtags) 
			throws org.apache.lucene.queryparser.classic.ParseException{
		
		for(Map.Entry<Integer, Feedback> entry : stat.getFeedbacks().entrySet()){
			double boostBase = step * 10;
//			double boostBase = 1.0;
			int topno = entry.getKey();
			StringBuilder sb = new StringBuilder(lastQueries.get(topno));

			if(withTopTerms){
				//add top M selected terms in top N retrieved tweets into the query
				int cnt = 0;
				Map<String, Float> termMap = entry.getValue().getTermScores();
				float cur = Collections.max(termMap.values());
				
				for(String term : termMap.keySet()){
					if(termMap.get(term) < cur){
						cnt ++;
						cur = termMap.get(term);
					}
					double boost = boostBase - step * cnt;
					if(boost < 0) boost = 0;
					sb.append(' ' + term + '^' + String.valueOf(boost));
				}
			}
			//add hashtags in top N retrieved tweets into the query
			if(withHashtags){
				for(String htag : entry.getValue().getHashtags())
					sb.append(' ' + htag);
			}

			lastQueries.put(topno, sb.toString());
		}
		
		return getQueries();
	}
	
	public Analyzer getAnalyzer(){
		return analyzer;
	}
}
