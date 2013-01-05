# geohash

A Clojure library for computing geohashes.

You are responsible for [acquiring DJIA data](http://wiki.xkcd.com/geohashing/Dow_Jones_Industrial_Average),
but this library will at least tell you which date to acquire it for.

* Fully 30W-compliant
* Negative-zero compliant
* Avoids the scientific notation bug

## Usage

Add as a Leiningen dependency:

```clojure
[org.timmc/geohash "1.0.0"]
```

Compute a geohash:

```clojure
(require '[org.timmc.geohash :as gh])
;;= nil

(gh/dow-date 42.37 -0.5 (org.joda.time.LocalDate. 2012 3 5))
;;= #<LocalDate 2012-03-04> ;; different from input due to 30W rule

(slurp (java.net.URI. "http://geo.crox.net/djia/2012/03/04"))
;;= "12980.75"

(gh/geohash 42.37 -0.5 (org.joda.time.LocalDate. 2012 3 5) "12980.75")
;;= [42.71583060829183 -0.17616573365392707]
```

See the org.timmc.geohash namespace documentation for more information:

```clojure
(-> (the-ns 'org.timmc.geohash) meta :doc println)
```

## Building

Built with Leiningen 2.x, although 1.x should work as well.

## License

Copyright © 2013 Tim McCormack

Distributed under the Eclipse Public License, the same as Clojure.
