# reason-alpha

FIXME: description

## Installation

Download from http://example.com/FIXME.

## Conventions

https://guide.clojure.style

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

