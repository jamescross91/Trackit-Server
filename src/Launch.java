import org.restlet.Component;
import org.restlet.Server;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.util.Series;

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

		//Create a new HTTPS server listening on port 8182.
		Server server = component.getServers().add(Protocol.HTTPS, 2610);
		Series<Parameter> params = server.getContext().getParameters(); 
		
		params.add("sslContextFactory", "org.restlet.engine.security.DefaultSslContextFactory");
		params.add("keyStorePath", "/lib/server.jks");
		params.add("keystorePassword", "password");
		params.add("keyPassword", "password");
		params.add("keystoreType", "JKS");

		//Attach the sample application.
		component.getDefaultHost().attach("/device", new LocationApplication());

		//Start the component.
		component.start();
	}
}
