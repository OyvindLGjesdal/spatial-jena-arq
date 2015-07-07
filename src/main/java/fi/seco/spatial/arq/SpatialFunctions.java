package fi.seco.spatial.arq;

import com.hp.hpl.jena.sparql.pfunction.PropertyFunction;
import com.hp.hpl.jena.sparql.pfunction.PropertyFunctionFactory;
import com.hp.hpl.jena.sparql.pfunction.PropertyFunctionRegistry;

public class SpatialFunctions {
	
	static {
		init();
	}
	
    public static void init() {
		PropertyFunctionRegistry.get().put("http://www.seco.tkk.fi/spatial#withinPolygon", new PropertyFunctionFactory() {
            @Override
            public PropertyFunction create(String uri) {
                return new IsWithinPolygonPF();
            }
        });		
    }
}