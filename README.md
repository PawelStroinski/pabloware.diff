# pabloware.diff

Readable diff & patch lib. Best used with maps.

[![Clojars Project](https://img.shields.io/clojars/v/pabloware.diff.svg)](https://clojars.org/pabloware.diff)

## Rationale

While there are many diff & patch libs around there, I could not find one which *shows* the shape of a diffed data in the diff itself, i.e. I wanted a diff of a map to be a map. Also, it should be able to use that diff to do patching.

This lib does exactly that but is very basic in terms of diffing lists and vectors. There are alternative libs for that.

## Usage

```clojure
=> (require '[pabloware.diff :as p])

=> (def band
     {:id     1
      :name   "Esbjörn Svensson Trio"
      :active {:from 1993}
      :genres #{:jazz :instrumental-music}
      :albums [{:title "When Everyone Has Gone", :year  1993}
               {:title "EST Plays Monk"          :year  1996}]})

=> (-> band
       (dissoc :id)
       (assoc-in [:active :to] 2008)
       (update :genres conj :bepop)
       (update :albums conj {:title "Winter in Venice", :year 1997}))

=> (p/diff band *1)
{:id     ::p/-
 :active {:to 2008}
 :genres #{:bepop}
 :albums [::p/+ {:title "Winter in Venice", :year 1997}]}

=> (= *2 (p/patch band *1))
true
```

The test namespace contains edge cases as well as a generative test using spec.

## Bullet points

Goals:

* Straightforward implementation
* Easy to read by humans diffs, especially for maps

Nice-to-haves:

* Small diff size

Non-goals:

* Speed

## License

Copyright © 2019 Paweł Stroiński

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
