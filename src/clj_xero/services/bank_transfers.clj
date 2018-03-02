(ns clj-xero.services.bank-transfers
  (:require [clj-xero.core :as core]))

(core/set-client! :bank-transfers #{:get :attachments} *ns*)
