package run;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Query;

import common.exception.FileExistsException;
import common.exception.InstanceExistsException;
import common.exception.InvalidParameterException;
import common.exception.ResetException;
import common.exception.TweetSearchEvaluatorException;
import common.exception.WrongFileTypeException;
import query.TermCollector;
import query.TweetQueryLauncher;
import query.TweetQueryMaker;
import eval.TweetSearchEvaluator;

public class Search {

	public static void main(String[] args) 
			throws Exception{

	}
	
	private final static String RESULT_BASE = "test-collection/result-";
	private final static String INDEX_BASE = System.getProperty("user.home") + "/Documents/tweets.index.2";
	private final static String REC_BASE = System.getProperty("user.home") + "/Documents/records";
	private final static String TOP_PATH = "test-collection/topics.MB1-50.txt";
	private final static String QREL_PATH = "test-collection/microblog11-qrels.txt";
	
	private final static String ALL_METRICS = "all_trec";
	private final static String ALL_QUERIES = "all";
	
	private long timestamp;
	private TweetQueryMaker qmaker;
	private SearchTracker tracker;
	private Statistics state;
	private int numDocs;
	private int numTerms;
	
	public Search(Analyzer analyzer, int numDocs, int numTerms) 
			throws IOException, ParseException{
		qmaker = new TweetQueryMaker(TOP_PATH, analyzer);
		tracker = new SearchTracker(REC_BASE);
		this.numDocs = numDocs;
		this.numTerms = numTerms;
	}
	
	public void start(String name) 
			throws org.apache.lucene.queryparser.classic.ParseException, IOException, InvalidParameterException, TweetSearchEvaluatorException, FileExistsException, WrongFileTypeException, InstanceExistsException, ResetException{
		state = new Statistics();
		timestamp = new Date().getTime();
		String result = new StringBuilder(RESULT_BASE)
			.append(timestamp)
			.append(".txt").toString();
		TermCollector collector = new TermCollector(numDocs, numTerms);
		TweetQueryLauncher launcher = new TweetQueryLauncher(INDEX_BASE, result, collector);
		TweetSearchEvaluator evaluator = new TweetSearchEvaluator(QREL_PATH, result);
		
		state.setName(name);
		state.setTimestamp(timestamp);
		state.setResult(result);
		state.setAnalyzer(qmaker.getAnalyzer().getClass().getSimpleName());
		for(Map.Entry<Integer, Query> entry : qmaker.getQueries().entrySet()){
			Feedback f = new Feedback();
			int topno = entry.getKey();
			Query q = entry.getValue();
			launcher.query(topno, q);
			f.setQuery(q.toString());
			f.setQueryTerms(collector.getQueryTerms());
			f.setHashtags(collector.getHashtags());
			f.setTermScores(collector.getTerms());
			state.addFeedback(topno, f);
		}
		
		evaluator.evaluate(false, ALL_METRICS);
		state.setMetrics(evaluator.getScores(ALL_QUERIES));
		
		tracker.writeStat(state);
		launcher.close();
	}
	
	public long getTimestamp(){
		return timestamp;
	}
	
	public SearchTracker getTracker(){
		return tracker;
	}
	
	
	public void expandQueriesWithTopTerms(double step)
			throws org.apache.lucene.queryparser.classic.ParseException{
		qmaker.expandQueries(state, step, true, false);
	}
	
	public void expandQueriesWithHashtags(double step) 
			throws org.apache.lucene.queryparser.classic.ParseException{
		qmaker.expandQueries(state, step, false, true);
	}
	
	public void expandQueriesWithAllTerms(double step) 
			throws org.apache.lucene.queryparser.classic.ParseException{
		qmaker.expandQueries(state, step, true, true);
	}
	
	public void close() 
			throws IOException{
		tracker.close();
	}
	
}
