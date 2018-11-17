# Hello World with Cypress.

## Introduction

This tutorial walks you through building a trivial app with Cypress, starting from something like the figwheel template (run `lein new figwheel cypress-example` to get a copy).
The application will count the number of mouse down and up events.

The full source of this application is available [in the examples folder][example].
You can either open up that source alongside this and use this as a guide to that source, or if you find you learn better by doing, create a generic lein application and follow the instructions you'll encounter as you read to build the example yourseld..

[example]: ../examples/hello-world

## Getting Started

For those of you doing it all yourself, add these dependencies to the project.clj:
```clj
[cypress "0.3.0"]
[org.omcljs/om "1.0.0-beta1"]
```

FIXME: actually, maybe make a template for this? they should have figwheel & cljsbuild set up, but going over that part is outside the scope of this tutorial.

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
  (cypress/init! js/document down-then-up !state {:logging true}))

(main)
```

When we save our file, load up it up in the browser, and open the console, then we can observe messages from Cypress about the state transitions in our app.
We now have a technically correct but useless Cypress app!
It's almost like we're mathematicians over here.

## Making It Do Things

It's uninteresting for two reasons:
  1) we don't reflect the app's state in the DOM in any way, which is you know, the whole point of the web;
  2) in our state machine transitions, we didn't define any handlers!

When we omit a handler, Cypress supplies the identity handler as a default; it just returns the same app state it's given.

### Render the State

Showing the state in the DOM is easily solved with Om.
Let's go back up to the top of the file and add Om to the namespace form:
```clj
(ns tiny-example
  (:require [cypress.core :as cyp]
              [cypress.state-machine :as sm]
              [om.core :as om]
              [om.dom :as dom]))
```

Bouncing back down to the `main` function at the bottom, add a simple Om app that renders the click counts.
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
```

### Change the State on Events

Now we're ready to define some handlers to increment the state's counts.
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

Then we can add them to our state machine by passing them to `cypress.state-machine/add-transition`:

```clj
(def click-unclick
  (-> (sm/blank-state-machine :idle)
    (sm/add-transition :idle :active :mouse-down inc-downs)
    (sm/add-transition :active :idle :mouse-up inc-ups)))
```

After saving the changes and moving back to the browser, we can see the counts go up when we click.
Congratulations! You now have your first complete Cypress application.
