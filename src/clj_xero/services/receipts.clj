(ns clj-xero.services.receipts
  (:require [clj-xero.core :as core]))

(core/set-client! :receipts #{:get :post :put} *ns*)
