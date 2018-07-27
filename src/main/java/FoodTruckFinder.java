import java.io.Console;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class FoodTruckFinder {

	public static void main(String[] args) {		
		Date now = new Date();
		String day = new SimpleDateFormat("EEEE").format(now);
		String time = new SimpleDateFormat("HH:mm").format(now);
		
		try {
			String resource = "https://data.sfgov.org/resource/bbb8-hzi6.json";				
			JSONArray trucks = getTrucks(resource, day, time);			
			renderToConsole(trucks, System.console(), 10, 30);
		}
		catch (UnirestException e) {
			System.out.println(e);
		}
	}
	
	/**
	 * Get a sorted JSONArray of food trucks found at "resource" that are open
	 * on given day at time24
	 * 
	 * @param resource The url of the food truck API
	 * @param day Capitalized day of the week (eg: Monday, Tuesday ...)
	 * @param time24 24h format of time of day (eg: 09:04 )
	 * @return  JSONArray of matching trucks
	 * @throws UnirestException
	 */
	public static JSONArray getTrucks(String resource, String day, String time24) throws UnirestException {
		// API is query-able, let's abuse it.
		HttpResponse<JsonNode> asJson = Unirest
				.get(resource)
				.queryString("$select", "applicant,location")
				.queryString("$where", String.format( "start24<='%1$s' AND end24>'%1$s' AND dayofweekstr='%2$s'", time24, day))
				.queryString("$order", "applicant ASC")
				.asJson();
		return asJson.getBody().getArray();
	}
	
	
	/**
	 * Render to a console a list of JSONObject reprensenting food trucks.
	 * The json documents must contain a "applicant" and "location" field.
	 * 
	 * The output starts with a header and may be followed by 0 or more lines
	 * describing the trucks.
	 * 
	 * The output is rendered in two columns spanning at most 80 characters.

	 * The output is paginated. Pages are navigated forward by reading a newline from 
	 * the console input stream.
	 *  
	 * Example: 
	 * 
	 * NAME                            ADDRESS                                         
	 * Alfaro Truck                    306 VALENCIA ST                                 
     * Athena SF Gyro                  10 SOUTH VAN NESS AVE   
	 * Press <Enter> for more.
	 *  
	 * 
	 * @param trucks  JSONArray of JSONObject for trucks
	 * @param console  Where to render
	 * @param pageSize how many trucks to render per page. Must be > 0.
	 * @param colwidth width of the first column, must be less or equal to 78.
	 */
	public static void renderToConsole(JSONArray trucks, Console console, int pageSize, int colwidth) {
		if (colwidth > 78) {
			throw new IllegalArgumentException("colwidth must be <= 78");
		}
		if (pageSize <= 0) {
			throw new IllegalArgumentException("pageSize must be > 0");
		}
		if (trucks == null || console == null) {
			return;
		}
		// pretty columns, on an 80 character wide page, with a 2-space gap between them.
		String layout = String.format("%%1$-%1$d.%1$ds  %%2$-%2$d.%2$ds\n", colwidth, 80 - 2 - colwidth); 

		console.format(layout, "NAME", "ADDRESS");
		Iterator<Object> truck = trucks.iterator();
		for (;;) {
			for (int i = 0; i < pageSize && truck.hasNext(); ++i) {
				JSONObject t = (JSONObject)truck.next(); 
				console.format(layout, t.get("applicant"), t.get("location"));
			}
			if (truck.hasNext()) {
				console.format("Press <Enter> for more.");
				console.flush();
				console.readLine();
			} else {
				break;
			}
		}
		console.flush();
	}
}
