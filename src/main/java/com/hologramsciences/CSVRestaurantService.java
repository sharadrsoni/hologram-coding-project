package com.hologramsciences;

import io.atlassian.fugue.Option;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CSVRestaurantService {
    private final List<Restaurant> restaurantList;

    /**
     *
     * From the CSVRecord which represents a single line from src/main/resources/rest_hours.csv
     * Write a parser to read the line and create an instance of the Restaurant class (Optionally, using the Option class)
     *
     * Example Line:
     *
     *  "Burger Bar","Mon,Tue,Wed,Thu,Sun|11:00-22:00;Fri,Sat|11:00-0:00"
     *
     *  '|'   separates the list of applicable days from the hours span
     *  ';'   separates groups of (list of applicable days, hours span)
     *
     *  So the above line would be parsed as:
     *
     *  Map<DayOfWeek, OpenHours> m = new HashMap<>();
     *  m.put(MONDAY,    new OpenHours(LocalTime.of(11, 0), LocalTime.of(22, 0)));
     *  m.put(TUESDAY,   new OpenHours(LocalTime.of(11, 0), LocalTime.of(22, 0)));
     *  m.put(WEDNESDAY, new OpenHours(LocalTime.of(11, 0), LocalTime.of(22, 0)));
     *  m.put(THURSDAY,  new OpenHours(LocalTime.of(11, 0), LocalTime.of(22, 0)));
     *  m.put(SUNDAY,    new OpenHours(LocalTime.of(11, 0), LocalTime.of(22, 0)));
     *
     *  m.put(FRIDAY,    new OpenHours(LocalTime.of(11, 0), LocalTime.of(0, 0)));
     *  m.put(SATURDAY,  new OpenHours(LocalTime.of(11, 0), LocalTime.of(0, 0)));
     *
     *  Option.some(new Restaurant("Burger Bar", m))
     *
     * This method returns Option.some(parsedRestaurant),
     *       IF the String name, and Map<DayOfWeek, OpenHours> openHours is found in the CSV,
     *         - assume if both columns are in the CSV then they are both parsable.
     *       AND if all values in openHours have !startTime.equals(endTime)
     *
     * This method returns Option.none() when any of the OpenHours for a given restaurant have the same startTime and endDate
     *
     *
     * NOTE, the getDayOfWeek method should be helpful, and the LocalTime should be parsable by LocalDate.parse
     *
     */
    public static Option<Restaurant> parse(final CSVRecord r) {
        try {
            return Option.some(new Restaurant(r.get(0), parseOpenHour(r.get(1))));
        } catch (UnsupportedOperationException | IndexOutOfBoundsException exception) {
            return Option.none();
        }
    }

    /**
     * This is a useful helper method
     */
    public static Map<DayOfWeek, Restaurant.OpenHours> parseOpenHour(final String openHoursString) {
        Map<DayOfWeek, Restaurant.OpenHours> restaurantTimings = new EnumMap<>(DayOfWeek.class);
        StringTokenizer tokenizer = new StringTokenizer(openHoursString, ";");
        while (tokenizer.hasMoreTokens()) {
            StringTokenizer perDayTime = new StringTokenizer(tokenizer.nextToken(), "|");
            StringTokenizer days = new StringTokenizer(perDayTime.nextToken(), ",");
            StringTokenizer timing = new StringTokenizer(perDayTime.nextToken(), "-");
            String startTme = timing.nextToken();
            String endTime = timing.nextToken();
            if(startTme.equals(endTime)) {
                throw new UnsupportedOperationException("Start time and end time are same");
            }
            while(days.hasMoreTokens()) {
                restaurantTimings.put(getDayOfWeek(days.nextToken()).get(), new Restaurant.OpenHours(LocalTime.parse(startTme), LocalTime.parse(endTime)));
            }
        }
        return restaurantTimings;
    }

    public CSVRestaurantService() throws IOException {
        this.restaurantList = ResourceLoader.parseOptionCSV("rest_hours.csv", CSVRestaurantService::parse);
    }

    public List<Restaurant> getAllRestaurants() {
        return restaurantList;
    }

    /**
     *
     *  A restaurant is considered open when the OpenHours for the dayOfWeek has:
     *
     *  startTime < localTime   && localTime < endTime
     *
     *  If the open hours are 16:00-20:00  Then
     *
     *  15:59 open = false
     *  16:00 open = false
     *  16:01 open = true
     *  20:00 open = false
     *
     *
     *  If the startTime endTime spans midnight, then consider an endTime up until 5:00 to be part of same DayOfWeek as the startTime
     *
     *  SATURDAY, OpenHours are: 20:00-04:00    SUNDAY, OpenHours are: 10:00-14:00
     *
     *  (SATURDAY, 03:00) => open = false
     *  (SUNDAY, 03:00)   => open = true
     *  (SUNDAY, 05:00)   => open = false
     *
     */
    public List<Restaurant> getOpenRestaurants(final DayOfWeek dayOfWeek, final LocalTime localTime) {
        return getAllRestaurants().stream().filter(o -> {
            if (localTime.compareTo(LocalTime.parse("00:00")) >= 0 && localTime.compareTo(LocalTime.parse("05:00")) <= 0) {
                Restaurant.OpenHours openHours = o.getOpenHoursMap().get(dayOfWeek.minus(1));
                return openHours != null && openHours.spansMidnight() && (openHours.getStartTime().compareTo(localTime) <= 0 || openHours.getEndTime().compareTo(localTime) >= 0);
            } else {
                Restaurant.OpenHours openHours = o.getOpenHoursMap().get(dayOfWeek);
                return openHours != null && openHours.getStartTime().compareTo(localTime) <= 0 && openHours.getEndTime().compareTo(localTime) >= 0;
            }
        }).collect(Collectors.toList());
    }

    public List<Restaurant> getOpenRestaurantsForLocalDateTime(final LocalDateTime localDateTime) {
        return getOpenRestaurants(localDateTime.getDayOfWeek(), localDateTime.toLocalTime());
    }

    public static Option<DayOfWeek> getDayOfWeek(final String s) {

        switch (s) {
            case "Mon":
                return Option.some(DayOfWeek.MONDAY);
            case "Tue":
                return Option.some(DayOfWeek.TUESDAY);
            case "Wed":
                return Option.some(DayOfWeek.WEDNESDAY);
            case "Thu":
                return Option.some(DayOfWeek.THURSDAY);
            case "Fri":
                return Option.some(DayOfWeek.FRIDAY);
            case "Sat":
                return Option.some(DayOfWeek.SATURDAY);
            case "Sun":
                return Option.some(DayOfWeek.SUNDAY);
            default:
                return Option.none();
        }
    }

    public static <S, T> Function<S, Stream<T>> toStreamFunc(final Function<S, Option<T>> function) {
        return s -> function.apply(s).fold(Stream::empty, Stream::of);
    }

    /**
     * NOTE: Useful for generating the data.sql file in src/main/resources/
     */
    public static void main (final String [] args) throws IOException {
        final CSVRestaurantService csvRestaurantService = new CSVRestaurantService();

        csvRestaurantService.getAllRestaurants().forEach(restaurant -> {

            final String name = restaurant.getName().replace("'", "''");

            System.out.println("INSERT INTO restaurants (name) values ('" + name  + "');");

            restaurant.getOpenHoursMap().forEach((dayOfWeek, value) -> {
                final LocalTime startTime = value.getStartTime();
                final LocalTime endTime = value.getEndTime();

                System.out.println("INSERT INTO open_hours (restaurant_id, day_of_week, start_time_minute_of_day, end_time_minute_of_day) select id, '" + dayOfWeek.toString() + "', " + startTime.get(ChronoField.MINUTE_OF_DAY) + ", " + endTime.get(ChronoField.MINUTE_OF_DAY) + " from restaurants where name = '" + name + "';");

            });
        });
    }
}
