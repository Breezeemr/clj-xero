(ns clj-xero.services.tracking-categories
  (:require [clj-xero.core :as core]))

(core/set-client! :tracking-categories #{:get :post :put :delete} *ns*)

;; TODO: put/post tracking category options
