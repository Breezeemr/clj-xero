(ns clj-xero.services.bank-transactions
  (:require [clj-xero.core :as core]))

(core/set-client! :bank-transactions #{:get :post :put :attachments} *ns*
                  :get-all-paging-type :page)
