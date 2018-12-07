# Hello World with Plastic

## Introduction

This tutorial walks you through building a trivial app with plastic, starting from something like the figwheel template (run `lein new figwheel plastic-example` to get a copy).
The application will count the number of mouse down and up events.

The full source of this application is available [in the examples folder][example].
You can either open up that source alongside this and use this as a guide to that source, or if you find you learn better by doing, create a generic lein application and follow the instructions you'll encounter as you read to build the example yourseld..

[example]: ../examples/hello-world

### How to Follow Along

This guide is meant to be read alongside the [source code to the embedded hello-world example][hello-world-main] in the examples folder.
You should open that source code in a new browser tab or editor pane such that you can see both that code and this guide at the same time.
This will allow you to see how all the pieces fit together and jump to parts of the code covered earlier if you want to refresh your memory.

You should also open up a live instance of the example, either [the one hosted on GitHub][gh-hello-world], or by running [`lein figwheel`][figwheel] in the `examples/hello-world` directory (which will also allow you to make your own changes and see their effects shortly after saving).

[click-game-main]: ../examples/hello-world/src/cljs/plastic/examples/hello_world/main.cljs
[figwheel]: https://github.com/bhauman/lein-figwheel
[gh-click-game]: TODO

## Getting Started

Open up the main clojurescript source file, and import the plastic namespaces we'll be using:
```clj
(ns tiny-example
  (:require [plastic.core :as plastic]
            [plastic.state-machine :as state]))
```

Then we can define the application's starting state.
It's pretty simple because all we care up is the two numbers, which we'll call `:ups` and `:downs`.
```clj
(def !state (atom {:ups 0, :downs 0}))
```

### State Machine Definition

Plastic is all about state machines, so we should get right to writing the state machine to describe this application's behavior.
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

Now that we have a valid state machine, we can start up plastic's event loop.
Let's define a function called `main` to kick everything off, then call it:

```clj
(defn main
  []
  (plastic/init! js/document down-then-up !state {:logging true}))

(main)
```

When we save our file, load up it up in the browser, and open the console, then we can observe messages from plastic about the state transitions in our app.
We now have a technically correct but useless plastic app!
It's almost like we're mathematicians over here.

## Making It Do Things

It's uninteresting for two reasons:
  1) we don't reflect the app's state in the DOM in any way, which is you know, the whole point of the web;
  2) in our state machine transitions, we didn't define any handlers!

When we omit a handler, plastic supplies the identity handler as a default; it just returns the same app state it's given.

### Render the State

Showing the state in the DOM is easily solved with Om.
Let's go back up to the top of the file and add Om to the namespace form:
```clj
(ns tiny-example
  (:require [plastic.core :as plastic]
            [plastic.state-machine :as sm]
            [om.core :as om]
            [om.dom :as dom]))
```

Bouncing back down to the `main` function at the bottom, add a simple Om app that renders the click counts.
```clj
(defn main
  []
  (plastic/init! js/document down-then-up !state {:logging true})
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
Event handlers for plastic must always take three arguments:
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

Then we can add them to our state machine by passing them to `plastic.state-machine/add-transition`:

```clj
(def click-unclick
  (-> (sm/blank-state-machine :idle)
    (sm/add-transition :idle :active :mouse-down inc-downs)
    (sm/add-transition :active :idle :mouse-up inc-ups)))
```

After saving the changes and moving back to the browser, we can see the counts go up when we click.
Congratulations! You now have your first complete plastic application.
