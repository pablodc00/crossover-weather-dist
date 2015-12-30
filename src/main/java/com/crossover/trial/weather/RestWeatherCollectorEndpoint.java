package com.crossover.trial.weather;

import com.google.gson.Gson;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static com.crossover.trial.weather.RestWeatherQueryEndpoint.*;

/**
 * A REST implementation of the WeatherCollector API. Accessible only to airport weather collection
 * sites via secure VPN.
 *
 * @author code test administrator
 */

@Path("/collect")
public class RestWeatherCollectorEndpoint implements WeatherCollector {
    public final static Logger LOGGER = Logger.getLogger(RestWeatherCollectorEndpoint.class.getName());

    /** shared gson json to object factory */
    public final static Gson gson = new Gson(); //CR: narrow the visibility to this class only, public static too wide

    static {
        init();
    }

    @GET //CR: Could use the method HEAD with no response body once is lighter. HTTP Response code 200 should do it
    @Path("/ping")
    @Override
    public Response ping() {
        return Response.status(Response.Status.OK).entity("ready").build(); //CR: Specification defines 0 or 1 not ready
    }

    @POST
    @Path("/weather/{iata}/{pointType}") //CR: Post data on the URL is recorded in proxies, use request Body or @FormParam
    @Override
    public Response updateWeather(@PathParam("iata") String iataCode,
                                  @PathParam("pointType") String pointType,
                                  String datapointJson) {
        try {
            addDataPoint(iataCode, pointType, gson.fromJson(datapointJson, DataPoint.class));
        } catch (WeatherException e) {
            e.printStackTrace(); //CR: Log instead
        }
        return Response.status(Response.Status.OK).build(); //CR: Response code 201 for Post not 200, or 422 for client errors
    }

    @GET
    @Path("/airports")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public Response getAirports() {
        Set<String> retval = new HashSet<>();
        for (AirportData ad : airportData) {
            retval.add(ad.getIata());
        }
        return Response.status(Response.Status.OK).entity(retval).build();
    }

    @GET
    @Path("/airport/{iata}")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public Response getAirport(@PathParam("iata") String iata) {
        AirportData ad = findAirportData(iata);
        return Response.status(Response.Status.OK).entity(ad).build(); //CR: Should return 404 if ad is null
    }

    @POST
    @Path("/airport/{iata}/{lat}/{long}") //CR: Post data on the URL is recorded in proxies, use request Body or @FormParam
    @Override
    public Response addAirport(@PathParam("iata") String iata,
                               @PathParam("lat") String latString,
                               @PathParam("long") String longString) {
        addAirport(iata, Double.valueOf(latString), Double.valueOf(longString)); //CR: Unchecked conversions can raise exceptions
        return Response.status(Response.Status.OK).build(); //CR: Response code 201 for Post not 200, or 422 for client errors
    }

    @DELETE
    @Path("/airport/{iata}")
    @Override
    public Response deleteAirport(@PathParam("iata") String iata) {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    //
    // Internal support methods //CR: This is a controller these methods should be in a service class
    //

    /**
     * Update the airports weather data with the collected data.
     *
     * @param iataCode the 3 letter IATA code
     * @param pointType the point type {@link DataPointType}
     * @param dp a datapoint object holding pointType data
     *
     * @throws WeatherException if the update can not be completed
     */
    public void addDataPoint(String iataCode, String pointType, DataPoint dp) throws WeatherException {
        int airportDataIdx = getAirportDataIdx(iataCode); //CR: change variable to immutable using final
        AtmosphericInformation ai = atmosphericInformation.get(airportDataIdx);
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

        //CR: Use switch on DataPointType once is an Enum
        if (pointType.equalsIgnoreCase(DataPointType.WIND.name())) {
            if (dp.getMean() >= 0) {
                ai.setWind(dp);
                ai.setLastUpdateTime(System.currentTimeMillis());
                return;
            }
        }

        if (pointType.equalsIgnoreCase(DataPointType.TEMPERATURE.name())) {
            if (dp.getMean() >= -50 && dp.getMean() < 100) {
                ai.setTemperature(dp);
                ai.setLastUpdateTime(System.currentTimeMillis());
                return;
            }
        }

        if (pointType.equalsIgnoreCase(DataPointType.HUMIDTY.name())) {
            if (dp.getMean() >= 0 && dp.getMean() < 100) {
                ai.setHumidity(dp);
                ai.setLastUpdateTime(System.currentTimeMillis());
                return;
            }
        }

        if (pointType.equalsIgnoreCase(DataPointType.PRESSURE.name())) {
            if (dp.getMean() >= 650 && dp.getMean() < 800) {
                ai.setPressure(dp);
                ai.setLastUpdateTime(System.currentTimeMillis());
                return;
            }
        }

        if (pointType.equalsIgnoreCase(DataPointType.CLOUDCOVER.name())) {
            if (dp.getMean() >= 0 && dp.getMean() < 100) {
                ai.setCloudCover(dp);
                ai.setLastUpdateTime(System.currentTimeMillis());
                return;
            }
        }

        if (pointType.equalsIgnoreCase(DataPointType.PRECIPITATION.name())) {
            if (dp.getMean() >=0 && dp.getMean() < 100) {
                ai.setPrecipitation(dp);
                ai.setLastUpdateTime(System.currentTimeMillis());
                return;
            }
        }

        throw new IllegalStateException("couldn't update atmospheric data"); //CR: Should't throw WeatherException?
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
    public static AirportData addAirport(String iataCode, double latitude, double longitude) { //CR: narrow the visibility to this class only, public static too wide
        AirportData ad = new AirportData(); //CR: change variable to immutable using final or use Builder pattern
        airportData.add(ad);

        AtmosphericInformation ai = new AtmosphericInformation(); //CR: change object to immutable using final or use Builder pattern
        atmosphericInformation.add(ai);
        ad.setIata(iataCode);
        ad.setLatitude(latitude);
        ad.setLatitude(longitude);
        return ad;
    }

    /**
     * A dummy init method that loads hard coded data
     */
    protected static void init() {
        airportData.clear(); atmosphericInformation.clear(); requestFrequency.clear();
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("airports.dat");
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String l = null;

        try {
            while ( (l = br.readLine()) != null) {
                String[] split = l.split(",");
                addAirport(split[0],
                        Double.valueOf(split[1]),
                        Double.valueOf(split[2]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
