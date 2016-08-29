(ns org.timmc.geohash
  "A library for calculating coordinates when provided with location,
local date, and the Dow Jones Industrial Average (DJIA) opening value for the
appropriate date. You are responsible for retrieving the DJIA data.

For geohashes:

* #'dow-date calculates which date to use for DJIA data (30W compliant)
* #'geohash calculates the local geohash coordinates

Similarly, #'globalhash-dow-date and #'globalhash are available for
calculating globalhashes.

All dates are Joda Time LocalDate objects. Input latitude and longitude
as floating-point numbers to preserve negative zero. (Enforced.)"
  (:import (java.math BigDecimal)
           (org.joda.time LocalDate)
           (org.joda.time.format DateTimeFormat)))

;; http://wiki.xkcd.com/wgh/index.php?title=30W_Time_Zone_Rule&oldid=5846
(def first-day-of-30W
  (LocalDate. 2008 5 27))

(defn dow-date
  "Derive the DJIA source date, for a given location and geohashing date.
Also see #'globalhash-dow-date."
  [lat lon ^LocalDate date]
  (if (and (> (int lon) -30) ;; "locations east of latitude 30W"
           (<= 0 (.compareTo date first-day-of-30W)))
    (.minusDays date 1)
    date))

(defn globalhash-dow-date
  "Just returns the previous day's date.
Also see #'dow-date for normal geohashes."
  [^LocalDate date]
  (.minusDats date 1))

(def ^:internal date-formatter
  (DateTimeFormat/forPattern "yyyy-MM-dd"))

(def ^:internal hex-vals
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

(defn fractional
  "Produce fractional coordinates. Output is [lat lon] as floating point,
each in the interval [0,1)."
  [^LocalDate date ^String djia]
  {:pre [(cast LocalDate date), (cast String djia)]}
  (let [hash-str (hexify (hash-data date djia))
        mk-coord #(.doubleValue (hex-to-fractional %))]
    [(mk-coord (.substring hash-str 0 16))
     (mk-coord (.substring hash-str 16))]))

(defn geohash
  "Compute geohash coordinates from location, date, and opening DJIA.
Input coordinates must be floating-point so as to preserve negative zero.
Output is [lat, lon] as floating point."
  [lat lon date djia]
  {:pre [(float? lat) (float? lon)]}
  (letfn [(mk-coord [given frac]
            (* (Math/signum given)
               (+ (int (Math/abs given))
                  frac)))]
    (vec (map mk-coord [lat lon] (fractional date djia)))))

(defn globalhash
  "Compute globalhash coordinates from the date and appropriate DJIA.
Use #'globalhash-dow-date to compute the DJIA source date.
Output is [lat, lon] as floating point."
  [date djia]
  (let [[latf lonf] (fractional date djia)]
    [(- (* 180 latf) 90)
     (- (* 360 lonf) 180)]))
