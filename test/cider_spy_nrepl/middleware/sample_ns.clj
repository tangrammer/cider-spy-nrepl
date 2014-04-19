(ns cider-spy-nrepl.middleware.sample-ns
  (:require [cider-spy-nrepl.middleware.sample-ns :refer :all]
            [clojure.test :refer :all]))

(defn foo
  "There currently has to be one public var in this ns for tracking to pick it up.
   This should not be needed and demonstrates a flaw in when tracking is applied."
  [])
