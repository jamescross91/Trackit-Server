import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import snaq.db.ConnectionPool;

//Class implementing core database functionality.  Everything is static, this will never be instanced
public class DatabaseCore {
	private static ConnectionPool pool = null;
	private static Logger logger = Logger.getLogger(DatabaseCore.class);

	private static void connect() throws Exception {
		try {
			// Load the MySQL driver required to connect to the database server,
			// and register it
			@SuppressWarnings("rawtypes")
			Class c = Class.forName(ReadProperties.getProperty("dbdriver"));
			Driver dbDriver = (Driver) c.newInstance();
			DriverManager.registerDriver(dbDriver);

			// Build up a connection string object, e.g.
			// jdbc:mysql://localhost/dbname?
			String connectionString = (ReadProperties
					.getProperty("connectionprotocol")
					+ ReadProperties.getProperty("dbserver")
					+ "/"
					+ ReadProperties.getProperty("dbname") + "?");

			// Create a new connection pool
			pool = new ConnectionPool("dbpool", 0, 10, 100, 10,
					connectionString,
					ReadProperties.getProperty("locwriteuser"),
					ReadProperties.getProperty("locwritepass"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static List<HashMap<String, Object>> extractData(ResultSet result)
			throws SQLException {

		// Extracts the data from the ResultSet generated by an SQL expression,
		// and puts it into a list of hash maps, relating the column name
		// (string) to the value (object)
		ResultSetMetaData meta = result.getMetaData();
		int columns = meta.getColumnCount();
		List<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();

		while (result.next()) {
			HashMap<String, Object> row = new HashMap<String, Object>(columns);
			for (int i = 1; i <= columns; ++i) {
				row.put(meta.getColumnName(i), result.getObject(i));
			}
			list.add(row);
		}
		return list;
	}

	public static List<HashMap<String, Object>> executeSqlQuery(
			String statementString, LinkedHashMap<Object, String> data)
			throws Exception {
		ResultSet result = null;
		Connection con = null;

		// Faster, cacheable, and helps to prevent SQL injection attacks vs a
		// normal statement
		PreparedStatement statement = null;
		List<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();

		if (pool == null)
			connect();

		try {
			con = pool.getConnection(Long.parseLong(ReadProperties
					.getProperty("pooltimeout")));
			if (con != null) {
				// Create an SQL statement and attempt to execute it against the
				// database
				statement = con.prepareStatement(statementString);

				// Iterate over the map containing datatype:data. Switch on
				// datatype and call the relavent function
				// To insert this data into the prepared statement
				int index = 1;
				for (Entry<Object, String> entry : data.entrySet()) {

					// Switch on the datatype, and then verify it
					switch (entry.getValue()) {
					case "string": {
						String datatype = entry.getKey().getClass().getName();
						if (datatype != "java.lang.String") {
							logger.error("Datatype of data: "
									+ entry.getValue()
									+ " expected string, found: " + datatype);
							throw (new Exception(
									"Invalid data type in SQL parameter"));
						}
						statement.setString(index, (String) entry.getKey());
						break;
					}

					case "int": {
						String datatype = entry.getValue().getClass().getName();
						if (datatype != "java.lang.Integer") {
							logger.error("Datatype of data: "
									+ entry.getValue()
									+ " expected int, found: " + datatype);
							throw (new Exception(
									"Invalid data type in SQL parameter"));
						}
						statement.setString(index, (String) entry.getKey());
						break;
					}
					}
					index++;
				}

				logger.info("Executing: " + statement.toString());
				result = statement.executeQuery();

				list = extractData(result);

			} else {
				logger.error("Critial error: unable to get access to the database because the pool was saturated with requests!");
			}

		} catch (Exception e) {
			logger.error("Error executing SQL statement: " + statement
					+ " on the database\n");
			e.printStackTrace();
		} finally {
			// Do this in a finally block so we release the resource even if
			// there is an exception!
			result.close();
			statement.close();
			con.close();
		}
		return list;
	}

	public static boolean executeSqlUpdate(String statementString,
			LinkedHashMap<Object, String> data) throws Exception {
		// Attempts to execute an insert statement against the database using a
		// connection resource extracted from the pool
		Connection con = null;
		// Faster, cacheable, and helps to prevent SQL injection attacks vs a
		// normal statement
		PreparedStatement statement = null;

		if (pool == null)
			connect();

		try {
			con = pool.getConnection(Long.parseLong(ReadProperties
					.getProperty("pooltimeout")));
			if (con != null) {

				statement = con.prepareStatement(statementString);

				// Iterate over the map containing datatype:data. Switch on
				// datatype and call the relavent function
				// To insert this data into the prepared statement
				int index = 1;
				for (Entry<Object, String> entry : data.entrySet()) {

					// Switch on the datatype, and then verify it
					switch (entry.getValue()) {
					case "string": {
						String datatype = entry.getKey().getClass().getName();
						if (datatype != "java.lang.String") {
							logger.error("Datatype of data: "
									+ entry.getValue()
									+ " expected string, found: " + datatype);
							throw (new Exception(
									"Invalid data type in SQL parameter"));
						}
						statement.setString(index, (String) entry.getKey());
						break;
					}
					
					case "double": {
						String datatype = entry.getKey().getClass().getName();
						if (datatype != "java.lang.Double") {
							logger.error("Datatype of data: "
									+ entry.getValue()
									+ " expected double, found: " + datatype);
							throw (new Exception(
									"Invalid data type in SQL parameter"));
						}
						statement.setDouble(index, (Double) entry.getKey());
						break;
					}
					
					case "boolean": {
						String datatype = entry.getKey().getClass().getName();
						if (datatype != "java.lang.Boolean") {
							logger.error("Datatype of data: "
									+ entry.getValue()
									+ " expected double, found: " + datatype);
							throw (new Exception(
									"Invalid data type in SQL parameter"));
						}
						statement.setBoolean(index, (Boolean) entry.getKey());
						break;
					}

					case "int": {
						String datatype = entry.getKey().getClass().getName();
						if (datatype != "java.lang.Integer") {
							logger.error("Datatype of data: "
									+ entry.getValue()
									+ " expected int, found: " + datatype);
							throw (new Exception(
									"Invalid data type in SQL parameter"));
						}
						statement.setInt(index, (Integer) entry.getKey());
						break;
					}
					}
					index++;
				}

				logger.info("Executing: " + statement.toString());
				statement.executeUpdate();
			} else {
				logger.error("Critial error: unable to get access to the database because the pool was saturated with requests!");
				return false;
			}
		} catch (SQLException e) {
			logger.error("Error inserting data into the database, failed with: "
					+ e.getMessage());
			return false;
		} finally {
			// Do this in a finally block so we release the resource even if
			// there is an exception!
			statement.close();
			con.close();
		}
		return true;
	}
}