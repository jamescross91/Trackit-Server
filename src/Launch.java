import org.restlet.Component;
import org.restlet.data.Protocol;

public class Launch {
	public static void main(String[] args) throws Exception {

//		List<HashMap<String, Object>> results = DatabaseCore.executeSqlQuery("SELECT * FROM location_details" );
//
//		for(int i = 0; i < results.size(); i++){
//			HashMap<String, Object> thisMap = new HashMap<String, Object>();
//			thisMap = results.get(i);
//			
//			for(Map.Entry<String, Object> entry : thisMap.entrySet()){
//				System.out.println(entry.getKey() + "/" + entry.getValue());
//			}
//				
//		}	
		
		//Eventually its going to be better to have something that builds an SQL statement for me, instead of like this!!
		String sql = "INSERT INTO location_details VALUES (2, 1, '2010-06-21 13:28:17', 30, 10, 'gps', 2, 2, 1, 1, 1, 'guff', '4g', 2)";
		DatabaseCore.executeSqlUpdate(sql);
		
		//Create a new Component.
		Component component = new Component();

		//Create a new HTTP server listening on port 8182.
		component.getServers().add(Protocol.HTTP, 2610);

		//Attach the sample application.
		component.getDefaultHost().attach("/device", new LocationApplication());

		//Start the component.
		component.start();
	}
}
