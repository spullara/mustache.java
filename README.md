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

Alternate Formats
-----------------

Since YAML is a reasonably complex format that not every language has good
tools for working with, we are also providing JSON versions of the specs.
These should be kept identical to the YAML specifications, but if you find the
need to regenerate them, they can be trivially rebuilt by invoking `rake
build`.

It is also worth noting that some specifications (notably, the lambda module)
rely on YAML "tags" to denote special types of data (e.g. source code).  Since
JSON offers no way to denote this, a special key ("`__tag__`") is injected with
the name of the tag as its value.  See `TESTING.md` for more information about
handling tagged data.
