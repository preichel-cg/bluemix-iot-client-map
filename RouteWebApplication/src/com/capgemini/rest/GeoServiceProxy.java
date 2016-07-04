package com.capgemini.rest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.paho.client.mqttv3.MqttClient;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;

public class GeoServiceProxy {

	private static final Logger LOG = Logger.getLogger(GeoServiceProxy.class.getName());
	private static final ResponseHandler<Void> RESPONSE_HANDLER = new PrintResponesToConsoleHandler();
	
	private GeospatialAnalytics environment;
	
	public GeoServiceProxy(){
		String vcap = System.getenv("VCAP_SERVICES");
		Gson gson = new Gson();
		JsonReader reader = new JsonReader(new StringReader(vcap));
		reader.setLenient(true);
		VCAP_Services env = gson.fromJson(reader, VCAP_Services.class);
		environment = env.geospatialAnalytics[0];
	}
	
	public GeoServiceProxy(VCAP_Services vcap){
		environment = vcap.geospatialAnalytics[0];
	}
	
	public void start() throws Exception {
		// Vorbereitung des Headers
		HttpClient client = createHttpClient();
		HttpPut request = new HttpPut();
		request.setURI(createUri(environment.credentials.start_path));
		createHeader(request);
		
		GeoMqtt geomqtt = new GeoMqtt();
		geomqtt.mqtt_client_id_input = "a:0yngjl:geoInput" + (int) Math.floor(Math.random() * 1000);
		geomqtt.mqtt_client_id_notify = "a:0yngjl:geoNotify" + (int) Math.floor(Math.random() * 1000);
		geomqtt.mqtt_uid ="a-0yngjl-fn8uegycq1";
		geomqtt.mqtt_pw = "nSbn2DYpS6qTh7Is+B";
		geomqtt.mqtt_uri = "0yngjl.messaging.internetofthings.ibmcloud.com:1883";
		geomqtt.mqtt_input_topics = "iot-2/type/RaspberryPi/id/aca21322819c/evt/location/fmt/json";
		geomqtt.mqtt_notify_topic = "iot-2/type/alerts/id/geospatialAlerts/cmd/geoAlert/fmt/json";
		geomqtt.device_id_attr_name = "vin";
		geomqtt.latitude_attr_name = "latitude";
		geomqtt.longitude_attr_name = "longtitude";

		request.setEntity(new StringEntity(new Gson().toJson(geomqtt)));

		LOG.info("client id notify=" + geomqtt.mqtt_client_id_notify);
		
		client.execute(request, RESPONSE_HANDLER);

	}
	
	public void callback() throws Exception{
		MqttClient client = new MqttClient("0yngjl.messaging.internetofthings.ibmcloud.com:1883", "a:0yngjl:geoNotify42");
		
	}

	public void status(GeospatialAnalytics evironment) throws Exception {
		HttpClient client = createHttpClient();
		HttpGet request = new HttpGet();
		request.setURI(createUri(evironment.credentials.status_path));
		createHeader(request);

		client.execute(request, RESPONSE_HANDLER);

	}

	public void stop(GeospatialAnalytics environment) throws Exception {
		HttpClient client = createHttpClient();
		HttpPut request = new HttpPut();
		request.setURI(createUri(environment.credentials.stop_path));
		createHeader(request);

		client.execute(request, RESPONSE_HANDLER);
	}

	public void addRegion(String regionName, String latitude, String longtitude, String radius) throws Exception{
		LOG.info(regionName + "=" + latitude + "/" + longtitude + "/" + radius);
		JsonObject parameter = new JsonObject();
		JsonObject region = new JsonObject();

		region.add("region_type", new JsonPrimitive("regular"));
		region.add("name", new JsonPrimitive(regionName));
		region.add("notifyOnEntry", new JsonPrimitive("true"));
		region.add("notifyOnExit", new JsonPrimitive("true"));
		region.add("minimumDwellTime", new JsonPrimitive("0"));
		region.add("timeout", new JsonPrimitive("0"));
		region.add("center_latitude", new JsonPrimitive(latitude));
		region.add("center_longitude", new JsonPrimitive(longtitude));
		region.add("number_of_sides", new JsonPrimitive("32"));
		region.add("distance_to_vertices", new JsonPrimitive(radius));
		
		parameter.add("regions", new JsonArray());
		parameter.get("regions").getAsJsonArray().add(region);
		
		HttpClient client = createHttpClient();
		HttpPut request = new HttpPut();
		request.setURI(createUri(environment.credentials.add_region_path));
		createHeader(request);
		
		request.setEntity(new StringEntity(new Gson().toJson(parameter)));

		client.execute(request, RESPONSE_HANDLER);

	}
	
	
	private URI createUri(String path) throws Exception {
		URIBuilder uriBuilder = new URIBuilder();
		uriBuilder.setHost(environment.credentials.geo_host);
		uriBuilder.setPort(Integer.parseInt(environment.credentials.geo_port));
		uriBuilder.setPath(path);
		uriBuilder.setScheme("https");
		return uriBuilder.build();
	}

	private void createHeader(HttpRequest request) throws Exception {
		request.addHeader("Content-Type", "application/json");
		
	}
	
	private HttpClient createHttpClient(){
		CredentialsProvider provider = new BasicCredentialsProvider();
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(environment.credentials.userid, environment.credentials.password);
		provider.setCredentials(AuthScope.ANY, credentials);
		HttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
		return client;
	}

	public static void main(String... args) throws Exception {
		URL fileURL = ClassLoader.getSystemClassLoader().getResource("test.json");
		JsonReader reader = new JsonReader(new InputStreamReader(fileURL.openStream()));
		reader.setLenient(true);

		Gson gson = new Gson();
		VCAP_Services services = gson.fromJson(reader, VCAP_Services.class);

		GeoServiceProxy geo = new GeoServiceProxy(services);
		
		geo.stop(services.geospatialAnalytics[0]);
		geo.start();
//		geo.addRegion( "testRegion3", "51.530839939713594", "-0.10346981440044545","5000");
//		geo.status(services.geospatialAnalytics[0]);
		
	}

	
	public static class PrintResponesToConsoleHandler implements ResponseHandler<Void> {

		@Override
		public Void handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
			LOG.info(String.valueOf(response.getStatusLine().getStatusCode()));
			LOG.info(response.getStatusLine().getReasonPhrase());
			LOG.info(IOUtils.toString(response.getEntity().getContent()));
			return null;
		}
		
	}
	
	
	public static class VCAP_Services {

		@SerializedName("cloudantNoSQLDB")
		private CloudantNoSQLDB[] cloudantNoSQLDB;
		@SerializedName("Geospatial Analytics")
		private GeospatialAnalytics[] geospatialAnalytics;

	}

	public static class CloudantNoSQLDB {

		private String name;
		private String label;
		private String plan;
		@SerializedName("credentials")
		private Credentials credentials;

	}

	public static class GeospatialAnalytics {

		private String name;
		private String label;
		private String plan;
		@SerializedName("credentials")
		private GeoCredentials credentials;

	}

	public static class Credentials {

		private String password;
		private String username;
		private String host;
		private String port;
		private String url;

	}

	public static class GeoCredentials {

		private String password;
		private String geo_host;
		private String dashboard_path;
		private String stop_path;
		private String geo_port;
		private String remove_region_path;
		private String restart_path;
		private String start_path;
		private String add_region_path;
		private String userid;
		private String status_path;
	}

	public static class GeoMqtt {
		private String mqtt_uid;
		private String mqtt_pw;
		private String mqtt_uri;
		private String mqtt_input_topics;
		private String mqtt_notify_topic;
		private String device_id_attr_name;
		private String latitude_attr_name;
		private String longitude_attr_name;
		private String mqtt_client_id_input;
		private String mqtt_client_id_notify;
	}

}
