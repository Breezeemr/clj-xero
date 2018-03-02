(ns clj-xero.services.accounts
  (:require [clj-xero.core :as core]))

(core/set-client! :accounts #{:get :post :put :delete :attachments} *ns*)
