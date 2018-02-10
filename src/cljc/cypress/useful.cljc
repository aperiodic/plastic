(ns cypress.useful)

(defn kw
  "Take any number of things and turn them into a keyword of their string
  representation. Multiple arguments are concatenated with no intervening
  characters after stringifying, then keywordified."
  ([x] (-> x str keyword))
  ([x0 x1 & xs] (kw (apply str (concat [x0 x1] xs)))))

(defn cartesian
  "Given two collections, return the cartesian product of 2-vecs with an
  element"
  [xs ys]
  (for [x xs, y ys] [x y]))
