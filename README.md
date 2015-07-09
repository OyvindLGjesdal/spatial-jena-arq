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
?place **seco-spatial:withinPolygon** `('polygon' ['delimiter_point'] ['delimiter_longlat'] [lat_long])`

 `polygon`: string containing the individual points of a polygon in format `'long_1 lat_1, long_2 lat_2, ...'` (default)  
 `delimiter_point`: delimiter used between the individual points of a polygon, default: `', '`  
 `delimiter_longlat`: delimiter used between longitude and latitude coordinates of a point in polygon, default: `' '`  
 `lat_long`: is latitude before longitude in a point in polygon, default: `false`  
  
### Examples:
 
Simple:           ?place **seco-spatial:withinPolygon** `'24.9422920760 59.9224888308, 25.1585533582 59.9424526638, 25.1687391225 60.0270350324, 24.9422920760 59.9224888308'`

WKT:              ?place **seco-spatial:withinPolygon** `'POLYGON ((24.9422920760 59.9224888308, 25.1585533582 59.9424526638, 25.1687391225 60.0270350324, 24.9422920760 59.9224888308))'^^<http://www.opengis.net/ont/geosparql#wktLiteral>`

SAPO:             ?place **seco-spatial:withinPolygon** `('24.9422920760,59.9224888308 25.1585533582,59.9424526638 25.1687391225,60.0270350324 24.9422920760,59.9224888308' ' ' ',')`
   
Lat before Long:  ?place **seco-spatial:withinPolygon** `('59.9224888308 24.9422920760, 59.9424526638 25.1585533582, 60.0270350324 25.1687391225, 59.9224888308 24.9422920760' ', ' ' ' true)`
   
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