import java.awt.geom.Point2D;

import org.json.JSONException;
import org.json.JSONObject;

import com.jhlabs.map.proj.MercatorProjection;

public class ConvexHullPoint implements Jsonifiable,
		Comparable<ConvexHullPoint>, Cloneable {
	private double latitude;
	private double longitude;
	private long marker_id;
	private long old_marker_id = 1;
	private String nice_name;
	private int group_id;
	private double cartesianX;
	private double cartesianY;

	public ConvexHullPoint(double latitude, double longitude, long marker_id,
			String nice_name) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.marker_id = marker_id;
		this.setNice_name(nice_name);

		// Get a projection
		MercatorProjection projection = new MercatorProjection();

		// Project the points latitude and longitude into cartesian space
		Point2D.Double cartesian = projection.project(longitude, latitude,
				new Point2D.Double());

		this.cartesianX = cartesian.x;
		this.cartesianY = cartesian.y;
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
			object.put("nicename", this.nice_name);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return object;
	}

	// Sort on the x-coordinate of the cartesian point representation
	public int compareTo(ConvexHullPoint testPoint) {
		return (int) (this.cartesianX - testPoint.getCartesianX());
	}
	
	public ConvexHullPoint clone() {
		try {
			return (ConvexHullPoint) super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
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

	public int getGroup_id() {
		return group_id;
	}

	public void setGroup_id(int group_id) {
		this.group_id = group_id;
	}

	public double getCartesianX() {
		return cartesianX;
	}

	public void setCartesianX(double cartesianX) {
		this.cartesianX = cartesianX;
	}

	public double getCartesianY() {
		return cartesianY;
	}

	public void setCartesianY(double cartesianY) {
		this.cartesianY = cartesianY;
	}
}
