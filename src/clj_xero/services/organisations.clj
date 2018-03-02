(ns clj-xero.services.organisations
  (:require [clj-xero.core :as core]))

(core/set-client! :organisations #{:get} *ns*)
