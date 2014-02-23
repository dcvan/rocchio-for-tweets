package parse;

import java.util.HashMap;
import java.util.Map;

public class Tweet {
	private String docNo;
	private long datetime;
	private String user;
	private String text;
	
	public Tweet(){}
	public Tweet(String docNo, long dateTime, String user, String text){
		this.docNo = docNo;
		this.datetime = dateTime;
		this.user = user;
		this.text = text;
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
	
	public Map<String, Object> getAttrMap(){
		Map<String, Object> attrMap = new HashMap<String, Object>();
		attrMap.put("docno", docNo);
		attrMap.put("datetime", datetime);
		attrMap.put("user", user);
		attrMap.put("text", text);
		
		return attrMap;
	}
	
	public String toString(){
		return "DocNo: " + docNo + "\nDate Time: " + datetime + "\nUser: " + user + "\nText: " + text + "\n";  
	}
}
