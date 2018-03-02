(ns clj-xero.services.reports
  (:require [clj-xero.core :as core]))

(core/set-client! :reports #{:get} *ns* :private? true)

(defn get-bank-statements
  ([account-id params]
   {:pre [(not (nil? account-id)) (or (map? params) (nil? params))]}
   (get-bank-statements core/*current-credentials* account-id params))
  ([credentials account-id params]
   {:pre [(not (nil? account-id)) (or (map? params) (nil? params))]}
   (get-report-by-guid-and-params credentials "BankStatement" (assoc params :bank-account-id account-id))))

(defn get-balance-sheet
  ([params]
   {:pre [(or (map? params) (nil? params))]}
   (get-balance-sheet core/*current-credentials* params))
  ([credentials params]
   {:pre [(or (map? params) (nil? params))]}
   (get-report-by-guid-and-params credentials "BalanceSheet" params)))

(defn get-profit-and-loss
  ([params]
   {:pre [(or (map? params) (nil? params))]}
   (get-profit-and-loss core/*current-credentials* params))
  ([credentials params]
   {:pre [(or (map? params) (nil? params))]}
   (get-report-by-guid-and-params credentials "ProfitAndLoss" params)))

(defn get-published-reports
  ([]
   (get-published-reports core/*current-credentials*))
  ([credentials]
   (get-all-reports credentials)))

(defn get-published-report
  ([report-id]
   {:pre [(not (nil? report-id))]}
   (get-published-report core/*current-credentials* report-id))
  ([credentials report-id]
   {:pre [(not (nil? report-id))]}
   (get-report-by-guid credentials report-id)))
