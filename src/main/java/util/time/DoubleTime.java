package util.time;

/**
 * Immutable class, default unit of measure is milliseconds
 */
public class DoubleTime implements Time {

    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MILLIS;
    private static final DoubleTime ZERO = new DoubleTime(0);
    private final double time;
    private final TimeUnit timeUnit;

    // region constructor
    public DoubleTime() {
        this(0);
    }

    public DoubleTime(double time) {
        this(time, DEFAULT_TIME_UNIT);
    }

    public DoubleTime(double time, TimeUnit timeUnit) {
        this.time = time;
        this.timeUnit = timeUnit;
    }
    // endregion

    static public Time zero() {
        return ZERO;
    }

    static public Time fromSeconds(double time) {
        return new DoubleTime(time * 1e3);
    }

    @Override
    public Time as(TimeUnit timeUnit) {
        return new DoubleTime(this.timeUnit.convertTo(time, timeUnit), timeUnit);
    }

    @Override
    public double getAs(TimeUnit timeUnit) {
        return this.timeUnit.convertTo(time, timeUnit);
    }

    @Override
    public double asNano() {
        return getAs(TimeUnit.NANOS);
    }

    @Override
    public double asMilli() {
        return getAs(TimeUnit.MILLIS);
    }

    @Override
    public double asSecond() {
        return getAs(TimeUnit.SECONDS);
    }

    @Override
    public double asMinute() {
        return getAs(TimeUnit.MINUTES);
    }

    @Override
    public double asHour() {
        return getAs(TimeUnit.HOURS);
    }

    @Override
    public int getDay() {
        return (int)asHour() % 24;
    }

    @Override
    public Time plus(Time time) {
        return plus(time.asMilli(), TimeUnit.MILLIS);
    }

    private DoubleTime plus(double value, TimeUnit timeUnit) {
        return new DoubleTime(time + timeUnit.convertTo(value, this.timeUnit), this.timeUnit);
    }

    @Override
    public Time plusNanos(double nanoSeconds) {
        return plus(nanoSeconds, TimeUnit.NANOS);
    }

    @Override
    public Time plusMillis(double milliSeconds) {
        return plus(milliSeconds, TimeUnit.MILLIS);
    }

    @Override
    public Time plusSeconds(double seconds) {
        return plus(seconds, TimeUnit.SECONDS);
    }

    @Override
    public Time plusMinutes(double minutes) {
        return plus(minutes, TimeUnit.MINUTES);
    }

    @Override
    public Time plusHours(double hours) {
        return plus(hours, TimeUnit.HOURS);
    }

    @Override
    public Time minus(Time time, boolean onlyPositive) {
        var value = this.time - TimeUnit.MILLIS.convertTo(time.asMilli(), this.timeUnit);
        if (value < 0 && onlyPositive) {
            throw new IllegalStateException("after the subtraction the time is negative");
        }
        return new DoubleTime(value, this.timeUnit);
    }

    @Override
    public boolean isAfter(Time other) {
        return asMilli() > other.asMilli();
    }

    @Override
    public boolean isBefore(Time other) {
        return asMilli() < other.asMilli();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DoubleTime that = (DoubleTime) o;
        return asMilli() == that.asMilli();
    }

    @Override
    public int hashCode() {
        return Double.hashCode(time) ^ timeUnit.hashCode();
    }

    @Override
    public String toString() {
        return "DoubleTime[" +
            "time=" + time +
            ", timeUnit=" + timeUnit +
            ']';
    }
}
