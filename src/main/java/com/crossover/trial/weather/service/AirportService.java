package com.crossover.trial.weather.service;

import com.crossover.trial.weather.DataPoint;
import com.crossover.trial.weather.DataPointType;
import com.crossover.trial.weather.WeatherException;
import com.crossover.trial.weather.endpoints.RestWeatherQueryEndpoint;
import com.crossover.trial.weather.representations.AirportData;
import com.crossover.trial.weather.representations.AtmosphericInformation;
import com.sun.tools.internal.ws.wscompile.Options;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.NoSuchElementException;

/**
 * Created by thiago-rs on 12/30/15.
 */
public class AirportService {

    private static AirportService instance;

    private AirportService() {
    }

    public static AirportService getInstance() {
        if(instance == null) {
            instance = new AirportService();
        }
        return instance;
    }

    private WeatherService weatherService = WeatherService.getInstance();

    /**
     * Update the airports weather data with the collected data.
     *
     * @param iataCode the 3 letter IATA code
     * @param pointType the point type {@link DataPointType}
     * @param dp a datapoint object holding pointType data
     *
     * @throws WeatherException if the update can not be completed
     */
    public void addDataPoint(String iataCode, String pointType, DataPoint dp) throws NoSuchElementException, WeatherException {

        final int airportDataIdx = weatherService.getAirportDataIdx(iataCode)
                .orElseThrow(() -> new NoSuchElementException());

        final AtmosphericInformation ai = weatherService.getAtmosphericInformation().get(airportDataIdx);
        updateAtmosphericInformation(ai, pointType, dp);
    }

    /**
     * update atmospheric information with the given data point for the given point type
     *
     * @param ai the atmospheric information object to update
     * @param pointType the data point type as a string
     * @param dp the actual data point
     */
    public void updateAtmosphericInformation(AtmosphericInformation ai, String pointType, DataPoint dp) throws WeatherException { //CR: Exception is never thrown
        final DataPointType dptype = DataPointType.valueOf(pointType.toUpperCase());
        final long currentTimeMillis = System.currentTimeMillis();

        switch (dptype){
            case WIND:
                if (dp.getMean() >= 0){
                    ai.setWind(dp);
                    ai.setLastUpdateTime(currentTimeMillis);}
                break;
            case TEMPERATURE:
                if (dp.getMean() >= -50 && dp.getMean() < 100){
                    ai.setTemperature(dp);
                    ai.setLastUpdateTime(currentTimeMillis);}
                break;
            case HUMIDTY:
                if (dp.getMean() >= 0 && dp.getMean() < 100){
                    ai.setHumidity(dp);
                    ai.setLastUpdateTime(currentTimeMillis);}
                break;
            case PRESSURE:
                if (dp.getMean() >= 650 && dp.getMean() < 800){
                    ai.setPressure(dp);
                    ai.setLastUpdateTime(currentTimeMillis);}
                break;
            case CLOUDCOVER:
                if (dp.getMean() >= 0 && dp.getMean() < 100){
                    ai.setCloudCover(dp);
                    ai.setLastUpdateTime(currentTimeMillis);}
                break;
            case PRECIPITATION:
                if (dp.getMean() >=0 && dp.getMean() < 100){
                    ai.setPrecipitation(dp);
                    ai.setLastUpdateTime(currentTimeMillis);
                }
                break;
        }

        if(ai.getLastUpdateTime() < currentTimeMillis)
            throw new WeatherException("couldn't update atmospheric data"); //CR: Should't throw WeatherException?
    }

    /**
     * Add a new known airport to our list.
     *
     * @param iataCode 3 letter code
     * @param latitude in degrees
     * @param longitude in degrees
     *
     * @return the added airport
     */
    public AirportData addAirport(String iataCode, double latitude, double longitude) {
        final AirportData ad = new AirportData();
        weatherService.getAirportData().add(ad);

        final AtmosphericInformation ai = new AtmosphericInformation();
        weatherService.getAtmosphericInformation().add(ai);
        ad.setIata(iataCode);
        ad.setLatitude(latitude);
        ad.setLatitude(longitude);
        return ad;
    }


}
