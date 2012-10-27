//Class containing common functionality for applications wishing to connect to the system externally
public class ExtConnect {
	// Authenticate
	// Login
	// Execute SQL - remember we will need to handle both querys and inserts
	// here
	// Privs will need to be changed in DatabaseCore. Try mapping from a class
	// to a username/password - which will in turn be configured in MySQL

	public boolean login(String username, String password) {
		// Perform an SQL query against the database to check and if the string
		// username exists, and if it does
		// get the salted hash of its password

		String sqlString = "SELECT * FROM parent_details WHERE username = "
				+ username;
		try {
			DatabaseCore.executeSqlQuery(sqlString);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	}

	public boolean authenticateDevice(String username, double deviceID) {
		// Check that for the parents username provided, this is a valid device
		// ID

		// Could also check to see when this device ID was last authenticated
		// with a username and password
		// and if a timeout is period is exceeded, request these details again
		// from the device

		return true;
	}
}
