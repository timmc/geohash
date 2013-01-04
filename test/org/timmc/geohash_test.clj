(ns org.timmc.geohash-test
  (:use clojure.test
        org.timmc.geohash)
  (:import (org.joda.time LocalDate)))

(defn prfloat
  [digits x]
  (format (str "%." digits "f") x))

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
         (= (map #(prfloat 5 %) (geohash ilat ilon date djia))
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
  (is (= (prfloat 7 (hex-to-fractional "8b672cb305440f97"))
         "0.5445431")))

(deftest hashing
  (testing "formatting"
    (is (= (format-date (LocalDate. 2005 5 26)) "2005-05-26")))
  (testing "hash step"
    (is (= (hexify (hash-data (LocalDate. 2005 5 26) "10458.68"))
           "db9318c2259923d08b672cb305440f97"))))

(deftest geohashes
  (testing "original comic"
    (let [[lat lon] (geohash 37.42 -122.08
                             (LocalDate. 2005 5 26) "10458.68")]
      (is (= (prfloat 6 lat) "37.857713"))
      ;; The comic is wrong on this one, ending with 4 instead of 3
      (is (= (prfloat 6 lon) "-122.544543"))))
  (testing "own hash"
    (let [[lat lon] (geohash 38.5 -78.5
                             (LocalDate. 2008 6 8) "12602.74")]
      (is (= (prfloat 6 lat) "38.047582"))
      (is (= (prfloat 6 lon) "-78.230916")))))

(deftest fractions
  (testing "Fractional coordinates"
    (let [[latf lonf] (fractional (LocalDate. 2012 2 26) "12981.20")]
      (is (= (prfloat 6 latf) "0.000047"))
      (is (= (prfloat 6 lonf) "0.483719")))
    (let [[latf lonf] (fractional (LocalDate. 2008 5 30) "12593.87")]
      (is (= (prfloat 5 latf) "0.32272"))
      (is (= (prfloat 5 lonf) "0.70458")))))

(deftest tricky-things
  (testing "Avoid scientific notation bug"
    (is (= (->> (geohash 68.0 -35.0 (LocalDate. 2012 2 26) "12981.20")
                first
                (prfloat 6))
           "68.000047")))
  (testing "Negative zero"
    (let [date (LocalDate. 2011 4 15)
          neg (geohash -0.2 5.5 date "1234.56")
          pos (geohash +0.2 5.5 date "1234.56")]
      (is (= (first neg) (- (first pos)))))))

(deftest globalhashes
  (testing "Sample from 30W page"
    (are [date djia olat olon]
         (= (map #(prfloat 5 %) (globalhash date djia))
            [olat olon])
         ;; before the 30W switchover, globalhashes *still* used previous day
         (LocalDate. 2008 5 26) "12620.90" "31.16306" "38.63088"
         ;; after
         (LocalDate. 2008 5 30) "12593.87" "-31.91030" "73.65004")))
