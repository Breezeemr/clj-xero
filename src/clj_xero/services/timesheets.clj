(ns clj-xero.services.timesheets
  (:require [clj-xero.core :as core]))

;; intended for use with payroll API
;; Create credentials like normal with core/private-credentials,
;; then (assoc credentials :base-api-url "https://api.xero.com/payroll.xro/1.0/")

(core/set-client! :timesheets #{:get :post} *ns*
                  :get-all-paging-type :page)
