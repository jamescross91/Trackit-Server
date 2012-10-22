import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

//Class holding TrackiT application properties
public class ReadProperties {

	private Properties trackitProps = null;
	
	private Properties loadProps(){
		 
		try{
			File propsFile = new File("TrackiT.properties");
			FileInputStream fileInput = new FileInputStream(propsFile);
			
			trackitProps.load(fileInput);

			fileInput.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		return trackitProps;	
	}
	
	//If !null check on trackit props

}
