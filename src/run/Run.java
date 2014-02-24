package run;


import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;

import common.exception.InvalidParameterException;
import common.exception.TweetSearchEvaluatorException;
import query.TweetQueryLauncher;
import query.topic.Topic;
import query.topic.TopicReader;
import eval.TweetSearchEvaluator;

public class Run {

	public static void main(String[] args) 
			throws Exception{
		if(args.length < 5){
			System.err.println("<Usage> java Run <topic file> <index dir> <qrel file> <result file> <recs dir>");
			System.exit(1);
		}
		
		String topics = args[0], indexDir = args[1], qrel = args[2], result = args[3], recs = args[4];
		
		TopicReader reader = new TopicReader(topics);
		TweetQueryLauncher launcher = new TweetQueryLauncher(indexDir, result);
		TweetSearchEvaluator evaluator = new TweetSearchEvaluator(qrel, result);
		RunTracker tracker = new RunTracker(recs);
		Run run = new Run(launcher, reader, evaluator, EnglishAnalyzer.class, tracker);
		run.run("First run");
		System.out.println(tracker.getAllStat());
	}
	
	private final static String ALL_METRICS = "all_trec";
	private final static String ALL_QUERIES = "all";
	private final static String TEXT = "text";
	
	private Analyzer analyzer;
	private TweetQueryLauncher launcher;
	private TopicReader reader;
	private TweetSearchEvaluator evaluator;
	private RunTracker tracker;
	
	public Run(TweetQueryLauncher launcher, TopicReader reader, TweetSearchEvaluator evaluator, Class<?> analyzer, RunTracker tracker) 
			throws InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException{
		Object obj = newAnalyzer(analyzer);
		if(!(obj instanceof Analyzer))
			throw new IllegalArgumentException("Parameter analyzer must be an instance of Analyzer.");
		this.launcher = launcher;
		this.reader = reader;
		this.evaluator = evaluator;
		this.tracker = tracker;
		this.analyzer = (Analyzer)obj;
	}
	
	public synchronized void run(String name) 
			throws IOException, ParseException, InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException, org.apache.lucene.queryparser.classic.ParseException, InvalidParameterException, TweetSearchEvaluatorException{
		tracker.writeName(name);
		tracker.writeTimestamp(new Date());
		tracker.writeAnalyzer(analyzer.getClass().getSimpleName());
		QueryParser parser = new QueryParser(Version.LUCENE_46, TEXT, analyzer);
		while(reader.hasNext()){
			Topic t = reader.next();
			int topno = t.getTopNo();
			Query q = parser.parse(t.getTitle());
			launcher.query(topno, q);
			tracker.writeQuery(topno, q.toString());
			tracker.writeQueryTerms(topno, extractTerms(q));
			tracker.writeFeedbackTerms(topno, launcher.getTermMap(t.getTopNo(), 10));
		}
		reader.close();
		launcher.close();
		evaluator.evaluate(new String[]{ALL_METRICS}, false);
		tracker.writeMetrics(evaluator.getScores(ALL_QUERIES));
		tracker.commit();
	}
	
	private Analyzer newAnalyzer(Class<?> analyzer) 
			throws InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException{
		Constructor<?> ctor = analyzer.getConstructor(Version.class);
		return (Analyzer)ctor.newInstance(Version.LUCENE_46);
	}
	
	private Set<String> extractTerms(Query q){
		Set<Term> tset = new HashSet<Term>();
		Set<String> res = new HashSet<String>();
		q.extractTerms(tset);
		for(Term t : tset)
			res.add(t.text());
		return res;
	}
	
}
