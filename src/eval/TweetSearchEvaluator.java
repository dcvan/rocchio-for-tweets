package eval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;

import common.exception.InstanceExistsException;
import common.exception.InvalidParameterException;
import common.exception.WrongFileTypeException;

public class TweetSearchEvaluator {
	private final static String TREC_EVAL = "trec_eval/trec_eval -q -m ";
	
	private static Set<String> metrics;
	static{
		//TODO add metrics to the set
	}
	private static TweetSearchEvaluator eval;
	
	private String qrel;
	private String result;
	
	public static void main(String[] args) 
			throws WrongFileTypeException, InstanceExistsException, IOException, InvalidParameterException{
		TweetSearchEvaluator eval = TweetSearchEvaluator.create("/home/dc/Documents/microblog11-qrels.txt", 
				"/home/dc/Documents/results.txt");
		
		System.out.println(eval.evaluate());
	}
	
	public static TweetSearchEvaluator create(String qpath, String rpath) 
			throws FileNotFoundException, WrongFileTypeException, InstanceExistsException{
		File q = new File(qpath);
		File r = new File(rpath);
		
		if(eval != null)
			throw new InstanceExistsException(TweetSearchEvaluator.class);
		if(!q.exists())
			throw new FileNotFoundException("Qrel file " + qpath + " not found.");
		if(!q.isFile())
			throw new WrongFileTypeException(qpath + " is not a text file");
		
		if(!r.exists())
			throw new FileNotFoundException("Result file " + qpath + " not found.");
		if(!r.isFile())
			throw new WrongFileTypeException(qpath + " is not a text file");
		
		return new TweetSearchEvaluator(qpath, rpath);
	}
	
	public String evaluate() 
			throws IOException, InvalidParameterException{
		return evaluate("all", "all");
	}
	
	public String evaluate(String metric) 
			throws IOException, InvalidParameterException{
		return evaluate(metric, "all");
	}
	
	public String evaluate(int topNo) 
			throws IOException, InvalidParameterException{
		return evaluate("all", String.valueOf(topNo));
	}
	
	//TODO change topNo to int
	public String evaluate(String metric, String topNo) 
			throws IOException, InvalidParameterException{
		if(metric == null || topNo == null)
			throw new InvalidParameterException("(" + metric + ", " + topNo + ")");
		StringBuilder cb = new StringBuilder();
		StringBuilder res = new StringBuilder();
		if("all".equals(metric))
			metric = "all_trec";
		cb.append(TREC_EVAL)
			.append(metric).append(' ')
			.append(qrel).append(' ')
			.append(result);
		
		Process p = Runtime.getRuntime().exec(cb.toString());
		String line;
		
		BufferedReader resReader = new BufferedReader(
				new InputStreamReader(p.getInputStream()));
		while((line = resReader.readLine()) != null){
			String[] fields = line.split("\t");
			if(topNo.equals(fields[1]))
				res.append(line).append('\n');
		}
		resReader.close();
			
		BufferedReader errReader = new BufferedReader(
				new InputStreamReader(p.getErrorStream()));
		line = null;
		while((line = errReader.readLine()) != null){
			res.append(line).append('\n');
		}
		errReader.close();
		
		return res.toString();
	}
	
	private TweetSearchEvaluator(String q, String r){
		qrel = q;
		result = r;
	} 
	

	
}
