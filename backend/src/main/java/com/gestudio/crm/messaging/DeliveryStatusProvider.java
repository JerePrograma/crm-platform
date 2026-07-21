package com.gestudio.crm.messaging;

import java.util.List;

public interface DeliveryStatusProvider {
  List<DeliveryStatus> readStatuses(String cursor, int limit);

  record DeliveryStatus(
      String providerEventId, String externalMessageId, String status, String nextCursor) {}
}
