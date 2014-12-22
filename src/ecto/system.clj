(ns ecto.system
  "Components and their dependency relationships"
  (:refer-clojure :exclude (read))
  (:require
   [clojure.java.io :as io]
   [clojure.tools.reader :refer (read)]
   [clojure.string :as str]
   [clojure.tools.reader.reader-types :refer (indexing-push-back-reader)]
   [com.stuartsierra.component :refer (system-map system-using using)]
   [modular.maker :refer (make)]
   [ecto.website :refer (new-website)]
   [modular.aleph :refer (new-http-server)]))

(defn ^:private read-file
  [f]
  (read
   ;; This indexing-push-back-reader gives better information if the
   ;; file is misconfigured.
   (indexing-push-back-reader
    (java.io.PushbackReader. (io/reader f)))))

(defn ^:private config-from
  [f]
  (if (.exists f)
    (read-file f)
    {}))

(defn ^:private user-config
  []
  (config-from (io/file (System/getProperty "user.home") ".ecto.edn")))

(defn ^:private config-from-classpath
  []
  (if-let [res (io/resource "ecto.edn")]
    (config-from (io/file res))
    {}))

(defn config
  "Return a map of the static configuration used in the component
  constructors."
  []
  (merge (config-from-classpath)
         (user-config)))

(defn http-server-components [system config]
  (assoc system
    :http-server
    (->
      (make new-http-server config)
      (using []))))

(defn website-components [system config]
  (assoc system
    :website
    (->
      (make new-website config)
      (using []))))

(defn new-system-map
  [config]
  (apply system-map
    (apply concat
      (-> {}
          (http-server-components config)
          (website-components config)))))

(defn new-dependency-map
  []
  {:http-server {:request-handler :website}})

(defn new-co-dependency-map
  []
  {})

(defn new-production-system
  "Create the production system"
  []
  (-> (new-system-map (config))
      (system-using (new-dependency-map))))
