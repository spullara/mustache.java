This repository acts as an attempt at specifying both standard- and edge-case
behavior for libraries parsing Mustache (or a superset thereof).  Early
development will focus on describing idealized output, with later work being
done to normalize expectations.  *It is not expected that any implementation
support any draft prior to v1.0.0.*

Optional Modules
----------------

Specification files beginning with a tilde (~) describe optional modules.  At
present, the only module being described as optional is regarding support for
lambdas.  As a guideline, a module may be a candidate for optionality when:

  * It does not affect the core syntax of the language.
  * It does not significantly affect the output of rendered templates.
  * It concerns implementation language features or data types that are not
    common to or core in every targeted language.
  * The lack of support by an implementation does not diminish the usage of
    Mustache in the target language.

As an example, the lambda module is primarily concerned with the handling of a
particular data type (code).  This is a type of data that may be difficult to
support in some languages, and users of those languages will not see the lack
as an 'inconsistency' between implementations.

Support for specific pragmas or syntax extensions, however, are best managed
outside this core specification, as adjunct specifications.

Implementors are strongly encouraged to support any and all modules they are
reasonably capable of supporting.
