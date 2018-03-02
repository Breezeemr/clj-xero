(ns clj-xero.test
  (:require
    [clj-xero.core :as core]
    [clj-xero.services.accounts :as accounts]
    [clj-xero.services.invoices :as invoices]
    [clj-xero.services.journals :as journals]
    [clj-xero.services.contacts :as contacts]
    [clj-xero.services.bank-transactions :as bank-transactions]
    [clj-xero.services.organisations :as organisations]
    [clj-xero.services.reports :as reports]
    [clojure.java.io :as io])
  (:import (java.security KeyStore)))

(comment
  ;;partner
  (def consumer (core/partner-consumer "ABC1234"
                                       "-----BEGIN RSA PRIVATE KEY-----
                                       ABC
                                       -----END RSA PRIVATE KEY-----"))

  (def entrust-store {:keystore "temp.p12"
                      :keystore-type "PKCS12"
                      :keystore-pass "bPeKdwY4RQjyhGtqj2MV"})


  (def entrust-store
    {:keystore (let [keystore (KeyStore/getInstance "PKCS12")]
                 (with-open [is (io/input-stream "temp.p12")]
                   (.load keystore is (.toCharArray "bPeKdwY4RQjyhGtqj2MV"))
                   keystore))
     :keystore-pass "bPeKdwY4RQjyhGtqj2MV"})

  (def request-token (core/authorisation-request-token consumer
                                                       "http://localhost:3000/callback"
                                                       entrust-store))

;;   (core/partner-authorised-credentials consumer request-token "4170502" entrust-store)


  ;; public consumer
  (def consumer (core/public-consumer "RHZLT4WRX9UZ9MSFBQEBE9KWPGWIYO"
                                      "3HHHUOZTGIUUAQCNPV6DQGPIUI1VP0"))

  (def request-token (core/authorisation-request-token consumer "http://localhost:8080/callback"))

  (def credentials (core/public-authorised-credentials consumer request-token "33214"))




  (accounts/get-all-accounts credentials)

  (accounts/get-account-by-guid credentials "13918178-849a-4823-9a31-57b7eac713d7")

  (invoices/get-all-invoices credentials)


  (invoices/get-invoice-by-guid credentials "ead21871-a8f4-4149-9612-30f00fcac54c")

  (invoices/add-invoices! credentials
                          {:type "ACCREC",
                           :contact {:name "Martin Hudson"},
                           :date "2015-08-05T00:00:00",
                           :due-date "2015-08-12T00:00:00",
                           :line-amount-types "Exclusive",
                           :line-items [
                            {:description "Monthly rental for property at 56a Wilkins Avenue",
                             :quantity "4.3400",
                             :unit-amount "395.00",
                             :account-code "200"}]})


  ;; private credentials
  (def consumer-key "UYD3XBTSFG7SCB5DJOKA5EJWSHFYWO")
  (def consumer-secret "SKAZWLAAYAGQCJDFNXSFPEZZXTKAQ0")
  (def credentials (core/private-credentials consumer-key
                                             consumer-secret
                                             "-----BEGIN RSA PRIVATE KEY-----
                                             MIICXAIBAAKBgQDtSt5iNxOpjDv1TwMDG1JMb3pkfQhxaWFMKLkLiLvfDkKglMSt
                                             qWneLARx+b5V3EeFjSuVEjWccb7YO5aIQxQQ0hEkTF7vN9yLS6RH6IFpLhIf0S7d
                                             9Bkd9yONWAWfqxuqbOyKvDvbikO+iyjxS7tT/BMJANKxefO298vdzB1eTQIDAQAB
                                             AoGAGl3BTAR2qNYuK1m2Kfg1Ms0IOnYyI/fjmcTEmuV8ipJZEOK239z9KHSXodpw
                                             LbYmNE61UwEM9+8jl383gLiDWAxRI94heS8VUhZM4AcB6CXELiQOYCNF+KvmezoQ
                                             7H1pm3c8PAMYzAJP/2rMR6WXmKn4KZ2T/lRz9SVAghUoQLkCQQD/L0xceVPbRdjG
                                             UZPjsjeXJXX6fCutSuuylcFZSqV9/I1xsEZBbfolCxGsq1Sg2si4ae+gWtqEX5Gv
                                             UmYu/pbLAkEA7gzv8EWIx7lRt32mu/ha34slb5rat6ZVE2/OFclxDyp4/O7z23PY
                                             JU8dIHPSvZs1gD0v9KAzeNaIYNBk7EckRwJAc6b2DrsWHDytoEP8qKdutlvN+nYo
                                             PWPFKqzgch14n37EhBAF50V2py87FWyY8EX3zkyEw8IpYvEFT9YiZY4QvQJBAIq7
                                             e3I58/cB57/aOLu/h6ZT/6NFSkZRZ6+GL0K/PWarSAuQbwnsP4Gu07jAB4d81vc/
                                             sZ0NaDH6RUy25rpU060CQGXIt3qUfAEqtULPnEx6Vk1URHR46GDG8ODd6XZyRKlQ
                                             oTnUKwHkNbS6vGMP6MGsG/g9I0BDJfb+QEhs2v2hets=
                                             -----END RSA PRIVATE KEY-----"))

  (organisations/get-all-organisations credentials)

  (accounts/get-all-accounts credentials)

  (core/with-credentials credentials
   (reports/get-balance-sheet {}))

  (reports/get-balance-sheet credentials {:date "2014-03-01"})

  (core/with-credentials credentials
    (reports/get-profit-and-loss {}))

  (reports/get-profit-and-loss credentials {:fromDate "2014-03-01" :to-date "2014-03-31"})

  (core/with-credentials credentials
    (reports/get-bank-statements "13918178-849a-4823-9a31-57b7eac713d7" {}))

  (reports/get-bank-statements credentials "13918178-849a-4823-9a31-57b7eac713d7" {})

  (core/with-credentials credentials
    (reports/get-published-reports))

  (reports/get-published-reports credentials)

  (core/with-credentials credentials
    (reports/get-published-report "F24EB3E0-6501-483C-A557-D15CEE7CD78A"))

  (reports/get-published-report credentials "F24EB3E0-6501-483C-A557-D15CEE7CD78A")

  (reports/get-report-by-guid credentials "F24EB3E0-6501-483C-A557-D15CEE7CD78A")

  (core/with-credentials credentials
    (invoices/add-invoices! [{:type "ACCREC",
                              :contact {:name "Peter Thingy"},
                              :date "2015-08-04T00:00:00",
                              :due-date "2015-08-15T00:00:00",
                              :line-amount-types "Exclusive",
                              :line-items [
                               {:description "Some rental for property at 56a Wilkins Avenue",
                                :quantity "4.6400",
                                :unit-amount "400.00",
                                :account-code "200"}]}
                             {:type "ACCREC",
                              :contact {:name "Martin Hudson"},
                              :date "2015-08-05T00:00:00",
                              :due-date "2015-08-12T00:00:00",
                              :line-amount-types "Exclusive",
                              :line-items [
                               {:description "Monthly rental for property at 56a Wilkins Avenue",
                                :quantity "4.3400",
                                :unit-amount "395.00",
                                :account-code "200"}]}]))

  (accounts/get-account-by-guid credentials "13918178-849a-4823-9a31-57b7eac713d7")

  (invoices/get-all-invoices credentials)

  (invoices/add-attachment! credentials "0009ecbb-2e3a-4c3b-bf4a-da94be009693" "berri.pdf" (clojure.java.io/input-stream (clojure.java.io/file "/home/kbergamin/Documents/sourceCode/payreq-mvp/resources/test-pdfs/berri.pdf")))

  (core/with-credentials credentials
    (invoices/get-invoice-attachments-by-guid "0009ecbb-2e3a-4c3b-bf4a-da94be009693"))



  ;;(invoices/get-invoice-attachments-by-guid-and-filename credentials "0009ecbb-2e3a-4c3b-bf4a-da94be009693" "BizTalk.pdf")

  (account/get-account-by-guid credentials "13918178-849a-4823-9a31-57b7eac713d7")


  (apply max (map :journal-number (journals/get-all-journals credentials)))

  (count (contacts/get-all-contacts credentials))



  )
