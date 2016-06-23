/*
 * Copyright (c) 2016 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.hansolo.accs;

import org.json.simple.JSONObject;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;


/**
 * Created by hansolo on 15.06.16.
 */
public class Location {
    public enum CardinalDirection {
        N("North", 348.75, 11.25),
        NNE("North North-East", 11.25, 33.75),
        NE("North-East", 33.75, 56.25),
        ENE("East North-East", 56.25, 78.75),
        E("East", 78.75, 101.25),
        ESE("East South-East", 101.25, 123.75),
        SE("South-East", 123.75, 146.25),
        SSE("South South-East", 146.25, 168.75),
        S("South", 168.75, 191.25),
        SSW("South South-West", 191.25, 213.75),
        SW("South-West", 213.75, 236.25),
        WSW("West South-West", 236.25, 258.75),
        W("West", 258.75, 281.25),
        WNW("West North-West", 281.25, 303.75),
        NW("North-West", 303.75, 326.25),
        NNW("North North-West", 326.25, 348.75);

        public String direction;
        public double from;
        public double to;

        private CardinalDirection(final String DIRECTION, final double FROM, final double TO) {
            direction = DIRECTION;
            from      = FROM;
            to        = TO;
        }
    }

    // Location related information
    public Instant timestamp;
    public double  latitude;
    public double  longitude;
    public double  altitude;
    public String  name;
    public String  info;
    public boolean isUpToDate;


    // ******************** Constructors **************************************
    public Location() { this(new JSONObject()); }
    public Location(final JSONObject JSON) {
        this(Double.parseDouble(JSON.getOrDefault("latitude", "0").toString()),
             Double.parseDouble(JSON.getOrDefault("longitude", "0").toString()),
             Double.parseDouble(JSON.getOrDefault("altitude", "0").toString()),
             Instant.ofEpochSecond(Long.parseLong(JSON.getOrDefault("timestamp", Instant.now().getEpochSecond()).toString())),
             JSON.getOrDefault("name", "").toString(),
             JSON.getOrDefault("info", "").toString());
    }
    public Location(final double LATITUDE, final double LONGITUDE, final String NAME) {
        this(LATITUDE, LONGITUDE, 0, Instant.now(), NAME, "");
    }
    public Location(final double LATITUDE, final double LONGITUDE, final String NAME, final String INFO) {
        this(LATITUDE, LONGITUDE, 0, Instant.now(), NAME, INFO);
    }
    public Location(final double LATITUDE, final double LONGITUDE, final double ALTITUDE, final Instant TIMESTAMP, final String NAME, final String INFO) {
        name       = NAME;
        latitude   = LATITUDE;
        longitude  = LONGITUDE;
        altitude   = ALTITUDE;
        timestamp  = TIMESTAMP;
        info       = INFO;
        isUpToDate = true;
    }


    // ******************** Methods *******************************************
    public LocalDateTime getLocaleDateTime() { return getLocalDateTime(ZoneId.systemDefault()); }
    public LocalDateTime getLocalDateTime(final ZoneId ZONE_ID) { return LocalDateTime.ofInstant(timestamp, ZONE_ID); }

    public void update(final double LATITUDE, final double LONGITUDE) { set(LATITUDE, LONGITUDE); }

    public void set(final double LATITUDE, final double LONGITUDE) {
        latitude  = LATITUDE;
        longitude = LONGITUDE;
        timestamp = Instant.now();
    }
    public void set(final double LATITUDE, final double LONGITUDE, final double ALTITUDE, final Instant TIMESTAMP) {
        latitude  = LATITUDE;
        longitude = LONGITUDE;
        altitude  = ALTITUDE;
        timestamp = TIMESTAMP;
    }
    public void set(final Location LOCATION) {
        latitude    = LOCATION.latitude;
        longitude   = LOCATION.longitude;
        altitude    = LOCATION.altitude;
        timestamp   = LOCATION.timestamp;
        info        = LOCATION.info;
    }

    public double getDistanceTo(final Location LOCATION) { return calcDistanceInMeter(this, LOCATION); }

    public boolean isWithinRangeOf(final Location LOCATION, final double METERS) { return getDistanceTo(LOCATION) < METERS; }

    public double calcDistanceInMeter(final Location P1, final Location P2) {
        return calcDistanceInMeter(P1.latitude, P1.longitude, P2.latitude, P2.longitude);
    }
    public double calcDistanceInKilometer(final Location P1, final Location P2) {
        return calcDistanceInMeter(P1, P2) / 1000.0;
    }
    public double calcDistanceInMeter(final double LAT_1, final double LON_1, final double LAT_2, final double LON_2) {
        final double EARTH_RADIUS      = 6_371_000; // m
        final double LAT_1_RADIANS     = Math.toRadians(LAT_1);
        final double LAT_2_RADIANS     = Math.toRadians(LAT_2);
        final double DELTA_LAT_RADIANS = Math.toRadians(LAT_2-LAT_1);
        final double DELTA_LON_RADIANS = Math.toRadians(LON_2-LON_1);

        final double A = Math.sin(DELTA_LAT_RADIANS * 0.5) * Math.sin(DELTA_LAT_RADIANS * 0.5) + Math.cos(LAT_1_RADIANS) * Math.cos(LAT_2_RADIANS) * Math.sin(DELTA_LON_RADIANS * 0.5) * Math.sin(DELTA_LON_RADIANS * 0.5);
        final double C = 2 * Math.atan2(Math.sqrt(A), Math.sqrt(1-A));

        final double DISTANCE = EARTH_RADIUS * C;

        return DISTANCE;
    }
    public double calcDistanceInMiles(final Location P1, final Location P2) { return (calcDistanceInMeter(P1.latitude, P1.longitude, P2.latitude, P2.longitude) * 0.000621371); }

    public double getAltitudeDifferenceInMeter(final Location LOCATION) { return (altitude - LOCATION.altitude); }

    public double getBearingTo(final Location LOCATION) {
        return calcBearingInDegree(latitude, longitude, LOCATION.latitude, LOCATION.longitude);
    }
    public double getBearingTo(final double LATITUDE, final double LONGITUDE) {
        return calcBearingInDegree(latitude, longitude, LATITUDE, LONGITUDE);
    }

    public boolean isZero() { return Double.compare(latitude, 0d) == 0 && Double.compare(longitude, 0d) == 0; }

    public double calcBearingInDegree(final double LAT_1, final double LON_1, final double LAT_2, final double LON_2) {
        double lat1     = Math.toRadians(LAT_1);
        double lon1     = Math.toRadians(LON_1);
        double lat2     = Math.toRadians(LAT_2);
        double lon2     = Math.toRadians(LON_2);
        double deltaLon = lon2 - lon1;
        double deltaPhi = Math.log(Math.tan(lat2 * 0.5 + Math.PI * 0.25) / Math.tan(lat1 * 0.5 + Math.PI * 0.25));
        if (Math.abs(deltaLon) > Math.PI) {
            if (deltaLon > 0) {
                deltaLon = -(2.0 * Math.PI - deltaLon);
            } else {
                deltaLon = (2.0 * Math.PI + deltaLon);
            }
        }
        double bearing = (Math.toDegrees(Math.atan2(deltaLon, deltaPhi)) + 360.0) % 360.0;
        return bearing;
    }

    public String getCardinalDirectionFromBearing(final double BEARING) {
        double bearing = BEARING % 360.0;
        for (CardinalDirection cardinalDirection : CardinalDirection.values()) {
            if (Double.compare(bearing, cardinalDirection.from) >= 0 && Double.compare(bearing, cardinalDirection.to) < 0) {
                return cardinalDirection.direction;
            }
        }
        return "";
    }


    // ******************** Misc **********************************************
    @Override public boolean equals(final Object OBJECT) {
        if (OBJECT instanceof Location) {
            final Location LOCATION = (Location) OBJECT;
            return (Double.compare(latitude, LOCATION.latitude) == 0 &&
                    Double.compare(longitude, LOCATION.longitude) == 0 &&
                    Double.compare(altitude, LOCATION.altitude) == 0);
        } else {
            return false;
        }
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        jsonObject.put("timestamp", new Long(timestamp.getEpochSecond()));
        jsonObject.put("latitude", new Double(latitude));
        jsonObject.put("longitude", new Double(longitude));
        jsonObject.put("altitude", new Double(altitude));
        jsonObject.put("info", new String(info));
        return jsonObject;
    }
    public String toJSONString() { return toJSON().toJSONString(); }

    @Override public String toString() {
        return new StringBuilder().append("Name     : ").append(name).append("\n")
                                  .append("Timestamp: ").append(timestamp).append("\n")
                                  .append("Latitude : ").append(latitude).append("\n")
                                  .append("Longitude: ").append(longitude).append("\n")
                                  .append("Altitude : ").append(String.format(Locale.US, "%.1f", altitude)).append("\n")
                                  .append("Info     : ").append(info).append("\n")
                                  .toString();
    }

    @Override public int hashCode() { return Objects.hashCode(this); }
}
