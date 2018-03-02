(defproject clj-xero "0.5.6-SNAPSHOT"
  :description "Clojure wrapper for the Xero API"
  :url "https://bitbucket.org/icm-consulting/clj-xero"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojars.eraffoul/clj-oauth "1.5.6-20160815.051845-1"]
                 [org.tobereplaced/lettercase "1.0.0"]
                 [inflections "0.9.14"]
                 [clj-time "0.12.0"]
                 [throttler "1.0.0"]
                 [clj-http "2.0.1"]]
  :plugins [[lein-codox "0.9.5"]]
  :codox {:output-path "doc"}

  :release-tasks [["vcs" "assert-committed"]
                  ["codox"]
                  ["change" "version"
                   "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  ["deploy" "clojars"]]

  :aliases {"release!" ["do" "clean" ["test"] ["install"] ["release"]]})
