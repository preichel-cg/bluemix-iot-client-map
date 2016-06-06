package com.capgemini.mqtt;

/**
 * This is a class for ambulance. Each ambulance has a vin (vehicle identification number), current location 
 *  and the state (free or not free)
 *  
 *  @author cmammado
 */
public class Ambulance {

	String vin;
	CurrentLocation currentLocation;
	Boolean isFree = true;

	public void setVin(String s) {
		vin = s;
	}

	public void setCurrentLocation(CurrentLocation l) {
		currentLocation = l;
	}

	public String getVin() {
		return vin;
	}

	public CurrentLocation getCurrentLocation() {
		return currentLocation;
	}

	public void setIsFree(Boolean b) {
		isFree = b;
	}

	public Boolean getIsFree() {
		return isFree;
	}

}
