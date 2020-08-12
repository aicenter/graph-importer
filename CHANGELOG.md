# 3.0.0

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