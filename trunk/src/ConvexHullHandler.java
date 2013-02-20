import java.util.HashMap;

import org.json.JSONObject;


public class ConvexHullHandler implements Jsonifiable{
	private long group_id;
	private HashMap<String, ConvexHullPoint> pointList;

	public ConvexHullHandler(long group_id, HashMap<String, ConvexHullPoint> pointList){
		this.group_id = group_id;
		this.pointList = pointList;
	}
	
	public void savePoints(){
		
	}

	@Override
	public JSONObject toJson() {
		return null;
	}
}
