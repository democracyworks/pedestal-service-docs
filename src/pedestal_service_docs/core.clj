(ns pedestal-service-docs.core
  (:require [hiccup.page :as page]
            [hiccup.util :as hutil]
            [clojure.string :as s]
            [schema.core :as schema]))

(defn keyword->var [keyword]
  (find-var (symbol (namespace keyword)
                    (name keyword))))

(defn find-schema [interceptor]
  (-> interceptor
      meta
      :schema))

(defn route-data [route]
  (let [{:keys [path-params path method interceptors query-constraints]} route
        schema (some find-schema interceptors)
        terminal-interceptor (last interceptors)
        method (-> method name .toUpperCase)
        metadata (-> terminal-interceptor
                     :name
                     keyword->var
                     meta)
        doc (:doc metadata)
        data {:path path
              :method method
              :doc doc}]
    (cond-> data
            schema (assoc :schema schema)
            (not (empty? path-params)) (assoc :path-params path-params)
            (not (empty? query-constraints)) (assoc :query-constraints
                                               query-constraints))))

(defn route-id [route-data]
  (str (:method route-data)
       (:path route-data)))

(defn route-anchor [route-data]
  (->> route-data
       route-id
       (str "#")))

(defn schema-key->html [key]
  (if (schema/optional-key? key)
    [:span.optional-key (:k key)]
    [:span.required-key key]))

(defn schema->html [schema]
  [:table.schema-code
   (for [[k v] schema]
     [:tr
      [:td (schema-key->html k)]
      [:td
       (cond (map? v) (schema->html v)
             (class? v) (.getSimpleName v)
             :else v)]])])

(defn route-data->html [route-data]
  (let [{:keys [path method doc schema
                path-params query-constraints]} route-data
        base-html [:div.route {:id (route-id route-data)}
                   [:div.method-path
                    [:div.method method]
                    [:div.path path]]
                   [:div.doc doc]]]
    (cond-> base-html
            schema (conj [:div.schema
                          "Schema:" (schema->html schema)])
            path-params (conj [:div.path-params
                               "Path params: " (hutil/as-str path-params)])
            query-constraints (conj [:div.query-constraints
                                     "Query constraints: "
                                     (hutil/as-str query-constraints)]))))

(defn route-data->toc [route-data]
  (let [{:keys [method path]} route-data]
    [:li
     [:a {:href (route-anchor route-data)}
      (str method " " path)]]))



(defn generate-docs [head service]
  (let [service (if (fn? service) (service) service)
        routes (:io.pedestal.service.http/routes service)
        routes-data (map route-data routes)]
    (page/html5
     head
     [:body
      [:ul#toc
       (map route-data->toc routes-data)]
      (map route-data->html routes-data)])))
