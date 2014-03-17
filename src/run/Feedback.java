package run;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Feedback {
	private String query;
	private Set<String> queryTerms;
	private Set<String> hashtags;
	private Map<String, Float> termScores;
	
	public Feedback(){
		queryTerms = new HashSet<String>();
		hashtags = new HashSet<String>();
		termScores = new HashMap<String, Float>();
	}
	
	public String getQuery(){
		return query;
	}
	
	public void setQuery(String query) {
		this.query = query;
	}
	
	public Set<String> getQueryTerms(){
		return new HashSet<String>(queryTerms);
	}
	
	public void setQueryTerms(Set<String> queryTerms){
		this.queryTerms = new HashSet<String>(queryTerms);
	}
	
	public void addQueryTerms(String t){
		queryTerms.add(t);
	}
	
	public Set<String> getHashtags(){
		return new HashSet<String>(hashtags);
	}
	
	public void setHashtags(Set<String> htags){
		this.hashtags = htags;
	}
	
	public void addHashtag(String htag){
		hashtags.add(htag);
	}
	
	public Map<String, Float> getTermScores() {
		return new HashMap<String, Float>(termScores);
	}
	
	public void setTermScores(Map<String, Float> termScores){
		this.termScores = new HashMap<String, Float>(termScores);
	}
	
	public void addTerm(String term, Float score) {
		termScores.put(term, score);
	}
	
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		return sb.append("query: (").append(query).append(")\n")
			.append("query terms: ").append(queryTerms).append('\n')
			.append("hashtags: ").append(hashtags).append('\n')
			.append("feedback terms: ").append(termScores).append('\n').toString();
	}
}
