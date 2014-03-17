package parse;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Tweet {
	private String docNo;
	private long datetime;
	private String user;
	private String text;
	private Set<String> hashtags;
	
	public Tweet(){}
	public Tweet(String docNo, long dateTime, String user, String text){
		this(docNo, dateTime, user, text, new HashSet<String>());
	}
	
	public Tweet(String docNo, long dateTime, String user, String text, Set<String> hashtags){
		this.docNo = docNo;
		this.datetime = dateTime;
		this.user = user;
		this.text = text;
		this.hashtags = hashtags;
	}
	
	public String getDocNo() {
		return docNo;
	}
	public void setDocNo(String docNo) {
		this.docNo = docNo;
	}
	public long getDateTime() {
		return datetime;
	}
	public void setDateTime(long datetime) {
		this.datetime = datetime;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	
	public Set<String> getHashtags(){
		return new HashSet<String>(hashtags);
	}
	
	public void addHashtag(String hashtag){
		hashtags.add(hashtag);
	}
	
	public Map<String, Object> getAttrMap(){
		Map<String, Object> attrMap = new HashMap<String, Object>();
		attrMap.put("docno", docNo);
		attrMap.put("datetime", datetime);
		attrMap.put("user", user);
		attrMap.put("text", text);
		attrMap.put("hashtags", hashtags);
		
		return attrMap;
	}
	
	public String toString(){
		return "DocNo: " + docNo 
				+ "\nDate Time: " + datetime 
				+ "\nUser: " + user 
				+ "\nText: " + text 
				+ "\nHashtag: " + hashtags 
				+ "\n";  
	}
}
