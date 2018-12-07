# Building a Calculator Using Custom Events

Plastic makes it easy to use DOM events in the UI state machines, not only to trigger transitions but also to influence logic in update and dispatch functions.
However, there may come the occasion when you want to supply your own events to plastic, in addition to or instead of the DOM events that it captures for you.
For example, these could be DOM events from a node that is not a descendent of the application root node you pass to `plastic.core/init`, contents of a response to an asynchronous HTTP request or message received on a websocket, or information passed to a callback function you provided to an external library.

Fortunately, plastic provides several facilities for injecting custom events into its event loop and using those events to trigger transitions and influence logic in the UI state machine you define for your application.
The structure of your custom events is completely up to you: they can be any kind of value you choose, from primitives to maps, vectors, or even JS objects.

This guide walks you through creating a four-operation calculator, where the logic is driven by custom events emitted on button presses.
The use of custom events lets us define all the buttons as static HTML, and to exist outside of both the plastic and Om root node in the DOM tree.
Om is used only to render the calculator's display, simplifying that side of things considerably.
Using custom events also allows the triggers for the state machine's transitions to be expressed in the domain of the calculator (e.g. digits, operations, equals) rather than that of the DOM.

## How to Follow Along

This guide is meant to be read alongside the source code to the embedded calculator example in the examples folder of this repo.
There are two important namespaces in the example: [the calculator's core namespace][calc-core] which defines the logic of the calculator, and the [calculator's main namespace][calc-main] which defines how to tie the calculator's logic to the DOM.
We will start in the main namespace to see how the custom events are defined and passed to plastic, then move to the core namespace to see how they are used in the state machine definition.
Because there are many definitions in those namespaces that we will use but not directly cover, you should open the source for both namespaces in such a way that you can see one of the namespaces and this guide at the same time.
This will allow you to satiate your curiousity about those other definitions, as well as understand how all the pieces fit together.

You may also wish to open up a live instance of this game, either [the one hosted on GitHub][gh-calculator], or by running [`lein figwheel`][figwheel] in the `examples/calculator` directory (which will also allow you to make your own changes and see their effects shortly after saving).

[calc-core]: ../examples/calculator/src/cljc/plastic/examples/calculator/core.cljc
[calc-main]: ../examples/calculator/src/cljs/plastic/examples/calculator/main.cljs
[figwheel]: https://github.com/bhauman/lein-figwheel
[gh-calculator]: TODO

## Passing Custom Events to Plastic

Custom events are passed to plastic by providing `plastic.core/init!` a map of keywords to core.async channels.
Each keyword is an event 'category', which is how you'll refer to all the events produced by the corresponding channel in the state machine definition.
plastic will merge the provided channels with its internal channel producing events on the root DOM node you pass it, to advance the state machine when an event is produced from any of the channels.

In our calculator example, passing the custom event channels happens in the `start!` function:
```clj
(defn start!
  []
  (let [custom-event-channels (add-event-emitters!)]
    (plastict! (root-node) custom-event-channels calculator !state
               {:logging true}))
```
just as I have described above.

However, this is not particularly illuminating; clearly more interesting things are happening in the `add-event-emitters!` function.
This function finds the DOM nodes of all the buttons, and attaches mouse down handlers to each one which emit a custom event on the appropriate core.async channel when fired.
Let's take a closer look.

The first thing it does is define the channels for each category of event:
```clj
(defn add-event-emitters!
  []
  (let [digit-channel (async/chan 2)
        operation-channel (async/chan 2)
        c-channel (async/chan 2)
        ac-channel (async/chan 2)
        =-channel (async/chan 2)]
```
Then it links up all the digit buttons to the digit channel:
```clj
    (doseq [i (range 0 10)]
      (let [event {:value i}]
        (add-emitter! (digit-node i) "mousedown" event digit-channel)))
```

The `add-emitter!` function is what does the work of attaching a handler for each node that simply sticks the `event` value into the supplied channel on each mouse down event.
This means that the map `{:value i}` is one of our custom events.

You can see on the next few lines that the custom events for the mathematical operation buttons are defined similarly: each one is of the form `{:operation op}`, where `op` is one of `:add`, `:subtract`, `:multiply`, or `:divide`.
Finally, the last three calls to `add-emitter!` contain the custom event definitions for the three remaining buttons -- clear, all clear, and equals -- directly in the call.
In all three cases, they're simply keywords, as no additional information besides the event category is needed.

Now that we understand how to pass custom event sources to plastic, how the calculator example does it, and what the calculator's custom events are, we can move on to learn how to use custom events in a plastic state machine.

## Using the Custom Events in the State Machine

Having defined the category keyword for each of our custom events when passing them to plastic, we can use that keyword to reference the events in the state machine definition, in the same way we normally use plastic's DOM event keywords.

Moving to the [calculator example's core namespace][calc-core], we can find the definition of the calculator state machine at the bottom.
Here's an excerpt showing all the transitions out of the 1st-arg state (which is the state for when the user enters a number before choosing an operation)
```clj
(sm/add-transition :1st-arg :1st-arg   :digit enter-digit*)
(sm/add-transition :1st-arg :1st-arg   :clear-entry clear-entry*)
(sm/add-transition :1st-arg :choose-op :operation choose-operation*)
(sm/add-transition :1st-arg :identity  :equals track-ui-state)
(sm/add-transition :1st-arg :zero      :clear-all clear-all*)
```
We can see each category of custom event that we passed to plastic in the last section used in a transition from this state.

As stated previously, the custom event values themselves can be whatever you want them to be.
plastic passes them unmodified from the originating channel to the update handler attached to the transition that the event triggered (if any).
To see an example of this, let's move up in the namespace to the `enter-digit*` handler that's called when another digit is pressed while in the 1st-arg state:
```clj
(defn enter-digit*
  [app-state ui-state event]
  (-> (enter-digit app-state (:value event))
    (track-ui-state ui-state)))
```
If you'll recall from the previous section, the digit events are maps with the numeric value under the `:value` key; this handler extracts it from the event and passes it along to the `enter-digit` function, which is part of the core calculator logic that knows nothing whatsoever about plastic.

## Conclusion

Custom events allow you to hook into parts of the DOM not directly rendered by your app, incorporate information received over the network, and even introduce a level of abstraction between the DOM and the description of your application's behavior in its state machine (if it seems prudent to do so).
In this guide, we've covered the mechanics of how to define the event categories and pass the sources to plastic, as well as shown examples of their usage in both the state machine definition and its handlers.
You should now be well-equipped to use custom events when they seem useful.
