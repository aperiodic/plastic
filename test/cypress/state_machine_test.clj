(ns cypress.state-machine-test
  (:require [clojure.test :refer [are deftest is testing]]
            [cypress.state-machine :refer :all]
            [cypress.useful :refer [kw cartesian]]))

(def blank (blank-state-machine :idle))

(deftest add-transition-test
  (testing "binary arity of add-transition"
    (is (-> (add-transition blank [:idle :active :event])
          :transitions
          set
          (contains? {:from :idle, :to :active, :on :event
                      :update identity-update})))

    (is (-> (add-transition blank [:idle :active :event inc])
          :transitions
          set
          (contains? {:from :idle, :to :active, :on :event
                      :update inc})))))

(deftest has-start-state?-test
  (testing "state machines gotta be something with a :start entry"
    (is (has-start-state? (blank-state-machine :idle)))
    (is (has-start-state? {:start :idle}))
    (is (not (has-start-state? {})))
    (is (not (has-start-state? [{}])))
    (is (not (has-start-state? nil)))))

(defn add-i-to-i+-transition
  [sm i]
  (add-transition sm (kw "state-" i) (kw "state-" (inc i)) :event))

(defn add-i-to-i+-on-ev-transition
  [sm [si ei]]
  (add-transition sm
                  (kw "state-" si)
                  (kw "state-" (inc si))
                  (kw "event-" ei)))

(deftest connected?-test
  (are [x] (connected? x)
    (testing "trivial state machines are trivially connected "
      (blank-state-machine :idle))

    (testing "cycles in the state machine are not required"
      ;; but maybe they should be?
      (-> (blank-state-machine :start)
        (add-transition :start :a-state :_)
        (add-transition :a-state :b-state :_)
        (add-transition :b-state :c-state :_)))

    (testing "the state-walking logic can handle multiple branches"
      (-> (blank-state-machine :state-0)
        (add-transition :state-0 :state-1 :_)
        (add-transition :state-0 :state-2 :_)
        (add-transition :state-0 :state-3 :_)
        (add-transition :state-3 :state-4 :_)))

    (testing "a lot of transitions still works in the positive case"
        (reduce add-i-to-i+-transition
                (blank-state-machine :state-0)
                (range 1e3))))

  (are [x] (not (connected? x))
    (testing "simplest disconnected case"
      (-> (blank-state-machine :oregon)
        (add-transition :minnesota :south-dakota :road-trip)))

    (testing "the next simplest"
      (-> (blank-state-machine :start)
        (add-transition :start :next :_)
        (add-transition :zarquat :plaquitrax :_)))

    (testing "disconnected state not in last transition added"
      (-> (blank-state-machine :state-0)
        (add-transition :state-0 :state-1 :_)
        (add-transition :state-1 :state-2 :_)
        (add-transition :oh :how-did-i-get-here :_)
        (add-transition :state-2 :state-0 :_)))

    (testing "disconnected start"
      (-> (blank-state-machine :start)
        (add-transition :not-start :also-not-start :_)
        (add-transition :also-not-start :this-is-way-out-there :_))

      (testing "with lot of transitions"
        (reduce add-i-to-i+-transition
                (blank-state-machine :start-disconnected)
                (range 1e3))))))

(deftest unique-transitions?-test
  (are [x] (unique-transitions? x)
    (testing "no transitions are trivially unique"
      (blank-state-machine :idle))

    (testing "one transition is also unique"
      (-> blank
        (add-transition :idle :active :_)))

    (testing "lots of unique transitions are still unique"
      (reduce add-i-to-i+-on-ev-transition
              (blank-state-machine :state-0)
              (cartesian (range 100) (range 100)))))

  (are [x] (not (unique-transitions? x))
    (testing "simplest duplicate"
      (-> (blank-state-machine :start)
        (add-transition :start :next :_)
        (add-transition :start :next :_)))

    (testing "next simplest duplicate"
      (-> (blank-state-machine :start)
        (add-transition :start :next :_)
        (add-transition :next :the-one-after-next :_)
        (add-transition :start :next :_)))

    (testing "transitions to different states can still be duplicates"
      (-> (blank-state-machine :start)
        (add-transition :start :rome :the-event)
        (add-transition :start :damascus :the-event)))

    (testing "needle in the haystack"
      (let [w-10k-unique (reduce add-i-to-i+-on-ev-transition
                                 (blank-state-machine :state-0)
                                 (cartesian (range 100) (range 100)))
            duped (add-i-to-i+-on-ev-transition
                    w-10k-unique [(rand-int 100) (rand-int 100)])]
        (reduce add-i-to-i+-on-ev-transition
                duped
                (cartesian (range 100 200) (range 100 200)))))))

(deftest valid?-test
  (is (valid? (-> blank
                (add-transition :idle :active :input)
                (add-transition :active :idle :end-of-input))))

  (testing "valid? appears to test for"
    (are [x] (not (valid? x))
      (testing "start state presence"
        {}))

    (testing "transition uniqueness"
      (-> blank
        (add-transition :idle :rome :saturnalia)
        (add-transition :idle :rome :coronation)))

    (testing "connectedness"
      (-> blank
        (add-transition :elswyr :skyrim :boredom)))))
