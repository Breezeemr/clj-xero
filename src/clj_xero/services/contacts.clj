(ns clj-xero.services.contacts
  (:require [clj-xero.core :as core]))

(core/set-client! :contacts #{:get :post :put :attachments} *ns*
                  :get-all-paging-type :page)
