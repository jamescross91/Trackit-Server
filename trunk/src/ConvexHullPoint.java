import org.json.JSONException;
import org.json.JSONObject;

public class ConvexHullPoint implements Jsonifiable{
	private double latitude;
	private double longitude;
	private long marker_id;

	public ConvexHullPoint(double latitude, double longitude, long marker_id) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.marker_id = marker_id;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public long getMarker_id() {
		return marker_id;
	}

	public void setMarker_id(long marker_id) {
		this.marker_id = marker_id;
	}

	@Override
	public JSONObject toJson() {
		JSONObject object = new JSONObject();
		
		try {
			object.put("lng", this.longitude);
			object.put("marker_id", this.marker_id);
			object.put("lat", this.latitude);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return object;
	}
}
