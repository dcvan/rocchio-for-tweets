package main;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.util.Version;

import common.exception.FileExistsException;
import common.exception.InstanceExistsException;
import common.exception.InvalidParameterException;
import common.exception.TweetSearchEvaluatorException;
import common.exception.WrongFileTypeException;
import eval.TweetSearchEvaluator;
import analyzer.TweetAnalyzer;
import query.TweetQueryLauncher;
import query.topic.Topic;
import query.topic.TopicReader;

public class QueryEvaluation {

	public static void main(String[] args) 
			throws InstanceExistsException, CorruptIndexException, FileExistsException, IOException, ParseException, org.apache.lucene.queryParser.ParseException, WrongFileTypeException, InvalidParameterException, TweetSearchEvaluatorException {
		if(args.length < 4){
			System.err.println("<Usage> java QueryEvaluation <topic file> <index dir> <qrel file> <result file>");
			System.exit(1);
		}
		
		String topics = args[0], indexDir = args[1], qrel = args[2], result = args[3];
		
		QueryParser parser = new QueryParser(Version.LUCENE_36, "text", 
				new TweetAnalyzer());
		TopicReader reader = new TopicReader(topics);
		TweetQueryLauncher launcher = new TweetQueryLauncher(indexDir, result);
		TweetSearchEvaluator evaluator = new TweetSearchEvaluator(qrel, result);
		
		while(reader.hasNext()){
			Topic t = reader.next();
			launcher.query(t.getTopNo(), parser.parse(t.getTitle()));
		}
		
		reader.close();
		launcher.close();
		
		evaluator.evaluate(new String[]{
			"map", "P.30", "ndcg"
		}, false);
		
		Map<String, String> res = evaluator.getScores("all");
		for(String m : res.keySet()){
			System.out.println(m + "\t" + res.get(m));
		}
		
	}

}
