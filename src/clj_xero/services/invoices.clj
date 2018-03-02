(ns clj-xero.services.invoices
  (:require [clj-xero.core :as core]))

(core/set-client! :invoices #{:get :post :put :delete :attachments} *ns*
                  :get-all-paging-type :page)
