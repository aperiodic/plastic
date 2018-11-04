(ns cypress.examples.calculator.util)

(defn invert-map
  "Note: assumes map encodes a bijection! If it's not 1:1, then the inverse
  mapping is undefined behavior."
  [m]
  (into {} (for [[k v] m]
             [v k])))
