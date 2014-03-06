package run;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;

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

public class Run {

	public static void main(String[] args) 
			throws Exception{
		Run run = new Run(new EnglishAnalyzer(Version.LUCENE_46), 10, 10);
		run.run("first run");
		run.expandQueries();
		run.run("second run");
		for(Statistics stat : run.getTracker().getAllStat()){
			System.out.println(stat);	
		}
		run.close();
	}
	
	private final static String RESULT_BASE = "test-collection/result-";
	private final static String INDEX_BASE = System.getProperty("user.home") + "/Documents/tweets.index";
	private final static String REC_BASE = System.getProperty("user.home") + "/Documents/records";
	private final static String TOP_PATH = "test-collection/topics.MB1-50.txt";
	private final static String QREL_PATH = "test-collection/microblog11-qrels.txt";
	
	private final static String ALL_METRICS = "all_trec";
	private final static String ALL_QUERIES = "all";
	
	private Date timestamp;
	private TweetQueryMaker qmaker;
	private RunTracker tracker;
	private Statistics state;
	private int numDocs;
	private int numTerms;
	
	public Run(Analyzer analyzer, int numDocs, int numTerms) 
			throws IOException, ParseException{
		qmaker = new TweetQueryMaker(TOP_PATH, analyzer);
		tracker = new RunTracker(REC_BASE);
		this.numDocs = numDocs;
		this.numTerms = numTerms;
	}
	
	public synchronized void run(String name) 
			throws org.apache.lucene.queryparser.classic.ParseException, IOException, InvalidParameterException, TweetSearchEvaluatorException, FileExistsException, WrongFileTypeException, InstanceExistsException, ResetException{
		state = new Statistics();
		timestamp = new Date();
		String result = new StringBuilder(RESULT_BASE)
			.append(name.trim().replaceAll(" ", "-"))
			.append('-')
			.append(timestamp.getTime())
			.append(".txt").toString();
		TermCollector collector = new TermCollector(numDocs, numTerms);
		TweetQueryLauncher launcher = new TweetQueryLauncher(INDEX_BASE, result, collector);
		TweetSearchEvaluator evaluator = new TweetSearchEvaluator(QREL_PATH, result);
		
		state.setName(name);
		state.setTimestamp(timestamp.getTime());
		state.setResult(result);
		state.setAnalyzer(qmaker.getAnalyzer().getClass().getSimpleName());
		for(Map.Entry<Integer, Query> entry : qmaker.getQueries().entrySet()){
			Feedback f = new Feedback();
			int topno = entry.getKey();
			Query q = entry.getValue();
			launcher.query(topno, q);
			f.setQuery(q.toString());
			f.setQueryTerms(collector.getQueryTerms());
			f.setTermScores(collector.getTerms());
			state.addFeedback(topno, f);
		}
		
		evaluator.evaluate(false, ALL_METRICS);
		state.setMetrics(evaluator.getScores(ALL_QUERIES));
		
		tracker.writeStat(state);
		launcher.close();
	}
	
	public Date getTimestamp(){
		return timestamp;
	}
	
	public RunTracker getTracker(){
		return tracker;
	}
	
	public void expandQueries() 
			throws org.apache.lucene.queryparser.classic.ParseException{
		qmaker.expandQueries(state);
	}
	
	public void close() 
			throws IOException{
		tracker.close();
	}
}
