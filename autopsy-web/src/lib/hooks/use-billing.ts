import useSWR from "swr";
import { getBilling, getDeliveryPrefs } from "../api-client";
import type { BillingResponse, DeliveryPreferencesResponse } from "../types";

export function useBilling() {
  return useSWR<BillingResponse>("billing", getBilling);
}

export function useDeliveryPrefs() {
  return useSWR<DeliveryPreferencesResponse>("settings/delivery", getDeliveryPrefs);
}
