package com.crossover.trial.weather.service;

import com.crossover.trial.weather.representations.AirportData;
import com.crossover.trial.weather.representations.AtmosphericInformation;

import javax.inject.Named;
import java.util.*;

/**
 * Created by thiago-rs on 12/30/15.
 */
public class WeatherService {

    private static WeatherService instance;

    private WeatherService() {
    }

    public static WeatherService getInstance() {
        if(instance == null) {
            instance = new WeatherService();
        }
        return instance;
    }

    /** earth radius in KM */
    public static final double R = 6372.8;


    /** all known airports */
    private List<AirportData> airportData = new ArrayList<>(); //CR: Controller/Endpoint Should not keep state

    /** atmospheric information for each airport, idx corresponds with airportData */
    private  List<AtmosphericInformation> atmosphericInformation = new LinkedList<>();

    /**
     * Internal performance counter to better understand most requested information, this map can be improved but
     * for now provides the basis for future performance optimizations. Due to the stateless deployment architecture
     * we don't want to write this to disk, but will pull it off using a REST request and aggregate with other
     * performance metrics {@link #ping()}
     */
    private  Map<AirportData, Integer> requestFrequency = new HashMap<AirportData, Integer>(); //CR: Controller/Endpoint Should not keep state

    private  Map<Double, Integer> radiusFreq = new HashMap<Double, Integer>(); //CR: Controller/Endpoint Should not keep state

    public  List<AirportData> getAirportData() {
        return airportData;
    }

    public  List<AtmosphericInformation> getAtmosphericInformation() {
        return atmosphericInformation;
    }

    public  Map<AirportData, Integer> getRequestFrequency() {
        return requestFrequency;
    }

    public  Map<Double, Integer> getRadiusFreq() {
        return radiusFreq;
    }

    /**
     * Records information about how often requests are made
     *
     * @param iata an iata code
     * @param radius query radius
     */
    public void updateRequestFrequency(String iata, Double radius) {
        AirportData airportData = findAirportData(iata);
        getRequestFrequency().put(airportData, getRequestFrequency().getOrDefault(airportData, 0) + 1);
        getRadiusFreq().put(radius, getRadiusFreq().getOrDefault(radius, 0));
    }

    /**
     * Given an iataCode find the airport data
     *
     * @param iataCode as a string
     * @return airport data or null if not found
     */
    public AirportData findAirportData(String iataCode) {
        return getAirportData().stream()
                .filter(ap -> ap.getIata().equals(iataCode))
                .findFirst().orElse(null);
    }

    /**
     * Given an iataCode find the airport data
     *
     * @param iataCode as a string
     * @return airport data or null if not found
     */
    public  Optional<Integer> getAirportDataIdx(String iataCode) {
        final AirportData ad = findAirportData(iataCode);
        final int index = getAirportData().indexOf(ad);
        return index > -1?Optional.of(index):Optional.empty();
    }

    /**
     * Haversine distance between two airports.
     *
     * @param ad1 airport 1
     * @param ad2 airport 2
     * @return the distance in KM
     */
    public double calculateDistance(AirportData ad1, AirportData ad2) {
        double deltaLat = Math.toRadians(ad2.getLatitude() - ad1.getLatitude());
        double deltaLon = Math.toRadians(ad2.getLongitude() - ad1.getLongitude());
        double a =  Math.pow(Math.sin(deltaLat / 2), 2) + Math.pow(Math.sin(deltaLon / 2), 2)
                * Math.cos(ad1.getLatitude()) * Math.cos(ad2.getLatitude());
        double c = 2 * Math.asin(Math.sqrt(a));
        return R * c;
    }

}
