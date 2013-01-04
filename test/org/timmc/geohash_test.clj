(ns org.timmc.geohash-test
  (:use clojure.test
        org.timmc.geohash)
  (:import (org.joda.time LocalDate)))

(deftest thirty-west-rule
  (testing "Own data"
    (let [before (LocalDate. 2008 1 1)
          on (LocalDate. 2008 5 27)
          on-1 (LocalDate. 2008 5 26)
          after (LocalDate. 2008 12 21)
          after-1 (LocalDate. 2008 12 20)]
      (are [lon cdate ddate] (= (dow-date 5 lon cdate) ddate)
           ;; no difference before
           -29 before before
           -31 before before
           -31 on-1 on-1
           ;; a range of 30W corrections
           100 on on-1
           -29.99 on on-1
           ;; W coords
           -30 on on
           ;; later is same as first day of rule
           -29.5 after after-1
           ;; later, west
           -30.5 after after)))
  (testing "Compliance matrix from wiki"
    (are [date djia ilat ilon olat olon]
         (= (map #(format "%.5f" %) (coordinates ilat ilon date djia))
            [olat olon])
         ;; before
         (LocalDate. 2008 5 26) "12620.90" 68.0 -30.0 "68.67313" "-30.60731"
         (LocalDate. 2008 5 26) "12620.90" 68.0 -29.0 "68.67313" "-29.60731"
         ;; after -- note the mismatched DOW dates
         (LocalDate. 2008 5 27) "12479.63" 68.0 -30.0 "68.20968" "-30.10144"
         (LocalDate. 2008 5 27) "12620.90" 68.0 -29.0 "68.12537" "-29.57711")))

(deftest math
  (is (= (hex-to-fractional "100008")
         0.062500476837158203125M))
  (is (= (format "%.7f" (hex-to-fractional "8b672cb305440f97"))
         "0.5445431")))

(deftest hashing
  (testing "formatting"
    (is (= (format-date (LocalDate. 2005 5 26)) "2005-05-26")))
  (testing "hash step"
    (is (= (hexify (hash-data (LocalDate. 2005 5 26) "10458.68"))
           "db9318c2259923d08b672cb305440f97"))))

(deftest the-original-comic
  (testing "final-output"
    (let [[lat lon] (coordinates 37.42 -122.08
                                 (LocalDate. 2005 5 26) "10458.68")]
      (is (= (format "%.6f" lat) "37.857713"))
      ;; The comic is wrong on this one, ending with 4 instead of 3
      (is (= (format "%.6f" lon) "-122.544543")))))

(deftest tricky-things
  (testing "Avoid scientific notation bug"
    (is (= (->> (coordinates 68.0 -35.0 (LocalDate. 2012 2 26) "12981.20")
                first
                (format "%.6f"))
           "68.000047")))
  (testing "Negative zero"
    (let [date (LocalDate. 2011 4 15)
          neg (coordinates -0.2 5.5 date "1234.56")
          pos (coordinates +0.2 5.5 date "1234.56")]
      (is (= (first neg) (- (first pos)))))))
