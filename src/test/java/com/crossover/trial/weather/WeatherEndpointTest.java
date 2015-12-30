package com.crossover.trial.weather;

import com.crossover.trial.weather.endpoints.RestWeatherCollectorEndpoint;
import com.crossover.trial.weather.endpoints.RestWeatherQueryEndpoint;
import com.crossover.trial.weather.endpoints.WeatherCollector;
import com.crossover.trial.weather.endpoints.WeatherQueryEndpoint;
import com.crossover.trial.weather.representations.AtmosphericInformation;
import com.crossover.trial.weather.service.AirportService;
import com.crossover.trial.weather.service.WeatherService;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class WeatherEndpointTest {

    RestWeatherQueryEndpoint _query = new RestWeatherQueryEndpoint();

    RestWeatherCollectorEndpoint _update = new RestWeatherCollectorEndpoint();

    private Gson _gson = new Gson();

    private DataPoint _dp;
    @Before
    public void setUp() throws Exception {
        _update.init();
        _dp = new DataPoint.Builder()
                .withCount(10).withFirst(10).withMedian(20).withLast(30).withMean(22).build();
        _update.updateWeather("BOS", "wind", _gson.toJson(_dp));
        _query.get("BOS", "0").getEntity();
    }

    @Test
    public void testPing() throws Exception {
        String ping = _query.ping();
        JsonElement pingResult = new JsonParser().parse(ping);
        assertEquals(1, pingResult.getAsJsonObject().get("datasize").getAsInt());
        assertEquals(5, pingResult.getAsJsonObject().get("iata_freq").getAsJsonObject().entrySet().size());
    }

    @Test
    public void testGet() throws Exception {
        List<AtmosphericInformation> ais = (List<AtmosphericInformation>) _query.get("BOS", "0").getEntity();
        assertEquals(ais.get(0).getWind(), _dp);
    }

    @Test
    public void testGetNearby() throws Exception {
        // check datasize response
        _update.updateWeather("JFK", "wind", _gson.toJson(_dp));
        _dp.setMean(40);
        _update.updateWeather("EWR", "wind", _gson.toJson(_dp));
        _dp.setMean(30);
        _update.updateWeather("LGA", "wind", _gson.toJson(_dp));

        List<AtmosphericInformation> ais = (List<AtmosphericInformation>) _query.get("JFK", "200").getEntity();
        assertEquals(3, ais.size());
    }

    @Test
    public void testUpdate() throws Exception {

        DataPoint windDp = new DataPoint.Builder()
                .withCount(10).withFirst(10).withMedian(20).withLast(30).withMean(22).build();
        _update.updateWeather("BOS", "wind", _gson.toJson(windDp));
        _query.get("BOS", "0").getEntity();

        String ping = _query.ping();
        JsonElement pingResult = new JsonParser().parse(ping);
        assertEquals(1, pingResult.getAsJsonObject().get("datasize").getAsInt());

        DataPoint cloudCoverDp = new DataPoint.Builder()
                .withCount(4).withFirst(10).withMedian(60).withLast(100).withMean(50).build();
        _update.updateWeather("BOS", "cloudcover", _gson.toJson(cloudCoverDp));

        List<AtmosphericInformation> ais = (List<AtmosphericInformation>) _query.get("BOS", "0").getEntity();
        assertEquals(ais.get(0).getWind(), windDp);
        assertEquals(ais.get(0).getCloudCover(), cloudCoverDp);
    }

}