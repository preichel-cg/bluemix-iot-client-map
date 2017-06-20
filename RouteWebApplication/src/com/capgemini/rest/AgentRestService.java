package com.capgemini.rest;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import com.capgemini.mqtt.AgentListener;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.iotf.client.AbstractClient;

/**
 * 
 * @author cmammado
 *
 */
// Specify the path to the REST-service
@Path("/")
public class AgentRestService {

	private static final String CLASS_NAME = AbstractClient.class.getName();
	private static final Logger LOG = Logger.getLogger(CLASS_NAME);

	/**
	 * This method will be called each time an emergency happens
	 * 
	 * @param msg
	 *            message which is sent to the rest service by clicking on the
	 *            map
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public void getEmergencyLocation(String msg) {
		JsonObject data = new JsonParser().parse(msg).getAsJsonObject();
		// Get the latitude of emergency location
		Double emergencyLatitude = data.get("latitude").getAsDouble();
		// Get the longitude of the emergency location
		Double emergencyLongitude = data.get("longitude").getAsDouble();

		// TODO practice 3: implement send emergency command to IoT
		data.addProperty("groupId", "simulator");
		data.addProperty("latitude", Double.toString(emergencyLatitude));
		data.addProperty("longitude", Double.toString(emergencyLongitude));

		// Publish a command via IoT to the hospital device with the emergency 
		LOG.info("publishing emergency command to hospital.");
		AgentListener.getAgent().getClient().publishCommand("hospital", "hospital1", "emergency", data, 0);
		LOG.info("done publishing emergency command.");
	}

}
