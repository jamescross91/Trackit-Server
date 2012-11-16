import org.restlet.Component;
import org.restlet.data.Form;
import org.restlet.data.Protocol;
import org.restlet.representation.Representation;
import org.restlet.service.ConverterService;

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
		
		//System.out.println(newUser.login("password"));
		
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