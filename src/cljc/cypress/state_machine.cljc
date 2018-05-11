(ns cypress.state-machine)

(defn identity-update
  [app-state _ui-state _triggering-event]
  app-state)

(defn blank-state-machine
  [start-state]
  {:start start-state
   :transitions ()})

(defn add-transition
  "Add a new transition to the given state machine description. The
  `from` and `to` are arbitrary keywords identifying states in the
  UI state machine, which you get to define; `on` is a keyword for the DOM event
  that should trigger the transition (e.g. :mousedown), and the optional
  `update-state` argument is a function to call when this transition occurs that
  will update the application state. This update function is given three arguments:
    * the current application state;
    * the new UI state (`to`) that the UI state machine has transitioned to;
    * the DOM event object that triggered the transition.
  If `update-state` is omitted, then it defaults to the identity function."
  ([state-machine transition-seq]
   (apply add-transition state-machine transition-seq))
  ([state-machine from to on]
   (add-transition state-machine from to on identity-update))
  ([state-machine from to on update-state]
   (when-not (every? keyword? [from to on])
     (throw (IllegalArgumentException.
              (str "the 'from' and 'to' states and the type of event to trigger"
                   " 'on' must all be keywords"))))
   (update state-machine :transitions conj {:from from, :to to, :on on
                                            :update update-state})))

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
    (concat (keep :to (:transitions state-machine)))
    (concat (keep :from (:transitions state-machine)))
    set))

(defn events
  "Given a state machine, return the set of events used by the state machine."
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
               [state (into {} (for [t (transitions-from sm state)]
                                 [(:on t) (select-keys t [:to :update])]))]))))

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
       (connected? state-machine)))

(defn validation-error
  [state-machine]
  (cond
    (not (has-start-state? state-machine)) ::no-start
    (not (unique-transitions? state-machine)) ::duplicate-transitions
    (not (connected? state-machine)) ::disconnected
    :otherwise nil))
