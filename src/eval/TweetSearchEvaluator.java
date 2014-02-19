package eval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import common.exception.InstanceExistsException;
import common.exception.InvalidParameterException;
import common.exception.TweetSearchEvaluatorException;
import common.exception.WrongFileTypeException;

public class TweetSearchEvaluator {
	private final static String TREC_EVAL = "trec_eval/trec_eval";
	private final static String TOP_OP = "-q";
	private final static String METRIC_OP = "-m";
	
	private String qrel;
	private String result;
	private Map<String, Map<String, String>> scoreMap;
	
	//Tester
	public static void main(String[] args) 
			throws WrongFileTypeException, InstanceExistsException, IOException, InvalidParameterException, TweetSearchEvaluatorException{
		if(args.length < 2){
			System.err.println("<Usage> java TweetSearchEvaluator <qrel> <result>");
			System.exit(1);
		}
		
		TweetSearchEvaluator eval = new TweetSearchEvaluator(args[0], args[1]);

		eval.evaluate(new String[]{
			"map", "P.30", "ndcg"	
		}, false);
		
		Map<String, String> res = eval.getScores("all");
		for(String m : res.keySet()){
			System.out.println(m + "\t" + res.get(m));
		}
	}
	
	/**
	 * constructor 
	 * 
	 * @param q - qrel file path
	 * @param r - result file path
	 * @throws WrongFileTypeException 
	 * @throws InstanceExistsException 
	 * @throws FileNotFoundException 
	 */
	public TweetSearchEvaluator(String qpath, String rpath) throws WrongFileTypeException, InstanceExistsException, FileNotFoundException{
		File q = new File(qpath);
		File r = new File(rpath);
		
		if(!q.exists())
			throw new FileNotFoundException("Qrel file " + qpath + " not found.");
		if(!q.isFile())
			throw new WrongFileTypeException(qpath + " is not a text file");
		
		if(!r.exists())
			throw new FileNotFoundException("Result file " + rpath + " not found.");
		if(!r.isFile())
			throw new WrongFileTypeException(rpath + " is not a text file");
		
		qrel = qpath;
		result = rpath;
		scoreMap = new HashMap<String, Map<String, String>>();
	} 
	
	/**
	 * 
	 * @param metrics - a list of evaluation metrics 
	 * @param forEach - whether include evaluation for each query or not
	 * @throws InvalidParameterException
	 * @throws IOException
	 * @throws TweetSearchEvaluatorException
	 */
	public void evaluate(String[] metrics, boolean forEach) 
			throws InvalidParameterException, IOException, TweetSearchEvaluatorException{
		if(metrics == null)
			throw new InvalidParameterException("Parameters cannot be null");
		if(metrics.length == 0)
			throw new InvalidParameterException("Parameters cannot be empty");
		
		Set<String> mset = new HashSet<String>(Arrays.asList(metrics));
		
		StringBuilder cb = new StringBuilder(TREC_EVAL + ' ');
		if(forEach) cb.append(TOP_OP + ' ');
		for(String m : mset)
			cb.append(METRIC_OP + ' ' + m + ' ');
		cb.append(qrel + ' ' + result);
		
		Process p = Runtime.getRuntime().exec(cb.toString());
		BufferedReader resReader = new BufferedReader(
				new InputStreamReader(p.getInputStream()));
		BufferedReader errReader = new BufferedReader(
				new InputStreamReader(p.getErrorStream()));
		String line;
		while((line = resReader.readLine()) != null){
			String[] fs = line.split("\t");

			if(scoreMap.containsKey(fs[1])){
				scoreMap.get(fs[1]).put(fs[0], fs[2]);
			}else{
				Map<String, String> msmap = new HashMap<String, String>();
				msmap.put(fs[0], fs[2]);
				scoreMap.put(fs[1], msmap);
			}
		}
		
		StringBuilder errMsg = new StringBuilder();
		while((line = errReader.readLine()) != null)
			errMsg.append(line + '\n');
		if(errMsg.length() > 0)
			throw new TweetSearchEvaluatorException(errMsg.toString());
	}
	
	/**
	 * 
	 * @param topno - query number
	 * @return 
	 * @throws InvalidParameterException
	 */
	public Map<String, String> getScores(String topno) 
			throws InvalidParameterException{
		if(topno == null)
			throw new InvalidParameterException("Parameters cannot be null");
		return scoreMap.get(topno);
	}
	
}
