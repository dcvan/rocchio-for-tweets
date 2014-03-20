package query;

import java.util.Comparator;
import java.util.Map;

public class ValueComparator implements Comparator<String>{
	private Map<String, Float> map;
	public ValueComparator(Map<String, Float> map){
		this.map = map;
	}
	
	@Override
	public int compare(String arg0, String arg1) {
		Float v0 = map.get(arg0);
		Float v1 = map.get(arg1);
		if(v0.equals(v1))
			return arg0.compareTo(arg1);
		return - v0.compareTo(v1);
	}
}
