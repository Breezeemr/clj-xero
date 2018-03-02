(ns clj-xero.services.payments
  (:require [clj-xero.core :as core]))

(core/set-client! :payments #{:get :post :put} *ns*)
