(ns leiningen.pedestal-service-docs
  (:require [leiningen.core.eval :refer [eval-in-project]]))

(defn head [project]
  [:head
   [:title (str (:name project) " API documentation")]
   [:style (slurp (clojure.java.io/resource "docs.css"))]])

(defn pedestal-service-docs
  "Generate Pedestal service docs"
  [project & args]
  (let [profile {:dependencies [['pedestal-service-docs "0.1.0-SNAPSHOT"]]}
        project (leiningen.core.project/merge-profiles project [profile])
        service-ns (str (:name project) ".service")
        service-ns-sym (symbol service-ns)
        service (symbol service-ns "service")
        head (head project)]
    (let [body (eval-in-project project
                                `(do
                                   (println "Generating docs at docs/api.html")
                                   (let [docs# (pedestal-service-docs.core/generate-docs
                                                ~head
                                                ~service)
                                         out-file# (clojure.java.io/file "docs" "api.html")]
                                     (clojure.java.io/make-parents out-file#)
                                     (spit out-file# docs#))
                                   (println "Done!")
                                   (System/exit 0))
                                `(require '~service-ns-sym
                                          'pedestal-service-docs.core))])))
