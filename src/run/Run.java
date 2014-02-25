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
import common.exception.TweetSearchEvaluatorException;
import common.exception.WrongFileTypeException;
import query.TweetQueryLauncher;
import query.TweetQueryMaker;
import eval.TweetSearchEvaluator;

public class Run {

	public static void main(String[] args) 
			throws Exception{
		Run run = new Run(new EnglishAnalyzer(Version.LUCENE_46));
		run.run("first run");
		run.getQueryMaker().expandQueries(run.getLatestStat());
		run.run("second run");
		for(Statistics stat : run.getTracker().getAllStat()){
			System.out.println(stat);	
		}
		run.close();
	}
	
	private final static String RESULT_BASE = "test-collection/result-";
	private final static String INDEX_BASE = System.getProperty("user.home") + "/Documents/tweets.index.1";
	private final static String REC_BASE = System.getProperty("user.home") + "/Documents/records";
	private final static String TOP_PATH = "test-collection/topics.MB1-50.txt";
	private final static String QREL_PATH = "test-collection/microblog11-qrels.txt";
	private final static int NUM_FEEDBACK_TERMS = 10;
	
	private final static String ALL_METRICS = "all_trec";
	private final static String ALL_QUERIES = "all";
	
	private Date timestamp;
	private TweetQueryMaker qmaker;
	private RunTracker tracker;
	private int numFeedbackTerms;
	
	public Run(Analyzer analyzer) 
			throws IOException, ParseException{
		this(analyzer, NUM_FEEDBACK_TERMS);
	}
	
	public Run(Analyzer analyzer, int numFeedbackTerms) 
			throws IOException, ParseException{
		qmaker = new TweetQueryMaker(TOP_PATH, analyzer);
		tracker = new RunTracker(REC_BASE);
		this.numFeedbackTerms = numFeedbackTerms;
	}
	
	public synchronized void run(String name) 
			throws org.apache.lucene.queryparser.classic.ParseException, IOException, InvalidParameterException, TweetSearchEvaluatorException, FileExistsException, WrongFileTypeException, InstanceExistsException{
		timestamp = new Date();
		String result = new StringBuilder(RESULT_BASE)
			.append(name.trim().replaceAll(" ", "-"))
			.append('-')
			.append(timestamp.getTime())
			.append(".txt").toString();
		
		TweetQueryLauncher launcher = new TweetQueryLauncher(INDEX_BASE, result);
		TweetSearchEvaluator evaluator = new TweetSearchEvaluator(QREL_PATH, result);
		
		tracker.writeName(name);
		tracker.writeTimestamp(timestamp);
		tracker.writeResultFile(result);
		tracker.writeAnalyzer(qmaker.getAnalyzer().getClass().getSimpleName());
		for(Map.Entry<Integer, Query> entry : qmaker.getQueries().entrySet()){
			int topno = entry.getKey();
			Query q = entry.getValue();
			launcher.query(topno, q);
			tracker.writeQuery(topno, q.toString());
			tracker.writeQueryTerms(topno, launcher.getQueryTerms(topno));
			tracker.writeFeedbackTerms(topno, launcher.getFeedbackTerms(topno, numFeedbackTerms));
		}
		
		evaluator.evaluate(new String[]{ALL_METRICS}, false);
		tracker.writeMetrics(evaluator.getScores(ALL_QUERIES));
		tracker.commit();

		launcher.close();
	}
	
	public Date getTimestamp(){
		return timestamp;
	}
	
	public TweetQueryMaker getQueryMaker(){
		return qmaker;
	}
	
	public RunTracker getTracker(){
		return tracker;
	}
	
	public Statistics getLatestStat() 
			throws IOException{
		return tracker.getStatbyTimeStamp(timestamp);
	}
	
	public void close() 
			throws IOException{
		tracker.close();
	}
}
