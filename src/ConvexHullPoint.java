import org.json.JSONException;
import org.json.JSONObject;

public class ConvexHullPoint implements Jsonifiable{
	private double latitude;
	private double longitude;
	private long marker_id;
	private long old_marker_id = 1;
	private String nice_name;

	public ConvexHullPoint(double latitude, double longitude, long marker_id, String nice_name) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.marker_id = marker_id;
		this.setNice_name(nice_name);
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

	public long getOld_marker_id() {
		return old_marker_id;
	}

	public void setOld_marker_id(long old_marker_id) {
		this.old_marker_id = old_marker_id;
	}

	public String getNice_name() {
		return nice_name;
	}

	public void setNice_name(String nice_name) {
		this.nice_name = nice_name;
	}
}
