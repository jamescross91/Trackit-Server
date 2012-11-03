import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.PreparedStatement;

import org.apache.log4j.Logger;

//Class containing common functionality for applications wishing to connect to the system externally
public class ExtConnect {
	// Authenticate
	// Login
	// Execute SQL - remember we will need to handle both querys and inserts
	// here
	// Privs will need to be changed in DatabaseCore. Try mapping from a class
	// to a username/password - which will in turn be configured in MySQL

	private static Logger logger = Logger.getLogger(DatabaseCore.class);

	public boolean authenticateDevice(String username, double deviceID,
			String authToken) {
		// Check that for the parents username provided, this is a valid device
		// ID

		String sqlString = "SELECT * FROM " + ReadProperties.getProperty("devicedetails") + "WHERE device_id = " + "\"" + deviceID + "\"";
		
		return true;
	}

	// Generate an authentication token, save it to the database, and return it
	private String generateAuth(double deviceID) {
		String token = generateToken();

		String sqlString = "UPDATE "
				+ ReadProperties.getProperty("devicedetails")
				+ "SET auth_token=" + token + "WHERE device_id = " + "\""
				+ deviceID + "\"";
		
		try {
			if(DatabaseCore.executeSqlUpdate(sqlString))
			{
				return token;
			}
		} catch (Exception e) {
			logger.error("Unable to write authentication token to the database for device id: " + deviceID);
			e.printStackTrace();		
		}
		

		return new String();
	}

	private String generateToken() {
		// Create a random authentication token to be used with the device, in
		// the same way we create a salt for passwords
		SecureRandom randomSalt = new SecureRandom();
		byte[] salt = new byte[Integer.parseInt(ReadProperties
				.getProperty("salt_bytes")) * 2];
		randomSalt.nextBytes(salt);

		BigInteger bigInt = new BigInteger(1, salt);
		String hex = bigInt.toString(16);
		int paddingLength = (salt.length * 2) - hex.length();
		if (paddingLength > 0)
			return String.format("%0" + paddingLength + "d", 0) + hex;
		else
			return hex;
	}
}
