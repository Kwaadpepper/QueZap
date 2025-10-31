package com.quezap.lib.ddd.valueobjects;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;

import org.jspecify.annotations.Nullable;

public record TimelinePoint(Instant position) implements Comparable<TimelinePoint> {
  public static TimelinePoint at(Instant position) {
    return new TimelinePoint(position);
  }

  public static TimelinePoint at(ZonedDateTime position) {
    return new TimelinePoint(position.toInstant());
  }

  public static TimelinePoint now() {
    return new TimelinePoint(Instant.now());
  }

  public boolean isBefore(TimelinePoint other) {
    return position.isBefore(other.position);
  }

  public boolean isAfter(TimelinePoint other) {
    return position.isAfter(other.position);
  }

  public boolean isBetween(TimelinePoint start, TimelinePoint end) {
    return !this.isBefore(start) && !this.isAfter(end);
  }

  public ZonedDateTime atZone(ZoneId zone) {
    return position.atZone(zone);
  }

  public static Duration between(TimelinePoint start, TimelinePoint end) {
    return Duration.between(start.position, end.position);
  }

  public TimelinePoint plus(TemporalAmount amountToAdd) {
    return new TimelinePoint(position.plus(amountToAdd));
  }

  public TimelinePoint plus(long amountToAdd, TemporalUnit unit) {
    return new TimelinePoint(position.plus(amountToAdd, unit));
  }

  public TimelinePoint minus(TemporalAmount amountToSubtract) {
    return new TimelinePoint(position.minus(amountToSubtract));
  }

  public TimelinePoint minus(long amountToSubtract, TemporalUnit unit) {
    return new TimelinePoint(position.minus(amountToSubtract, unit));
  }

  @Override
  public int compareTo(@Nullable TimelinePoint o) {
    if (o == null) {
      return 1;
    }

    return this.position.compareTo(o.position);
  }
}
