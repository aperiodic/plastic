# Cypress

Cypress is a UI logic framework.
It allows you to express your web application as a state machine with transitions triggered by DOM events.
When the transitions fire, they call handler functions you define that use the event to update the application's state.

This lets you describe your application's behavior in pure functions that have a level of separation from the details of DOM interaction.
It works best in concert with React to translate the application state back into the DOM using a different set of pure functions.
With both of these, you end up with something not too dissimilar from Elm.

Currently, the most sophisticated production use of Cypress is [Lab Maniac][lab-maniac], a Magic the Gathering deckbuilding simulator.
It uses Cypress to provide a richly interactive drag & drop interface with subtle affordances.
Compared to the [previous UI framework][zelkova], the use of a state machine abstraction made the logic of those affordances easier to follow, and the runtime cost of state updating dropped from 8.1 ms to 0.71 ms&mdash;a 91% reduction&mdash;for the average mouse move handler.

The source code for Lab Maniac is [available on GitHub][lab-maniac-source].

[lab-maniac]: http://labmaniac.com
[lab-maniac-source]: https://github.com/aperiodic/arcane-lab

## Quick Start

If you want to get going in the shortest possible time, use the leiningen template:
```clj
lein new cypress hello-world-cypress
```
It contains a trivial Cypress app that's rendered with [Om][om] and comes with a [figwheel][figwheel] development workflow.

[om]: https://github.com
[figwheel]: https://github.com/bhauman/lein-figwheel

Open up the main file at `src/cljs/your-project-name/main.cljs` to see the whole app.
If you'd like a more guided introduction, read along below as it walks you through the structure of the main file

## Introduction by Example

This short guide walks you through building a trivial app with Cypress, starting from something like the figwheel template (run `lein new figwheel cypress-example` to get a copy).
The application will count the number of mouse down and up events.

Add these dependencies to the project.clj:
```clj
[cypress "1.0.0"]
[org.omcljs/om "1.0.0-beta1"]
```

Now open up the main clojurescript source file, and import the Cypress namespaces we'll be using:
```clj
(ns tiny-example
  (:require [cypress.core :as cypress]
            [cypress.state-machine :as state]))
```

Then we can define the application's starting state.
It's pretty simple because all we care up is the two numbers, which we'll call `:ups` and `:downs`.
```clj
(def !state (atom {:ups 0, :downs 0}))
```

### State Machine Definition

Cypress is all about state machines, so we should get right to writing the state machine to describe this application's behavior.
We'll call the state machine 'down-then-up.'

```clj
(def down-then-up
  (-> (state/blank-state-machine :idle)
    (state/add-transition :idle :active :mouse-down)
    (state/add-transition :active :idle :mouse-up)))
```

The first line creates a state machine with nothing but the starting state `:idle`.
The next two lines add two transitions: the first from the `:idle` state to `:active` on mouseDown events, and the other from `:active` back to `:idle` on mouseUp events.

### Event Loop Kickoff

Now that we have a valid state machine, we can start up cypress's event loop.
Let's define a function called `main` to kick everything off, then call it:

```clj
(defn main
  []
  (cypress/init! js/document down-then-up !state {:logging true})
  (om/root
   (fn [state _]
      (reify
        om/IRender
        (render [_]
          (dom/p #js {:style #js {:font-size "6em"}}
                 (str "↓:" (:downs state)
                      " ↑:" (:ups state) )))))
   !state
   {:target (.getElementById js/document "om-root")}))

(main)
```

When we save our file, load up it up in the browser, and open the console, then we can observe messages from Cypress about the state transitions in our app.
We now have a technically correct but useless Cypress app!
It's almost like we're mathematicians over here.

### Making It Do Things

It's uninteresting for two reasons:
  1) we don't reflect the app's state in the DOM in any way, which is you know, the whole point of the web;
  2) in our state machine transitions, we didn't define any handlers!
When we omit a handler, Cypress supplies the state identity handler as a default; it just returns the same app state it's given.

Showing the state in the DOM is easily solved with Om.
Let's go back up to the top of the file and add Om to the namespace form:
```
(ns tiny-example
  (:require [cypress.core :as cyp]
              [cypress.state-machine :as sm]
              [om.core :as om]
              [om.dom :as dom]))
```

And then we'll go back to the bottom

Now let us define some handlers to increment the state's counts instead.
Event handlers for Cypress must always take three arguments:
  * the current application state;
  * the keyword denoting the UI state machine's new state;
  * the DOM event that triggered the UI state machine transition associated with this handler.
Knowing this, we can define our event handlers up above the state machine:

```clj
(defn inc-downs
  [app-state _ui-state _event]
  (update app-state :downs inc))

(defn inc-ups
  [app-state _ui-state _event]
  (update app-state :ups inc))
```

Then we can add them to our state machine by passing them to `cypress.state-machine/add-tranisition`:

```clj
(def click-unclick
  (-> (sm/blank-state-machine :idle)
    (sm/add-transition :idle :active :mouse-down inc-downs)
    (sm/add-transition :active :idle :mouse-up inc-ups)))
```

After refreshing to load the new version, we can see the counts go up when we click.
Congratulations! You now have your first complete Cypress application.

## License

Copyright © 2018 DLP

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
