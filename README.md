# clj-xero

A simple clojure wrapper to the [Xero](http://xero.com) API.

[API Docs](http://icm-consulting.bitbucket.org/clj-xero/)

## Usage

### Install

Leiningen:

[![Clojars Project](https://img.shields.io/clojars/v/clj-xero.svg)](https://clojars.org/clj-xero)

### Authentication / Authorisation

clj-xero supports public, private and partner (the 3 P's) application authorisation schemes.

The end goal of each authentication/authorisation flow is to retrieve a set of ```credentials```, which are passed to each service call function.

All authorisation and credential functions are located in the ```clj-xero.core``` namespace.

#### Private applications
Private is the simplest authorisation scheme to use - no need to run through an oauth workflow in order to access Xero services.
Private authorisation involves creating credentials, using your private key that matches the public key registered with your Xero organisation that will be accessed.

```clojure
(require '[clj-xero.core :as xero])

(def my-consumer-key "AAA222XXX")
(def my-consumer-secret "TOTALLYSECRET")
(def my-private-key (slurp "my-private-key.pem"))
(def credentials (xero/private-credentials my-consumer-key
                                           my-consumer-secret
                                           my-private-key))
```

#### Public applications
[Xero public authorisation](https://developer.xero.com/documentation/getting-started/public-applications/) involves taking your end users through the Xero oauth workflow to provide your app with authorisation to access their organisation.

```clojure
;; 1. Create a public "consumer" with your consumer and secret keys
(def consumer (xero/public-consumer my-consumer-key my-secret-key))

;; 2. When you need to request authorisation - call this to retrieve the
;; URL to send the user to. Pass in the full callback URL that the user
;; will be sent to after they've authorised your app
(def request-token (xero/authorisation-request-token consumer "http://localhost:8080/callback"))
;; => {:authorisation-url "https://api.xero.com/oauth/Authorize..."
;;     :oath_token "BLAH"
;;     :oauth_token_secret "SHHH" ...}

;; 3. Once authorisation was received via your callback endpoint, generate
;; credentials which will be passed to each service function call.
;; The verifier token (the last arg) will be passed by Xero to your callback URL.
(defn credentials (xero/public-authorised-credentials consumer request-token "VERIFIER_TOKEN"))

```

#### Partner applications
[Xero partner](https://developer.xero.com/documentation/getting-started/partner-applications/) authorisation workflow is similar to Public applications. You will need to provide the entrust certificate that Xero provides you with when setting up the partner application.

```clojure
;; 1. Create a partner "consumer" with your consumer key and the private key of your application
(def consumer (xero/partner-consumer my-consumer-key
                                     my-private-key))
;; 2. Entrust keystore properties
(def entrust-keystore {:keystore "path/to/keystore" ;;or instance of java.security.KeyStore
                       :keystore-type "PKCS12"
                       :keystore-pass "bPeKdwY4RQjyhGtqj2MV"})

;; 2. Get the fetch token, with the authorisation URL. This differs slightly
;; from the public application; the path to the entrust keystore, and its
;; password, are passed as the 3rd argument
(def request-token (xero/authorisation-request-token consumer
                                                "http://localhost:3000/callback"
                                                entrust-keystore))

;; 3. After authorisation, fetch the credentials
(def credentials (xero/partner-authorised-credentials consumer request-token "VERIFIER_TOKEN" entrust-keystore))

```

### Throttling

TODO

### Services

TODO

## Change Log

### 0.5.3
- Add function for refreshing partner credentials

## License

Copyright © 2016 [ICM Consulting Pty Ltd]("http://www.icm-consulting.com.au")

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
