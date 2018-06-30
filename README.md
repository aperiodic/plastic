# cypress

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



## Tutorial

## Lein Template (Figwheel Included!)

## License

Copyright © 2018 DLP

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
