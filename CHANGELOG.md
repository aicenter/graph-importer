# 5.0.0
## Fixed
- Handling of parallel edge discard fixed (integer division, speed unit support)

## Added
- Reporting of discarded parallel edges
- All edge shape location checked in assert mode
- generic `getParam` method added to `InternalEdgeBuilder`
- new containsParam method for InternalEdge and InternalEdgeBuilder
- serialized graph filename logging

## Changed
- AIC maven repo is now accessed from https


# 4.0.0

## Fixed
- fixed one way handeling

## Added
- custom parameters for nodes
- all edge parameters from the geojson file are now part of the edge custom parameters map and can be read in client 
code
- logging of discarded duplicate edges (AgentPolis does not support multigraph, duplicate edges were discarded silently
in the past)

## Changed
- geographtools related unit changes. Edge length now has a centimeter precision, max speed is now in km/h.
- removing of the minor components turned of as it is done in the preprocessing step
- start of the semantic versioning

## Deprecated
- osm reader (whole package)

## Removed
- edge osmid parameter

# 3.0.0 
version 3.0.0 was mistakenly skipped
