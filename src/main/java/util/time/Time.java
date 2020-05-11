package util.time;

import org.jetbrains.annotations.NotNull;

public interface Time extends Comparable<Time> {

    Time as(TimeUnit timeUnit);

    double getAs(TimeUnit timeUnit);

    double asNano();

    double asMilli();

    double asSecond();

    double asMinute();

    double asHour();

    int getDay();

    Time plus(Time time);

    Time plusNanos(double nanoSeconds);

    Time plusMillis(double milliSeconds);

    Time plusSeconds(double seconds);

    Time plusMinutes(double minutes);

    Time plusHours(double hours);

    default Time minus(Time time) {
        return minus(time, true);
    }

    Time minus(Time time, boolean onlyPositive);

    boolean isAfter(Time other);

    boolean isBefore(Time other);

    @Override
    default int compareTo(@NotNull Time o) {
        return equals(o) ? 0 : isAfter(o) ? 1 : -1;
    }
}
