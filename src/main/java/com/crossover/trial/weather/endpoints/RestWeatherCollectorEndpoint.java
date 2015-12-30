package com.crossover.trial.weather.endpoints;

import com.crossover.trial.weather.*;
import com.crossover.trial.weather.representations.AirportData;
import com.crossover.trial.weather.service.AirportService;
import com.crossover.trial.weather.service.WeatherService;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.glassfish.jersey.server.ContainerRequest;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A REST implementation of the WeatherCollector API. Accessible only to airport weather collection
 * sites via secure VPN.
 *
 * @author code test administrator
 */
@Path("/collect")
public class RestWeatherCollectorEndpoint implements WeatherCollector {
    public final static Logger LOGGER = Logger.getLogger(RestWeatherCollectorEndpoint.class.getName());

    @Inject
    private Provider<ContainerRequest> req;

    private AirportService airportService = AirportService.getInstance();

    private WeatherService weatherService = WeatherService.getInstance();

    /** shared gson json to object factory */
    private final Gson gson = new Gson();

    @HEAD
    @Path("/ping")
    @Override
    public Response ping() {
        return Response.status(Response.Status.OK).build();
    }

    @POST
    @Path("/weather/{iata}/{pointType}") //CR: Post data on the URL is recorded in proxies, use request Body or @FormParam
    @Override
    public Response updateWeather(@PathParam("iata") String iataCode,
                                  @PathParam("pointType") String pointType,
                                  String datapointJson) {
        try {
            airportService.addDataPoint(iataCode, pointType, gson.fromJson(datapointJson, DataPoint.class));
            return Response.status(Response.Status.CREATED).build();
         } catch (WeatherException|NoSuchElementException|JsonSyntaxException e) {
            LOGGER.log(Level.INFO,e.getMessage(),e);
            return Response.status(422).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,e.getMessage(),e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/airports")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Override
    public Response getAirports() {
        final Set<String> retval = new HashSet<>();
        for (AirportData ad : weatherService.getAirportData()) {
            retval.add(ad.getIata());
        }

        return Response
                .status(Response.Status.OK)
                .entity(
                    MediaType.APPLICATION_XML.equals(
                            req.get().getRequestHeader("Accept").get(0))?
                            new JaxbSet(retval):
                            retval
                ).build();
    }

    @GET
    @Path("/airport/{iata}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Override
    public Response getAirport(@PathParam("iata") String iata) {
        final AirportData ad = weatherService.findAirportData(iata);
        final Response.Status resCode = ad != null?
                Response.Status.OK: Response.Status.NOT_FOUND;

        return Response.status(resCode).entity(ad).build();
    }

    @POST
    @Path("/airport/{iata}/{lat}/{long}")
    @Override
    public Response addAirport(@PathParam("iata") String iata,
                               @PathParam("lat") String latString,
                               @PathParam("long") String longString) {
        try{
            airportService.addAirport(iata, Double.valueOf(latString), Double.valueOf(longString));
            return Response.status(Response.Status.CREATED).build();
        }catch (NumberFormatException nfe){
            return Response.status(422).build();
        }
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
     * A dummy init method that loads hard coded data
     */
    @PostConstruct
    public void init() {
        weatherService.getAirportData().clear();
        weatherService.getAtmosphericInformation().clear();
        weatherService.getRequestFrequency().clear();
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("airports.dat");
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String l = null;

        try {
            while ( (l = br.readLine()) != null) {
                String[] split = l.split(",");
                airportService.addAirport(split[0],
                        Double.valueOf(split[1]),
                        Double.valueOf(split[2]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
