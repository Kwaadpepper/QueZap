package com.quezap.lib.ddd;

import com.quezap.lib.ddd.valueobjects.TimelinePoint;

public interface TracksUpdatedAt {
  void setUpdateAt(TimelinePoint now);

  TimelinePoint getUpdatedAt();
}
