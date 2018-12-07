# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.3.1] - 2018-12-6
### Added
- Added guided documentation on getting started, using dispatch functions, and using custom events, which each accompany one of the embedded examples.
- Add the 'drag-and-drop' example.
- Change name from 'cypress' to 'plastic'

### Fixed
- Actually log the application state in the event loop when logging is enabled.

## [0.3.0] - 2018-11-10
### Added
- Support for transitions with "custom triggers": functions on events that decide when the transition will be followed.
- `compose-transitions` utility in state machine namespace.
- Replace maintainer-oriented examples namespaces with three fully functional & hackable embedded projects: a hello world example, a simple game, and a calculator.

### Changed
- Custom event channels can now be passed as a flat collection of channels rather than a map of event kind keyword to channel producing that kind of event.
- Rename the `identity-update` function in plastic.state-machine to simply `identity`.
- When debug logging enabled for event loop, log each event using the console's native logging facilities.

## [0.2.0] - 2018-06-29
### Added
- Optional logging in the event loop to help with debugging.
- Support for custom events delivered via core.async channels.

### Fixed
- Following skip transitions in the event loop.
- Throwing the proper exceptions for the runtime in the cljc state machine namespace.

## [0.1.0] - 2018-05-18
### Added
- Define UI state machines in pure clojure.
- Event loop in clojurescript to run UI state machines off of DOM events.
- Support for "dispatched transitions" that use a function of event & app state to choose target state.
- Fake "skip" event to allow more flexibility in state machine definition.

[0.3.1]: https://github.com/aperiodic/plastic/compare/0.3.0...0.3.1
[0.3.0]: https://github.com/aperiodic/plastic/compare/0.2.0...0.3.0
[0.2.0]: https://github.com/aperiodic/plastic/compare/0.1.0...0.2.0
[0.1.0]: https://github.com/aperiodic/plastic/releases/tag/0.1.0
