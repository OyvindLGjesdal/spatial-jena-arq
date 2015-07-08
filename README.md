Extra spatial functions for Jena ARQ
======

Includes a property function for Jena ARQ for determining if a geo point (WGS84) is inside a polygon.

Requirements:
 * Jena Spatial module, see [http://jena.apache.org/documentation/query/spatial-query.html][1]
 * JTS Topology Suite, see [http://tsusiatsoftware.net/jts/main.html][2]

The prefix *seco-spatial* is `<http://www.seco.tkk.fi/spatial#>`.
  
Property function seco-spatial:withinPolygon
-----
  
This property function requires the subject to be a resource with spatial-indexed geo point  
(using the properties from the namespace *http://www.w3.org/2003/01/geo/wgs84_pos#*) or unbound,  
and the object to be a string or list representing a polygon. If the polygon is not closed,  
i.e. it's last point is not the same as the first point, it's closed automatically.

### Usage:

?place **seco-spatial:withinPolygon** `'polygon'`  
OR  
?place **seco-spatial:withinPolygon** `('polygon' ['delimiter_point'] ['delimiter_latlong'] [long_lat])`

 `polygon`: string containing the individual points of a polygon  
 `delimiter_point`: delimiter used between the individual points of a polygon, default: ', '  
 `delimiter_latlong`: delimiter used between latitude and longitude coordinates of a point in polygon, default: ' '  
 `long_lat`: is longitude before latitude in a point in polygon (case SAPO), default: false  
  
### Examples:
 
Simple:  ?place **seco-spatial:withinPolygon** `'59.9224888308 24.9422920760, 59.9424526638 25.1585533582, 60.0270350324 25.1687391225'`

WKT:     ?place **seco-spatial:withinPolygon** `'POLYGON ((59.9224888308 24.9422920760, 59.9424526638 25.1585533582, 60.0270350324 25.1687391225))'^^<http://www.opengis.net/ont/geosparql#wktLiteral>`

SAPO:    ?place **seco-spatial:withinPolygon** `('24.9422920760,59.9224888308 25.1585533582,59.9424526638 25.1687391225,60.0270350324' ' ' ',' true)`
   
See Java class `examples.SpatialFunctionsExample` for complete SPARQL query examples.
 
Using in Fuseki
------

Package the functions into a distributable JAR file with Maven command:  
`mvn package`

Copy the generated `target/spatial-arq-1.0.0-SNAPSHOT-with-dependencies.jar` to somewhere Fuseki can see it and add it to Fuseki's classpath.

Add the following line to Fuseki's configuration file:  
`[] ja:loadClass "fi.seco.spatial.arq.SpatialFunctions" .`

Restart Fuseki, and the extra spatial functions are now usable with Fuseki services based on [spatial datasets][1].


  [1]: http://jena.apache.org/documentation/query/spatial-query.html
  [2]: http://tsusiatsoftware.net/jts/main.html