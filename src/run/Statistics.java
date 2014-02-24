package run;

import java.util.HashMap;
import java.util.Map;

public class Statistics {
	private String name;
	private long timestamp;
	private String analyzer;
	private Map<Integer, Feedback> feedbacks;
	private Map<String, Double> metrics;
	
	public Statistics(){
		feedbacks = new HashMap<Integer, Feedback>();
		metrics = new HashMap<String, Double>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getAnalyzer() {
		return analyzer;
	}

	public void setAnalyzer(String analyzer) {
		this.analyzer = analyzer;
	}

	public Map<Integer, Feedback> getFeedbacks() {
		return feedbacks;
	}
	
	public Feedback getFeedback(Integer topno){
		return feedbacks.get(topno);
	}

	public void addFeedback(Integer topno, Feedback f) {
		feedbacks.put(topno, f);
	}

	public Map<String, Double> getMetrics() {
		return metrics;
	}

	public void addMetric(String name, Double value) {
		metrics.put(name, value);
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("name: ").append(name)
			.append("\ntimestamp: ").append(timestamp)
			.append("\nanalyzer: ").append(analyzer)
			.append("\nmetrics: ").append(metrics)
			.append("\nfeedbacks: \n");
		
		for(Integer topno : feedbacks.keySet()){
			sb.append(topno).append(": \n").append(feedbacks.get(topno));
		}
		
		return sb.toString();
	}
}
