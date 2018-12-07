# Cypress

Cypress is a UI logic library for clojurescript.
It allows you to define your web application's behavior in a state machine, with transitions triggered by DOM events.
When the transitions fire, they call handler functions you define that use the event to update the application's state.

This lets you describe your application's behavior in pure functions that have a level of separation from the details of DOM interaction.
It works best in concert with React to translate the application state back into the DOM using a different set of pure functions.
With both of these, you end up with something not too dissimilar from Elm.

Currently, the most sophisticated production use of Cypress is [Lab Maniac][lab-maniac], a Magic the Gathering deckbuilding simulator.
It uses Cypress to provide a richly interactive drag & drop interface with subtle affordances.
Compared to the [previous UI framework][zelkova], the use of a state machine abstraction made the logic of those affordances easier to follow, and the runtime cost of updating state dropped from 8.1 ms to 0.71 ms&mdash;a 91% reduction&mdash;for the average mouse move handler.

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

## Documentation & Examples

The [doc folder][doc] contains a collection of tutorials that walk you through building simple applications with Cypress to demonstrate its features.
Each tutorial corresponds to one of the [example applications in the examples folder][examples], so it's best to open up the tutorial and the corresponding example source side by side.

Each example is an embedded application.
You can move to an example's folder and run `lein figwheel` to get a development server, then open up the source and start hacking on it.

[doc]: ./doc
[examples]: ./examples

## License

Copyright © 2018 DLP

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
