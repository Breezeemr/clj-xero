(ns clj-xero.services.journals
  (:require [clj-xero.core :as core]))

(core/set-client! :journals #{:get} *ns* :get-all-paging-type :offset)
