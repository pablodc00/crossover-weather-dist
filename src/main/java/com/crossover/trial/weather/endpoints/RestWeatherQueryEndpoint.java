package com.crossover.trial.weather.endpoints;

import com.crossover.trial.weather.representations.AirportData;
import com.crossover.trial.weather.representations.AtmosphericInformation;
import com.crossover.trial.weather.service.WeatherService;
import com.google.gson.Gson;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The Weather App REST endpoint allows clients to query, update and check health stats. Currently, all data is
 * held in memory. The end point deploys to a single container
 *
 * @author code test administrator
 */
@Path("/query")
public class RestWeatherQueryEndpoint implements WeatherQueryEndpoint {

    public final static Logger LOGGER = Logger.getLogger("WeatherQuery"); //CR: Use same pattern for logger Names

    /** shared gson json to object factory */
    public static final Gson gson = new Gson(); //CR: narrow the visibility to this class only, public static too wide

    private WeatherService weatherService = WeatherService.getInstance();


    /**
     * Retrieve service health including total size of valid data points and request frequency information.
     *
     * @return health stats for the service as a string
     */
    @GET
    @Path("/ping")
    public String ping() {
        final Map<String, Object> retval = new HashMap<>(); //CR: change object to immutable using final

        int datasize = 0;
        for (AtmosphericInformation ai : weatherService.getAtmosphericInformation()) {
            // we only count recent readings
            if (ai.getCloudCover() != null
                || ai.getHumidity() != null
                || ai.getPressure() != null
                || ai.getPrecipitation() != null
                || ai.getTemperature() != null
                || ai.getWind() != null) {
                // updated in the last day
                if (ai.getLastUpdateTime() > System.currentTimeMillis() - 86400000) {
                    datasize++;
                }
            }
        }
        retval.put("datasize", datasize);

        final Map<String, Double> freq = new HashMap<>(); //CR: change object to immutable using final
        // fraction of queries
        for (AirportData data : weatherService.getAirportData()) {
            double frac = (double)weatherService. getRequestFrequency().getOrDefault(data, 0) / weatherService. getRequestFrequency().size(); //CR: avoid casting
            freq.put(data.getIata(), frac);
        }
        retval.put("iata_freq", freq);

        final int m = weatherService.getRadiusFreq().keySet().stream() //CR: change variable to immutable using final
                .max(Double::compare)
                .orElse(1000.0).intValue() + 1;

        final int[] hist = new int[m]; //CR: change variable to immutable using final
        for (Map.Entry<Double, Integer> e : weatherService.getRadiusFreq().entrySet()) {
            int i = e.getKey().intValue() % 10;
            hist[i] += e.getValue();
        }
        retval.put("radius_freq", hist);

        return gson.toJson(retval);
    }

    /**
     * Given a query in json format {'iata': CODE, 'radius': km} extracts the requested airport information and
     * return a list of matching atmosphere information.
     *
     * @param iata the iataCode
     * @param radiusString the radius in km
     *
     * @return a list of atmospheric information
     */
    @GET
    @Path("/weather/{iata}/{radius}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("iata") String iata, @PathParam("radius") String radiusString) {
        double radius = radiusString == null || radiusString.trim().isEmpty() ? 0 : Double.valueOf(radiusString);
        weatherService.updateRequestFrequency(iata, radius);

        List<AtmosphericInformation> retval = new ArrayList<>(); //CR: change object to immutable using final
        if (radius == 0) {
            int idx = weatherService.getAirportDataIdx(iata).orElse(-1); //CR: might fail on optional
            retval.add(weatherService.getAtmosphericInformation().get(idx));
        } else {
            AirportData ad = weatherService.findAirportData(iata);
            for (int i=0;i< weatherService.getAirportData().size(); i++){
                if (weatherService.calculateDistance(ad, weatherService.getAirportData().get(i)) <= radius){
                    AtmosphericInformation ai = weatherService.getAtmosphericInformation().get(i);
                    if (ai.getCloudCover() != null || ai.getHumidity() != null || ai.getPrecipitation() != null
                       || ai.getPressure() != null || ai.getTemperature() != null || ai.getWind() != null){
                        retval.add(ai);
                    }
                }
            }
        }
        return Response.status(Response.Status.OK).entity(retval).build();
    }

}
