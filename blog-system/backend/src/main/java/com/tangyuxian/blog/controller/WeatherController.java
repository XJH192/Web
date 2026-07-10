package com.tangyuxian.blog.controller;

import com.tangyuxian.blog.common.ApiResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.Proxy;
import java.util.Map;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {
    private final RestTemplate restTemplate = createRestTemplate();

    @GetMapping
    public ApiResponse<Map> current(@RequestParam("latitude") double latitude,
                                    @RequestParam("longitude") double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            return ApiResponse.fail("经纬度参数不正确");
        }

        String url = UriComponentsBuilder
                .fromHttpUrl("https://api.open-meteo.com/v1/forecast")
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("current", "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m")
                .queryParam("daily", "weather_code,temperature_2m_max,temperature_2m_min")
                .queryParam("timezone", "Asia/Shanghai")
                .queryParam("forecast_days", 2)
                .build()
                .encode()
                .toUriString();

        try {
            Map data = restTemplate.getForObject(url, Map.class);
            if (data == null || !(data.get("current") instanceof Map)) {
                return ApiResponse.fail("天气服务返回的数据不完整");
            }
            return ApiResponse.ok(data);
        } catch (RestClientException error) {
            return ApiResponse.fail("实时天气服务暂时不可用，请稍后重试");
        }
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(6000);
        factory.setReadTimeout(10000);
        factory.setProxy(Proxy.NO_PROXY);
        return new RestTemplate(factory);
    }
}
