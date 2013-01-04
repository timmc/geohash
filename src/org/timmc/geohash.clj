(ns org.timmc.geohash
  "All dates are Joda Time dates."
  (:import (java.math BigDecimal)
           (org.joda.time LocalDate)
           (org.joda.time.format DateTimeFormat)))

;; http://wiki.xkcd.com/wgh/index.php?title=30W_Time_Zone_Rule&oldid=5846
(def first-day-of-30W
  (LocalDate. 2008 5 27))

(defn dow-date
  "Derive the DJIA source date, for a given location and hashing date."
  [lat lon ^LocalDate date]
  (if (and (> (int lon) -30) ;; "locations east of latitude 30W"
           (<= 0 (.compareTo date first-day-of-30W)))
    (.minusDays date 1)
    date))

(def date-formatter
  (DateTimeFormat/forPattern "yyyy-MM-dd"))

(def hex-vals
  {\0 0 \1 1 \2 2 \3 3 \4 4 \5 5 \6 6 \7 7 \8 8 \9 9
   \a 10 \b 11 \c 12 \d 13 \e 14 \f 15})

(defn ^:internal hex-to-fractional
  "Convert a lowercase hex string into a BigDecimal fractional part."
  [hex]
  {:post [(<= 0 % 1)]}
  (reduce (fn [accum c]
            (.. accum
                (add (BigDecimal. (hex-vals c)))
                (divide 16M)))
          0M (reverse hex)))

(defn ^:internal format-date
  [date]
  (.print date-formatter date))

(defn ^:internal hash-data
  "Hash the standard input data into a 16 byte array."
  [date djia]
  (.digest (java.security.MessageDigest/getInstance "MD5")
           (.getBytes (format "%s-%s" (format-date date) djia) "UTF-8")))

(defn ^:internal hexify
  "Convert a byte array into an even-length lowercase hex string."
  [bs]
  (apply str (map #(format "%02x" %) bs)))

(defn coordinates
  "Compute coordinates from location, date, and opening DJIA.
Input coordinates must be floating-point so as to preserve negative zero.
Output is [lat, lon] as floating point."
  [lat lon ^LocalDate date ^String djia]
  {:pre [(float? lat) (float? lon) (cast LocalDate date), (cast String djia)]}
  (let [hash-bytes (hash-data date djia)
        hash-str (hexify hash-bytes)
        mk-coord (fn [given hex]
                   (* (Math/signum given)
                      (+ (int (Math/abs given))
                         (.doubleValue (hex-to-fractional hex)))))]
    [(mk-coord lat (.substring hash-str 0 16))
     (mk-coord lon (.substring hash-str 16))]))
