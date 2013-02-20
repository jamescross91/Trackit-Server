public class ConvexHullPoint {
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
}
