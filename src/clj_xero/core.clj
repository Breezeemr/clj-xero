(ns clj-xero.core
  (:require [clojure.data.json :as json]
            [org.tobereplaced.lettercase :as lettercase]
            [inflections.core :as inflections]
            [clj-time.core :as ct]
            [clj-http.client :as clj-http]
            [throttler.core :refer [throttle-fn]]
            [oauth.client :as oauth.client]))

(defn public-consumer
  "Returns an oauth consumer, usable for obtaining authorisation for a public user to their xero organisation."
  [key secret]
  (oauth.client/make-consumer key secret
                              "https://api.xero.com/oauth/RequestToken"
                              "https://api.xero.com/oauth/AccessToken"
                              "https://api.xero.com/oauth/Authorize"
                              :hmac-sha1))

(defn authorisation-request-token
  [consumer callback-url]
  (let [request-token (oauth.client/request-token consumer callback-url)]
    (assoc request-token :authorisation-url
                         (->> request-token
                              (:oauth_token)
                              (oauth.client/user-approval-uri consumer)))))

(defn public-authorised-credentials
  "Create credentials for a pre-authorised public user"
  [consumer request-token verifier-code]
  {:pre [(string? verifier-code) (map? request-token) consumer]}
  (-> (oauth.client/access-token consumer request-token verifier-code)
      (assoc :consumer consumer)))

(def base-api-url "https://api.xero.com/api.xro/2.0/")
(def partner-api-url "https://api.xero.com/api.xro/2.0/")

(defn partner-authorised-credentials
  "Create credentials for a pre-authorised partner user"
  [consumer request-token verifier-code]
  {:pre [(string? verifier-code) (map? request-token) consumer]}
  (-> (oauth.client/access-token consumer request-token verifier-code)
      (assoc :consumer consumer
             :base-api-url partner-api-url)))

(defn partner-authorised-credentials-refresh
  "Create credentials for a partner user with an existing token refresh"
  [consumer request-token verifier-code]
  {:pre [(map? request-token) consumer]}
  (-> (oauth.client/refresh-token consumer request-token verifier-code)
      (assoc :consumer consumer
             :base-api-url partner-api-url)))

(defn private-credentials
  [key secret private-key]
  {:pre [(string? private-key)]}
  {:consumer (oauth.client/make-consumer key
                                         private-key
                                         "https://api.xero.com/api.xro/2.0/oauth/RequestToken"
                                         "https://api.xero.com/api.xro/2.0/oauth/AccessToken"
                                         "https://api.xero.com/api.xro/2.0/oauth/Authorise"
                                         :rsa-sha1)
   :consumer-secret secret})

(defn partner-consumer [key private-key]
  (oauth.client/make-consumer key
                              private-key
                              "https://api.xero.com/oauth/RequestToken"
                              "https://api.xero.com/oauth/AccessToken"
                              "https://api.xero.com/oauth/Authorize"
                              :rsa-sha1))

(def ^:dynamic *current-credentials*)

(defmacro with-credentials
  [credentials & body]
  `(binding [*current-credentials* ~credentials]
     ~@body))

(def action-keywords
  {:get "get"
   :post "add-or-update"
   :put "add"
   :delete "delete"
   :attachments "Attachments"})

(defn- format-entity-name
  [entity-name]
  (lettercase/lower-hyphen (name entity-name)))

(defn- fn-name
  [action endpoint & {:keys [postfix prefix]}]
  (let [action-name (action-keywords action)
        endpoint (format-entity-name endpoint)
        punc (if (#{:put :post :delete} action) "!" "")]
    (symbol (format "%s-%s%s%s%s%s%s"
                    action-name
                    (if prefix (lettercase/lower-hyphen (name prefix)) "")
                    (if prefix "-" "")
                    endpoint
                    (if postfix "-" "")
                    (if postfix (lettercase/lower-hyphen (name postfix)) "")
                    punc))))

;;TODO: add partner url + way to work out with url to use

(defn- build-query
  [params]
  {:pre [(map? params)]}
  ;;TODO - return the xero query string from a map of args
  nil
  )

(defn- make-credentials
  [{:keys [consumer oauth_token oauth_token_secret consumer-secret]} method for-url & [params]]
  (oauth.client/credentials consumer
                            (or oauth_token (:key consumer))
                            (or oauth_token_secret consumer-secret)
                            method
                            for-url
                            (or params {})))

(defn- lowername-keys [params]
  (zipmap (map (comp lettercase/lower-keyword name) (keys params)) (vals params)))

(defn- do-request-for-auth-type
  [request-fn url headers query-params body input-stream]
  (request-fn url (merge {:headers headers
                          :query-params query-params}
                         (if body {:form-params {:json body}} {:body input-stream}))))

(defn- private-credentials? [credentials]
  (contains? credentials :consumer))

(defn- public-or-private-credentials? [credentials]
  (every? true? (map #(contains? credentials %)
                     #{:consumer :xero_org_muid :oauth_token :oauth_token_secret})))

(defn valid-credentials-for-auth-type?
  "Checks all required keys are present and entrust store is provided if using the partner api"
  [credentials]
  (or (public-or-private-credentials? credentials)
      (private-credentials? credentials)))

(defn- do-request*
  [request-method endpoint credentials & {:keys [params body guid modified-since attachments filename input-stream page offset]}]
  {:pre [(map? credentials) (valid-credentials-for-auth-type? credentials)]}
  (let [url (str (or (:base-api-url credentials) base-api-url) (lettercase/capitalized-name endpoint) (when guid (str "/" guid))
                 (when attachments (str "/" attachments )) (when filename (str "/" filename)))
        request-fn ({:GET clj-http/get :POST clj-http/post :PUT clj-http/put :DELETE clj-http/delete} request-method)
        params (merge (lowername-keys params) (when page {:page page}) (when offset {:offset offset}))
        credentials (make-credentials credentials request-method url params)
        headers (merge {"Accept" "application/json"}
                       (when body {"Content-Type" "application/x-www-form-urlencoded"})
                       (when input-stream {"Content-Type" "application/pdf"})
                       (when modified-since {"If-Modified-Since" ""}))
        body (when body (json/write-str body :key-fn (comp lettercase/capitalized name)))
        resp-key (if attachments (keyword (lettercase/lower attachments)) endpoint)
        query-params (merge credentials params)
        {:keys [body status] :as resp} (do-request-for-auth-type request-fn url headers query-params body input-stream)]
    (case status
      200 (->
           (json/read-str body :key-fn lettercase/lower-hyphen-keyword)
             resp-key)
      404 nil
      (throw (ex-info "Error while executing Xero API request." resp)))))

(def ^:private do-request (throttle-fn do-request* 60 :minute))

(defmulti client-fns (fn [action _ _] action))

(defn- get-all-by-page*
  [fetch-fn]
  (loop [entities (fetch-fn :page 1)
         last-results entities
         page 1]
    (if (not= 100 (count last-results))
      entities
      (let [next-page (fetch-fn :page (inc page))]
        (recur (concat entities next-page)
               next-page
               (inc page))))))

(def ^:private get-all-by-page (throttle-fn get-all-by-page* 1 :second))

(defn- get-all-by-offset*
  [fetch-fn endpoint]
  (let [offset-kw (-> (inflections/singular endpoint) name (str "-number") keyword)]
    (loop [entities (fetch-fn)
           last-results entities]
      (if (not= 100 (count last-results))
        entities
        (let [next-results (fetch-fn :offset (apply max (map offset-kw last-results)))]
          (recur (concat entities next-results)
                 next-results))))))

(def ^:private get-all-by-offset (throttle-fn get-all-by-offset* 1 :second))

(defmethod client-fns :get [action endpoint {:keys [get-all-paging-type private?]}]
  {;; get all
   (with-meta
     (fn-name action (inflections/plural endpoint) :prefix :all)
     {:doc (str "Retrieve a seq of all " (name (name endpoint)))
      :private private?
      :arglists [[] ['credentials]]})
   (fn f
     ([] (f *current-credentials*))
     ([credentials]
      (let [fetch (partial do-request :GET endpoint credentials)]
        (case get-all-paging-type
          :page (get-all-by-page fetch)
          :offset (get-all-by-offset fetch endpoint)
          (fetch)))))

   ;; get by id
   (with-meta
     (fn-name action (inflections/singular endpoint) :postfix :by-guid)
     {:doc (str "Retrieve the " (name (inflections/singular endpoint)) " by the given guid")
      :private private?
      :arglists [['guid] ['credentials 'guid]]})
   (fn f
     ([guid] (f *current-credentials* guid))
     ([credentials guid]
      (first (do-request :GET endpoint credentials :guid guid))))

   ;;get by id with filter params
   (with-meta
     (fn-name action (inflections/singular endpoint) :postfix :by-guid-and-params)
     {:doc (str "Retrieve the " (name (inflections/singular endpoint)) " by the given guid, with params")
      :private private?
      :arglists [['guid 'params] ['credentials 'guid 'params]]})
   (fn f
     ([guid params] (f *current-credentials* guid params))
     ([credentials guid params]
      (first (do-request :GET endpoint credentials :guid guid :params params))))

   ;; get by where clause and order
   ;; TODO
   })

(defn- decorate-ents
  "Decorate the given entity or entities so that the data structure is in an acceptable
  format for xero json."
  [endpoint ents]
  {:pre [(keyword? endpoint) (coll? ents)]}
  {endpoint ents})

(defmethod client-fns :put [action endpoint _]
  {(with-meta
     (fn-name action endpoint)
     {:doc (str "Add new " (name endpoint))
      :arglists [[(symbol (name endpoint))] ['credentials (symbol (name endpoint))]]})
   (fn f
     ([ents] (f *current-credentials* ents))
     ([credentials ents]
      {:pre [(seq ents)]}
      (let [decorated-ent (decorate-ents endpoint ents)]
        (do-request :PUT endpoint credentials :body decorated-ent))))})

(defmethod client-fns :post [action endpoint _]
  {(with-meta
     (fn-name action endpoint)
     {:doc (str "Add a new or update the given " (name endpoint))
      :arglists [[(symbol (name endpoint))] ['credentials (symbol (name endpoint))]]})
   (fn f
     ([ent] (f *current-credentials* ent))
     ([credentials ents]
      {:pre [(seq ents)]}
      (let [decorated-ent (decorate-ents endpoint ents)]
        (do-request :POST endpoint credentials :body decorated-ent))))})


(defmethod client-fns :attachments [action endpoint _]
  {;; get all attachments
   (with-meta
     (fn-name :get (inflections/singular endpoint) :postfix :attachments-by-guid)
     {:doc (str "Get all attachments for an " (name (inflections/singular endpoint)) " " (name action) "by the given guid")
      :arglists [['guid] ['credentials 'guid]]})
   (fn f
     ([guid] (f *current-credentials* guid))
     ([credentials guid]
     (do-request :GET endpoint credentials :guid guid :attachments "Attachments")))

   ;; TODO: get the contents of an attachment. Not working because bytestream is returned

   ;; Create attachement for endpoint
   (with-meta
     (fn-name :put (inflections/singular action))
     {:doc (str "Create an attachment for an " (name (inflections/singular endpoint)))
      :arglists [['guid 'filename 'input] ['credentials 'guid 'filename 'input]]})
   (fn f
     ([guid filename input] (f *current-credentials* guid filename input))
     ([credentials guid filename input]
     (do-request :PUT endpoint credentials :guid guid :attachments "Attachments" :filename filename :input-stream input)))})

(defmethod client-fns :default [_ _ _] nil)

(defn- intern-fn
  [ns f action endpoint]
  (when f
    (intern ns (fn-name action endpoint) f)))

(defn set-client!
  [endpoint actions ns & {:as options}]
  {:pre [(keyword? endpoint) (set? actions)]}
  (doseq [action actions]
    (doseq [[sym f] (client-fns action endpoint options)]
      (intern ns sym f))))

