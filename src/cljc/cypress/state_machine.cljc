(ns cypress.state-machine
  (:refer-clojure :exclude [identity])
  (:require [clojure.string :as str]))

(defn blank-state-machine
  [start-state]
  {:start start-state
   :transitions ()})

(defn identity
  [app-state _ui-state _triggering-event]
  app-state)

(defn compose-transitions
  ([t1 t2]
   (fn [app-state ui-state event]
     (-> (t2 app-state ui-state event)
       (t1 ui-state event))))
  ([t1 t2 t3 & ts]
   (let [all-ts (reverse (concat [t1 t2 t3] (seq ts)))
         [tn tn1] (take 2 all-ts)
         other-ts (-> (drop 2 all-ts) reverse)]
     (apply compose-transitions
            (concat other-ts [(compose-transitions tn1 tn)])))))

(defn- name->culprit
  [nombre]
  (if (empty? nombre)
    "expecting"
    (str nombre " must be")))

(defn throw-unexpected-arg
  [& msg-parts]
  (let [msg (str/join " " msg-parts)]
    (throw #?(:clj (IllegalArgumentException. msg)
              :cljs (js/TypeError. msg)))))

(defn validate-kw
  ([x] (validate-kw x ""))
  ([x nombre]
   (when-not (keyword? x)
     (throw-unexpected-arg
       (name->culprit nombre) "a keyword, not a" (type x)))))

(defn validate-kw-or-fn
  ([x] (validate-kw-or-fn x ""))
  ([x nombre]
   (cond
     (keyword? x) nil
     (fn? x) nil
     :else (throw-unexpected-arg
             (name->culprit nombre) "a keyword or a function, not a" (type x)))))

(defn add-transition
  "Add a new transition to the given state machine description.

  `from` is a keyword specifying the state where the transition starts.

  `to` is either another keyword or a function that'll return a keyword; in
  both cases the keyword is the state where the transition ends.

  `on` is either a keyword specifying the kind of DOM event that should trigger
  the transition (e.g. :mousedown), or a recognition function that takes the
  triggering event and returns truthy if the transition should fire due to that
  event, or falsey if not. When `on` is a keyword, the transition is said to
  have a 'DOM' trigger; when it's a recognition function, a 'custom' trigger.

  The optional `update-state` argument is a function to call when this
  transition occurs that will update the application state.

  The `update-state` function is called with three arguments:
    * the current application state;
    * the new UI state (`to`) that the UI state machine has transitioned to;
    * the DOM event object that triggered the transition;
  and returns the new application state. If `update-state` is omitted, then it
  defaults to a function that returns the same application state it's given.

  If `to` is a function instead of a keyword, then the transition is called
  a 'dispatched' transition, and `to` is the dispatch function. When the
  transition is triggered, the dispatch function is called with two arguments:
    * the current application state;
    * the DOM event that triggered the transition;
  and returns the keyword of the state where the transition will go this time.

  Adding a dispatched transition does prevent cypress from checking a few
  properties of the state machine graph: since the behavior of the dispatch
  function is unknown, we can no longer guarantee that every state has some
  transition into it, nor that the graph is connected. The only thing cypress
  can check is that there aren't any conflicting transitions out of a state that
  have the same event type.

  Transitions with custom triggers (i.e. that have a recognition function for
  `on` instead of a DOM event keyword) is primarily useful when you're
  triggering your own custom events using a core.async channel passed to
  cypress's init fn; otherwise you should stick to the DOM triggers."
  ([state-machine transition-seq]
   (apply add-transition state-machine transition-seq))
  ([state-machine from to on]
   (add-transition state-machine from to on identity))
  ([state-machine from to on update-state]
   (validate-kw from "the state to trigger 'from'")
   (validate-kw-or-fn on "the type of event to trigger 'on'")
   (validate-kw-or-fn to "the state to transition 'to'")
   (update state-machine :transitions conj {:from from, :to to, :on on
                                            :update update-state})))

;; define 'add' as an alias for add-transition, with the same docstring
(let [add-t-meta (meta #'add-transition)]
  (def ^{:doc (:doc add-t-meta), :arglists (:arglists add-t-meta)}
    add))

(defn- transitions-from*
  [state-or-frontier transitions]
  (let [away-from-here? (if (set? state-or-frontier)
                          #(state-or-frontier (:from %))
                          #(= (:from %) state-or-frontier))]
    (filter away-from-here? transitions)))

(defn transitions-from
  [state-machine state]
  (transitions-from* state (:transitions state-machine)))

(defn states
  "Given a state machine, return the set of its states."
  [state-machine]
  (->> (list (:start state-machine))
    (concat (->> (:transitions state-machine) (map :to) (filter keyword?)))
    (concat (keep :from (:transitions state-machine)))
    set))

(defn events
  "Given a state machine, return the set of events used by the state machine.
  Normally, this is a set of keywords, but if the state machine has any
  trasitions that are custom triggered (i.e. have event recognition functions
  instead of event keywords), then this set will include its recognition fns."
  [state-machine]
  (set (keep :on (:transitions state-machine))))

(defn unroll-machine
  "Transform a state machine from the user-facing representation constructed by
  functions in the SM namespace to an equivalent one that's optimized for
  looking up the next state & update action based on the current state & event
  just fired."
  [state-machine]
  (let [sm state-machine
        all-states (states sm)]
    (into {} (for [state all-states]
               (let [outs (transitions-from sm state)
                     custom-trigger? (comp fn? :on)
                     custom-outs (filter custom-trigger? outs)
                     static-outs (remove custom-trigger? outs)
                     with-custom (if (empty? custom-outs)
                                    {}
                                    {:cypress/event-recognizers custom-outs})]
                 [state (into with-custom
                              (for [t static-outs]
                                 [(:on t) (select-keys t [:to :update])]))])))))

(defn dispatched-transition?
  "Returns true if the state machine has at least one state whose transition is
  dispatched (see `add-dispatched-transition`)."
  [state-machine]
  (not (every? keyword? (map :to (:transitions state-machine)))))

(defn- step-to-next-states
  [frontier reached? transitions-left]
  (let [transitions-out (filter transitions-left #(frontier (:from %)))
        new-frontier (->> (transitions-from* frontier transitions-left)
                       (map :to)
                       set)
        from-old-frontier? #(contains? frontier (:from %))]
    {:frontier new-frontier
     :reached? (reduce (fn [reached? state] (assoc reached? state true))
                      reached? new-frontier)
     :transitions-left (remove from-old-frontier? transitions-left)}))

(defn- visit-connected-states
  [frontier reached? transitions-left]
  (loop [frontier frontier
         reached? reached?
         transitions-left transitions-left]
    (cond
      (empty? transitions-left) reached?
      (empty? (transitions-from* frontier transitions-left)) reached?
      :otherwise
      (let [next-step (step-to-next-states frontier reached? transitions-left)]
        (recur (:frontier next-step)
               (:reached? next-step)
               (:transitions-left next-step))))))

(defn connected?
  [state-machine]
  (let [start (:start state-machine)
        no-states-reached (zipmap (states state-machine) (repeat false))
        state-reached? (visit-connected-states
                         #{start}
                         (assoc no-states-reached start true)
                         (:transitions state-machine))]
    (every? true? (vals state-reached?))))

(defn unique-transitions?
  [state-machine]
  (let [transitions (:transitions state-machine)]
    (= (count transitions)
       (count (distinct (map #(select-keys % [:from :on]) transitions))))))

(defn no-skipped-transitions?
  [state-machine]
  (if-not (contains? (events state-machine) :skip)
    true
    (let [by-state (unroll-machine state-machine)]
      ;; ensure that every state's transitions either don't trigger on :skip, or
      ;; have only the :skip transition and no others
      (every? #(or (not (contains? % :skip))
                   (= 1 (count %)))
              ;; this turns the unrolled state machine into a sequence of sets,
              ;; one for each state, where each state's set has all the events
              ;; that trigger transitions out of that state
              (->> (vals by-state)
                (map keys)
                (map set))))))

(defn has-start-state?
  [state-machine]
  (boolean (:start state-machine)))

(defn valid?
  "Validate a state machine description to ensure we can use it as a UI state
  machine in a cypress app. Specifically, that means:
    * it has a start state;
    * every state has a transition into it (with the possible exception of the
      start state).
    * the state graph is connected, meaning every state is reachable from the
      start state;
    * no state has multiple transitions out of the state for the same event;
    * if a state has a :skip transition out of it, it has no other transitions.
  The second and third conditions may seem to be equivalent, but you can have
  transitions into every state in a disconnected graph by having a cycle of
  states that are all unreachable from the start state."
  [state-machine]
  (and (has-start-state? state-machine)
       (unique-transitions? state-machine)
       (no-skipped-transitions? state-machine)
       ;; graphs with a dispatched transition can't be checked for connectedness
       (or (dispatched-transition? state-machine)
           (connected? state-machine))))

(defn validation-error
  [state-machine]
  (cond
    (not (has-start-state? state-machine)) ::no-start
    (not (unique-transitions? state-machine)) ::duplicate-transitions
    (not (connected? state-machine)) ::disconnected
    :otherwise nil))
