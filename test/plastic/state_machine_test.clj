(ns plastic.state-machine-test
  (:refer-clojure :exclude [identity])
  (:require [clojure.test :refer [are deftest is testing]]
            [plastic.state-machine :refer :all]
            [plastic.useful :refer [kw cartesian]]))

(def blank (blank-state-machine :idle))
(def transitions-of (comp set :transitions))

(defn mark-tn
  [n]
  (fn [state ui-state event]
    (let [kw (keyword (str "t" n))]
      (update state :key (fnil #(conj % kw) [])))))

(deftest compose-transitions-test
  (testing "transitions compose"
    (let [t1 (fn [state ui-state event] (assoc state :t1 [ui-state event]))
          t2 (fn [state ui-state event] (assoc state :t2 [ui-state event]))
          composed (compose-transitions t1 t2)
          result (composed {} :ui :event)]
      (is (= [:ui :event] (:t1 result))
          (= [:ui :event] (:t2 result))))

    (testing "in the correct direction"
      (let [t1 (mark-tn 1)
            t2 (mark-tn 2)
            composed (compose-transitions t2 t1)
            result (composed {} :ui :event)]
        (is (= [:t1 :t2] (:key result)))))

    (testing "when there are more than two"
      (let [t1 (mark-tn 1)
            t2 (mark-tn 2)
            t3 (mark-tn 3)
            composed (compose-transitions t3 t2 t1)
            result (composed {} :ui :event)]
        (is (= [:t1 :t2 :t3] (:key result)))))))

(deftest add-transition-test
  (testing "binary arity of add-transition"
    (is (-> (add-transition blank [:idle :active :event])
          transitions-of
          (contains? {:from :idle, :to :active, :on :event
                      :update identity})))

    (is (-> (add-transition blank [:idle :active :event inc])
          transitions-of
          (contains? {:from :idle, :to :active, :on :event
                      :update inc}))))

  (testing "type validation"
    (are [t] (thrown? IllegalArgumentException t)
      (add-transition blank "start" :stop :mouse-up)
      (add-transition blank :start "stop" :mouse-up)
      (add-transition blank :start :stop "mouse-up"))

    (are [t] (is t) ; roundabout assertion that exception is not thrown
      (add-transition blank :start :stop :mouse-up)
      (add-transition blank :start (constantly :stop) :mouse-up)))

  (testing "dispatched transitions"
    (let [dispatch (constantly :active)]
      (is (contains?
            (transitions-of (add-transition blank :idle dispatch :event))
            {:from :idle, :to dispatch, :on :event, :update identity}))

      (is (contains?
            (transitions-of (add-transition blank :idle dispatch :event inc))
            {:from :idle, :to dispatch, :on :event, :update inc}))))

  (testing "event recognition fns"
    (let [parity (blank-state-machine :even)
          const-odd (constantly :odd)]
      (is (contains?
            (transitions-of (add-transition parity :even :odd odd?))
            {:from :even, :to :odd, :on odd?, :update identity}))
      (is (contains? (transitions-of (add-transition parity :even :odd odd? const-odd))
                     {:from :even, :to :odd, :on odd?, :update const-odd})))))

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

(deftest dispatched-transition?-test
  (testing "dispatched-transition?"
    (testing "finds dispatched transitions"
      (are [x] (dispatched-transition? (update x :transitions shuffle))
        (add-transition blank :idle (constantly :active) :_)

        (-> blank
          (add-transition :idle :active :_)
          (add-transition :active #(rand-nth [:idle :active]) :_))

        (-> (blank-state-machine :neutral)
          (add-transition :neutral :1st-gear :go)
          (add-transition :1st-gear :2nd-gear :5mph)
          (add-transition :2nd-gear #(rand-nth [:3rd-gear :1st-gear]) :traffic))

        (-> (reduce add-i-to-i+-transition blank (range 20))
          (add-transition :idle :state-0 :_)
          (add-transition :state-20 #(rand-nth [:idle :state-10]) :_))))

    (testing "returns false for state machines without dispatched transitions"
      (are [x] (not (dispatched-transition? x))
        blank
        (add-transition blank :idle :active :_)

        (-> blank
          (add-transition :idle :active :_)
          (add-transition :active :idle :_))

        (reduce add-i-to-i+-transition blank (range 20))))))

(deftest connected?-test
  (testing "trivial state machines are trivially connected "
    (is (connected? (blank-state-machine :idle))))

  (testing "cycles in the state machine are not required"
    ;; but maybe they should be?
    (is (connected? (-> (blank-state-machine :start)
                      (add-transition :start :a-state :_)
                      (add-transition :a-state :b-state :_)
                      (add-transition :b-state :c-state :_)))))

  (testing "the state-walking logic can handle multiple branches"
    (is (connected? (-> (blank-state-machine :state-0)
                      (add-transition :state-0 :state-1 :_)
                      (add-transition :state-0 :state-2 :_)
                      (add-transition :state-0 :state-3 :_)
                      (add-transition :state-3 :state-4 :_)))))

  (testing "a lot of transitions still works in the positive case"
    (is (connected? (reduce add-i-to-i+-transition
                            (blank-state-machine :state-0)
                            (range 33)))))

  (testing "simplest disconnected case"
    (is (not (connected? (-> (blank-state-machine :oregon)
                           (add-transition :minnesota :south-dakota :road-trip))))))

  (testing "the next simplest disconnected case"
    (is (not (connected? (-> (blank-state-machine :start)
                           (add-transition :start :next :_)
                           (add-transition :zarquat :plaquitrax :_))))))

  (testing "disconnected state not in last transition added"
    (is (not (connected? (-> (blank-state-machine :state-0)
                           (add-transition :state-0 :state-1 :_)
                           (add-transition :state-1 :state-2 :_)
                           (add-transition :oh :how-did-i-get-here :_)
                           (add-transition :state-2 :state-0 :_))))))

  (testing "disconnected start"
    (is (not (connected? (-> (blank-state-machine :start)
                           (add-transition :not-start :also-not-start :_)
                           (add-transition :also-not-start :this-is-way-out-there :_)))))

    (testing "with lot of transitions"
      (is (not (connected? (-> (reduce add-i-to-i+-transition
                                       (blank-state-machine :idle)
                                       (range 33))
                             (add-transition :nowhere :noplace :_))))))))

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
              (cartesian (range 10) (range 10 20)))))

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
      (let [w-many-unique (reduce add-i-to-i+-on-ev-transition
                                  (blank-state-machine :state-0)
                                  (cartesian (range 10) (range 10)))
            duped (add-i-to-i+-on-ev-transition
                    w-many-unique [(rand-int 10) (rand-int 10)])]
        (reduce add-i-to-i+-on-ev-transition
                duped
                (cartesian (range 10 20) (range 10 20)))))))

(deftest no-skipped-transitions?-test
  (are [x] (no-skipped-transitions? x)
    blank
    (-> blank
      (add-transition :idle :active :mouse-down))
    (-> blank
      (add-transition :idle :active :mouse-down)
      (add-transition :idle :ui-popover :key-press))
    (-> blank
      (add-transition :idle   :active :mouse-down)
      (add-transition :active :apply  :key-press)
      (add-transition :apply  :idle   :skip)))

  (are [x] (not (no-skipped-transitions? x))
    (-> blank
      (add-transition :idle :primed :skip)
      (add-transition :idle :active :mouse-down))
    (-> blank
      (add-transition :idle :active :mouse-down)
      (add-transition :idle :popover :key-press)
      (add-transition :active :apply :mouse-up)
      (add-transition :apply :idle :skip)
      (add-transition :apply :cancel :key-down))))

(deftest valid?-test
  (is (valid? (-> blank
                (add-transition :idle :active :input)
                (add-transition :active :idle :end-of-input))))

  (testing "valid? tests for"
    (testing "start state presence"
      (is (not (valid? {}))))

    (testing "transition uniqueness"
      (is (not (valid? (-> blank
                         (add-transition :idle :heisenbergs_house :observation)
                         (add-transition :idle :bohrs_house :observation))))))

    (testing "skipped transitions"
      (is (not (valid? (-> blank
                         (add-transition :idle :primed :skip)
                         (add-transition :idle :active :mouse-down))))))

      (testing "connectedness"
        (is (not (valid? (add-transition blank :elswyr :skyrim :wanderlust))))

        (testing "unless there's a dispatched transition"
          (is (valid? (add-transition blank
                                      :elswyr, #(rand-nth [:skyrim :morrowind])
                                      :wanderlust)))))))
