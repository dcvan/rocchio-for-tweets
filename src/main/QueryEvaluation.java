package main;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;

import common.exception.FileExistsException;
import common.exception.InstanceExistsException;
import common.exception.InvalidParameterException;
import common.exception.TweetSearchEvaluatorException;
import common.exception.WrongFileTypeException;
import eval.TweetSearchEvaluator;
import query.TweetQueryLauncher;
import query.expansion.TermCollector;
import query.topic.Topic;
import query.topic.TopicReader;

public class QueryEvaluation {

	public static void main(String[] args) 
			throws InstanceExistsException, CorruptIndexException, FileExistsException, IOException, ParseException, WrongFileTypeException, InvalidParameterException, TweetSearchEvaluatorException, org.apache.lucene.queryparser.classic.ParseException {
		if(args.length < 4){
			System.err.println("<Usage> java QueryEvaluation <topic file> <index dir> <qrel file> <result file>");
			System.exit(1);
		}
		
		String topics = args[0], indexDir = args[1], qrel = args[2], result = args[3];
		
		QueryParser parser = new QueryParser(Version.LUCENE_46, "text",
				new EnglishAnalyzer(Version.LUCENE_46));
		TopicReader reader = new TopicReader(topics);
		TweetQueryLauncher launcher = new TweetQueryLauncher(indexDir, result);
		while(reader.hasNext()){
			Topic t = reader.next();
			launcher.query(t.getTopNo(), parser.parse(t.getTitle()));
		}
		reader.close();
		launcher.close();
		
		Set<String> commonTerms = new HashSet<String>();
		commonTerms.add("http");
		commonTerms.add("bit.li");
		commonTerms.add("new");
		commonTerms.add("like");
		commonTerms.add("good");
		commonTerms.add("from");
		commonTerms.add("your");
		commonTerms.add("you");
		
		reader = new TopicReader(topics);
		String result2 = "/Users/dc/git/rocchio-for-tweets/test-collection/result.txt.4";
		Map<Integer, TermCollector> colMap = launcher.getAllCollector();
		launcher = new TweetQueryLauncher(indexDir, result2);
		TweetSearchEvaluator evaluator = new TweetSearchEvaluator(qrel, result2);
		while(reader.hasNext()){
			Topic t = reader.next();
			int topNum = t.getTopNo();
			Set<String> termSet = colMap.get(topNum).getTerms(10).keySet();
			StringBuilder queryBuilder = new StringBuilder(t.getTitle() + ' ');
			for(String term : termSet){
				if(commonTerms.contains(term))
					queryBuilder.append(term + "^0.5 ");
				else if(term.length() > 9)
					queryBuilder.append(term + "^1.5 ");
				else
					queryBuilder.append(term + ' ');
			}
			Query q = parser.parse(queryBuilder.toString());
			System.out.println(q);
			launcher.query(topNum, q);
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
