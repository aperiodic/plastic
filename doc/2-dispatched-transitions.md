# Building a Click-the-Dots Game with Dispatched Transitions

The hello world example used a static state machine, meaning the same events always lead to the same transitions to the same UI states, regardless of the app state.
In real-world applications, we will often want to choose which target UI state we want to transition to based on the app state and the triggering event.
Cypress refers to these transitions as 'dispatched' transitions.
You may have already thought of an easy way to define them: when calling add-transition, give a function instead of a keyword as the 'to' argument which designates where the transition will go.

In the domain of Cypress apps, the dispatch function is often a hit test.
This guide walks you through creating an uninteresting game where you must precisely click on all the targets to win, without misclicking even once.
On each click, our dispatch function decides to progress the game if it's a hit, or end the game in failure if a miss.

### How to Follow Along

This guide is meant to be read alongside the [source code to the embedded click-game example][click-game-main] in the examples folder.
Because there's a lot of functions that we'll use without defining, you should open that source code in a new browser tab or editor pane such that you can see both that code and this guide at the same time.
This will allow you to satiate your curiousity about how those functions actually work, as well as understand how all the pieces fit together.

You should also open up a live instance of this game, either [the one hosted on GitHub][gh-click-game], or by running [`lein figwheel`][figwheel] in the `examples/click-game` directory (which will also allow you to make your own changes and see their effects shortly after saving).

[click-game-main]: ../examples/click-game/src/cljs/cypress/examples/click_game.cljs
[figwheel]: https://github.com/bhauman/lein-figwheel
[gh-click-game]: TODO

## Overview of the Game

Before we can get into the nitty-gritty details of defining the transition function, we need an understanding of how the click game will work.

The general flow of the game is simple.
We present the player with some number of targets that they must click.
Every time the player clicks on a target (a 'hit'), that target is removed.
If the removed target is the last target, the player has won and we show them a victory screen.
Any click that is not on a target (a 'miss') immediately ends the game in a loss, and we show the player a defeat screen.
On either the victory or loss screens, the player may click anywhere to begin a new game.

This description lends itself naturally to a state machine with three states: 'playing', 'won', and 'lost'.
A mouse down event in either the won or lost state leads back to 'playing' regardless of where it happens.
The tricky part is that a mouse down in the playing state could cause a transition to any state:
  * 'lost' if the click is a miss;
  * 'won' if the click is a hit and it hit the last target remaining;
  * back to 'playing' if the click is a hit and there are other targets.

It's this requirement to have the same DOM event result in a transition to different UI states depending on the app state that necessitates a dispatch function.
Let's now turn our attention to the problem of defining this dispatch function.

## Defining the Dispatch Function

Dispatch functions always take two arguments: the first is the app state at the time of the event, and the second is the triggering event.
In the case of this click game, all of the transitions will be on mouse down events, so we know that the second argument will be a DOM mouse down event in particular.

With this information, we can at least name our dispatch function and its arguments:
```clj
(defn hit-or-miss
  [app-state mouse-down]
  )
```
The app state we'll use will be a map with two keys
  * `:targets` is a list of targets the player has not yet hit;
  * `:status` is a keyword indicating the state of the game (`:playing`, `:won`, or `:lost`).

We want our transition to hinge on whether or not the user's click is any of the state's targets.
In order to determine that, we'll assume that we have two functions we can use:
  * `target-hit?`, that when given a list of targets and a click's x and y coordinates, returns a boolean indicating whether the click was in any target.
  * `event->pos`, that when given a DOM mouse down event, returns that event's position within the DOM node that serves as the game board as a vector of the x and y coordinates.

Using the above two functions, our `hit-or-miss` dispatch function can now define the boolean `hit?` that represents whether the click is a hit:
```clj
(let [[mx my] (event->pos mouse-down)
      hit? (target-hit? (:targets app-state) mx my)]
  )
```

Our dispatch function is now capable of distinguish between hits and misses, but it still needs to distinguish between a hit that wins the game from one that merely advances the game.
To do that, we'll define the boolean `one-target?` that's true when there is only one target left in the game at the time of the click:
```clj
(let [...
      ...
      one-target? (= 1 (count (:targets app-state)))]
  )
```

Now all that's left is to tie it all together and use the two booleans to return the appropriate state to transition to:
```clj
(defn hit-or-miss
  [app-state mouse-down]
  (let [[mx my] (event->pos mouse-down)
        hit? (target-hit? (:targets app-state) mx my)
        one-target? (= 1 (count (:targets app-state)))]
    (cond
      (not hit?) :lost
      (and hit? one-target?) :won
      :else-hit :playing)))
```

This is our desired dispatch function!
We now need to define a state machine for the whole game that uses this dispatch function.

## Using the Dispatch Function

As mentioned in the introduction, dispatch functions can be used in place of a UI state keyword as the `to` argument to `cypress.state-machine/add-transition`.
Assuming that `cypress.state-machine` is required as `sm`, we can define a state machine for this game:
```clj
(def target-hitting-game
  (-> (sm/blank-state-machine :playing)
    (sm/add-transition :playing hit-or-miss :mouse-down advance-and-track-status)
    (sm/add-transition :lost :playing :mouse-down new-game)
    (sm/add-transition :won :playing :mouse-down new-game)))
```
You can see how most of the game's logic is encoded in the dispatched transition using `hit-or-miss` as its dispatch function.
Indeed, the `advance-and-track-status` update function uses the return value from `hit-or-miss` to set the `:status` value of the app state map (along with removing any targets the user hit from the app state); it has no logic itself for determining a victory or defeat.
The only other transitions needed are the two that start new games from the victory & defeat screens.

## Conclusion

This contrived example is a somewhat extreme case, but I've found that most of my Cypress applications make use of dispatch functions.
Frequently, they will have a handful of primary modes of interaction, and a dispatch function will define the conditions that lead to each mode.
For example, in [Lab Maniac][lab-maniac], whenever the user clicks a dispatch function figures out whether a the click was on an unselected card, a selected card, or the 'table', and transitions to the single-card drag, selection drag, or selecting states accordingly.
Now that you've read this guide, you should understand when you'll need to use a dispatch function, and how to define one.

[lab-maniac]: http://labmaniac.com
