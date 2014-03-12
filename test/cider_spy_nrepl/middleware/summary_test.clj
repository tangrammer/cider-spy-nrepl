(ns cider-spy-nrepl.middleware.summary-test
  (:require [cider-spy-nrepl.middleware.summary :refer :all]
            [clojure.test :refer :all])
  (:import [org.joda.time LocalDateTime Seconds]))

(deftest test-add-to-commands-frequency
  (is (= {'(println "Hi") 1}) (track-command {} {:code "(println \"Hi\")"})))

(deftest test-add-to-namespace-trail
  (let [trail (-> '()
                  (track-namespace {:ns 'user})
                  (track-namespace {:ns 'user})
                  (track-namespace {:ns 'bob}))]
    (is (= '(bob user) (map :ns trail)))))

(deftest test-show-summary
  (is (= "Your namespace trail:\n  bob (Am here)\n  user (29 seconds)"
         (sample-summary (list {:dt (LocalDateTime. 2010 1 1 0 0 30) :ns 'bob}
                               {:dt (LocalDateTime. 2010 1 1 0 0 1) :ns 'user}) {}))))

(deftest test-enrich-with-duration
  (is (= '(nil 29) (map :seconds (enrich-with-duration (list {:dt (LocalDateTime. 2010 1 1 0 0 30)}
                                                             {:dt (LocalDateTime. 2010 1 1 0 0 1)}))))))