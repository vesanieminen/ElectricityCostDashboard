package com.vesanieminen.froniusvisualizer.services.model;

public class FmiObservationResponse {
    public FmiObservationResponse(FmiObservation[] observations) {
        this.observations = observations;
    }

    FmiObservation[] observations;

    public class FmiObservation {
        public FmiObservation(String name, String localtz, String localtime, Float t2m, Float dewPoint,
                Float precipitation1h, Integer totalCloudCover, Float windSpeedMS, Integer windDirection,
                Float windGust, Float pressure, Integer humidity, Integer visibility, Integer snowDepth) {
            this.name = name;
            this.localtz = localtz;
            this.localtime = localtime;
            this.t2m = t2m;
            DewPoint = dewPoint;
            Precipitation1h = precipitation1h;
            TotalCloudCover = totalCloudCover;
            WindSpeedMS = windSpeedMS;
            WindDirection = windDirection;
            WindGust = windGust;
            Pressure = pressure;
            Humidity = humidity;
            Visibility = visibility;
            SnowDepth = snowDepth;
        }

        String name;            //"Katinen"
        String localtz;         //"Europe/Helsinki"
        String localtime;       //"20230317T215000"
        Float t2m;              // 0.1
        Float DewPoint;         // -5.4
        Float Precipitation1h;  // 1.0 
        Integer TotalCloudCover;// 8
        Float WindSpeedMS;      // 4.5
        Integer WindDirection;  // 151
        Float WindGust;         // 6.9
        Float Pressure;         // 1021.4
        Integer Humidity;       // 67
        Integer Visibility;     // 35031
        Integer SnowDepth;      // 44

        public String getName() {
            return name;
        }

        public String getLocaltz() {
            return localtz;
        }

        public String getLocaltime() {
            return localtime;
        }

        public Float getTemperature() {
            return t2m;
        }

        public Float getDewPoint() {
            return DewPoint;
        }

        public Float getPrecipitation1h() {
            return Precipitation1h;
        }

        public Integer getTotalCloudCover() {
            return TotalCloudCover;
        }

        public Float getWindSpeedMS() {
            return WindSpeedMS;
        }

        public Integer getWindDirection() {
            return WindDirection;
        }

        public Float getWindGust() {
            return WindGust;
        }

        public Float getPressure() {
            return Pressure;
        }

        public Integer getHumidity() {
            return Humidity;
        }

        public Integer getVisibility() {
            return Visibility;
        }

        public Integer getSnowDepth() {
            return SnowDepth;
        }
    }

    public FmiObservation[] getObservations() {
        return observations;
    }
}