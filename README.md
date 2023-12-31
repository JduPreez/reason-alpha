# reason-alpha

A trade management tool, with an emphasis on risk management.

## Installation

Download from http://example.com/FIXME.

## Tests

Run tests with:
- Default, non-integration tests: `lein test`.
- Integration tests only: `lein test :integration`.
- All tests, including integration tests: `lein test :all`.

### Private vars & functions
Are prefixed with a dash "-".
    
    (def ^:private -some-var)
    
    (defn- -do-something [...]
        ...)

## Concepts

### Entities

* An entity is a type of thing.

* In the UI when working with the data collection of a type of entity, the plural name is used.

* In the rest API the plural name of the type of entity is used.

* When working with a single entity, all its keys are namespaced with the singular type of the entity.


## License

Copyright © 2021 Jacques du Preez

This program and the accompanying materials are made available under the
terms of the Business Source License 1.1 which is available at
https://github.com/JduPreez/reason-alpha/blob/master/LICENSE.

